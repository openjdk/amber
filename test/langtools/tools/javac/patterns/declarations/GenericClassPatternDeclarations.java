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
 * @compile -parameters GenericClassPatternDeclarations.java
 * @run main GenericClassPatternDeclarations
 */
import java.util.Objects;
import java.util.List;

public class GenericClassPatternDeclarations {
    public static void main(String... args) {
        Box<Integer> l = new Box<>();

        switch (l) {
            case Box(GBox<Integer> ll) -> {  }
            default -> {}
        }
    }

    public static class Box<T extends Integer> {
        public pattern Box(GBox<T> o) {
            match Box(new GBox<T>());
        }
    }

    static class GBox<T> {}
    private static <T> void assertEquals(T expected, T actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected: " + expected + ", but got: " + actual);
        }
    }
}
