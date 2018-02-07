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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Optional;

/**
 * A type which can be stored in the constant pool of a Java classfile.
 * {@linkplain Constable} describe themselves for storage in the constant pool
 * by a {@link SymbolicRef}.  {@linkplain Constable} types include the types
 * that act as their own symbolic references ({@link String}, {@link Integer},
 * {@link Long}, {@link Float}, {@link Double}), types for which explicit
 * constant pool forms exist ({@link Class}, {@link MethodType}, and {@link MethodHandle}),
 * and other types corresponding to core language features ({@link Enum}).
 *
 * <p>The symbolic description is obtained via {@link #toSymbolicRef(MethodHandles.Lookup)}.
 * Not all instances of a {@linkplain Constable} type need be representable in
 * the constant pool; for example, direct method handles have a native form
 * in the constant pool, but the result of method handle combinators such as
 * {@link MethodHandle#asType(MethodType)} do not.  This method uses {@link Optional}
 * to represent whether or not the {@linkplain Constable} can be stored in the
 * constant pool.
 */
public interface Constable<T> {
    /**
     * Return a symbolic reference for this instance, if one can be constructed.
     *
     * @implSpec This method behaves as if {@link #toSymbolicRef(MethodHandles.Lookup)}
     * were called with a lookup parameter of {@code MethodHandles.publicLookup()}.
     *
     * @return An {@link Optional} describing the resulting symbolic reference,
     * or an empty {@link Optional} if one cannot be constructed
     */
    default Optional<? extends SymbolicRef<T>> toSymbolicRef() {
        return toSymbolicRef(MethodHandles.publicLookup());
    }

    /**
     * Return a symbolic reference for this instance, if one can be constructed
     * and this object (and any classes needed to construct its symbolic description)
     * is accessible from the class described by the {@code lookup} parameter.
     *
     * @param lookup A {@link MethodHandles.Lookup} to be used to perform
     *               access control determinations
     * @return An {@link Optional} describing the resulting symbolic reference,
     * or an empty {@link Optional} if one cannot be constructed or this object
     * is not accessible from {@code lookup}
     */
    Optional<? extends SymbolicRef<T>> toSymbolicRef(MethodHandles.Lookup lookup);
}
