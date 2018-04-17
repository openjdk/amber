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
 * Classes and interfaces to represent <em>nominal descriptors</em> for run-time
 * entities such as classes or method handles, and classfile entities such as
 * constant pool entries or {@code invokedynamic} call sites.  These classes
 * are suitable for use in bytecode reading and writing APIs, {@code invokedynamic}
 * bootstraps, bytecode intrinsic APIs (such as {@link java.lang.invoke.Intrinsics#ldc(java.lang.invoke.constant.ConstantRef)}),
 * and compile-time or link-time program analysis tools.
 *
 * <p>Every API that reads and writes bytecode instructions needs to model the
 * operands to these instructions and other classfile structures (such as entries
 * in the bootstrap methods table or stack maps, which frequently reference
 * entries in the classfile constant pool.) Such entries can denote values of
 * fundamental types, such as strings or integers; parts of a program, such as
 * classes or method handles; or values of arbitrary user-defined types.  The
 * {@link java.lang.invoke.constant.ConstantRef} hierarchy provides a representation of
 * constant pool entries in nominal form that is convenient for APIs to model
 * operands of bytecode instructions.
 *
 * <p>A {@link java.lang.invoke.constant.ConstantRef} is a description of a constant
 * value.  Such a description is the <em>nominal form</em> of the constant value;
 * it is not the value itself, but rather a "recipe" for storing the value in
 * a constant pool entry, or reconstituting the value given a class loading
 * context.  Every {@link java.lang.invoke.constant.ConstantRef} knows how to <em>resolve</em>
 * itself -- compute the value that it describes -- via the
 * {@link java.lang.invoke.constant.ConstantRef#resolveConstantRef(java.lang.invoke.MethodHandles.Lookup)}
 * method.  This allows an API which accepts {@link java.lang.invoke.constant.ConstantRef}
 * objects to evaluate them reflectively, provided that the classes and methods
 * referenced in their nominal description are present and accessible.
 *
 * <p>The subtypes of of {@link java.lang.invoke.constant.ConstantRef} describe various kinds
 * of constant values.  For each type of loadable constant pool entry defined in JVMS 4.4,
 * there is a corresponding subtype of {@link java.lang.invoke.constant.ConstantRef}:
 * {@code ConstantClassRef}, {@code ConstantMethodTypeRef},
 * {@code ConstantMethodHandleRef}, {@code String}, {@code Integer}, {@code Long},
 * {@code Float}, {@code Double}, and {@code DynamicConstantRef}.  These classes
 * provides type-specific accessor methods to extract the nominal information for
 * that kind of constant.  When a bytecode-writing API encounters a {@link java.lang.invoke.constant.ConstantRef},
 * it should examine it to see which of these types it is, cast it, extract
 * its nominal information, and generate the corresponding entry to the constant pool.
 * When a bytecode-reading API encounters a constant pool entry, it can
 * convert it to the appropriate type of nominal descriptor.  For dynamic
 * constants, bytecode-reading APIs may wish to use the factory
 * {@link java.lang.invoke.constant.DynamicConstantRef#ofCanonical(java.lang.invoke.constant.ConstantMethodHandleRef, java.lang.String, java.lang.invoke.constant.ClassRef, java.lang.invoke.constant.ConstantRef[])},
 * which will inspect the bootstrap and, for well-known bootstraps, return
 * a more specific subtype of {@link java.lang.invoke.constant.DynamicConstantRef}, such as
 * {@link java.lang.invoke.constant.EnumRef}.
 *
 * <p>Another way to obtain the nominal description of a value is to ask the value
 * itself.  A {@link java.lang.invoke.constant.Constable} is a type whose values
 * can describe themselves in nominal form as a {@link java.lang.invoke.constant.ConstantRef}.
 * Fundamental types such as {@link java.lang.String} and {@link java.lang.Class}
 * implement {@link java.lang.invoke.constant.Constable}, as can user-defined
 * classes.  Entities that generate classfiles (such as compilers) can introspect
 * over constable objects to obtain a more efficient way to represent their values
 * in classfiles.
 *
 * <p>This package also includes {@link java.lang.invoke.constant.DynamicCallSiteRef},
 * which represents a (non-loadable) {@code Constant_InvokeDynamic_info} constant
 * pool entry.  It describes the bootstrap method, invocation name and type,
 * and bootstrap arguments associated with an {@code invokedynamic} instruction.
 * It is also suitable for describing {@code invokedynamic} call sites in bytecode
 * reading and writing APIs, and and is used by the
 * {@link java.lang.invoke.Intrinsics#invokedynamic(java.lang.invoke.constant.DynamicCallSiteRef, java.lang.Object...)}
 * API for expressing {@code invokedynamic} call sites in Java source files.
 *
 */
package java.lang.invoke.constant;

