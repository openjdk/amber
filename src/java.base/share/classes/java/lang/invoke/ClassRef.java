/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.TrackableConstant;
import java.lang.reflect.Array;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import sun.invoke.util.Wrapper;

import static java.util.stream.Collectors.joining;

/**
 * A descriptor for a {@linkplain Class} constant.
 */
public final class ClassRef implements ConstantRef.WithTypeDescriptor<Class<?>> {
    private static final Pattern TYPE_DESC = Pattern.compile("(\\[*)(V|I|J|S|B|C|F|D|Z|L[^/.\\[;][^.\\[;]*;)");

    /**  ClassRef representing the primitive type int */
    @TrackableConstant public static final ClassRef CR_int = ClassRef.ofDescriptor("I");
    /**  ClassRef representing the primitive type long */
    @TrackableConstant public static final ClassRef CR_long = ClassRef.ofDescriptor("J");
    /**  ClassRef representing the primitive type float */
    @TrackableConstant public static final ClassRef CR_float = ClassRef.ofDescriptor("F");
    /**  ClassRef representing the primitive type double */
    @TrackableConstant public static final ClassRef CR_double = ClassRef.ofDescriptor("D");
    /**  ClassRef representing the primitive type short */
    @TrackableConstant public static final ClassRef CR_short = ClassRef.ofDescriptor("S");
    /**  ClassRef representing the primitive type byte */
    @TrackableConstant public static final ClassRef CR_byte = ClassRef.ofDescriptor("B");
    /**  ClassRef representing the primitive type char */
    @TrackableConstant public static final ClassRef CR_char = ClassRef.ofDescriptor("C");
    /**  ClassRef representing the primitive type boolean */
    @TrackableConstant public static final ClassRef CR_boolean = ClassRef.ofDescriptor("Z");
    /**  ClassRef representing the void type */
    @TrackableConstant public static final ClassRef CR_void = ClassRef.ofDescriptor("V");

    /**  ClassRef representing the class java.lang.Object */
    @TrackableConstant public static final ClassRef CR_Object = ClassRef.of("java.lang.Object");
    /**  ClassRef representing the class java.lang.String */
    @TrackableConstant public static final ClassRef CR_String = ClassRef.of("java.lang.String");
    /**  ClassRef representing the class java.lang.Class */
    @TrackableConstant public static final ClassRef CR_Class = ClassRef.of("java.lang.Class");
    /**  ClassRef representing the class java.lang.Number */
    @TrackableConstant public static final ClassRef CR_Number = ClassRef.of("java.lang.Number");
    /**  ClassRef representing the class java.lang.Integer */
    @TrackableConstant public static final ClassRef CR_Integer = ClassRef.of("java.lang.Integer");
    /**  ClassRef representing the class java.lang.Long */
    @TrackableConstant public static final ClassRef CR_Long = ClassRef.of("java.lang.Long");
    /**  ClassRef representing the class java.lang.Float */
    @TrackableConstant public static final ClassRef CR_Float = ClassRef.of("java.lang.Float");
    /**  ClassRef representing the class java.lang.Double */
    @TrackableConstant public static final ClassRef CR_Double = ClassRef.of("java.lang.Double");
    /**  ClassRef representing the class java.lang.Short */
    @TrackableConstant public static final ClassRef CR_Short = ClassRef.of("java.lang.Short");
    /**  ClassRef representing the class java.lang.Byte */
    @TrackableConstant public static final ClassRef CR_Byte = ClassRef.of("java.lang.Byte");
    /**  ClassRef representing the class java.lang.Character */
    @TrackableConstant public static final ClassRef CR_Character = ClassRef.of("java.lang.Character");
    /**  ClassRef representing the class java.lang.Boolean */
    @TrackableConstant public static final ClassRef CR_Boolean = ClassRef.of("java.lang.Boolean");
    /**  ClassRef representing the class java.lang.Void */
    @TrackableConstant public static final ClassRef CR_Void = ClassRef.of("java.lang.Void");
    /**  ClassRef representing the class java.lang.Throwable */
    @TrackableConstant public static final ClassRef CR_Throwable = ClassRef.of("java.lang.Throwable");
    /**  ClassRef representing the class java.lang.Exception */
    @TrackableConstant public static final ClassRef CR_Exception = ClassRef.of("java.lang.Exception");

