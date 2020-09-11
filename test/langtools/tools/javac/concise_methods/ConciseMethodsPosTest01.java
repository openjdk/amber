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

/*
 * @test
 * @summary smoke test for concise methods
 * @modules jdk.compiler/com.sun.tools.javac.util
 */

import com.sun.tools.javac.util.Assert;

public class ConciseMethodsPosTest01 {
    String hello = "hello";

    int length(String s) -> s.length();
    void printLength(String s) -> System.out.println(s.length());

    int length2(String s) = String::length;

    String dowahoo(int x) = this::wahoo;

    String wahoo(int x) {
        return "wahoo " + x;
    }

    static ConciseMethodsPosTest01 make() = ConciseMethodsPosTest01::new;

    class TT {
        int i;
        public TT(int i) {
            this.i = i;
        }
    }

    TT makeTT(int i) = TT::new;

    int[] intArray(int i) = int[]::new;

    public static void main(String... args) {
        ConciseMethodsPosTest01 t = new ConciseMethodsPosTest01();
        Assert.check(t.length("hey") == 3);
        Assert.check(t.length2("hey") == 3);

        Assert.check(t.dowahoo(1).equals("wahoo 1"));

        ConciseMethodsPosTest01 t2 = ConciseMethodsPosTest01.make();
        Assert.check(t2.hello.equals("hello"));

        TT tt = t.makeTT(5);
        Assert.check(tt.i == 5);

        int[] arr = t.intArray(2);
        for (int i = 0; i < 2; i++) {
            arr[i] = i;
            Assert.check(arr[i] == i);
        }
    }
}
