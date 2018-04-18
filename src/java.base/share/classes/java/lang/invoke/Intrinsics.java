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
package java.lang.invoke;

import java.lang.invoke.constant.ConstantDesc;
import java.lang.invoke.constant.DynamicCallSiteDesc;

/**
 * Intrinsics
 *
 * @author Brian Goetz
 */
public final class Intrinsics {
    /**
     * Instructs the compiler to generate an {@code ldc} instruction for the
     * constant described by the given {@link ConstantDesc}. A compile-time error
     * will be issued if this {@code ConstantRef} is not itself a constant.
     *
     * @implNote The implementation of this method always throws an
     * {@link UnsupportedOperationException} at runtime and therefore cannot be
     * called reflectively; invocations of this method should always be
     * intrinsified at compile time.
     *
     * @param <T> the type of the constant
     * @param constant a nominal descriptor for the constant to be loaded
     * @return the constant value
     */
    public static <T> T ldc(ConstantDesc<T> constant) {
        throw new UnsupportedOperationException("no reflective access");
    }

    /**
     * Instructs the compiler to generate an {@code invokedynamic} instruction given
     * nominal descriptions of a {@link DynamicCallSiteDesc} and a set of boostrap
     * arguments. A compile-time error will be issued if the {@link DynamicCallSiteDesc}
     * is not itself a constant.
     *
     * <p>Like {@link MethodHandle.PolymorphicSignature} methods such as
     * {@link MethodHandle#invoke(Object...)}, the signature of parameters and
     * return value of {@linkplain #invokedynamic(DynamicCallSiteDesc, Object...)}
     * is specified as {@linkplain Object}, but the invocation type of the
     * {@code invokedynamic} instruction will be derived from the {@code callSiteRef}
     * and will not necessarily requiring boxing of arguments and return value.
     *
     * @implNote The implementation of this method always throws an
     * {@link UnsupportedOperationException} at runtime and therefore cannot be
     * called reflectively; invocations of this method should always be
     * intrinsified at compile time.
     *
     * @param callSiteRef a nominal descriptor for a dynamic call site
     * @param args the dynamic arguments to the {@code invokedynamic} instruction
     * @return the result of the invocation
     * @throws java.lang.Throwable the targeted method throws any exception
     */
    public static Object invokedynamic(DynamicCallSiteDesc callSiteRef,
                                       Object... args)
            throws Throwable {
        throw new UnsupportedOperationException("no reflective access");
    }
}
