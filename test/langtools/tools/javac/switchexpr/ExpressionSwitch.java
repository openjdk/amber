/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @compile ExpressionSwitch.java
 * @run main ExpressionSwitch
 */

import java.util.Objects;

public class ExpressionSwitch {
    public static void main(String... args) {
        new ExpressionSwitch().run();
    }

    private void run() {
        check(null, "NULL");
        check(T.A, "A");
        check(T.B, "B");
        check(T.C, "other");
        exhaustive1(T.C);
        exhaustive2(null);
    }

    private String print(T t) {
        return switch (t) {
            case null -> "NULL";
            case A -> "A";
            case B -> "B";
            default: break "other";
        };
    }

    private String exhaustive1(T t) {
        return switch (t) {
            case A -> "A";
            case B -> "B";
            case C -> "C";
            case D: break "D";
        };
    }

    private String exhaustive2(T t) {
        return switch (t) {
            case null -> "NULL";
            case A -> "A";
            case B -> "B";
            case C -> "C";
            case D: break "D";
        };
    }

    private void check(T t, String expected) {
        String result = print(t);
        if (!Objects.equals(result, expected)) {
            throw new AssertionError("Unexpected result: " + result);
        }
    }

    enum T {
        A, B, C, D;
    }
}
