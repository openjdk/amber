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
package java.lang.sym;

import java.lang.annotation.Foldable;
import java.lang.invoke.Intrinsics;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Optional;

/**
 * A purely nominal descriptor for a constant value expressible in a classfile
 * constant pool or other classfile structure.
 *
 * <p> Native constant types that don't require linkage ({@link String}, {@link
 * Integer}, {@link Long}, {@link Float}, and {@link Double}) implement
 * {@linkplain SymbolicRef.OfSelf}, indicating that they serve as their own
 * symbolic reference.  Native linkable constant types ({@link
 * Class}, {@link MethodType}, and {@link MethodHandle}) have counterpart
 * {@linkplain SymbolicRef} types: ({@link ClassRef}, {@link MethodTypeRef},
 * and {@link MethodHandleRef}.)  Dynamic constants are represented by
 * {@link DynamicConstantRef}.
 *
 * <p>APIs that perform generation or parsing of bytecode are encouraged to use
 * {@linkplain SymbolicRef} to describe the operand of an {@code ldc} instruction
 * (including dynamic constants), the static bootstrap arguments of
 * {@code invokedynamic} instructions and dynamic constants, and other bytecodes
 * or classfile structures that make use of the constant pool (such as describing
 * the supertypes of a class or the types of a field.)
 *
 * <p>The {@linkplain SymbolicRef} types are also used by the {@link Intrinsics}
 * API to express {@code ldc} and {@code invokedynamic} instructions.
 *
 * <p>Like names in the constant pool of a class, names in a {@linkplain SymbolicRef}
 * are independent of a class loader.  When the nominal contents of a
 * {@linkplain SymbolicRef} are written to a classfile, they will be interpreted
 * relative to the class loader loading that class, just like any other names
 * appearing in the constant pool.
 *
 * <p>Symbolic references can be reflectively resolved via
 * {@link SymbolicRef#resolveRef(MethodHandles.Lookup)}.
 *
 * <p>Symbolic references may themselves be described symbolically, and therefore
 * stored in the constant pool.  {@linkplain SymbolicRef} implements {@link Constable}
 * for this reason.
 *
 * <p>Constants describing various useful symbolic references (such as {@link ClassRef}
 * constants for platform classes) can be found in {@link SymbolicRefs}.
 *
 * <p>Non-platform classes should not implement {@linkplain SymbolicRef} directly.
 * Instead, they should extend {@link DynamicConstantRef}.
 *
 * <p>Implementations of {@linkplain SymbolicRef} must be
 * <a href="../doc-files/ValueBased.html">value-based</a> classes.
 *
 * @apiNote In the future, if the Java language permits, {@linkplain SymbolicRef}
 * may become a {@code sealed} interface, which would prohibit subclassing except by
 * explicitly permitted types.
 *
 * @param <T> The type of the object which this {@linkplain SymbolicRef}
 *            describes
 * @see Constable
 * @see Intrinsics
 * @see SymbolicRefs
 */
public interface SymbolicRef<T> extends Constable<T> {

    /**
     * Resolve this symbolic reference reflectively, using a {@link MethodHandles.Lookup}
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
     *  Returns a symbolic reference that describes this symbolic reference.
     *
     * @param lookup ignored
     * @return the symbolic reference
     */
    Optional<? extends SymbolicRef<T>> toSymbolicRef(MethodHandles.Lookup lookup);

    /**
     * A {@linkplain SymbolicRef} which is associated with a type descriptor
     * string that would be the target of a {@code NameAndType} constant.
     *
     * @param <T> The type of the object which this {@linkplain SymbolicRef}
     *            describes
     */
    interface WithTypeDescriptor<T> extends SymbolicRef<T> {
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
     * @param <T> The type of the object which this {@linkplain SymbolicRef}
     *            describes
     */
    interface OfSelf<T extends SymbolicRef<T>> extends SymbolicRef<T>, Constable<T> {
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
