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
package java.lang.invoke.constant;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.Optional;

/**
 * Represents a type which is <em>constable</em>.  A constable type is one whose
 * values can be represented in the constant pool of a Java classfile.
 *
 * <p>The basic constable types are types whose values have a native representation
 * in the constant pool: ({@link String}, {@link Integer}, {@link Long}, {@link Float},
 * {@link Double}, {@link Class}, {@link MethodType}, and {@link MethodHandle}).
 *
 * <p>Other reference types can be constable if their instances can describe
 * themselves in nominal form as a {@link ConstantRef}. Examples in the Java SE
 * Platform API are types that support Java language features, such as {@link Enum},
 * and runtime support classes, such as {@link VarHandle}.
 *
 * <p>The nominal form of an instance of a constable type is obtained via
 * {@link #toConstantRef(MethodHandles.Lookup)}. A {@linkplain Constable} need
 * not be able to (or may choose not to) render all its instances in the form of
 * a {@link ConstantRef}; this method returns an {@link Optional} to indicate
 * whether a nominal reference could be created for a particular instance. (For
 * example, {@link MethodHandle} will produce nominal references for direct
 * method handles, but not necessarily for method handles resulting from method
 * handle combinators such as {@link MethodHandle#asType(MethodType)}.)
 *
 * @param <T> the type of the class implementing {@linkplain Constable}
 */
public interface Constable<T> {
    /**
     * Return a nominal reference for this instance, if one can be
     * constructed.
     *
     * @implSpec This method behaves as if {@link #toConstantRef(MethodHandles.Lookup)}
     * were called with a lookup parameter of {@code MethodHandles.publicLookup()}.
     *
     * @return An {@link Optional} describing the resulting nominal reference,
     * or an empty {@link Optional} if one cannot be constructed
     */
    default Optional<? extends ConstantRef<? super T>> toConstantRef() {
        return toConstantRef(MethodHandles.publicLookup());
    }

    /**
     * Return a nominal reference for this instance, if one can be
     * constructed.  This object (and any classes needed to construct its
     * nominal description) must be accessible from the class described by the
     * {@code lookup} parameter.
     *
     * @param lookup A {@link MethodHandles.Lookup} to be used to perform
     *               access control determinations
     * @return An {@link Optional} describing the resulting nominal reference,
     * or an empty {@link Optional} if one cannot be constructed or this object
     * is not accessible from {@code lookup}
     */
    Optional<? extends ConstantRef<? super T>> toConstantRef(MethodHandles.Lookup lookup);
}
