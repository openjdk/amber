/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @run main sealedTest
 */

public class sealedTest {

    sealed class Sealed1 permits Sub1 {}

    class Sub1 extends Sealed1 {}

    sealed interface SealedI1 permits Sub2 {}

    class Sub2 implements SealedI1 {}

    sealed class Sealed2 {}

    class Sub3 extends Sealed2 {}

    Sub1 sub1 = new Sub1();
    Sub2 sub2 = new Sub2();
    Sub3 sub3 = new Sub3();

    public static void main(String... args) {
        System.out.println("Basic testing of sealed types");
    }
}
