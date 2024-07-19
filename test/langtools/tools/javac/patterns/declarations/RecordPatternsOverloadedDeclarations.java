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
 * @compile -parameters RecordPatternsOverloadedDeclarations.java
 * @run main RecordPatternsOverloadedDeclarations
 */
import java.util.Objects;

public class RecordPatternsOverloadedDeclarations {
    public static void main(String... args) {
        assertEquals("A:B",  test1A(new Person1("A", "B", false)));
        assertEquals("A",    test1B(new Person1("A", "B", false)));
        assertEquals("Duke", test2(new Person1("Duke", "Java", false)));
        assertEquals("DUKE", test2(new Person1("Duke", "Java", true)));
        assertEquals("Duke", test3(new Person1("Duke", "Java", true)));
    }

    private static String test1A(Object o) {
        if (o instanceof Person1(String name, String username)) {
            return name + ":" + username;
        }
        return null;
    }

    private static String test1B(Object o) {
        if (o instanceof Person1(String name)) {
            return name;
        }
        return null;
    }

    private static String test2(Object o) {
        if (o instanceof Person1(String name)) {
            return name;
        }
        return null;
    }

    private static String test3(Object o) {
        if (o instanceof Person1(String name, String username, boolean capitalize)) {
            return name;
        }
        return null;
    }

    public static record Person1(String name, String username, boolean capitalize) {

        public Person1(String name) {
            this(name, "default", false);
        }

        public pattern Person1(String name, String username) {
             match Person1(this.name, this.username);
        }

        public pattern Person1(String name) {
            if (capitalize) {
                match Person1(this.name.toUpperCase());
            } else {
                match Person1(this.name);
            }
        }

        public pattern Person1(int name) {
            match Person1(42);
        }

        public pattern Person1(int[] t) {
            match Person1(new int[]{1, 2, 3});
        }
    }

    private static <T> void assertEquals(T expected, T actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected: " + expected + ", but got: " + actual);
        }
    }
}
