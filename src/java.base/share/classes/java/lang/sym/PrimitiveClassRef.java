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
import java.util.Objects;

import sun.invoke.util.Wrapper;

/**
 * PrimitiveClassRef
 *
 * @author Brian Goetz
 */
class PrimitiveClassRef extends DynamicConstantRef<Class<?>> implements ClassRef {
    private final String descriptor;

    PrimitiveClassRef(String descriptor) {
        super(SymbolicRefs.BSM_PRIMITIVE_CLASS, checkDescriptor(descriptor), SymbolicRefs.CR_Class);
        this.descriptor = descriptor;
    }

    private static String checkDescriptor(String descriptor) {
        if (descriptor == null
            || descriptor.length() != 1
            || "IJCSBFDZV".indexOf(descriptor.charAt(0)) < 0)
            throw new IllegalArgumentException("Invalid primitive type descriptor " + descriptor);
        return descriptor;
    }

    @Override
    public String canonicalName() {
        return Wrapper.forBasicType(descriptor.charAt(0)).primitiveSimpleName();
    }

    @Override
    @Foldable
    public String descriptorString() {
        return descriptor;
    }

    @Override
    public ClassRef inner(String innerName) {
        throw new IllegalStateException("Outer class is not a non-array reference type");
    }

    @Override
    public ClassRef inner(String firstInnerName, String... moreInnerNames) {
        throw new IllegalStateException("Outer class is not a non-array reference type");
    }

    @Override
    public Class<?> resolveRef(MethodHandles.Lookup lookup) {
        return Wrapper.forBasicType(descriptor.charAt(0)).primitiveType();
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

