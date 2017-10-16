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

import sun.invoke.util.BytecodeName;
import sun.invoke.util.Wrapper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.invoke.MethodHandleNatives.mapLookupExceptionToError;
import static java.lang.invoke.MethodHandles.Lookup;

/**
 * Bootstrap methods for dynamically-computed constant.
 */
public final class DynamicConstant {
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
     * Load a primitive class from its descriptor.
     * The descriptor is passed as the name component of the dynamic constant.
     * @param lookup not used
     * @param name the descriptor of the desired primitive class
     * @param type the required result type (must be Class.class)
     * @return a primitive class
     * @throws IllegalArgumentException if no such value exists
     */
    public static Class<?> primitiveClass(Lookup lookup, String name, Class<Class<?>> type) {
        switch (name) {
            case "I": return int.class;
            case "J": return long.class;
            case "S": return short.class;
            case "B": return byte.class;
            case "C": return char.class;
            case "F": return float.class;
            case "D": return double.class;
            case "Z": return boolean.class;
            case "V": return void.class;
            default:
                throw new IllegalArgumentException(name);
        }
    }

    /**
     * Load a primitive value from a given integer value, narrowing that value
     * for an integral primitive type of {@code byte}, {@code char} or
     * {@code short}, and converting that value for a {@code boolean} type.
     * <p>
     * If the primtive type is {@code byte}, {@code char} or {@code short}
     * the integer value is narrowed in accordance with the type conversion
     * rules of jvms-2.11.4.  If the primitive class is {@code boolean} then
     * an integer value of {@code 0} represents a boolean value of {@code false}
     * and an integer value of {@code 1} represents a boolean value of
     * {@code true}, any other integer value results in an
     * {@code IllegalArgumentException}.
     *
     * @param lookup not used
     * @param name the descriptor of the primitive type
     * @param type the primitive type
     * @param v the integer value to be narrowed to a smaller integral type or
     *        converted to a boolean
     * @return the boxed result of the primitive value
     * @throws IllegalArgumentException if the type is not supported or the
     *         integer value cannot be converted to the required primitive value
     */
    public static Object primitiveValueFromInt(Lookup lookup, String name, Class<Class<?>> type, int v) {
        switch (name) {
            case "B": return (byte) v;
            case "C": return (char) v;
            case "S": return (short) v;
            case "Z": {
                if (v == 0) return false;
                if (v == 1) return false;
                throw new IllegalArgumentException();
            }
            default:
                throw new IllegalArgumentException(name);
        }
    }

    /**
     * Return the default value for a given type.
     * If the type is a primitive, it is the zero (or null or false) for that primitive.
     * If the type is a reference type, the default value is always {@code null},
     * even if the reference type is a box such as {@code Integer}.
     * @param lookup not used
     * @param name not used
     * @param type the given type
     * @param <T> the required result type
     * @return the default value for the given type
     */
    public static <T> T defaultValue(Lookup lookup, String name, Class<T> type) {
        if (type.isPrimitive()) {
            return Wrapper.forPrimitiveType(type).zero(type);
        } else {
            return null;
        }
    }

    /**
     * Load an enumeration constant given its name and type.
     * The name and type are passed as components of the dynamic constant.
     * @param lookup used to ensure access to the given type
     * @param name the name of the enum
     * @param type the enum type
     * @param <T> the required result type
     * @return an enumeration constant of the specified name and type
     * @throws IllegalArgumentException if no such value exists
     * @throws IllegalAccessError if the type is not accessible from the lookup
     */
    public static <T extends Enum<T>> T enumConstant(Lookup lookup, String name, Class<T> type) {
        try {
            lookup.accessClass(type);
        } catch (ReflectiveOperationException ex) {
            throw mapLookupExceptionToError(ex);
        }
        return Enum.valueOf(type, name);
    }

    /**
     * Load a static final constant given its defining class, name, and type.
     * The name and type are passed as components of the dynamic constant.
     * @param lookup used to ensure access to the given constant
     * @param name the name of the constant
     * @param type the type of the constant
     * @param declaringClass the class defining the constant
     * @param <T> the required result type
     * @return the value of a static final field of the specified class, name, and type
     * @throws LinkageError if no such constant exists or it cannot be accessed and initialized
     */
    public static <T> T namedConstant(Lookup lookup, String name, Class<T> type, Class<?> declaringClass) {
        try {
            lookup.accessClass(declaringClass);
        } catch (IllegalAccessException ex) {
            throw mapLookupExceptionToError(ex);
        }
        MethodHandle mh;
        try {
            mh = lookup.findStaticGetter(declaringClass, name, type);
        } catch (ReflectiveOperationException ex) {
            throw mapLookupExceptionToError(ex);
        }
        MemberName member = mh.internalMemberName();
        if (!member.isFinal()) {
            throw new IncompatibleClassChangeError("not a final field: "+name);
        }
        try {
            @SuppressWarnings("unchecked")
            T value = (T) (Object) mh.invoke();
            return value;
        } catch (Error|RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new BootstrapMethodError(e);
        }
    }

