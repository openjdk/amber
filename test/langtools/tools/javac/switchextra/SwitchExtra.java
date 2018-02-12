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
 * @compile SwitchExtra.java
 * @run main SwitchExtra
 */
public class SwitchExtra {
    public static void main(String... args) {
        new SwitchExtra().run();
    }

    private static final long LONG_KEY = 2L << 32;

    private void run() {
        assertEquals(0, longSwitch(-1));
        assertEquals(1, longSwitch(Long.MIN_VALUE));
        assertEquals(2, longSwitch(Long.MAX_VALUE));
        assertEquals(3, longSwitch(LONG_KEY));
        assertEquals(4, longSwitch(0));
        assertEquals(5, longSwitch(LONG_KEY + 1));
        assertEquals(0, longSwitchBoxed(-1l));
        assertEquals(1, longSwitchBoxed(Long.MIN_VALUE));
        assertEquals(2, longSwitchBoxed(Long.MAX_VALUE));
        assertEquals(3, longSwitchBoxed(LONG_KEY));
        assertEquals(4, longSwitchBoxed(0l));
        assertEquals(5, longSwitchBoxed(LONG_KEY + 1));
        assertEquals(0, floatSwitch(-1f));
        assertEquals(1, floatSwitch(Float.MIN_VALUE));
        assertEquals(2, floatSwitch(Float.MAX_VALUE));
        assertEquals(3, floatSwitch(Float.NaN));
        assertEquals(4, floatSwitch(Float.NEGATIVE_INFINITY));
        assertEquals(5, floatSwitch(Float.POSITIVE_INFINITY));
        assertEquals(6, floatSwitch(0));
        assertEquals(7, floatSwitch(3.14f));
        assertEquals(0, floatSwitchBoxed(-1f));
        assertEquals(1, floatSwitchBoxed(Float.MIN_VALUE));
        assertEquals(2, floatSwitchBoxed(Float.MAX_VALUE));
        assertEquals(3, floatSwitchBoxed(Float.NaN));
        assertEquals(4, floatSwitchBoxed(Float.NEGATIVE_INFINITY));
        assertEquals(5, floatSwitchBoxed(Float.POSITIVE_INFINITY));
        assertEquals(6, floatSwitchBoxed(0f));
        assertEquals(7, floatSwitchBoxed(3.14f));
        assertEquals(0, doubleSwitch(-1d));
        assertEquals(1, doubleSwitch(Double.MIN_VALUE));
        assertEquals(2, doubleSwitch(Double.MAX_VALUE));
        assertEquals(3, doubleSwitch(Double.NaN));
        assertEquals(4, doubleSwitch(Double.NEGATIVE_INFINITY));
        assertEquals(5, doubleSwitch(Double.POSITIVE_INFINITY));
        assertEquals(6, doubleSwitch(0));
        assertEquals(7, doubleSwitch(3.14));
        assertEquals(0, doubleSwitchBoxed(-1d));
        assertEquals(1, doubleSwitchBoxed(Double.MIN_VALUE));
        assertEquals(2, doubleSwitchBoxed(Double.MAX_VALUE));
        assertEquals(3, doubleSwitchBoxed(Double.NaN));
        assertEquals(4, doubleSwitchBoxed(Double.NEGATIVE_INFINITY));
        assertEquals(5, doubleSwitchBoxed(Double.POSITIVE_INFINITY));
        assertEquals(6, doubleSwitchBoxed(0d));
        assertEquals(7, doubleSwitchBoxed(3.14));
    }

    private int longSwitch(long l) {
        switch (l) {
            case -1: return 0;
            case Long.MIN_VALUE: return 1;
            case Long.MAX_VALUE: return 2;
            case LONG_KEY: return 3;
            case 0: return 4;
            default: return 5;
        }
    }

    private int longSwitchBoxed(Long l) {
        switch (l) {
            case -1: return 0;
            case Long.MIN_VALUE: return 1;
            case Long.MAX_VALUE: return 2;
            case LONG_KEY: return 3;
            case 0: return 4;
            default: return 5;
        }
    }

    private int floatSwitch(float f) {
        switch (f) {
            case -1: return 0;
            case Float.MIN_VALUE: return 1;
            case Float.MAX_VALUE: return 2;
            case Float.NaN: return 3;
            case Float.NEGATIVE_INFINITY: return 4;
            case Float.POSITIVE_INFINITY: return 5;
            case 0.0f: return 6;
            default: return 7;
        }
    }

    private int floatSwitchBoxed(Float f) {
        switch (f) {
            case -1: return 0;
            case Float.MIN_VALUE: return 1;
            case Float.MAX_VALUE: return 2;
            case Float.NaN: return 3;
            case Float.NEGATIVE_INFINITY: return 4;
            case Float.POSITIVE_INFINITY: return 5;
            case 0.0f: return 6;
            default: return 7;
        }
    }

    private int doubleSwitch(double d) {
        switch (d) {
            case -1: return 0;
            case Double.MIN_VALUE: return 1;
            case Double.MAX_VALUE: return 2;
            case Double.NaN: return 3;
            case Double.NEGATIVE_INFINITY: return 4;
            case Double.POSITIVE_INFINITY: return 5;
            case 0.0: return 6;
            default: return 7;
        }
    }

    private int doubleSwitchBoxed(Double d) {
        switch (d) {
            case -1: return 0;
            case Double.MIN_VALUE: return 1;
            case Double.MAX_VALUE: return 2;
            case Double.NaN: return 3;
            case Double.NEGATIVE_INFINITY: return 4;
            case Double.POSITIVE_INFINITY: return 5;
            case 0.0: return 6;
            default: return 7;
        }
    }

    private void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError();
        }
    }
}
