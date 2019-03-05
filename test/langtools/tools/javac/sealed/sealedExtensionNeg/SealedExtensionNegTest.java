/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 * @test
 * @summary check that sealed cant be extended by non-permitted subtypes
 * @compile -XDdontErrorIfSealedExtended SealedExtensionNegTest.java
 * @run main SealedExtensionNegTest
 */

public class SealedExtensionNegTest {
    final interface SealedI permits SubI1 {}
    class SubI1 implements SealedI {}
    class SubI2 implements SealedI {}
    interface I1 extends SealedI {}

    final class Sealed permits Sub1 {}
    class Sub1 extends Sealed {}
    class Sub2 extends Sealed {}

    public static void main(String... args) {
        Class<?> subClass1 = SubI1.class;  // ok

        try {
            Class<?> subClass2 = SubI2.class;
            throw new AssertionError("error expected");
        } catch (VerifyError ve) {
            // good
        }

        try {
            Class<?> i1 = I1.class;
            throw new AssertionError("error expected");
        } catch (VerifyError ve) {
            // good
        }

        Class<?> sub1 = Sub1.class;  // ok

        try {
            Class<?> subClass2 = Sub2.class;
            throw new AssertionError("error expected");
        } catch (VerifyError ve) {
            // good
        }
    }
}
