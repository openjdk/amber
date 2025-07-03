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

/**
 * @test
 * @enablePreview
 * @compile ExampleStringTest3.java
 * @run main ExampleStringTest3
 */

import java.util.Objects;

public class ExampleStringTest3 {
    public static pattern String ParsedInt(int datum) {
        try {
            int res = Integer.parseInt(that);
            match ParsedInt(res);
        } catch (NumberFormatException n) {
            match-fail();
        }
    }

    public static class Split {
        String delim;
        private Split(String delim) {
            this.delim = delim;
        }
        public static Split mkSplit(String delim) {
            return new Split(delim);
        }
        public case pattern String Split(String left, String right) {
            try{
                String[] parts = that.split(delim);
                match Split(parts[0], parts[1]);
            } catch (Exception _) {
                match-fail();
            }
        }
    }

    static int examineString(String s) {
        return switch (s) {
            case ExampleStringTest3.Split.mkSplit(":").Split(ExampleStringTest3.ParsedInt(int left), ExampleStringTest3.ParsedInt(int right)) -> left + right;
            case ExampleStringTest3.Split.mkSplit("-").Split(ExampleStringTest3.ParsedInt(int left), ExampleStringTest3.ParsedInt(int right)) -> left + right;
            default -> -1;
        };
    }

    public static void main(String[] args) {
        assertEquals(32, new ExampleStringTest3().examineString("12:20"));
        assertEquals(64, new ExampleStringTest3().examineString("24-40"));
    }

    private static <T> void assertEquals(T expected, T actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected: " + expected + ", but got: " + actual);
        }
    }
}
