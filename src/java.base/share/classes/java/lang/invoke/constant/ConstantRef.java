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

import java.lang.invoke.Intrinsics;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * A nominal reference to a loadable constant value, as defined in JVMS 4.4.
 * Such a nominal reference can be stored in the constant pool of a classfile
 * and resolved to yield the constant value itself.
 *
 * <p>Static constants that are expressible natively in the constant pool ({@link String},
 * {@link Integer}, {@link Long}, {@link Float}, and {@link Double}) implement
 * {@link ConstantRef}, serve as nominal references for themselves.
 * Native linkable constants ({@link Class}, {@link MethodType}, and
 * {@link MethodHandle}) have counterpart {@linkplain ConstantRef} types:
 * {@link ClassRef}, {@link MethodTypeRef}, and {@link MethodHandleRef}.
 * Other constants are represented by subtypes of {@link DynamicConstantRef}.
 *
 * <p>Non-platform classes should not implement {@linkplain ConstantRef} directly.
 * Instead, they should extend {@link DynamicConstantRef} (as {@link EnumRef}
 * and {@link VarHandleRef} do.)
 *
 * <p>Constants can be reflectively resolved via {@link ConstantRef#resolveConstantRef(MethodHandles.Lookup)}.
 *
 * <p>Constants describing various useful nominal references (such as {@link ClassRef}
 * instances for platform classes) can be found in {@link ConstantRefs}.
 *
 * <p>APIs that perform generation or parsing of bytecode are encouraged to use
 * {@linkplain ConstantRef} to describe the operand of an {@code ldc} instruction
 * (including dynamic constants), the static bootstrap arguments of
 * dynamic constants and {@code invokedynamic} instructions, and other
 * bytecodes or classfile structures that make use of the constant pool.
 *
 * <p>The {@linkplain ConstantRef} types are also used by {@link Intrinsics}
 * to express {@code ldc} instructions.
 *
 * <p>Implementations of {@linkplain ConstantRef} must be
 * <a href="../doc-files/ValueBased.html">value-based</a> classes.
 *
 * @apiNote In the future, if the Java language permits, {@linkplain ConstantRef}
 * may become a {@code sealed} interface, which would prohibit subclassing except by
 * explicitly permitted types.  Bytecode libraries can assume that the following
 * is an exhaustive set of direct subtypes: {@link String}, {@link Integer},
 * {@link Long}, {@link Float}, {@link Double}, {@link ClassRef},
 * {@link MethodTypeRef}, {@link MethodHandleRef}, and {@link DynamicConstantRef}.
 *
 * @see Constable
 * @see ConstantRef
 * @see Intrinsics
 * @see ConstantRefs
 *
 */
public interface ConstantRef<T> {
    /**
     * Resolve this reference reflectively, using a {@link MethodHandles.Lookup}
     * to resolve any type names into classes.
     *
     * @param lookup The {@link MethodHandles.Lookup} to be used in name resolution
     * @return the resolved object
     * @throws ReflectiveOperationException if this nominal reference refers
     * (directly or indirectly) to a class, method, or field that cannot be
     * resolved
     */
    T resolveConstantRef(MethodHandles.Lookup lookup) throws ReflectiveOperationException;
}
