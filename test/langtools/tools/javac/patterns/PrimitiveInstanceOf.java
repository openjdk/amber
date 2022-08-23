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

/**
 * @test
 * @summary Check behavior of instanceof for primitives
 * @compile -g --enable-preview -source ${jdk.version} PrimitiveInstanceOf.java
 * @run main/othervm --enable-preview PrimitiveInstanceOf
 */
public class PrimitiveInstanceOf {

    public static void main(String[] args) {
        assertEquals(true, identity_primitive_conversion());
        assertEquals(true, widening_primitive_conversion());
        assertEquals(true, narrowing_primitive_conversion());
        assertEquals(true, widening_and_narrowing_primitive_conversion());
        assertEquals(true, boxing_conversion());
        assertEquals(true, boxing_and_widening_reference_conversion());

        assertEquals(true, unboxing());
        assertEquals(true, unboxing_with_object());
        assertEquals(true, unboxing_and_convertible(42));
        assertEquals(true, unboxing_and_widening_primitive_exact());
        assertEquals(false, unboxing_and_widening_primitive_not_exact());
        assertEquals(true, unboxing_when_null_and_widening_primitive());
        assertEquals(true, narrowing_and_unboxing());

        assertEquals(true, pattern_ExtractRecordComponent());
        assertEquals(true, expr_method());
        assertEquals(true, expr_staticallyQualified());
    }

    public static boolean identity_primitive_conversion() {
        int i = 42;
        return i instanceof int;
    }

    public static boolean widening_primitive_conversion() {
        byte b = (byte) 42;
        short s = (short) 42;
        char c = 'a';

        return b instanceof int && s instanceof int && c instanceof int;
    }

    public static boolean narrowing_primitive_conversion() {
        long l_within_int_range = 42L;
        long l_outside_int_range = 999999999999999999L;

        return l_within_int_range instanceof int && !(l_outside_int_range instanceof int);
    }

    public static boolean widening_and_narrowing_primitive_conversion() { // TODO
        byte b = (byte) 42;
        byte b2 = (byte) -42;
        char c = (char) 42;
        return b instanceof char && c instanceof byte && !(b2 instanceof char);
    }

    public static boolean boxing_conversion() {
        int i = 42;

        return i instanceof Integer;
    }

    public static boolean boxing_and_widening_reference_conversion() {
        int i = 42;
        return i instanceof Object &&
                i instanceof Number &&
                i instanceof Comparable;
    }

    public static boolean unboxing() {
        Integer i = Integer.valueOf(1);
        return i instanceof int;
    }

    public static boolean unboxing_with_object() {
        Object o1 = (int) 42;
        Object o2 = (byte) 42;

        return o1 instanceof int i1 &&
                o2 instanceof byte b1 &&
                !(o1 instanceof byte b2 &&
                !(o2 instanceof int i2));
    }

    public static <T extends Integer> boolean unboxing_and_convertible(T i) {
        return i instanceof int;
    }

    public static boolean unboxing_and_widening_primitive_exact() {
        Byte b = Byte.valueOf((byte)42);
        Short s = Short.valueOf((short)42);
        Character c = Character.valueOf('a');

        return (b instanceof int) && (s instanceof int) && (c instanceof int);
    }

    public static boolean unboxing_and_widening_primitive_not_exact() {
        int smallestIntNotRepresentable = 16777217; // 2^24 + 1
        Integer i = Integer.valueOf(smallestIntNotRepresentable);

        return i instanceof float;
    }

    public static boolean unboxing_when_null_and_widening_primitive() {
        Byte b = null;
        Short s = null;
        Character c = null;

        return !(b instanceof int) && !(s instanceof int) && !(c instanceof int);
    }

    public static boolean narrowing_and_unboxing() {
        Number n = Byte.valueOf((byte) 42);

        return n instanceof byte;
    }

    public record P(int i) { }
    public static boolean pattern_ExtractRecordComponent() {
        Object p = new P(42);
        if (p instanceof P(byte b)) {
            return b == 42;
        }
        return false;
    }

    public static int meth() {return 42;}
    public static boolean expr_method() {
        return meth() instanceof int;
    }

    public class A1 {
        public static int i = 42;
    }
    public static boolean expr_staticallyQualified() {
        return A1.i instanceof int;
    }

    static void assertEquals(boolean expected, boolean actual) {
        if (expected != actual) {
            throw new AssertionError("Expected: " + expected + ", actual: " + actual);
        }
    }
}