/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * A nominal descriptor for a loadable constant value, as defined in JVMS 4.4.
 * Such a descriptor can be resolved via {@link ConstantRef#resolveConstantRef(MethodHandles.Lookup)}
 * to yield the constant value itself.
 *
 * <p>Class names in a nominal descriptor, like class names in the constant pool
 * of a classfile, must be interpreted with respect to a particular to a class
 * loader, which is not part of the nominal descriptor.
 *
 * <p>Static constants that are expressible natively in the constant pool ({@link String},
 * {@link Integer}, {@link Long}, {@link Float}, and {@link Double}) implement
 * {@link ConstantRef}, and serve as nominal descriptors for themselves.
 * Native linkable constants ({@link Class}, {@link MethodType}, and
 * {@link MethodHandle}) have counterpart {@linkplain ConstantRef} types:
 * {@link ClassRef}, {@link MethodTypeRef}, and {@link MethodHandleRef}.
 * Other constants are represented by subtypes of {@link DynamicConstantRef}.
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
 * <p>Constants describing various common constants (such as {@link ClassRef}
 * instances for platform types) can be found in {@link ConstantRefs}.
 *
 * <p>Implementations of {@linkplain ConstantRef} must be
 * <a href="../doc-files/ValueBased.html">value-based</a> classes.
 *
 * <p>Non-platform classes should not implement {@linkplain ConstantRef} directly.
 * Instead, they should extend {@link DynamicConstantRef} (as {@link EnumRef}
 * and {@link VarHandleRef} do.)
 *
 * @apiNote In the future, if the Java language permits, {@linkplain ConstantRef}
 * may become a {@code sealed} interface, which would prohibit subclassing except by
 * explicitly permitted types.  Bytecode libraries can assume that the following
 * is an exhaustive set of direct subtypes: {@link String}, {@link Integer},
 * {@link Long}, {@link Float}, {@link Double}, {@link ClassRef},
 * {@link MethodTypeRef}, {@link MethodHandleRef}, and {@link DynamicConstantRef};
 * this list may be extended to reflect future changes to the constant pool format
 * as defined in JVMS 4.4.
 *
 * @see Constable
 * @see Intrinsics
 * @see ConstantRefs
 *
 */
public interface ConstantRef<T> {
    /**
     * Resolve this descriptor reflectively, emulating the resolution behavior
     * of JVMS 5.4.3 and the access control behavior of JVMS 5.4.4.  The resolution
     * and access control context is provided by the {@link MethodHandles.Lookup}
     * parameter.  No caching of the resulting value is performed.
     *
     * @param lookup The {@link MethodHandles.Lookup} to provide name resolution
     *               and access control context
     * @return the resolved constant value
     * @throws ReflectiveOperationException if a class, method, or field
     * could not be reflectively resolved in the course of resolution
     */
    T resolveConstantRef(MethodHandles.Lookup lookup) throws ReflectiveOperationException;
}
