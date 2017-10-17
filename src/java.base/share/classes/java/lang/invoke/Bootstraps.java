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

import sun.invoke.util.Wrapper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static java.lang.invoke.MethodHandleNatives.mapLookupExceptionToError;
import static java.lang.invoke.MethodHandles.Lookup;

/**
 * Bootstrap methods for dynamically-computed constant.
 */
public final class Bootstraps {
    // implements the upcall from the JVM, MethodHandleNatives.linkDynamicConstant:
    /*non-public*/
    static Object makeConstant(MethodHandle bootstrapMethod,
                               // Callee information:
                               String name, Class<?> type,
                               // Extra arguments for BSM, if any:
                               Object info,
                               // Caller information:
                               Class<?> callerClass) {
        // BSMI.invoke handles all type checking and exception translation.
        // If type is not a reference type, the JVM is expecting a boxed
        // version, and will manage unboxing on the other side.
        return BootstrapMethodInvoker.invoke(
                type, bootstrapMethod, name, type, info, callerClass);
    }

    /**
     * X
     * @param lookup X
     * @param name X
     * @param type X
     * @param <T> X
     * @return X
     */
    public static <T> T defaultValue(Lookup lookup, String name, Class<T> type) {
        if (type.isPrimitive()) {
            return Wrapper.forPrimitiveType(type).zero(type);
        }
        else {
            return null;
        }
    }

    /**
     * X
     *
     * @param lookup X
     * @param name X
     * @param type X
     * @param <T> X
     * @return X
     */
    public static <T> T getStaticFinal(Lookup lookup, String name, Class<T> type) {
        return getStaticFinal(lookup, name, type, promote(type));
    }

    static Class<?> promote(Class<?> type) {
        if (type.isPrimitive()) {
            type = Wrapper.forPrimitiveType(type).wrapperType();
        }
        return type;
    }

    /**
     * X
     *
     * @param lookup X
     * @param name X
     * @param type X
     * @param declaringClass X
     * @param <T> X
     * @return X
     */
    public static <T> T getStaticFinal(Lookup lookup, String name, Class<T> type,
                                       Class<?> declaringClass) {
        MethodHandle mh;
        try {
            mh = lookup.findStaticGetter(declaringClass, name, type);
            MemberName member = mh.internalMemberName();
            if (!member.isFinal()) {
                throw new IncompatibleClassChangeError("not a final field: " + name);
            }
        }
        catch (ReflectiveOperationException ex) {
            throw mapLookupExceptionToError(ex);
        }

        try {
            // No need to cast because type was used to look up the MH
            @SuppressWarnings("unchecked")
            T value = (T) (Object) mh.invoke();
            return value;
        }
        catch (Error | RuntimeException e) {
            throw e;
        }
        catch (Throwable e) {
            throw new BootstrapMethodError(e);
        }
    }

    /**
     * X
     *
     * @param lookup X
     * @param name X
     * @param type X
     * @param mh X
     * @param args X
     * @param <T> X
     * @return X
     */
    public static <T> T invoke(Lookup lookup, String name, Class<T> type,
                               MethodHandle mh, Object... args) {
        if (!type.isAssignableFrom(mh.type().returnType())) {
            throw new IllegalArgumentException();
        }

        try {
            @SuppressWarnings("unchecked")
            T t = (T) mh.invokeWithArguments(args);
            return t;
        }
        catch (Error | RuntimeException e) {
            throw e;
        }
        catch (Throwable e) {
            throw new BootstrapMethodError(e);
        }
    }

    // ()T
    // (O)T
    // (A[])A
    // (ByteBuffer, int)PT, ByteOrder
    // (byte[], int)PT, ByteOrder

    /**
     * X
     *
     * @param lookup X
     * @param name X
     * @param type X
     * @param getter X
     * @param args X
     * @return X
     */
    public static VarHandle varHandle(MethodHandles.Lookup lookup, String name, Class<VarHandle> type,
                                      MethodType getter, Object... args) {
        int pc = getter.parameterCount();
        switch (pc) {
            // Static field
            case 0: {
                Class<?> variableType = getter.returnType();
                Class<?> declaringClass = (Class<?>) args[0];
                try {
                    return lookup.findStaticVarHandle(declaringClass, name, variableType);
                }
                catch (ReflectiveOperationException e) {
                    throw mapLookupExceptionToError(e);
                }
            }
            // Instance field
            case 1: {
                Class<?> variableType = getter.returnType();
                Class<?> coordType = getter.parameterType(0);

                if (!coordType.isArray()) {
                    try {
                        return lookup.findVarHandle(coordType, name, variableType);
                    }
                    catch (ReflectiveOperationException e) {
                        throw mapLookupExceptionToError(e);
                    }
                }
                else if (coordType.getComponentType() == variableType) {
                    return MethodHandles.arrayElementVarHandle(coordType);
                }
                break;
            }
            case 2: {
                if (getter.returnType().isPrimitive() && getter.parameterType(1) == int.class) {
                    Class<?> viewArrayClass = (Class<?>) args[0];
                    ByteOrder byteOrder = (ByteOrder) args[1];
                    if (getter.parameterType(0) == byte[].class) {
                        return MethodHandles.byteArrayViewVarHandle(viewArrayClass, byteOrder);
                    }
                    else if (getter.parameterType(0) == ByteBuffer.class) {
                        return MethodHandles.byteBufferViewVarHandle(viewArrayClass, byteOrder);
                    }
                }
                break;
            }
            default:
                // Fall through
        }
        throw new IllegalArgumentException();
    }
}
