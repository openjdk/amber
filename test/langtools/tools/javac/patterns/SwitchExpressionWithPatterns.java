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
 * @summary Basic test for patterns in switch expression
 * @compile --enable-preview -source 12 SwitchExpressionWithPatterns.java
 * @run main/othervm --enable-preview SwitchExpressionWithPatterns
 */
public class SwitchExpressionWithPatterns {

    public static void main(String[] args) {
        assertEquals(1, test((Integer) 42));
        assertEquals(2, test(41));
        assertEquals(-2, test((long) 0));
        assertEquals(-2, test((float) 0));
        assertEquals(-1, test((byte) 13));
    }

    private static int test(Object in) {
        int check = 0;
        return switch (in) {
            case 41: check++; //fall-through
            case Integer j: check++; break check;
            case Long l, Float f: break -2;
            default: break -1;
        };

    }

    private static void assertEquals(int expected, int actual) {
        if (expected != actual)
            throw new AssertionError("Expected: " + expected + ", actual: " + actual);
    }
}
