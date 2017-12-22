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

import java.lang.annotation.TrackableConstant;
import java.lang.invoke.MethodHandles;
import java.util.Objects;

/**
 * EnumRef
 *
 * @author Brian Goetz
 */
public final class EnumRef<E extends Enum<E>> extends DynamicConstantRef<E> {
    private final ClassRef enumClass;
    private final String constantName;

    private EnumRef(ClassRef enumClass, String constantName) {
        super(SymbolicRefs.BSM_ENUM_CONSTANT, constantName, enumClass);
        this.enumClass = enumClass;
        this.constantName = constantName;
    }

    @TrackableConstant
    public static<E extends Enum<E>> EnumRef<E> of(ClassRef enumClass, String constantName) {
        return new EnumRef<>(enumClass, constantName);
    }

    @TrackableConstant
    public ClassRef enumClass() {
        return enumClass;
    }

    @TrackableConstant
    public String constantName() {
        return constantName;
    }

    @Override
    @SuppressWarnings("unchecked")
    public E resolveRef(MethodHandles.Lookup lookup) throws ReflectiveOperationException {
        return Enum.valueOf((Class<E>) enumClass.resolveRef(lookup), constantName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnumRef<?> ref = (EnumRef<?>) o;
        return Objects.equals(enumClass, ref.enumClass) &&
               Objects.equals(constantName, ref.constantName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enumClass, constantName);
    }

    @Override
    public String toString() {
        return String.format("EnumRef[%s.%s]", enumClass, constantName);
    }
}
