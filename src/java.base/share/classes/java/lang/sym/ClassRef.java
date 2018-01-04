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

import java.lang.annotation.Foldable;
import java.lang.invoke.MethodHandles;
import java.util.Optional;

/**
 * A descriptor for a {@linkplain Class} constant.
 */
public interface ClassRef extends SymbolicRef.WithTypeDescriptor<Class<?>> {

    /**
     * Create a {@linkplain ClassRef} from a dot-separated class name
     *
     * @param name the class name
     * @return a {@linkplain ClassRef} describing the desired class
     * @throws IllegalArgumentException if the name string does not
     * describe a valid class name
     */
    @Foldable
    static ClassRef of(String name) {
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
    @Foldable
    static ClassRef of(String packageName, String className) {
        return ofDescriptor("L" + packageName.replace('.', '/') + (packageName.length() > 0 ? "/" : "") + className + ";");
    }

    /**
     * Create a {@linkplain ClassRef} from a descriptor string.
     *
     * @param descriptor the descriptor string
     * @return a {@linkplain ClassRef} describing the desired class
     * @throws NullPointerException if the descriptor string is null
     * @throws IllegalArgumentException if the descriptor string does not
     * describe a valid class descriptor
     */
    @Foldable
    static ClassRef ofDescriptor(String descriptor) {
        if (descriptor == null)
            throw new NullPointerException("descriptor");
        else if (descriptor.length() == 1)
            return new PrimitiveClassRef(descriptor);
        else
            return new NamedClassRef(descriptor);
    }

    /**
     * Create a {@linkplain ClassRef} describing an array of the type
     * described by this {@linkplain ClassRef}
     *
     * @return a {@linkplain ClassRef} describing an array type
     */
    @Foldable
    default ClassRef array() {
        return ClassRef.ofDescriptor("[" + descriptorString());
    }

    /**
     * Create a {@linkplain ClassRef} describing an inner class of the
     * non-array reference type described by this {@linkplain ClassRef}
     */
    @Foldable
    ClassRef inner(String innerName);

    /**
     * Create a {@linkplain ClassRef} describing an inner class of the
     * non-array reference type described by this {@linkplain ClassRef}
     */
    @Foldable
    ClassRef inner(String firstInnerName, String... moreInnerNames);

    /**
     * Returns whether this {@linkplain ClassRef}
     * describes an array type
     * @return whether this {@linkplain ClassRef}
     * describes an array type
     */
    default boolean isArray() {
        return descriptorString().startsWith("[");
    }

    /**
     * Returns whether this {@linkplain ClassRef}
     * describes a primitive type
     * @return whether this {@linkplain ClassRef}
     * describes a primitive type
     */
    default boolean isPrimitive() {
        return descriptorString().length() == 1;
    }

    /**
     * The component type of this {@linkplain ClassRef} if it describes
     * an array type, otherwise the type that it describes
     * @return the component type of the type described by this
     * @throws IllegalStateException if this reference does not describe an array type
     * {@linkplain ClassRef}
     */
    @Foldable
    default ClassRef componentType() {
        if (!isArray())
            throw new IllegalStateException();
        return ClassRef.ofDescriptor(descriptorString().substring(1));
    }

    /**
     * Return the canonical name of the type described by this descriptor
     * @return the canonical name of the type described by this descriptor
     */
    String canonicalName();

    @Override
    default Optional<? extends SymbolicRef<Class<?>>> toSymbolicRef(MethodHandles.Lookup lookup) {
        return Optional.of(DynamicConstantRef.<Class<?>>of(SymbolicRefs.BSM_INVOKE)
                                   .withArgs(SymbolicRefs.MHR_CLASSREF_FACTORY, descriptorString()));
    }
}
