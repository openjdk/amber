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
import java.lang.invoke.Intrinsics;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.Optional;

/**
 * A {@link SymbolicRef} for an object expressible in a classfile constant pool.
 *
 * <p>Static constants that can appear natively in the constant pool ({@link String},
 * {@link Integer}, {@link Long}, {@link Float}, and {@link Double}) implement
 * {@link ConstantRef}, serving as symbolic references for themselves.
 * Native linkable constant types ({@link Class}, {@link MethodType}, and
 * {@link MethodHandle}) have counterpart {@linkplain ConstantRef} types:
 * ({@link ClassRef}, {@link MethodTypeRef}, and {@link MethodHandleRef}.)
 * Dynamic constants are represented by {@link DynamicConstantRef}.
 *
 * <p>APIs that perform generation or parsing of bytecode are encouraged to use
 * {@linkplain ConstantRef} to describe the operand of an {@code ldc} instruction
 * (including dynamic constants), the static bootstrap arguments of
 * dynamic constants and {@code invokedynamic} instructions, and any other
 * bytecodes or classfile structures that make use of the constant pool.
 *
 * <p>The {@linkplain ConstantRef} types are also used by {@link Intrinsics}
 * to express {@code ldc} instructions.
 *
 * <p>Non-platform classes should not implement {@linkplain ConstantRef} directly.
 * Instead, they should extend {@link DynamicConstantRef} (as {@link EnumRef}
 * and {@link VarHandleRef} do.)
 *
 * <p>Constants can be reflectively resolved via {@link ConstantRef#resolveRef(MethodHandles.Lookup)}.
 *
 * <p>Constants describing various useful symbolic references (such as {@link ClassRef}
 * constants for platform classes) can be found in {@link SymbolicRefs}.
 *
 * <p>Implementations of {@linkplain ConstantRef} must be
 * <a href="../doc-files/ValueBased.html">value-based</a> classes.
 *
 * @apiNote In the future, if the Java language permits, {@linkplain ConstantRef}
 * may become a {@code sealed} interface, which would prohibit subclassing except by
 * explicitly permitted types.  Bytecode libraries can assume that the following
 * is an exhaustive set of subtypes: {@link String}, {@link Integer}, {@link Long},
 * {@link Float}, {@link Double}, {@link ClassRef}, {@link MethodTypeRef},
 * {@link MethodHandleRef}, and {@link DynamicConstantRef}.
 *
 * @see Constable
 * @see ConstantRef
 * @see Intrinsics
 * @see SymbolicRefs
 */
public interface ConstantRef<T> extends SymbolicRef {
    /**
     * Resolve this reference reflectively, using a {@link MethodHandles.Lookup}
     * to resolve any type names into classes.
     *
     * @param lookup The {@link MethodHandles.Lookup} to be used in name resolution
     * @return the resolved object
     * @throws ReflectiveOperationException if this symbolic reference refers
     * (directly or indirectly) to a class, method, or field that cannot be
     * resolved
     */
    T resolveRef(MethodHandles.Lookup lookup) throws ReflectiveOperationException;

    /**
     * A {@linkplain ConstantRef} which is associated with a type descriptor
     * string that would be the target of a {@code NameAndType} constant.
     *
     * @param <T> The type of the object which this {@linkplain ConstantRef}
     *            describes
     */
    interface WithTypeDescriptor<T> extends ConstantRef<T> {
        /**
         * Return the descriptor string associated with the object described
         * by this symbolic reference
         *
         * @return the descriptor string
         */
        @Foldable
        String descriptorString();
    }

    /**
     * An object that serves as its own symbolic reference.  Only the classes
     * {@link String}, {@link Integer}, {@link Long}, {@link Float}, and
     * {@link Double} should implement this interface.
     *
     * @param <T> The type of the object which this {@linkplain ConstantRef}
     *            describes
     */
    interface OfSelf<T extends ConstantRef.OfSelf<T>>
            extends ConstantRef<T>, Constable<T> {
        /**
         * {@inheritDoc}
         *
         * @implSpec This implementation returns its receiver
         *
         * @param lookup ignored
         * @return the symbolic reference
         */
        @Override
        @SuppressWarnings("unchecked")
        default Optional<T> toSymbolicRef(MethodHandles.Lookup lookup) {
            return Optional.of((T) this);
        }

        /**
         * {@inheritDoc}
         *
         * @implSpec This implementation returns its receiver
         *
         * @param lookup ignored
         * @return the symbolic reference
         */
        @Override
        @SuppressWarnings("unchecked")
        default T resolveRef(MethodHandles.Lookup lookup) {
            return (T) this;
        }
    }
}
