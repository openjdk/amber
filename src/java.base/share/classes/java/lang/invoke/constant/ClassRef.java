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
package java.lang.invoke.constant;

import jdk.internal.lang.annotation.Foldable;

import java.util.stream.Stream;

import sun.invoke.util.Wrapper;

import static java.lang.invoke.constant.ConstantUtils.binaryToInternal;
import static java.lang.invoke.constant.ConstantUtils.dropLastChar;
import static java.lang.invoke.constant.ConstantUtils.internalToBinary;
import static java.lang.invoke.constant.ConstantUtils.validateMemberName;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * A nominal descriptor for a {@link Class} constant.
 *
 * <p>For common system types, including all the primitive types, there are
 * predefined {@linkplain ClassRef} constants in {@link ConstantRefs}.  To create
 * a {@linkplain ClassRef} for a class or interface type, use {@link #of} or
 * {@link #ofDescriptor(String)}; to create a {@linkplain ClassRef} for an array
 * type, use {@link #ofDescriptor(String)}, or first obtain a
 * {@linkplain ClassRef} for the component type and then call the {@link #array()}
 * method.
 *
 * @see ConstantRefs
 */
public interface ClassRef
        extends ConstantRef<Class<?>>, Constable<ConstantRef<Class<?>>> {
    /**
     * Create a {@linkplain ClassRef} from a class name.
     *
     * @param name the fully qualified (dot-separated) binary class name
     * @return a {@linkplain ClassRef} describing the desired class
     * @throws IllegalArgumentException if the name string does not
     * describe a valid class name
     */
    @Foldable
    static ClassRef of(String name) {
        ConstantUtils.validateBinaryClassName(requireNonNull(name));
        return ClassRef.ofDescriptor("L" + binaryToInternal(name) + ";");
    }

    /**
     * Create a {@linkplain ClassRef} from a package name and an unqualified
     * class name.
     *
     * @param packageName the package name (dot-separated)
     * @param className the unqualified class name
     * @return a {@linkplain ClassRef} describing the desired class
     * @throws IllegalArgumentException if the package name or class name are
     * not in the correct format
     */
    @Foldable
    static ClassRef of(String packageName, String className) {
        ConstantUtils.validateBinaryClassName(requireNonNull(packageName));
        validateMemberName(requireNonNull(className));
        return ofDescriptor(String.format("L%s%s%s;",
                                          binaryToInternal(packageName),
                                          (packageName.length() > 0 ? "/" : ""),
                                          className));
    }

    /**
     * Create a {@linkplain ClassRef} from a descriptor string for a class
     *
     * @param descriptor a field descriptor string, as per JVMS 4.3.2
     * @return a {@linkplain ClassRef} describing the desired class
     * @throws NullPointerException if the descriptor string is null
     * @throws IllegalArgumentException if the descriptor string is not
     * a valid class descriptor
     */
    @Foldable
    static ClassRef ofDescriptor(String descriptor) {
        requireNonNull(descriptor);
        return (descriptor.length() == 1)
               ? new PrimitiveClassRef(descriptor)
               : new ConstantClassRef(descriptor);
    }

    /**
     * Create a {@linkplain ClassRef} describing an array of the type
     * described by this {@linkplain ClassRef}
     *
     * @return a {@linkplain ClassRef} describing the array type
     */
    @Foldable
    default ClassRef array() {
        return ClassRef.ofDescriptor("[" + descriptorString());
    }

    /**
     * Create a {@linkplain ClassRef} describing an array of the type
     * described by this {@linkplain ClassRef}, of the specified rank
     *
     * @param rank the rank of the array
     * @return a {@linkplain ClassRef} describing the array type
     * @throws IllegalArgumentException if the rank is zero or negative
     */
    @Foldable
    default ClassRef array(int rank) {
        if (rank <= 0)
            throw new IllegalArgumentException();
        ClassRef cr = this;
        for (int i=0; i<rank; i++)
            cr = cr.array();
        return cr;
    }

    /**
     * Create a {@linkplain ClassRef} describing an inner class of the
     * class or interface type described by this {@linkplain ClassRef}
     * @param innerName the unqualified name of the inner class
     * @return a {@linkplain ClassRef} describing the inner class
     * @throws IllegalStateException if this {@linkplain ClassRef} does not
     * describe a class or interface type
     */
    @Foldable
    default ClassRef inner(String innerName) {
        validateMemberName(innerName);
        if (!isClassOrInterface())
            throw new IllegalStateException("Outer class is not a class or interface type");
        return ClassRef.ofDescriptor(String.format("%s$%s;", dropLastChar(descriptorString()), innerName));
    }

    /**
     * Create a {@linkplain ClassRef} describing a multiply nested inner class of the
     * class or interface type described by this {@linkplain ClassRef}
     *
     * @param firstInnerName the name of the first level of inner class
     * @param moreInnerNames the name(s) of the remaining levels of inner class
     * @return a {@linkplain ClassRef} describing the inner class
     * @throws IllegalStateException if this {@linkplain ClassRef} does not
     * describe a class or interface type
     */
    @Foldable
    default ClassRef inner(String firstInnerName, String... moreInnerNames) {
        if (!isClassOrInterface())
            throw new IllegalStateException("Outer class is not a class or interface type");
        return moreInnerNames.length == 0
               ? inner(firstInnerName)
               : inner(firstInnerName + Stream.of(moreInnerNames).collect(joining("$", "$", "")));
    }

    /**
     * Returns whether this {@linkplain ClassRef} describes an array type
     *
     * @return whether this {@linkplain ClassRef} describes an array type
     */
    default boolean isArray() {
        return descriptorString().startsWith("[");
    }

    /**
     * Returns whether this {@linkplain ClassRef} describes a primitive type
     *
     * @return whether this {@linkplain ClassRef} describes a primitive type
     */
    default boolean isPrimitive() {
        return descriptorString().length() == 1;
    }

    /**
     * Returns whether this {@linkplain ClassRef} describes a class or interface type
     *
     * @return whether this {@linkplain ClassRef} describes a class or interface type
     */
    default boolean isClassOrInterface() {
        return descriptorString().startsWith("L");
    }

    /**
     * Returns the component type of this {@linkplain ClassRef}, if it describes
     * an array type
     *
     * @return a {@linkplain ClassRef} describing the component type
     * @throws IllegalStateException if this {@linkplain ClassRef} does not
     * describe an array type
     */
    @Foldable
    default ClassRef componentType() {
        if (!isArray())
            throw new IllegalStateException("not an array");
        return ClassRef.ofDescriptor(descriptorString().substring(1));
    }

    /**
     * Returns the package name of this {@linkplain ClassRef}, if it describes
     * a class or interface type
     *
     * @return the package name, or the empty string if no package
     * @throws IllegalStateException if this {@linkplain ClassRef} does not
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
     * Returns a human-readable name for the type described by this descriptor
     *
     * @return a human-readable name for the type described by this descriptor
     */
    default String displayName() {
        if (descriptorString().length() == 1)
            return Wrapper.forBasicType(descriptorString().charAt(0)).primitiveSimpleName();
        else if (descriptorString().startsWith("L")) {
            return descriptorString().substring(Math.max(1, descriptorString().lastIndexOf('/') + 1),
                                                descriptorString().length() - 1);
        }
        else if (descriptorString().startsWith(("["))) {
            int depth = ConstantUtils.arrayDepth(descriptorString());
            ClassRef c = this;
            for (int i=0; i<depth; i++)
                c = c.componentType();
            String name = c.displayName();
            StringBuilder sb = new StringBuilder(name.length() + 2*depth);
            sb.append(name);
            for (int i=0; i<depth; i++)
                sb.append("[]");
            return sb.toString();
        }
        else
            throw new IllegalStateException(descriptorString());
    }

    /**
     * Return the type descriptor string
     * @return the type descriptor string
     */
    @Foldable
    String descriptorString();
}
