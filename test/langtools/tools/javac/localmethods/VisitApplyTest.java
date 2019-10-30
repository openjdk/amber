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
 * @bug 8232849
 * @summary VerifyError: Local variable table overflow
 * @run main VisitApplyTest
 */

public class VisitApplyTest {
    static int x = 42;
    public static void main(String [] args) {
        int xxx (int y) {
            return x+= y;
        }
        int funnel(int y) {
            return xxx(y);
        }
        int funnel(int y, int y1) {
            return funnel(y + y1);
        }
        int funnel(int y, int y1, int y2, int y3) {
            return funnel(y + y1, y2 + y3);
        }
        if (funnel(58) != 100)
            throw new AssertionError("Broken");
        if (funnel(40,18) != 158)
            throw new AssertionError("Broken");
        if (funnel(18, 22, 13, 5) != 216)
            throw new AssertionError("Broken");
    }
}
