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
package java.lang.constant;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.TypeDescriptor;
import java.util.stream.Stream;

import sun.invoke.util.Wrapper;

import static java.lang.constant.ConstantUtils.binaryToInternal;
import static java.lang.constant.ConstantUtils.dropLastChar;
import static java.lang.constant.ConstantUtils.internalToBinary;
import static java.lang.constant.ConstantUtils.validateMemberName;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * A <a href="package-summary.html#nominal">nominal descriptor</a> for a
 * {@link Class} constant.
 *
 * <p>For common system types, including all the primitive types, there are
 * predefined {@linkplain ClassDesc} constants in {@link ConstantDescs}.  To create
 * a {@linkplain ClassDesc} for a class or interface type, use {@link #of} or
 * {@link #ofDescriptor(String)}; to create a {@linkplain ClassDesc} for an array
 * type, use {@link #ofDescriptor(String)}, or first obtain a
 * {@linkplain ClassDesc} for the component type and then call the {@link #arrayType()}
 * or {@link #arrayType(int)} methods.
 *
 * @apiNote In the future, if the Java language permits, {@linkplain ClassDesc}
 * may become a {@code sealed} interface, which would prohibit subclassing except
 * by explicitly permitted types.  Non-platform classes should not implement
 * {@linkplain ClassDesc} directly.
 *
 * @see ConstantDescs
 */
public interface ClassDesc
        extends ConstantDesc<Class<?>>,
                Constable<ConstantDesc<Class<?>>>,
                TypeDescriptor.OfField<ClassDesc> {

    /**
     * Create a {@linkplain ClassDesc} given the name of a class or interface
     * type, such as {@code "java.lang.String"}.  (To create a descriptor for an
     * array type, either use {@link #ofDescriptor(String)}
     * or {@link #arrayType()}; to create a descriptor for a primitive type, use
     * {@link #ofDescriptor(String)} or use the predefined constants in
     * {@link ConstantDescs}).
     *
     * @param name the fully qualified (dot-separated) binary class name
     * @return a {@linkplain ClassDesc} describing the desired class
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if the name string is not in the
     * correct format
     */
    static ClassDesc of(String name) {
        ConstantUtils.validateBinaryClassName(requireNonNull(name));
        return ClassDesc.ofDescriptor("L" + binaryToInternal(name) + ";");
    }

    /**
     * Create a {@linkplain ClassDesc} given a package name and an unqualified
     * class name.
     *
     * @param packageName the package name (dot-separated)
     * @param className the unqualified class name
     * @return a {@linkplain ClassDesc} describing the desired class
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if the package name or class name are
     * not in the correct format
     */
    static ClassDesc of(String packageName, String className) {
        ConstantUtils.validateBinaryClassName(requireNonNull(packageName));
        validateMemberName(requireNonNull(className));
        return ofDescriptor(String.format("L%s%s%s;",
                                          binaryToInternal(packageName),
                                          (packageName.length() > 0 ? "/" : ""),
                                          className));
    }

    /**
     * Create a {@linkplain ClassDesc} given a descriptor string.
     *
     * @param descriptor a field descriptor string, as per JVMS 4.3.2
     * @return a {@linkplain ClassDesc} describing the desired class
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if the name string is not in the
     * correct format
     * @jvms 4.3.2 Field Descriptors
     */
    static ClassDesc ofDescriptor(String descriptor) {
        requireNonNull(descriptor);
        return (descriptor.length() == 1)
               ? new PrimitiveClassDescImpl(descriptor)
               : new ReferenceClassDescImpl(descriptor);
    }

    /**
     * Create a {@linkplain ClassDesc} for an array type whose component type
     * is described by this {@linkplain ClassDesc}.
     *
     * @return a {@linkplain ClassDesc} describing the array type
     */
    default ClassDesc arrayType() {
        return arrayType(1);
    }

    /**
     * Create a {@linkplain ClassDesc} for an array type of the specified rank,
     * whose component type is described by this {@linkplain ClassDesc}.
     *
     * @param rank the rank of the array
     * @return a {@linkplain ClassDesc} describing the array type
     * @throws IllegalArgumentException if the rank is zero or negative
     */
    default ClassDesc arrayType(int rank) {
        if (rank <= 0)
            throw new IllegalArgumentException("rank: " + rank);
        return ClassDesc.ofDescriptor("[".repeat(rank) + descriptorString());
    }

    /**
     * Create a {@linkplain ClassDesc} for an inner class of the class or
     * interface type described by this {@linkplain ClassDesc}.
     *
     * @param innerName the unqualified name of the inner class
     * @return a {@linkplain ClassDesc} describing the inner class
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if this {@linkplain ClassDesc} does not
     * describe a class or interface type
     */
    default ClassDesc inner(String innerName) {
        validateMemberName(innerName);
        if (!isClassOrInterface())
            throw new IllegalStateException("Outer class is not a class or interface type");
        return ClassDesc.ofDescriptor(String.format("%s$%s;", dropLastChar(descriptorString()), innerName));
    }

    /**
     * Create a {@linkplain ClassDesc} for an inner class of the class or
     * interface type described by this {@linkplain ClassDesc}.
     *
     * @param firstInnerName the unqualified name of the first level of inner class
     * @param moreInnerNames the unqualified name(s) of the remaining levels of
     *                       inner class
     * @return a {@linkplain ClassDesc} describing the inner class
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if this {@linkplain ClassDesc} does not
     * describe a class or interface type
     */
    default ClassDesc inner(String firstInnerName, String... moreInnerNames) {
        if (!isClassOrInterface())
            throw new IllegalStateException("Outer class is not a class or interface type");
        return moreInnerNames.length == 0
               ? inner(firstInnerName)
               : inner(firstInnerName + Stream.of(moreInnerNames).collect(joining("$", "$", "")));
    }

    /**
     * Returns whether this {@linkplain ClassDesc} describes an array type.
     *
     * @return whether this {@linkplain ClassDesc} describes an array type
     */
    default boolean isArray() {
        return descriptorString().startsWith("[");
    }

    /**
     * Returns whether this {@linkplain ClassDesc} describes a primitive type.
     *
     * @return whether this {@linkplain ClassDesc} describes a primitive type
     */
    default boolean isPrimitive() {
        return descriptorString().length() == 1;
    }

    /**
     * Returns whether this {@linkplain ClassDesc} describes a class or interface type.
     *
     * @return whether this {@linkplain ClassDesc} describes a class or interface type
     */
    default boolean isClassOrInterface() {
        return descriptorString().startsWith("L");
    }

    /**
     * Returns the component type of this {@linkplain ClassDesc}, if it describes
     * an array type, or {@code null} otherwise.
     *
     * @return a {@linkplain ClassDesc} describing the component type, or {@code null}
     * if this descriptor does not describe an array type
     */
    default ClassDesc componentType() {
        return isArray() ? ClassDesc.ofDescriptor(descriptorString().substring(1)) : null;
    }

    /**
     * Returns the package name of this {@linkplain ClassDesc}, if it describes
     * a class or interface type.
     *
     * @return the package name, or the empty string if the class is in the
     * default package
     * @throws IllegalStateException if this {@linkplain ClassDesc} does not
     * describe a class or interface type
     */
    default String packageName() {
        if (!isClassOrInterface())
            throw new IllegalStateException("not a class or interface");
        String className = internalToBinary(ConstantUtils.dropFirstAndLastChar(descriptorString()));
        int index = className.lastIndexOf('.');
        return (index == -1) ? "" : className.substring(0, index);
    }

    /**
     * Returns a human-readable name for the type described by this descriptor.
     *
     * @return the human-readable name
     */
    default String displayName() {
        if (isPrimitive())
            return Wrapper.forBasicType(descriptorString().charAt(0)).primitiveSimpleName();
        else if (isClassOrInterface()) {
            return descriptorString().substring(Math.max(1, descriptorString().lastIndexOf('/') + 1),
                                                descriptorString().length() - 1);
        }
        else if (isArray()) {
            int depth = ConstantUtils.arrayDepth(descriptorString());
            ClassDesc c = this;
            for (int i=0; i<depth; i++)
                c = c.componentType();
            return c.displayName() + "[]".repeat(depth);
        }
        else
            throw new IllegalStateException(descriptorString());
    }

    /**
     * Constant bootstrap method for representing a {@linkplain ClassDesc} in
     * the constant pool of a classfile.
     *
     * @param lookup ignored
     * @param name ignored
     * @param clazz ignored
     * @param descriptor a field descriptor string for the class, as per JVMS 4.3.2
     * @return the {@linkplain ClassDesc}
     * @jvms 4.3.2 Field Descriptors
     */
    public static ClassDesc constantBootstrap(MethodHandles.Lookup lookup, String name, Class<ClassDesc> clazz,
                                              String descriptor) {
        return ClassDesc.ofDescriptor(descriptor);
    }

    /**
     * Return a field type descriptor string for this type, as per JVMS 4.3.2
     *
     * @return the descriptor string
     * @jvms 4.3.2 Field Descriptors
     */
    String descriptorString();
}