    /**  ClassRef representing the class java.lang.invoke.VarHandle */
    @TrackableConstant public static final ClassRef CR_VarHandle = ClassRef.of("java.lang.invoke.VarHandle");
    /**  ClassRef representing the class java.lang.invoke.MethodHandles */
    @TrackableConstant public static final ClassRef CR_MethodHandles = ClassRef.of("java.lang.invoke.MethodHandles");
    /**  ClassRef representing the class java.lang.invoke.MethodHandle */
    @TrackableConstant public static final ClassRef CR_MethodHandle = ClassRef.of("java.lang.invoke.MethodHandle");
    /**  ClassRef representing the class java.lang.invoke.MethodType */
    @TrackableConstant public static final ClassRef CR_MethodType = ClassRef.of("java.lang.invoke.MethodType");
    /**  ClassRef representing the class java.lang.invoke.CallSite */
    @TrackableConstant public static final ClassRef CR_CallSite = ClassRef.of("java.lang.invoke.CallSite");
    /**  ClassRef representing the class java.lang.invoke.MethodHandles.Lookup */
    @TrackableConstant public static final ClassRef CR_Lookup = CR_MethodHandles.inner("Lookup");

    /**  ClassRef representing the interface java.util.Collection */
    @TrackableConstant public static final ClassRef CR_Collection = ClassRef.of("java.util.Collection");
    /**  ClassRef representing the interface java.util.List */
    @TrackableConstant public static final ClassRef CR_List = ClassRef.of("java.util.List");
    /**  ClassRef representing the interface java.util.Set */
    @TrackableConstant public static final ClassRef CR_Set = ClassRef.of("java.util.Set");
    /**  ClassRef representing the interface java.util.Map */
    @TrackableConstant public static final ClassRef CR_Map = ClassRef.of("java.util.Map");

    private final String descriptor;

    private ClassRef(String descriptor) {
        if (!TYPE_DESC.matcher(descriptor).matches())
            throw new IllegalArgumentException(String.format("%s is not a valid type descriptor", descriptor));
        this.descriptor = descriptor;
    }

    /**
     * Create a {@linkplain ClassRef} from a live {@link Class} object
     *
     * @param clazz the {@code Class}
     * @return a {@linkplain ClassRef} describing the desired class
     */
    @TrackableConstant
    public static ClassRef of(Class<?> clazz) {
        return ClassRef.ofDescriptor(clazz.toDescriptorString());
    }

    /**
     * Create a {@linkplain ClassRef} from a dot-separated class name
     *
     * @param name the class name
     * @return a {@linkplain ClassRef} describing the desired class
     * @throws IllegalArgumentException if the name string does not
     * describe a valid class name
     */
    @TrackableConstant
    public static ClassRef of(String name) {
        return ClassRef.ofDescriptor("L" + name.replace('.', '/') + ";");
    }

    /**
     * Create a {@linkplain ClassRef} from a dot-separated package name and a class name
     *
     * @param packageName the package name, dot-separated
     * @param className the the class name
     * @return a {@linkplain ClassRef} describing the desired class
     * @throws IllegalArgumentException if the package name or class name are not in the correct format
     */
    @TrackableConstant
    public static ClassRef of(String packageName, String className) {
        return ofDescriptor("L" + packageName.replace('.', '/') + (packageName.length() > 0 ? "/" : "") + className + ";");
    }

    /**
     * Create a {@linkplain ClassRef} from a descriptor string.
     *
     * @param descriptor the descriptor string
     * @return a {@linkplain ClassRef} describing the desired class
     * @throws IllegalArgumentException if the descriptor string does not
     * describe a valid class descriptor
     */
    @TrackableConstant
    public static ClassRef ofDescriptor(String descriptor) {
        return new ClassRef(descriptor);
    }

    /**
     * Create a {@linkplain ClassRef} describing an array of the type
     * described by this {@linkplain ClassRef}
     *
     * @return a {@linkplain ClassRef} describing an array type
     */
    @TrackableConstant
    public ClassRef array() {
        return ofDescriptor("[" + descriptor);
    }

