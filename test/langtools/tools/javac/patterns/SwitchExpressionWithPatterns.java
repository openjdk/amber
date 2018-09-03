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

import java.util.List;
import java.util.function.ToIntFunction;

public class SwitchExpressionWithPatterns {

    public static void main(String[] args) {
        List<ToIntFunction<Object>> tests = List.of(
            SwitchExpressionWithPatterns::test1,
            SwitchExpressionWithPatterns::test2,
            SwitchExpressionWithPatterns::test3,
            SwitchExpressionWithPatterns::test4
        );
        for (ToIntFunction<Object> f : tests) {
            assertEquals(2, f.applyAsInt(41));
            assertEquals(1, f.applyAsInt((Integer) 42));
            assertEquals(3, f.applyAsInt((long) 0));
            assertEquals(3, f.applyAsInt((float) 0));
            assertEquals(4, f.applyAsInt((byte) 13));
        }
    }

    private static int test1(Object in) {
        int check = 0;
        return switch (in) {
            case 41: check++; //fall-through
            case Integer i: check++; break check;
            case Long l, Float f: break 3;
            default: break 4;
        };

    }

    private static int test2(Object in) {
        int check = 0;
        return switch (in) {
            case 41 -> 2;
            case Integer j -> { break 1; }
            case Long l, Float f -> 3;
            default -> { break 4; }
        };
    }

    private static int test3(Object in) {
        int check = 0;
        switch (in) {
            case 41: check++; //fall-through
            case Integer j: check++; break;
            case Long l, Float f: check = 3; break;
            default: check = 4; break;
        }
        return check;
    }

    private static int test4(Object in) {
        int check = 0;
        switch (in) {
            case 41 -> check = 2;
            case Integer j -> { check = 1; }
            case Long l, Float f -> check = 3;
            default -> { check = 4; }
        };
        return check;
    }

    private static void assertEquals(int expected, int actual) {
        if (expected != actual)
            throw new AssertionError("Expected: " + expected + ", actual: " + actual);
    }
}
