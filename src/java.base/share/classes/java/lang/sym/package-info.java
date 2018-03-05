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
 * Classes and interfaces to represent <em>nominal references</em> to run-time
 * entities such as classes or method handles and classfile entities such as
 * constant pool entries or {@code invokedynamic} call sites.  These classes
 * are suitable for use in bytecode reading and writing APIs, {@code invokedynamic}
 * bootstraps, bytecode intrinsic APIs (such as {@link java.lang.invoke.Intrinsics#ldc(java.lang.sym.ConstantRef)}),
 * or compile- and link-time program analysis.
 *
 * <p>Every API that reads and writes bytecode instructions needs to model the
 * operands to these instructions, or other classfile structures such as entries
 * in the bootstrap methods table or stack maps, which frequently correspond to
 * entries in the classfile constant pool. Such entries can denote values of
 * fundamental types, such as strings or integers; parts of a program, such as
 * classes or method handles; or values of arbitrary user-defined types.  The
 * {@link java.lang.sym.ConstantRef} hierarchy provides a representation of
 * constant pool entries in nominal form that is convenient for APIs to model
 * operands of bytecode instructions.
 *
 * <p>A {@link java.lang.sym.ConstantRef} is a description of a constant
 * value.  Such a description is the <em>nominal form</em> of the constant value;
 * it is not hte value itself, but rather a "recipe" for storing the value in
 * a constant pool entry, or reconstituting the value given a class loading
 * context.  Every {@link java.lang.sym.ConstantRef} knows how to <em>resolve</em>
 * itself -- compute the value that it describes -- via the
 * {@link java.lang.sym.ConstantRef#resolveConstantRef(java.lang.invoke.MethodHandles.Lookup)}
 * method.  This allows an API which accepts {@link java.lang.sym.ConstantRef}
 * objects to evaluate them reflectively, provided that the classes and methods
 * referenced in their nominal description are present and accessible.
 *
 * <p>The subtypes of of {@link java.lang.sym.ConstantRef} describe various kinds
 * of constant values.  For each type of constant pool entry defined in JVMS 4.4,
 * there is a corresponding subtype of {@link java.lang.sym.ConstantRef:
 * {@code ConstantClassRef}, {@code ConstantMethodTypeRef},
 * {@code ConstantMethodHandleRef}, {@code String}, {@code Integer}, {@code Long},
 * {@code Float}, {@code Double}, and {@code DynamicConstantRef}.  These classes
 * provides accessor methods to extrac the information needed to store a value
 * in the constant pool.  When a bytecode-writing API encounters a {@link java.lang.sym.ConstantRef},
 * it should case over these types to represent it in the constant pool.
 * When a bytecode-reading API encounters a constant pool entry, it can
 * convert it to the appropriate type of nominal reference.  For dynamic
 * constants, bytecode-reading APIs may wish to use the factory
 * {@link java.lang.sym.DynamicConstantRef#ofCanonical(java.lang.sym.MethodHandleRef, java.lang.String, java.lang.sym.ClassRef, java.lang.sym.ConstantRef[])},
 * which will inspect the bootstrap and, for well-known bootstraps, instantiate
 * a more specific subtype of {@link java.lang.sym.DynamicConstantRef}, such as
 * {@link java.lang.sym.EnumRef}.
 *
 * <p>Another way to obtain the nominal description of a value is to ask the value
 * itself.  A {@link java.lang.sym.Constable} is a type whose values can describe
 * themselves in nominal form as a {@link java.lang.sym.ConstantRef}.  Fundamental
 * types such as {@link java.lang.String} and {@link java.lang.Class} implement
 * {@link java.lang.sym.Constable}, and every instance of these classes can
 * provide itself in nominal form.  User-defined classes can also implement
 * {@link java.lang.sym.Constable}.
 *
 * <p>This package also includes the {@link java.lang.sym.DynamicCallSiteRef}
 * class, which is a representation of a constant pool entry that is never an
 * argument to bytecode instructions.  Instead, it is a description of the
 * bootstrap method, invocation name and type, and static arguments associated
 * with an {@code invokedynamic} instruction, and is used by the
 * {@link java.lang.invoke.Intrinsics#invokedynamic(java.lang.sym.DynamicCallSiteRef, java.lang.Object...)}
 * API for expressing {@code invokedynamic} call sites in Java source files.
 *
 */
package java.lang.sym;

