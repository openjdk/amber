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
package java.lang.sym;

import jdk.internal.vm.annotation.Foldable;

import java.util.stream.Stream;

import sun.invoke.util.Wrapper;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * A nominal reference for a {@link Class}.
 */
public interface ClassRef extends ConstantRef<Class<?>>, Constable<ConstantRef<Class<?>>> {
    /**
     * Create a {@linkplain ClassRef} from a fully-qualified, dot-separated
     * class name
     *
     * @param name the fully qualified class name, dot-separated
     * @return a {@linkplain ClassRef} describing the desired class
     * @throws IllegalArgumentException if the name string does not
     * describe a valid class name
     */
    @Foldable
    static ClassRef of(String name) {
        return ClassRef.ofDescriptor("L" + name.replace('.', '/') + ";");
    }

    /**
     * Create a {@linkplain ClassRef} from a dot-separated package name and an
     * unqualified class name
     *
     * @param packageName the package name, dot-separated
     * @param className the unqualified class name
     * @return a {@linkplain ClassRef} describing the desired class
     * @throws IllegalArgumentException if the package name or class name are
     * not in the correct format
     */
    @Foldable
    static ClassRef of(String packageName, String className) {
        if (className.contains("."))
            throw new IllegalArgumentException(className);
        return ofDescriptor("L" + packageName.replace('.', '/')
                            + (packageName.length() > 0 ? "/" : "")
                            + className + ";");
    }

    /**
     * Create a {@linkplain ClassRef} from a descriptor string for a class
     *
     * @param descriptor the descriptor string
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
     * Create a {@linkplain ClassRef} describing an inner class of the
     * non-array reference type described by this {@linkplain ClassRef}
     * @param innerName the name of the inner class
     * @return a {@linkplain ClassRef} describing the inner class
     */
    @Foldable
    default ClassRef inner(String innerName) {
        if (!descriptorString().startsWith("L"))
            throw new IllegalStateException("Outer class is not a non-array reference type");
        return ClassRef.ofDescriptor(descriptorString().substring(0, descriptorString().length() - 1) + "$" + innerName + ";");
    }

    /**
     * Create a {@linkplain ClassRef} describing a multiply nested inner class of the
     * non-array reference type described by this {@linkplain ClassRef}
     *
     * @param firstInnerName the name of the first level of inner class
     * @param moreInnerNames the name(s) of the remaining levels of inner class
     * @return a {@linkplain ClassRef} describing the inner class
     */
    @Foldable
    default ClassRef inner(String firstInnerName, String... moreInnerNames) {
        if (!descriptorString().startsWith("L"))
            throw new IllegalStateException("Outer class is not a non-array reference type");
        return moreInnerNames.length == 0
               ? inner(firstInnerName)
               : ClassRef.ofDescriptor(descriptorString().substring(0, descriptorString().length() - 1) + "$" + firstInnerName
                                       + Stream.of(moreInnerNames).collect(joining("$", "$", "")) + ";");
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
            throw new IllegalStateException();
        return ClassRef.ofDescriptor(descriptorString().substring(1));
    }

    /**
     * Returns a human-readable name for the type described by this descriptor
     *
     * @return a human-readable name for the type described by this descriptor
     */
    default String simpleName() {
        if (descriptorString().length() == 1)
            return Wrapper.forBasicType(descriptorString().charAt(0)).primitiveSimpleName();
        else if (descriptorString().startsWith("L")) {
            return descriptorString().substring(Math.max(1, descriptorString().lastIndexOf('/') + 1),
                                                descriptorString().length() - 1);
        }
        else if (descriptorString().startsWith(("["))) {
            int depth=arrayDepth();
            ClassRef c = this;
            for (int i=0; i<depth; i++)
                c = c.componentType();
            String name = c.simpleName();
            StringBuilder sb = new StringBuilder(name.length() + 2*depth);
            sb.append(name);
            for (int i=0; i<depth; i++)
                sb.append("[]");
            return sb.toString();
        }
        else
            throw new IllegalStateException(descriptorString());
    }

    private int arrayDepth() {
        int depth = 0;
        while (descriptorString().charAt(depth) == '[')
            depth++;
        return depth;
    }

    /**
     * Return the type descriptor string
     * @return the type descriptor string
     */
    @Foldable
    String descriptorString();
}
