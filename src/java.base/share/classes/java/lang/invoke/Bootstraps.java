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

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

import static java.lang.invoke.MethodHandleNatives.mapLookupExceptionToError;
import static java.lang.invoke.MethodHandles.Lookup;

/**
 * Bootstrap methods for dynamically computed constants.
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
     * Returns the default value for a variable of the type specified in the
     * {@code type} argument, as per JLS 4.12.5 ({@code null} for reference types,
     * zero for primitive numeric types, {@code false} for boolean.)
     *
     * @param lookup unused
     * @param name unused
     * @param type the type for which we are seeking the default value
     * @param <T> the type for which we are seeking the default value (or the
     *           corresponding box type, if the type is a primitive type)
     * @return the default value
     */
    public static <T> T defaultValue(Lookup lookup, String name, Class<T> type) {
        return (type.isPrimitive())
               ? Wrapper.forPrimitiveType(type).zero(type)
               : null;
    }

    /**
     * Returns the {@link Class} mirror for a primitive type from its type
     * descriptor.
     *
     * @param lookup unused
     * @param name the descriptor (JVMS 4.3) of the desired primitive type
     * @param type the required result type (must be {@code Class.class})
     * @return the {@link Class} mirror
     * @throws IllegalArgumentException if the name is not a descriptor for a
     *         primitive type
     */
    @SuppressWarnings("rawtypes")
    public static Class<?> primitiveClass(Lookup lookup, String name, Class<Class> type) {
        if (name == null || name.length() == 0 || name.length() > 1)
            throw new IllegalArgumentException("not primitive: " + name);
        return Wrapper.forPrimitiveType(name.charAt(0)).primitiveType();
    }

    /**
     * Returns the value of a static final field.
     *
     * @param lookup the lookup context describing the class performing the
     *               operation (normally stacked by the JVM)
     * @param name the name of the field
     * @param type the type of the field
     * @param declaringClass the class in which the field is declared
     * @param <T> the type if the field (or the corresponding box type,
     *           if the type is a primitive type)
     * @return the value of the field
     * @throws IllegalAccessError if the declaring class or the field is not
     * accessible to the class performing the operation
     * @throws IncompatibleClassChangeError if the specified field is not {@code final}
     * @throws NoSuchFieldError if the specified field does not exist
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
     * Returns the value of a static final field whose declaring class is the
     * same as the field's type.  This is a simplified form of {@link Bootstraps#getStaticFinal(Lookup, String, Class, Class)}
     * for the case where a class declares distinguished constant instances of
     * itself.
     *
     * @param lookup the lookup context describing the class performing the
     *               operation (normally stacked by the JVM)
     * @param name the name of the field
     * @param type the type of the field
     * @param <T> the type if the field (or the corresponding box type,
     *           if the type is a primitive type)
     * @return the value of the field
     * @throws IllegalAccessError if the declaring class or the field is not
     * accessible to the class performing the operation
     * @throws IncompatibleClassChangeError if the specified field is not {@code final}
     * @throws NoSuchFieldError if the specified field does not exist
     * @see Bootstraps#getStaticFinal(Lookup, String, Class, Class)
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
     * Returns the value of invoking a method handle with the provided arguments.
     *
     * @param lookup the lookup context describing the class performing the
     *               operation (normally stacked by the JVM)
     * @param name unused
     * @param type the type of the value to be returned, which must be compatible
     *             with the return type of the method handle
     * @param handle the method handle to be invoked
     * @param args the arguments to pass to the method handle, as if with
     * {@code invokeWithArguments}
     * @param <T> the type of the value to be returned (or the corresponding box
     *           type, if the type is a primitive type)
     * @return the result of invoking the method handle
     * @throws WrongMethodTypeException if the type is incompatible with the
     *         handle's return type
     * @throws Throwable TODO
     */
    public static <T> T invoke(Lookup lookup, String name, Class<T> type,
                               MethodHandle handle, Object... args) {
        if (type != handle.type().returnType()) {
            // Convert if the method handle return type and the constanr type
            // differ
            handle.asType(handle.type().changeReturnType(type));
        }
        try {
            @SuppressWarnings("unchecked")
            T t = (T) handle.invokeWithArguments(args);
            return t;
        }
        catch (Error | RuntimeException e) {
            throw e;
        }
        catch (Throwable e) {
            throw new BootstrapMethodError(e);
        }
    }

    /**
     * Finds a {@link VarHandle} for an instance field.
     *
     * @param lookup the lookup context describing the class performing the
     *               operation (normally stacked by the JVM)
     * @param name the name of the field
     * @param type unused; must be {@code Class<VarHandle>}
     * @param decl the type declaring the field
     * @param fieldType the field type
     * @return the {@code VarHandle}
     * @throws LinkageError if the {@code VarHandle} cannot be found
     * @throws NullPointerException if any unused argument is {@code null}
     */
    public static VarHandle varHandleInstanceField(MethodHandles.Lookup lookup, String name, Class<VarHandle> type,
                                                   Class<?> decl, Class<?> fieldType) {
        try {
            return lookup.findVarHandle(decl, name, fieldType);
        }
        catch (ReflectiveOperationException e) {
            throw mapLookupExceptionToError(e);
        }
    }

    /**
     * Finds a {@link VarHandle} for a static field.
     *
     * @param lookup the lookup context describing the class performing the
     *               operation (normally stacked by the JVM)
     * @param name the name of the field
     * @param type unused; must be {@code Class<VarHandle>}
     * @param decl the type declaring the field
     * @param fieldType the field type
     * @return the handle
     * @throws LinkageError if the {@code VarHandle} cannot be found
     * @throws NullPointerException if any unused argument is {@code null}
     */
    public static VarHandle varHandleStaticField(MethodHandles.Lookup lookup, String name, Class<VarHandle> type,
                                                 Class<?> decl, Class<?> fieldType) {
        try {
            return lookup.findStaticVarHandle(decl, name, fieldType);
        }
        catch (ReflectiveOperationException e) {
            throw mapLookupExceptionToError(e);
        }
    }

    /**
     * Finds a {@link VarHandle} for an array type.
     *
     * @param lookup unused; the lookup context describing the class performing
     *               the operation (normally stacked by the JVM)
     * @param name unused
     * @param type unused; must be {@code Class<VarHandle>}
     * @param arrayClass the array type
     * @return the handle
     * @throws IllegalArgumentException if arrayClass is not an array type
     * @throws NullPointerException if any unused argument is {@code null}
     */
    public static VarHandle varHandleArray(MethodHandles.Lookup lookup, String name, Class<VarHandle> type,
                                           Class<?> arrayClass) {
        return MethodHandles.arrayElementVarHandle(arrayClass);
    }

    /**
     * Return an unmodifiable list containing the specified elements.
     *
     * @param lookup unused
     * @param name unused
     * @param type unused, must be {@code Class<List<?>>}
     * @param elements the elements of the list
     * @return the list
     */
    public static List<?> list(Lookup lookup, String name, Class<List<?>> type, Object... elements) {
        return List.of(elements);
    }

    /**
     * Return an unmodifiable map containing the specified mappings.
     *
     * @param lookup unused
     * @param name unused
     * @param type unused, must be {@code Class<Map<?, ?>>}
     * @param elements the keys and values of the map, alternating
     * @return the map
     * @throws IllegalArgumentException if an odd number of elements is provided
     */
    public static Map<?, ?> map(Lookup lookup, String name, Class<Map<?, ?>> type, Object... elements) {
        if (elements.length % 2 != 0) {
            throw new IllegalArgumentException("Odd number of keys and values");
        } else {
            Map.Entry<?,?>[] entries = new Map.Entry<?, ?>[elements.length / 2];
            for (int i=0; i<elements.length / 2; i++)
                entries[i] = new AbstractMap.SimpleImmutableEntry<>(elements[2*i], elements[2*i+1]);
            return Map.ofEntries(entries);
        }
    }
}
