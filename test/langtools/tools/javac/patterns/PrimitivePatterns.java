/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;

/**
 * @test
 * @summary Check behavior of instanceof for primitives
 * @compile -g --enable-preview -source ${jdk.version} PrimitivePatterns.java
 * @run main/othervm --enable-preview PrimitivePatterns
 */
public class PrimitivePatterns {
    public static void main(String[] args) {
        assertEquals(42, primitivePattern());
        assertEquals(1,  primitiveSwitch());
        assertEquals(42, primitiveSwitch2());
        assertEquals(42, primitiveSwitch3());
        assertEquals(42, exhaustive0());
        assertEquals(1,  exhaustive1_byte());
        assertEquals(2,  exhaustive1_short());
        assertEquals(1,  real_exhaustive2());
        assertEquals(1,  real_exhaustive3());
        assertEquals(1,  real_exhaustive4());
        assertEquals(1,  real_exhaustive4_byte());
        assertEquals(2,  real_exhaustive4_double());
        assertEquals(1, exhaustive5());
        assertEquals(1, exhaustiveWithRecords1());
        assertEquals(1, exhaustiveWithRecords2());
        assertEquals(1, exhaustiveWithRecords4());
        assertEquals(1, exhaustiveWithRecords5());
        assertEquals(1, exhaustiveWithRecords6());
        assertEquals(2, ensureProperSelectionWithRecords());
        assertEquals(42, switchAndDowncastFromObjectPrimitive());
        assertEquals(42, dominationBetweenBoxedAndPrimitive());
        assertEquals(2, wideningAndUnboxing());
        assertEquals(2, wideningAndUnboxingInRecord());
        assertEquals(2, wideningAndInferredUnboxingInRecord());
        assertEquals(3, inferredUnboxingInRecordInEnhancedFor());
    }

    public static int primitivePattern() {
        int i = 42;
        if (i instanceof int p) {
            return p;
        }
        return -1;
    }

    public static int primitiveSwitch() {
        int i = 42;
        return switch (i) {
            case int j -> 1;
        };
    }

    public static int primitiveSwitch2() {
        Object o = Integer.valueOf(42);
        switch (o) {
            case int i: return i;
            default: break;
        }
        return -1;
    }

    public static int primitiveSwitch3() {
        int i = 42;
        switch (i) {
            case Integer ii: return ii;
        }
    }

    public static int exhaustive0() {
        Integer i = 42;
        switch (i) {
            case int j: return j;
        }
    }

    public static int exhaustive1_byte() {
        int i = 42;
        return switch (i) {
            case byte  b -> 1;
            default -> 2;
        };
    }

    public static int exhaustive1_short() {
        int i = 30000;
        return switch (i) {
            case byte  b -> 1;
            case short s -> 2;
            default -> 3;
        };
    }

    public static int real_exhaustive2() {
        int i = 42;
        return switch (i) {
            case Integer p -> 1;
        };
    }

    public static int real_exhaustive3() {
        int i = 42;
        return switch (i) {
            case long d -> 1;
        };
    }

    public static int real_exhaustive4() {
        int i = 42;
        return switch (i) {
            case double d -> 1;
        };
    }

    public static int real_exhaustive4_byte() {
        int i = 127;
        return switch (i) {
            case byte b -> 1;
            case double d -> 2;
        };
    }

    public static int real_exhaustive4_double() {
        int i = 127 + 1;
        return switch (i) {
            case byte b -> 1;
            case double d -> 2;
        };
    }

    public static int exhaustive5() {
        Integer i = Integer.valueOf(42);
        return switch (i) {
            case int p -> 1;
        };
    }

    public static int exhaustiveWithRecords1() {
        R_int r = new R_int(42);
        return switch (r) {
            // exhaustive, because Integer exhaustive at type int
            case R_int(Integer x) -> 1;
        };
    }

    public static int exhaustiveWithRecords2() {
        R_int r = new R_int(42);
        return switch (r) {
            // exhaustive, because double unconditional at int
            case R_int(double x) -> 1;
        };
    }

    public static int exhaustiveWithRecords4() {
        R_Integer r = new R_Integer(42);
        return switch (r) {
            // exhaustive, because R_Integer(int) exhaustive at type R_Integer(Integer), because int exhaustive at type Integer
            case R_Integer(int x) -> 1;
        };
    }

    public static int exhaustiveWithRecords5() {
        R_Integer r = new R_Integer(42);
        return switch (r) {
            // exhaustive, because double exhaustive at Integer
            case R_Integer(double x) -> 1;
        };
    }

    public static int exhaustiveWithRecords6() {
        R_int r = new R_int(42);
        return switch (r) {
            case R_int(byte x) -> 1;
            case R_int(int x) -> 2;
        };
    }

    public static int ensureProperSelectionWithRecords() {
        R_int r = new R_int(4242);
        return switch (r) {
            case R_int(byte x) -> 1;
            case R_int(int x) -> 2;
        };
    }

    public static int switchAndDowncastFromObjectPrimitive() {
        Object i = 42;
        return switch (i) {
            case Integer ib  -> ib;
            default -> -1;
        };
    }

    public static int dominationBetweenBoxedAndPrimitive() {
        Object i = 42;
        return switch (i) {
            case Integer ib  -> ib;
            case byte ip     -> ip;
            default -> -1;
        };
    }

    static int wideningAndUnboxing() {
        Number o = Integer.valueOf(42);
        return switch (o) {
            case byte b -> 1;
            case int i -> 2;
            case float f -> 3;
            default -> 4;
        };
    }

    static int wideningAndUnboxingInRecord() {
        Box<Number> box = new Box<>(Integer.valueOf(42));
        return switch (box) {
            case Box<Number>(byte b) -> 1;
            case Box<Number>(int i) -> 2;
            case Box<Number>(float f) -> 3;
            default -> 4;
        };
    }

    static int wideningAndInferredUnboxingInRecord() {
        Box<Number> box = new Box<>(Integer.valueOf(42));
        return switch (box) {
            case Box(byte b) -> 1;
            case Box(int i) -> 2;
            case Box(float f) -> 3;
            default -> 4;
        };
    }

    static int inferredUnboxingInRecordInEnhancedFor() {
        List<Box<Integer>> numbers = List.of(new Box<>(1), new Box<>(2));

        int acc = 0;
        for(Box(long b) : numbers) {
            acc += b;
        }

        return acc;
    }

    record R_Integer(Integer x) {}
    record R_int(int x) {}
    record Box<N extends Number>(N num) {}

    static void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("Expected: " + expected + ", actual: " + actual);
        }
    }
}
