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

/**
 * Represent <em>symbolic references</em> to run-time entities such as strings,
 * classes, and method handles, and classfile entities such as constant pool
 * entries and {@code invokedynamic} call sites.
 *
 * <p>The bytecode instructions of a class refer to classes, fields, methods,
 * and other values <em>symbolically</em>.  Namely, the operands of bytecode
 * instructions are <em>symbolic references</em> to classes, fields, methods,
 * and other values. The JVM resolves symbolic references stored in the constant
 * pool of a classfile them in order to link the class, as per JVMS 5.4.3.
 *
 * <p>Symbolic references are purely nominal; like names in the constant pool of
 * a classfile, names of classes in a symbolic reference are independent of a
 * class loader.  Symbolic references may be to basic values such as integers
 * and strings; these values are stored in the constant pool directly. Symbolic
 * references may also be to entities that denote parts of a program, such as
 * classes and method handles. Finally, symbolic references may be to constant
 * values that are computed at run time by user code.  Symbolic references for
 * loadable constants are represented as subtypes of {@link java.lang.sym.ConstantRef}.
 * The {@link java.lang.invoke.Intrinsics} API uses symbolic references to express
 * {@code ldc} and {@code invokedynamic} instructions.  Symbolic reference classes
 * must be <a href="../doc-files/ValueBased.html">value-based</a>.
 *
 * <p>APIs that generate or parse bytecode instructions can use the types in
 * this package to represent symbolic references, for example, as the operand of
 * an {@code ldc} instruction (such as a dynamically-computed constant), the
 * static arguments to the bootstrap of a dynamically-computed constant,
 * the static arguments to the bootstrap of an {@code invokedynamic} instruction,
 * or any other bytecode instructions or class file structure that make use of
 * the constant pool.
 *
 * <p>When a bytecode-generating API encounters a symbolic reference which it
 * wants to write to the constant pool, it can case over the types corresponding
 * to constant pool forms defined in JVMS 4.4: {@code ConstantClassRef},
 * {@code ConstantMethodTypeRef}, {@code ConstantMethodHandleRef}, {@code String},
 * {@code Integer}, {@code Long}, {@code Float}, {@code Double}, and
 * {@code DynamicConstantRef}, and then call the accessor methods defined on
 * these types to extract the symbolic information needed to represent these
 * forms in the constant pool.
 *
 * <p>When a bytecode-reading API encounters a constant pool entry, it can
 * convert it to the appropriate type of symbolic reference.  For dynamic
 * constants, bytecode-reading APIs may wish to use the factory
 * {@link java.lang.sym.DynamicConstantRef#ofCanonical(java.lang.sym.MethodHandleRef, java.lang.String, java.lang.sym.ClassRef, java.lang.sym.ConstantRef[])},
 * which will inspect the bootstrap and, for well-known bootstraps, instantiate
 * a more specific subtype of {@link java.lang.sym.DynamicConstantRef}, such as
 * {@link java.lang.sym.EnumRef}.
 *
 */
package java.lang.sym;

