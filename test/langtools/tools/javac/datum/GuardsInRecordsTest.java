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

/*
 * @test
 * @summary test for guards in records
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.util
 * @run main GuardsInRecordsTest
 */

public class GuardsInRecordsTest {
    static record Range1(int lo, int hi) where lo <= hi;
    static record Range2(int lo, int hi) where lo <= hi {}
    static record Range3(int lo, int hi) where lo <= hi {
        Range3(int lo, int hi) {
            default(lo, hi);
        }
    }

    public static void main(String... args) {
        try {
            Range1 r = new Range1(2, 1);
            throw new AssertionError("an exception was expected for Range1");
        } catch (IllegalArgumentException iae) {}

        try {
            Range2 r = new Range2(2, 1);
            throw new AssertionError("an exception was expected for Range2");
        } catch (IllegalArgumentException iae) {}

        try {
            Range3 r = new Range3(2, 1);
            throw new AssertionError("an exception was expected for Range3");
        } catch (IllegalArgumentException iae) {}
    }
}