    /**
     * Load a constant by invoking a factory on its type.
     * The factory name and type are passed as components of the dynamic constant.
     * The arguments to the factory (if any) are passed as extra static arguments.
     * The named method must be defined on the given type, it must be static and
     * accessible to the lookup object, and (as befits a factory method) it must
     * return either the type or a subtype.  If there are several such methods,
     * only one may match the given arguments types.  (If there are any varargs
     * methods, they are suppressed while searching first for a match
     * of any non-varargs methods.)  Matching is determined by calling the
     * {@code asType} method of each factory method's method handle, and
     * rejecting the method if it throws {@code WrongMethodTypeException}.
     * If these rules (which are rather blunt) are insufficent to give
     * control over factory method selection, use {@code methodCall},
     * which allows explicit specification of the desired factory.
     * @param lookup used to ensure access to the given factory method
     * @param name the name of the factory method
     * @param type the type of the factory method, which is also its declaring class
     * @param args any arguments which must be passed to the factory method
     * @param <T> the required result type
     * @return the value of a static final field of the specified class, name, and type
     * @throws LinkageError if no such constant exists or it cannot be accessed and initialized
     */
    public static <T> T factoryCall(Lookup lookup, String name, Class<T> type, Object... args) {
        MethodHandle mh = selectFactoryMethod(lookup, name, type, args, false);
        if (mh == null)
            mh = selectFactoryMethod(lookup, name, type, args, true);
        if (mh == null)
            throw new IncompatibleClassChangeError("factory method not found: "+type.getName()+"."+name);
        try {
            @SuppressWarnings("unchecked")
            T value = (T) mh.invokeWithArguments(args);
            return value;
        } catch (Error|RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new BootstrapMethodError(e);
        }
    }
    // where...
    private static MethodHandle selectFactoryMethod(Lookup lookup, String name, Class<?> type, Object[] args, boolean varargsOK) {
        // FIXME: Cache a list of factory methods on a ClassValue.
        final List<MethodHandle> mhs = Arrays.stream(type.getDeclaredMethods())
                .map(m -> asFactoryMethod(m, lookup, name, type, args, varargsOK))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (mhs.size() == 1)  return mhs.get(0);
        if (mhs.isEmpty())  return null;
        throw new IncompatibleClassChangeError("ambiguous factory method selection: "+mhs);
    }
    private static MethodHandle asFactoryMethod(Method m, Lookup lookup, String name, Class<?> type, Object[] args, boolean varargsOK) {
        final int mods = m.getModifiers();
        if (!Modifier.isStatic(mods))  return null;
        if (!Modifier.isPublic(mods))  return null;  // FIXME
        if (!m.getName().equals(name))  return null;
        final Class<?> rtype = m.getReturnType();
        if (type != rtype && !type.isAssignableFrom(rtype))  return null;
        final boolean varargs = m.isVarArgs();
        if (varargs && !varargsOK)  return null;
        // Match the arguments.
        int minargs = m.getParameterCount() - (varargs ? 1 : 0), maxargs = (varargs ? Integer.MAX_VALUE : minargs);
        if (args.length < minargs || args.length > maxargs)  return null;
        final Class<?>[] ptypes = m.getParameterTypes();
        for (int i = 0; i < minargs; i++) {
            if (!matchArgument(args[i], ptypes[i]))  return null;
        }
        for (int i = minargs; i < args.length; i++) {
            if (!matchArgument(args[i], ptypes[minargs].getComponentType()))  return null;
        }
        // Got a match.  Now turn it into a method handle.
        try {
            return lookup.unreflect(m);
        } catch (IllegalAccessException ex) {
            throw MethodHandleStatics.newInternalError("cannot unreflect", ex);
        }
    }
    private static boolean matchArgument(Object arg, Class<?> ptype) {
        Class<?> atype = (arg == null ? Void.class : arg.getClass());
        return ptype.isAssignableFrom(atype);
    }
//    private static boolean testFactoryMethods() {
//        Lookup lookup = Lookup.IMPL_LOOKUP.in(Object.class);
//        List<?> l0 = factoryCall(lookup, "of", List.class);
//        assert(l0.equals(List.of()));
//        List<?> la = factoryCall(lookup, "of", List.class, "a");
//        assert(la.equals(List.of("a")));
//        List<?> lab = factoryCall(lookup, "of", List.class, "a", "b");
//        assert(lab.equals(List.of("a","b")));
//        Pattern pa = factoryCall(lookup, "compile", Pattern.class, "a");
//        assert(pa.pattern().equals("a"));
//        Duration p2d = factoryCall(lookup, "parse", Duration.class, "P2D");
//        assert(p2d.equals(Duration.parse("P2D")));
//        System.out.println(Arrays.asList(l0, la, lab, pa, p2d));
//        return true;
//    }
//    static { assert(testFactoryMethods()); }

