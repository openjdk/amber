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
import java.lang.reflect.Array;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import sun.invoke.util.Wrapper;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * A symbolic reference for a {@link Class}.
 */
public class ClassRef implements ConstantRef.WithTypeDescriptor<Class<?>>, Constable<ConstantRef<Class<?>>> {
    private static final Pattern TYPE_DESC = Pattern.compile("(\\[*)(V|I|J|S|B|C|F|D|Z|L[^/.\\[;][^.\\[;]*;)");

    private final String descriptor;

    /**
     * Create a {@linkplain ClassRef} from a descriptor string for a class
     *
     * @param descriptor the descriptor string
     * @throws IllegalArgumentException if the descriptor string does not
     * describe a valid class name
     */
    private ClassRef(String descriptor) {
        // @@@ Replace validation with a lower-overhead mechanism than regex
        if (descriptor == null
            || !TYPE_DESC.matcher(descriptor).matches())
            throw new IllegalArgumentException(String.format("%s is not a valid type descriptor", descriptor));
        this.descriptor = descriptor;
    }

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
    public static ClassRef of(String name) {
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
    public static ClassRef of(String packageName, String className) {
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
    public static ClassRef ofDescriptor(String descriptor) {
        return new ClassRef(requireNonNull(descriptor));
    }

    /**
     * Create a {@linkplain ClassRef} describing an array of the type
     * described by this {@linkplain ClassRef}
     *
     * @return a {@linkplain ClassRef} describing the array type
     */
    @Foldable
    public ClassRef array() {
        return ClassRef.ofDescriptor("[" + descriptor);
    }

    /**
     * Create a {@linkplain ClassRef} describing an inner class of the
     * non-array reference type described by this {@linkplain ClassRef}
     * @param innerName the name of the inner class
     * @return a {@linkplain ClassRef} describing the inner class
     */
    @Foldable
    public ClassRef inner(String innerName) {
        if (!descriptor.startsWith("L"))
            throw new IllegalStateException("Outer class is not a non-array reference type");
        return ClassRef.ofDescriptor(descriptor.substring(0, descriptor.length() - 1) + "$" + innerName + ";");
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
    public ClassRef inner(String firstInnerName, String... moreInnerNames) {
        if (!descriptor.startsWith("L"))
            throw new IllegalStateException("Outer class is not a non-array reference type");
        return moreInnerNames.length == 0
               ? inner(firstInnerName)
               : ClassRef.ofDescriptor(descriptor.substring(0, descriptor.length() - 1) + "$" + firstInnerName
                                       + Stream.of(moreInnerNames).collect(joining("$", "$", "")) + ";");
    }

    /**
     * Returns whether this {@linkplain ClassRef} describes an array type
     *
     * @return whether this {@linkplain ClassRef} describes an array type
     */
    public boolean isArray() {
        return descriptor.startsWith("[");
    }

    /**
     * Returns whether this {@linkplain ClassRef} describes a primitive type
     *
     * @return whether this {@linkplain ClassRef} describes a primitive type
     */
    public boolean isPrimitive() {
        return descriptor.length() == 1;
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
    public ClassRef componentType() {
        if (!isArray())
            throw new IllegalStateException();
        return ClassRef.ofDescriptor(descriptor.substring(1));
    }

    /**
     * Returns the canonical name of the type described by this descriptor
     *
     * @return the canonical name of the type described by this descriptor
     */
    public String canonicalName() {
        if (descriptor.length() == 1)
            return Wrapper.forBasicType(descriptor.charAt(0)).primitiveSimpleName();
        else if (descriptor.startsWith("L"))
            return descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
        else if (descriptor.startsWith(("["))) {
            int depth=arrayDepth();
            ClassRef c = this;
            for (int i=0; i<depth; i++)
                c = c.componentType();
            String name = c.canonicalName();
            StringBuilder sb = new StringBuilder(name.length() + 2*depth);
            sb.append(name);
            for (int i=0; i<depth; i++)
                sb.append("[]");
            return sb.toString();
        }
        else
            throw new IllegalStateException(descriptor);
    }

    private int arrayDepth() {
        int depth = 0;
        while (descriptor.charAt(depth) == '[')
            depth++;
        return depth;
    }

    @Override
    public Class<?> resolveRef(MethodHandles.Lookup lookup) throws ReflectiveOperationException {
        if (isPrimitive()) {
            return Wrapper.forBasicType(descriptor.charAt(0)).primitiveType();
        }
        else {
            ClassRef c = this;
            int depth = arrayDepth();
            for (int i=0; i<depth; i++)
                c = c.componentType();

            if (c.descriptor.length() == 1)
                return Class.forName(descriptor, true, lookup.lookupClass().getClassLoader());
            else {
                Class<?> clazz = Class.forName(c.descriptor.substring(1, c.descriptor.length() - 1).replace('/', '.'), true, lookup.lookupClass().getClassLoader());
                for (int i = 0; i < depth; i++)
                    clazz = Array.newInstance(clazz, 0).getClass();
                return clazz;
            }
        }
    }

    @Override
    @Foldable
    public String descriptorString() {
        return descriptor;
    }

    @Override
    public Optional<ConstantRef<ConstantRef<Class<?>>>> toSymbolicRef(MethodHandles.Lookup lookup) {
        return Optional.of(DynamicConstantRef.<ConstantRef<Class<?>>>of(SymbolicRefs.BSM_INVOKE, SymbolicRefs.CR_ClassRef)
                                   .withArgs(SymbolicRefs.MHR_CLASSREF_FACTORY, descriptor));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClassRef constant = (ClassRef) o;
        return Objects.equals(descriptor, constant.descriptor);
    }

    @Override
    public int hashCode() {
        return descriptor != null ? descriptor.hashCode() : 0;
    }

    @Override
    public String toString() {
        return String.format("ClassRef[%s]", canonicalName());
    }
}
