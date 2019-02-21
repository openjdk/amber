/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.tools.javac.intrinsics;

import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Options;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.stream.IntStream;

/**
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Intrinsics {
    private static final Context.Key<Intrinsics> intrinsicsKey = new Context.Key<>();

     /** Registry map of available Intrinsic Processors */
    final Map<EntryKey, IntrinsicProcessor> registry = new HashMap<>();
    /** Lookup for resolving ConstantDesc */
    final MethodHandles.Lookup lookup = MethodHandles.lookup();

    public static Intrinsics instance(Context context) {
        Intrinsics instance = context.get(intrinsicsKey);
        if (instance == null)
            instance = new Intrinsics(context);
        return instance;
    }

    protected Intrinsics(Context context) {
        context.put(intrinsicsKey, this);
        Options options = Options.instance(context);
        boolean disableIntrinsics = options.isSet("disableIntrinsics");
        if (!disableIntrinsics) {
            ServiceLoader<IntrinsicProcessor> serviceLoader =
                    ServiceLoader.load(IntrinsicProcessor.class);
            for (IntrinsicProcessor ip : serviceLoader) {
                ip.register(this);
            }
        }
    }

    ClassDesc[] describeConstables(Class<?>[] types) {
        int length = types.length;
        ClassDesc[] classDescs = new ClassDesc[length];
        for (int i = 0; i < length; i++) {
            classDescs[i] = types[i].describeConstable().get();
        }
        return classDescs;
    }

    class EntryKey {
        final ClassDesc owner;
        final String methodName;
        final MethodTypeDesc methodType;

        EntryKey(ClassDesc owner,
                 String methodName,
                 MethodTypeDesc methodType) {
            this.owner = owner;
            this.methodName = methodName;
            this.methodType = methodType;
         }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other != null && other instanceof EntryKey) {
                EntryKey otherKey = (EntryKey)other;
                return owner.equals(otherKey.owner) &&
                       methodName.equals(otherKey.methodName) &&
                       methodType.equals(otherKey.methodType);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(owner, methodName, methodType);
        }
    }

    void register(IntrinsicProcessor processor,
                         Class<?> owner,
                         String methodName,
                         Class<?> returnType,
                         Class<?>... argTypes) {
        EntryKey key = new EntryKey(owner.describeConstable().get(),
                                    methodName,
                                    MethodTypeDesc.of(returnType.describeConstable().get(),
                                                      describeConstables(argTypes)));
        registry.put(key, processor);

    }

    Object getConstant(ClassDesc classDesc, ConstantDesc constantDesc) {
        try {
            Object constant = constantDesc.resolveConstantDesc(lookup);
            if (ConstantDescs.CD_boolean.equals(classDesc) ||
                    ConstantDescs.CD_Boolean.equals(classDesc)) {
                int value = ((Number)constant).intValue();
                constant = value == 0 ? Boolean.FALSE : Boolean.TRUE;
            } else if (ConstantDescs.CD_byte.equals(classDesc) ||
                    ConstantDescs.CD_Byte.equals(classDesc)) {
                int value = ((Number)constant).intValue();
                constant = (byte)value;
            } else if (ConstantDescs.CD_short.equals(classDesc) ||
                    ConstantDescs.CD_Short.equals(classDesc)) {
                int value = ((Number)constant).intValue();
                constant = (short)value;
            } else if (ConstantDescs.CD_char.equals(classDesc) ||
                    ConstantDescs.CD_Character.equals(classDesc)) {
                int value = ((Number)constant).intValue();
                constant = (char)value;
            }
            return constant;
        } catch (ReflectiveOperationException ex) {
            // Fall thru
        }
        return null;
    }

    Object[] getConstants(ClassDesc[] classDescs,
                                 ConstantDesc[] constantDescs,
                                 boolean skipReceiver) {
        int length = constantDescs.length;
        Object[] constants = skipReceiver ? new Object[length - 1] : new Object[length];
        int offset = skipReceiver ? 1 : 0;
        for (int i = offset; i < length; i++) {
            constants[i - offset] = getConstant(classDescs[i], constantDescs[i]);
        }
        return constants;
    }

    boolean isAllConstants(ConstantDesc[] constantDescs, boolean skipReceiver) {
        int length = constantDescs.length;
        for (int i = 0; i < length; i++) {
            if (constantDescs[i] == null && !(skipReceiver && i == 0)) {
                return false;
            }
        }
        return true;
    }

    boolean isArrayVarArg(ClassDesc[] argClassDescs, int i) {
        return i + 1 == argClassDescs.length && argClassDescs[i].isArray();
    }

    int[] dropArg(int n, int k) {
        return IntStream.range(0, n)
                        .filter(i -> i != k)
                        .toArray();
    }

    /**
     * @param ownerDesc       method owner
     * @param methodName      method name
     * @param methodType      method type descriptor
     * @param argClassDescs   class descriptors for each argument (includes receiver)
     * @param constantArgs    constant value for each argument (includes receiver), null means unknown
     * @return IntrinsicProcessor.Result value
     */
    public IntrinsicProcessor.Result tryIntrinsify(ClassDesc ownerDesc,
                                                          String methodName,
                                                          MethodTypeDesc methodType,
                                                          boolean isStatic,
                                                          ClassDesc[] argClassDescs,
                                                          ConstantDesc[] constantArgs) {
        EntryKey key = new EntryKey(ownerDesc, methodName, methodType);
        IntrinsicProcessor processor = registry.get(key);
        if (processor != null) {
            return processor.tryIntrinsify(
                    ownerDesc,
                    methodName,
                    methodType,
                    isStatic,
                    argClassDescs,
                    constantArgs
                    );
        }

        return new IntrinsicProcessor.Result.None();
    }
}
