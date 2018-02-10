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

import java.lang.invoke.Intrinsics;

/**
 * A purely nominal descriptor for an object, runtime entity, or classfile entity
 * such as a constant pool entry or classfile attribute. Like names in the
 * constant pool of a class, names of classes contained in a {@linkplain SymbolicRef}
 * are independent of a class loader.
 *
 * <p>APIs that perform generation or parsing of bytecode are encouraged to use
 * {@linkplain SymbolicRef} to describe classfile structures where appropriate,
 * especially the {@link ConstantRef} types that describe elements to be stored
 * in the constant pool.
 *
 * <p>The {@linkplain SymbolicRef} types are also used by the {@link Intrinsics}
 * API to express {@code ldc} and {@code invokedynamic} instructions.
 *
 * <p>Implementations of {@linkplain SymbolicRef} must be
 * <a href="../doc-files/ValueBased.html">value-based</a> classes.
 *
 * <p>Constants describing various useful symbolic references (such as {@link ClassRef}
 * constants for platform classes) can be found in {@link SymbolicRefs}.
 *
 * @apiNote In the future, if the Java language permits, {@linkplain SymbolicRef}
 * may become a {@code sealed} interface, which would prohibit subclassing except by
 * explicitly permitted types.
 * @see Constable
 * @see ConstantRef
 * @see Intrinsics
 * @see SymbolicRefs
 */
public interface SymbolicRef {

}
