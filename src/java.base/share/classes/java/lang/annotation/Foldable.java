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
package java.lang.annotation;

import java.lang.sym.Constable;
import java.lang.sym.SymbolicRef;
import java.lang.invoke.Intrinsics;

/**
 * Identifies a method that is a candidate for compile-time constant folding.
 * Such a method must be a <em>pure function</em> of its inputs, all inputs
 * (including the receiver, if applied to an instance method) must be value-based
 * types, and the output must be a value-based type that is representable
 * in the constant pool ({@link Constable} or {@link SymbolicRef}).
 *
 * <p>For accesses of fields annotated as {@linkplain Foldable}, and invocations
 * of methods annotated as {@linkplain Foldable} whose arguments (and, for instance
 * methods, the receiver) are all constant expressions, the compiler may evaluate
 * the expression reflectively at compile time and replace it with a constant
 * load of the result, or track the result as a constant expression for possible
 * intrinsification via methods in {@link Intrinsics}.
 *
 * @see Constable
 * @see SymbolicRef
 * @see Intrinsics
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD, ElementType.FIELD })
public @interface Foldable { }
