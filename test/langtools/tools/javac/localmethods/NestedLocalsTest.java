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
 * @bug 8233038
 * @summary Tests need to be written for local methods feature
 * @run main NestedLocalsTest
 */

public class NestedLocalsTest {
    int xxx(int x) {
        int xxx (int x1, int y) {
            int xxx (int x11, int y1, int z) {
                return x + x1 + x11 + y + y1 + z;
            }
            return xxx(x1, y, 20);
        }
        return xxx(x, 10);
    }
    public static void main(String [] args) {
       if (new NestedLocalsTest().xxx(10) != 70)
           throw new AssertionError("Broken");
    }
}