    /**
     * Create a {@linkplain ClassRef} describing an inner class of the
     * non-array reference type described by this {@linkplain ClassRef}
     */
    @TrackableConstant
    public ClassRef inner(String innerName) {
        if (!descriptor.startsWith("L"))
            throw new IllegalStateException("Outer class is not a non-array reference type");
        return ClassRef.ofDescriptor(descriptor.substring(0, descriptor.length() - 1) + "$" + innerName + ";");
    }

    /**
     * Create a {@linkplain ClassRef} describing an inner class of the
     * non-array reference type described by this {@linkplain ClassRef}
     */
    @TrackableConstant
    public ClassRef inner(String firstInnerName, String... moreInnerNames) {
        if (!descriptor.startsWith("L"))
            throw new IllegalStateException("Outer class is not a non-array reference type");
        return moreInnerNames.length == 0
               ? inner(firstInnerName)
               : ClassRef.ofDescriptor(descriptor.substring(0, descriptor.length() - 1) + "$" + firstInnerName
                                       + Stream.of(moreInnerNames).collect(joining("$", "$", "")) + ";");
    }


    /**
     * Returns whether this {@linkplain ClassRef}
     * describes an array type
     * @return whether this {@linkplain ClassRef}
     * describes an array type
     */
    public boolean isArray() {
        return descriptor.startsWith("[");
    }

    /**
     * Returns whether this {@linkplain ClassRef}
     * describes a primitive type
     * @return whether this {@linkplain ClassRef}
     * describes a primitive type
     */
    public boolean isPrimitive() {
        return descriptor.length() == 1;
    }

    /**
     * The component type of this {@linkplain ClassRef} if it describes
     * an array type, otherwise the type that it describes
     * @return the component type of the type described by this
     * @throws IllegalStateException if this reference does not describe an array type
     * {@linkplain ClassRef}
     */
    @TrackableConstant
    public ClassRef componentType() {
        if (!isArray())
            throw new IllegalStateException();
        return ofDescriptor(descriptor.substring(1));
    }

    /**
     * If this ref is a primitive class then return the boxed class, otherwise
     * return this.
     * @return the promoted class
     */
    @TrackableConstant
    ClassRef promote() {
        if (isPrimitive()) {
            switch (descriptor) {
                case "I": return CR_Integer;
                case "J": return CR_Long;
                case "F": return CR_Float;
                case "D": return CR_Double;
                case "S": return CR_Short;
                case "B": return CR_Byte;
                case "C": return CR_Character;
                case "Z": return CR_Boolean;
                case "V": return CR_Void;
                default:
                    throw new InternalError("Unreachable");
            }
        }
        else {
            return this;
        }
    }

    @Override
    @TrackableConstant
    public String descriptorString() {
        return descriptor;
    }

    /**
     * Return the canonical name of the type described by this descriptor
     * @return the canonical name of the type described by this descriptor
     */
    public String canonicalName() {
        // @@@ Arrays?
        if (isPrimitive())
            return Wrapper.forBasicType(descriptor.charAt(0)).primitiveSimpleName();
        else
            return descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
    }

    /**
     * Resolve to a Class
     * @param lookup the lookup
     * @return return the class
     * @throws ReflectiveOperationException exception
     */
    public Class<?> resolve(MethodHandles.Lookup lookup) throws ReflectiveOperationException {
        if (isPrimitive())
            return Wrapper.forBasicType(descriptor.charAt(0)).primitiveType();
        else {
            ClassRef comp = this;
            int depth = 0;
            while (comp.isArray()) {
                ++depth;
                comp = comp.componentType();
            }
            String compDescr = comp.descriptor;

            if (compDescr.length() == 1)
                return Class.forName(descriptor, true, lookup.lookupClass().getClassLoader());
            else {
                Class<?> clazz = Class.forName(compDescr.substring(1, compDescr.length() - 1).replace('/', '.'), true, lookup.lookupClass().getClassLoader());
                for (int i = 0; i < depth; i++)
                    clazz = Array.newInstance(clazz, 0).getClass();
                return clazz;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClassRef constant = (ClassRef) o;
        return descriptor != null ? descriptor.equals(constant.descriptor) : constant.descriptor == null;
    }

    @Override
    public int hashCode() {
        return descriptor != null ? descriptor.hashCode() : 0;
    }

    @Override
    public String toString() {
        return String.format("ClassRef[%s]", descriptorString());
    }
}
