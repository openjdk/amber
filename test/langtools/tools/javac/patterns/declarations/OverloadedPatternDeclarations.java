/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Objects;

/**
 * @test
 * @enablePreview
 * @compile OverloadedPatternDeclarations.java
 * @run main OverloadedPatternDeclarations
 */
public class OverloadedPatternDeclarations {
    public static void main(String... args) {
        assertEquals( 2, test1(new D()));
        assertEquals( 1, test2(new D()));
        assertEquals( 1, test3(new D()));
        assertEquals( 3, test4(new D()));
        assertEquals( 4, test5(new D()));
    }

    private static int test1(D o) {
        if (o instanceof D(String data, Integer outI)) {
            return outI;
        }
        return -1;
    }

    private static int test2(D o) {
        if (o instanceof D(Object data, Integer outI)) {
            return outI;
        }
        return -1;
    }

    private static int test3(D o) {
        if (o instanceof D(Integer data, Integer outI)) {
            return outI;
        }
        return -1;
    }

    private static int test4(D o) {
        if (o instanceof D(A data, Integer outI)) {
            return outI;
        }
        return -1;
    }

    private static Integer test5(D o) {
        if (o instanceof D(B data, Integer outI)) {
            return outI;
        }
        return null;
    }

    static class A {}
    static class B extends A {}

    public record D() {
        public pattern D(Object out, Integer outI) {
            match D(42, 1);
        }

        public pattern D(String out, Integer outI) {
            match D("2", 2);
        }

        public pattern D(A out, Integer outI) {
            match D(new A(), 3);
        }

        public pattern D(B out, Integer outI) {
            match D(new B(), 4);
        }
    }

    private static void assertEquals(int expected, int actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected: " + expected + ", but got: " + actual);
        }
    }
}
