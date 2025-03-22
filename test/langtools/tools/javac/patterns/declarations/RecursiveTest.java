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
 * @compile -parameters RecursiveTest.java
 * @run main RecursiveTest
 */
public class RecursiveTest {

    public static void main(String... args) {
        assertEquals(4, testList(List.of("This", "is", "the", "way")));
    }

    static int testList(List<String> lst) {
        return switch(lst) {
            case List(int size) -> size;
        };
    }

    static class List<A> {
        public A head;
        public List<A> tail;
        public List(A head, List<A> tail) {
            this.tail = tail;
            this.head = head;
        }

        public List() {
            this(null, null);
        }

        pattern List(int size) {
            match List(switch (tail) {
                case List(int inner) -> inner + 1;
                case null -> 0;
            });
        }

        public static <A> List<A> of(A x1, A x2, A x3, A x4) {
            return new List<A>(x1, new List<A>(x2, new List<A>(x3, new List<A>(x4, new List<A>()))));
        }
    }

    private static <T> void assertEquals(T expected, T actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected: " + expected + ", but got: " + actual);
        }
    }
}