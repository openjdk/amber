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

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.IntStream;

/**
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Intrinsics {
     /** Registry map of available Intrinsic Processors */
    static final Map<EntryKey, IntrinsicProcessorFactory> REGISTRY = new HashMap<>();
    /** Lookup for resolving ConstantDesc */
    static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    static {
        ServiceLoader<IntrinsicProcessorFactory> serviceLoader =
                ServiceLoader.load(IntrinsicProcessorFactory.class);
        for (IntrinsicProcessorFactory ip : serviceLoader) {
            ip.register();
        }
    }

    static class EntryKey {
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

    static void register(IntrinsicProcessorFactory factory,
                         Class<?> owner,
                         String methodName,
                         Class<?> returnType,
                         Class<?>... argTypes) {
        EntryKey key = new EntryKey(owner.describeConstable().get(),
                                    methodName,
                                    MethodTypeDesc.of(returnType.describeConstable().get(),
                                                      describeConstables(argTypes)));
        if (REGISTRY.put(key, factory) != null) {
            assert false : "multiple entry: " + owner + methodName;
        }
    }

    static ClassDesc[] describeConstables(Class<?>[] types) {
        int length = types.length;
        ClassDesc[] classDescs = new ClassDesc[length];
        for (int i = 0; i < length; i++) {
            classDescs[i] = types[i].describeConstable().get();
        }
        return classDescs;
    }

    static Class<?> getClass(ClassDesc classDesc) {
        try {
            return (Class<?>)classDesc.resolveConstantDesc(LOOKUP);
        } catch (ReflectiveOperationException ex) {
            // Fall thru
        }
        return null;
    }

    static Class<?>[] getClasses(ClassDesc[] classDescs) {
        int length = classDescs.length;
        Class<?>[] classes = new Class<?>[length];

        for (int i = 0; i < length; i++) {
            classes[i] = getClass(classDescs[i]);
        }

        return classes;
    }

    static Object getConstant(ClassDesc classDesc, ConstantDesc constantDesc) {
        try {
            Object constant = constantDesc.resolveConstantDesc(LOOKUP);
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

    static Object[] getConstants(ClassDesc[] classDescs,
                                 ConstantDesc[] constantDescs) {
        int length = constantDescs.length;
        Object[] constants = new Object[length];

        for (int i = 0; i < length; i++) {
            constants[i] = getConstant(classDescs[i], constantDescs[i]);
        }

        return constants;
    }

     static boolean isAllConstants(ConstantDesc[] constantDescs) {
        int length = constantDescs.length;

        for (int i = 0; i < length; i++) {
            if (constantDescs[i] == null) {
                return false;
            }
        }

        return true;
    }

    static boolean isArrayVarArg(ClassDesc[] argClassDescs, int i) {
        return i + 1 == argClassDescs.length && argClassDescs[i].isArray();
    }

    static int[] dropArg(int n, int k) {
        return IntStream.range(0, n)
                        .filter(i -> i != k)
                        .toArray();
    }

    static boolean checkRegex(IntrinsicContext intrinsicContext,
                              ConstantDesc[] constantArgs,
                              int arg) {
        if (constantArgs[arg] != null) {
            try {
                String regex = (String)constantArgs[arg];
                Pattern.compile(regex);
            } catch (PatternSyntaxException ex) {
                intrinsicContext.warning("Syntax error in regular expression: " +
                        ex.getMessage(), arg, ex.getIndex());
                return false;
            }
        }

        return true;
    }

    static ConstantDesc invoke(Class<?> owner,
                               String methodName,
                               boolean isStatic,
                               ClassDesc returnTypeDesc,
                               ClassDesc[] argClassDescs,
                               ConstantDesc[] constantArgs)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?>[] argTypes = getClasses(argClassDescs);
        Object[] constants = getConstants(argClassDescs, constantArgs);
        Object result;

        if (isStatic) {
            Method method = owner.getMethod(methodName, argTypes);
            result = method.invoke(owner, constants);
        } else {
            argTypes = Arrays.copyOfRange(argTypes, 1, argTypes.length);
            Method method = owner.getMethod(methodName, argTypes);
            Object receiver = constants[0];
            constants = Arrays.copyOfRange(constants, 1, constants.length);
            result = method.invoke(receiver, constants);
        }

        if (result == null) {
            return ConstantDescs.NULL;
        } else if (result instanceof Boolean) {
            return (Boolean)result ? 1 : 0;
        } else if (result instanceof Byte) {
            return ((Byte)result).intValue();
        } else if (result instanceof Short) {
            return ((Short)result).intValue();
        } else if (result instanceof Character) {
            return (int)(Character)result;
        } else if (result instanceof ConstantDesc) {
            return (ConstantDesc)result;
        }

        throw new RuntimeException("Unknown ConstantDesc");
    }

    /**
     * <p><b>This is NOT part of any supported API.</b>
     * @param ownerDesc       method owner
     * @param methodName      method name
     * @param methodType      method type descriptor
     * @return the intrinsic processor or null if not found
     */
    static public IntrinsicProcessorFactory getProcessorFactory(ClassDesc ownerDesc,
                                                  String methodName,
                                                  MethodTypeDesc methodType) {
        EntryKey key = new EntryKey(ownerDesc, methodName, methodType);

        return REGISTRY.get(key);
    }
 }
