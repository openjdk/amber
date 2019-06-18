/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @summary testing the canonical constructor for records
 * @modules jdk.compiler/com.sun.tools.javac.util
 */

import com.sun.tools.javac.util.Assert;

public class CanonicalConstructorTest {
    record R1(int i, int j);
    record R2(int i, int j) {
        public R2 {}
    }
    record R3(int i, int j) {
        public R3 {
            this.i = i;
        }
    }
    record R4(int i, int j) {
        public R4 {
            this.i = i;
            this.j = j;
        }
    }

    public static void main(String... args) {
        R1 r1 = new R1(1, 2);
        R2 r2 = new R2(1, 2);
        R3 r3 = new R3(1, 2);
        R4 r4 = new R4(1, 2);

        Assert.check(r1.i == r2.i && r2.i == r3.i && r3.i == r4.i && r4.i == 1 && r1.j == r2.j && r2.j == r3.j && r3.j == r4.j && r4.j == 2, "unexpected value of record component");
    }
}