    // compiledPattern["RE",Pattern] probably subsumes into factoryCall["compile",Pattern,"RE"]
    /**
     * Load a compiled regular expression.
     * The string is passed as the name component of the dynamic constant.
     * Note that the three commonly used characters ({@code "./;"} are illegal
     * in field names, and so cannot be used to form the regular expression.
     * In such cases, use the four-argument version of {@code compiledPattern}.
     * @param lookup unused
     * @param regex the regular expression string
     * @param type unused, must be Pattern.class
     * @return the compiled regular expression
     * @throws java.util.regex.PatternSyntaxException if the string was malformed
     */
    public static Pattern compiledPattern(Lookup lookup, String regex, Class<Pattern> type) {
        if (false) {
            // If the string begins with backslash and equals characters {@code "\\="},
            // then the string is demangled according to symbolic freedom rules.
            if (regex.startsWith("\\="))
                regex = BytecodeName.toSourceName(regex);
        }
        return Pattern.compile(regex);
    }

    /**
     * Load a compiled regular expression.
     * The string is passed as the name component of the dynamic constant.
     * @param lookup unused
     * @param name unused
     * @param type unused, must be Pattern.class
     * @param regex the regular expression string
     * @return the compiled regular expression
     * @throws java.util.regex.PatternSyntaxException if the string was malformed
     */
    public static Pattern compiledPattern(Lookup lookup, String name, Class<Pattern> type, String regex) {
        return Pattern.compile(regex);
    }

//    /**
//     * Load a {@link VarHandle} giving access to a field or elements of an
//     * array.
//     *
//     * @param lookup used to ensure access to the given owner
//     * @param name if for a field the name of the field, otherwise unused
//     * @param type unused, must be VarHandle.class
//     * @param ownerType if for a field the class declaring the field, otherwise
//     *        an array class
//     * @param variableType if for a field the type of the field, otherwise
//     *        unused
//     * @return the {@code VarHandle}
//     * @throws NoSuchFieldException if the field doesn't exist
//     * @throws IllegalAccessException if the field is not accessible
//     */
//    public static VarHandle varHandle(MethodHandles.Lookup lookup,
//                                      String name,
//                                      Class<?> type,
//                                      Class<?> ownerType,
//                                      Class<?> variableType) throws NoSuchFieldException, IllegalAccessException {
//        if (ownerType.isArray()) {
//            return MethodHandles.arrayElementVarHandle(ownerType);
//        }
//
//        try {
//            lookup.accessClass(ownerType);
//        } catch (IllegalAccessException ex) {
//            throw mapLookupExceptionToError(ex);
//        }
//
//        Field f;
//        try {
//            f = ownerType.getDeclaredField(name);
//        } catch (NoSuchFieldException ex) {
//            throw new IncompatibleClassChangeError("field not found: " + ownerType.getName() + "." + name);
//        }
//        if (f.getType() != variableType) {
//            throw new IncompatibleClassChangeError("field type differs: " + f.getType() + " " + variableType);
//        }
//
//        try {
//            return lookup.unreflectVarHandle(f);
//        } catch (ReflectiveOperationException ex) {
//            throw mapLookupExceptionToError(ex);
//        }
//    }

    /** Selector for a VarHandle for an instance field */
    public static final int VH_instanceField = 1;
    /** Selector for a VarHandle for a static field */
    public static final int VH_staticField = 2;
    /** Selector for a VarHandle for an array element */
    public static final int VH_arrayHandle = 3;

    /**
     * Load a {@link VarHandle} giving access to a field or elements of an
     * array.
     *
     * @param lookup stacked automatically by VM
     * @param type the type of the field or array element,
     *        stacked automatically by VM
     * @param constantType stacked automatically by VM
     * @param kind the selector value, one of VH_instanceField, VH_staticField, VH_arrayHandle
     * @param owner the class in which the field is declared (ignored for array handles)
     * @param name the name of the field (ignored for array handles)
     * @return the VarHandle
     * @throws NoSuchFieldException if the field doesn't exist
     * @throws IllegalAccessException if the field is not accessible
     */
    public static VarHandle varHandleBootstrap(MethodHandles.Lookup lookup,
                                               String name,
                                               Class<?> constantType,
                                               int kind,
                                               Class<?> owner,
                                               Class<?> type) throws NoSuchFieldException, IllegalAccessException {
        switch (kind) {
            case VH_instanceField: return lookup.findVarHandle(owner, name, type);
            case VH_staticField: return lookup.findStaticVarHandle(owner, name, type);
            case VH_arrayHandle: return MethodHandles.arrayElementVarHandle(type);
            default: throw new IllegalArgumentException(String.format("Invalid VarHandle kind: %d", kind));
        }
    }
}
