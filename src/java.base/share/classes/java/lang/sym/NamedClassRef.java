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
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

/**
 * NamedClassRef
 *
 * @author Brian Goetz
 */
public final class NamedClassRef implements ClassRef {
    static final Pattern TYPE_DESC = Pattern.compile("(\\[*)(V|I|J|S|B|C|F|D|Z|L[^/.\\[;][^.\\[;]*;)");

    private final String descriptor;

    NamedClassRef(String descriptor) {
        if (descriptor == null
            || !TYPE_DESC.matcher(descriptor).matches())
            throw new IllegalArgumentException(String.format("%s is not a valid type descriptor", descriptor));
        this.descriptor = descriptor;
    }

    /**
     * Create a {@linkplain ClassRef} describing an inner class of the
     * non-array reference type described by this {@linkplain ClassRef}
     */
    @Foldable
    public ClassRef inner(String innerName) {
        if (!descriptor.startsWith("L"))
            throw new IllegalStateException("Outer class is not a non-array reference type");
        return ClassRef.ofDescriptor(descriptor.substring(0, descriptor.length() - 1) + "$" + innerName + ";");
    }

    /**
     * Create a {@linkplain ClassRef} describing an inner class of the
     * non-array reference type described by this {@linkplain ClassRef}
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

    @Override
    @Foldable
    public String descriptorString() {
        return descriptor;
    }

    /**
     * Return the canonical name of the type described by this descriptor
     * @return the canonical name of the type described by this descriptor
     */
    public String canonicalName() {
        // @@@ Arrays?
        return descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
    }

    /**
     * Resolve to a Class
     * @param lookup the lookup
     * @return return the class
     * @throws ReflectiveOperationException exception
     */
    public Class<?> resolveRef(MethodHandles.Lookup lookup) throws ReflectiveOperationException {
        ClassRef comp = this;
        int depth = 0;
        while (comp.isArray()) {
            ++depth;
            comp = comp.componentType();
        }
        String compDescr = comp.descriptorString();

        if (compDescr.length() == 1)
            return Class.forName(descriptor, true, lookup.lookupClass().getClassLoader());
        else {
            Class<?> clazz = Class.forName(compDescr.substring(1, compDescr.length() - 1).replace('/', '.'), true, lookup.lookupClass().getClassLoader());
            for (int i = 0; i < depth; i++)
                clazz = Array.newInstance(clazz, 0).getClass();
            return clazz;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClassRef constant = (ClassRef) o;
        return Objects.equals(descriptorString(), constant.descriptorString());
    }

    @Override
    public int hashCode() {
        return descriptorString() != null ? descriptorString().hashCode() : 0;
    }

    @Override
    public String toString() {
        return String.format("ClassRef[%s]", descriptorString());
    }
}
