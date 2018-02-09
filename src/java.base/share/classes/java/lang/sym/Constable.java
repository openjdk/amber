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
import java.lang.invoke.VarHandle;
import java.util.Optional;

/**
 * A type whose instances can describe themselves as a {@link ConstantRef}.
 * {@linkplain Constable} types include those types which have native
 * representation in the constant pool ({@link String}, {@link Integer},
 * {@link Long}, {@link Float}, {@link Double}, {@link Class}, {@link MethodType},
 * and {@link MethodHandle}), runtime support classes such as {@link VarHandle},
 * and types corresponding to core language features ({@link Enum}).
 *
 * <p>The symbolic description is obtained via {@link #toSymbolicRef(MethodHandles.Lookup)}.
 * A {@linkplain Constable} need not be able to render all instances in the form
 * of a {@link ConstantRef}; this method returns {@link Optional} to indicate
 * whether such a description could be created for a particular instance.
 * (For example, {@link MethodHandle} will produce symbolic descriptions for
 * direct methods handles, but not necessarily for method handles resulting from
 * method handle combinators such as {@link MethodHandle#asType(MethodType)}.)
 *
 * @param <T> the type of the class implementing {@linkplain Constable}
 */
public interface Constable<T> {
    /**
     * Return a symbolic constant reference for this instance, if one can be
     * constructed.
     *
     * @implSpec This method behaves as if {@link #toSymbolicRef(MethodHandles.Lookup)}
     * were called with a lookup parameter of {@code MethodHandles.publicLookup()}.
     *
     * @return An {@link Optional} describing the resulting symbolic reference,
     * or an empty {@link Optional} if one cannot be constructed
     */
    default Optional<? extends ConstantRef<? super T>> toSymbolicRef() {
        return toSymbolicRef(MethodHandles.publicLookup());
    }

    /**
     * Return a symbolic constant reference for this instance, if one can be
     * constructed.  This object (and any classes needed to construct its
     * symbolic description) must be accessible from the class described by the
     * {@code lookup} parameter.
     *
     * @param lookup A {@link MethodHandles.Lookup} to be used to perform
     *               access control determinations
     * @return An {@link Optional} describing the resulting symbolic reference,
     * or an empty {@link Optional} if one cannot be constructed or this object
     * is not accessible from {@code lookup}
     */
    Optional<? extends ConstantRef<? super T>> toSymbolicRef(MethodHandles.Lookup lookup);
}
