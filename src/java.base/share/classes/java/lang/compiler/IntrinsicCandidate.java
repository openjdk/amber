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
package java.lang.compiler;

import java.lang.annotation.*;

/**
 * Indicates an <i>intrinsic candidate</i>: a method whose invocation may be <i>intrinsified</i> by the compiler in a
 * behaviorally-compatible way. Intrinsification generally involves the compiler recognizing when constant arguments
 * are passed to a passed to a variable-arity method, where the overhead of boxing is significant. In some cases,
 * the compiler can fold the entire invocation into a constant at compile time. In other cases, the compiler can generate
 * bytecode that invokes a specialized method (based on the constant arguments) at run time.
 * <p>
 * The compiler is free to optimize a given invocation in source code differently in each
 * compilation, and to optimize adjacent invocations of the same intrinsic candidate in source code in different ways.
 * <p>
 * This annotation may only be applied to {@code public} methods in classes of the {@code java.base} module.
 * Applying it to other methods is not an error, but has no effect.
 *
 * @apiNote
 * This type's retention policy ensures that annotations of this type are not available through the reflection API.
 * This prevents clients from identifying intrinsic candidates and thus prevents assumptions about the treatment of
 * intrinsic candidates by a Java compiler. Being an intrinsic candidate is never part of a method's specification.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD })
public @interface IntrinsicCandidate {
}
