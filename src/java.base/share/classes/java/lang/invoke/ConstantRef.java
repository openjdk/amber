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

import java.lang.annotation.TrackableConstant;

/**
 * Purely-nominal descriptor for a constant value expressible in a classfile
 * constant pool.
 *
 * <p> Native constant types that don't require linkage ({@link String}, {@link
 * Integer}, {@link Long}, {@link Float}, and {@link Double}) implement
 * {@linkplain ConstantRef} directly. Native linkable constant types ({@link
 * Class}, {@link MethodType}, and {@link MethodHandle}) are represented as
 * {@linkplain ConstantRef} via the symbolic reference classes {@link ClassRef},
 * {@link MethodTypeRef}, and {@link MethodHandleRef}.  Dynamic constants are
 * represented by the symbolic reference type {@link DynamicConstantRef}.
 *
 * <p>APIs that deal in generation or parsing of bytecode should use
 * {@linkplain ConstantRef} to describe the operand of an {@code LDC} instruction,
 * including dynamic constants, and the static argument lists of
 * {@code invokedynamic} instructions.  The {@linkplain ConstantRef} types are also
 * used by the {@link Intrinsics} API to express {@code LDC} and
 * {@code invokedynamic} instructions.
 *
 * <p> Like names in the constant pool, names in a {@linkplain ConstantRef} are
 * independent of a class loader.  When a {@linkplain ConstantRef} is
 * intrinsified, it is interpreted relative to the class loader that loaded the
 * class in which the intrinsic appears (just like names that appear in that
 * classes constant pool.)
 *
 * @param <T> The type of the object which this {@linkplain ConstantRef}
 *            describes
 * @see Constables
 * @see Intrinsics
 * @see TrackableConstant
 */
public interface ConstantRef<T> {

    /**
     * A {@link ConstantRef} representing a null reference
     *
     * @param <T> The type of the object which the resulting {@linkplain ConstantRef} describes
     * @return the {@link ConstantRef}
     */
    @TrackableConstant
    static<T> ConstantRef<T> ofNull() {
        return DynamicConstantRef.of(BootstrapSpecifier.of(Constables.BSM_NULL_CONSTANT), ClassRef.CR_Object);
    }

    /**
     * Construct a VarHandle for an instance field
     * @param owner the class containing the field
     * @param name the field name
     * @param type the field type
     * @return the VarHandle
     */
    static ConstantRef<VarHandle> fieldVarHandle(ClassRef owner, String name, ClassRef type) {
        return DynamicConstantRef.of(
                BootstrapSpecifier.of(Constables.BSM_VARHANDLE_FIELD, owner, type),
                name);
    }

    /**
     * Construct a VarHandle for a static field
     * @param owner the class containing the field
     * @param name the field name
     * @param type the field type
     * @return the VarHandle
     */
    static ConstantRef<VarHandle> staticFieldVarHandle(ClassRef owner, String name, ClassRef type) {
        return DynamicConstantRef.of(
                BootstrapSpecifier.of(Constables.BSM_VARHANDLE_STATIC_FIELD, owner, type),
                name);
    }

    /**
     * Construct a VarHandle for an array
     * @param arrayClass the array class
     * @return the VarHandle
     */
    static ConstantRef<VarHandle> arrayVarHandle(ClassRef arrayClass) {
        return DynamicConstantRef.of(
                BootstrapSpecifier.of(Constables.BSM_VARHANDLE_ARRAY, arrayClass));
    }

    /**
     * A {@linkplain ConstantRef} which is associated with a type descriptor
     * string that would be the target of a {@code NameAndType} constant.
     *
     * @param <T> The type to which this constant pool entry resolves
     */
    public interface WithTypeDescriptor<T> extends ConstantRef<T> {
        /**
         * Return the descriptor string associated with this constant pool entry
         *
         * @return the descriptor string
         */
        String descriptorString();
    }
}
