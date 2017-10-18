/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.invoke;

import java.util.stream.Stream;

/**
 * Classes for representing entries in the constant pool, which can be
 * intrinsified via methods in {@link Intrinsics}.
 */
public class Constables {
    static final ClassRef CLASS_CONDY = ClassRef.of("java.lang.invoke.Bootstraps");

    static final MethodHandleRef BSM_GET_STATIC_FINAL_SELF
            = MethodHandleRef.ofCondyBootstrap(CLASS_CONDY, "getStaticFinal", ClassRef.CR_Object);
    static final MethodHandleRef BSM_GET_STATIC_FINAL_DECL
            = MethodHandleRef.ofCondyBootstrap(CLASS_CONDY, "getStaticFinal", ClassRef.CR_Object, ClassRef.CR_Class);
    static final MethodHandleRef BSM_DEFAULT_VALUE
            = MethodHandleRef.ofCondyBootstrap(CLASS_CONDY, "defaultValue", ClassRef.CR_Object);
    static final MethodHandleRef BSM_VARHANDLE
            = MethodHandleRef.ofCondyBootstrap(CLASS_CONDY, "varHandle", ClassRef.CR_VarHandle, ClassRef.CR_MethodType, ClassRef.CR_Object.array());

    static final ConstantRef<?> NULL = ConstantRef.ofNull();

    /**
     * Resolve a {@link ConstantRef} relative to the provided lookup.  The {@link ConstantRef}
     * must be one of the following types: {@link String}, {@link Integer},
     * {@link Long}, {@link Float}, {@link Double}, {@link ClassRef},
     * {@link MethodTypeRef}, {@link MethodHandleRef}, or {@link DynamicConstantRef}.
     *
     * @param c The {@link ConstantRef}
     * @param lookup The lookup object to use
     * @param <T> The type of the object described by the {@link ConstantRef}
     * @return The resolved object
     * @throws ReflectiveOperationException If there is an error resolving the
     * constable
     */
    @SuppressWarnings("unchecked")
    static<T> T resolve(ConstantRef<T> c, MethodHandles.Lookup lookup) throws ReflectiveOperationException {
        if (c instanceof String
            || (c instanceof Integer)
            || (c instanceof Long)
            || (c instanceof Float)
            || (c instanceof Double))
            return (T) c;
        if (c instanceof ClassRef) {
            return (T) ((ClassRef) c).resolve(lookup);
        }
        else if (c instanceof MethodTypeRef) {
            return (T) ((MethodTypeRef) c).resolve(lookup);
        }
        else if (c instanceof MethodHandleRef) {
            return (T) ((MethodHandleRef) c).resolve(lookup);
        }
        else if (c instanceof DynamicConstantRef) {
            return (T) ((DynamicConstantRef) c).resolve(lookup);
        }
        else
            throw new IllegalArgumentException(c.getClass().getName());
    }

    static Object[] resolveArgs(MethodHandles.Lookup lookup, ConstantRef<?>[] args) {
        return Stream.of(args)
                     .map(arg -> {
                         try {
                             return resolve(arg, lookup);
                         }
                         catch (ReflectiveOperationException e) {
                             throw new RuntimeException(e);
                         }
                     })
                     .toArray();
    }

    /**
     * Returns a {@link ConstantRef}, if the argument is not a {@link ClassRef}
     * then the argument is returned unchanged. If the argument is a {@link ClassRef}
     * and it is a primitive {@link ClassRef}, then a fresh {@link DynamicConstantRef}
     * corresponding to it is created and returned.
     *
     * @param <T> The type of the object described by the {@link ConstantRef}
     * @param ref the given {@link ConstantRef}
     * @return the reduced {@link ConstantRef}
     */
    public static<T> ConstantRef<T> reduce(ConstantRef<T> ref) {
        if (ref instanceof ClassRef) {
            ClassRef cr = (ClassRef) ref;
            if (cr.isPrimitive()) {
                // Return a dynamic constant whose value is obtained by getting
                // static final TYPE field on the boxed class
                return DynamicConstantRef.of(BootstrapSpecifier.of(BSM_GET_STATIC_FINAL_DECL, cr.promote()),
                                             "TYPE", ClassRef.CR_Class);
            }
        }
        return ref;
    }

    /**
     * Convert a class literal to a descriptor string
     * @param clazz the class literal
     * @return the descriptor string
     */
    static String classToDescriptor(Class<?> clazz) {
        return MethodType.methodType(clazz).toMethodDescriptorString().substring(2);
    }
}