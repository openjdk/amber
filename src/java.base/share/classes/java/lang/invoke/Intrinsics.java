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

/**
 * Intrinsics
 *
 * @author Brian Goetz
 */
public class Intrinsics {
    /**
     * Instructs the compiler to generate an {@code ldc} instruction for the given
     * {@code Constable} instance. A compiler error will be issued if it cannot
     * be proved that the argument is a constant
     * @param <T> the type to which this constant pool entry resolves
     * @param constant a constant to be ldc'ed
     * @return the constant wrapped inside the {@code Constable} object
     */
    public static <T> T ldc(ConstantRef<T> constant) {
        throw new UnsupportedOperationException("no reflective access");
    }

    /**
     * Instructs the compiler to generate an {@code invokedynamic} instruction given
     * a {@code BootstrapSpecifier} and arguments. The compiler should be able to
     * prove that the given {@code BootstrapSpecifier} is a constant, in other case
     * an error should be issued.
     * @param indy the bootstrap specifier
     * @param invocationName invocation name for the {@code invokedynamic}
     * @param args the arguments
     * @return the result of invoking the indy
     * @throws java.lang.Throwable the targeted method can throw any exception
     */
    public static Object invokedynamic(BootstrapSpecifier indy,
                                       String invocationName,
                                       Object... args)
            throws Throwable {
        throw new UnsupportedOperationException("no reflective access");
    }
}
