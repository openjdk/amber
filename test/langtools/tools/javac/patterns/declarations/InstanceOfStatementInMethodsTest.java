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
import java.util.function.Supplier;

/**
 * @test
 * @enablePreview
 * @run main InstanceOfStatementInMethodsTest
 */
public class InstanceOfStatementInMethodsTest {
    public static void main(String[] args) {
        basicTest();
        assertMatchExceptionWithNested(InstanceOfStatementInMethodsTest::raiseExceptionTest, TestPatternFailed.class);
    }

    static void basicTest() {
        Point p = new Point(1, 2);
        p instanceof Point(Integer a, Integer b);
        assertEquals(3, a + b);

        IPoint ip   = new Point(3, 4);
        ip instanceof Point(var c, var d);
        assertEquals(7, c + d);

        p = new Point(1, null);
        p instanceof Point(var e, var f);
        assertEquals(null, f);

        PointP wp = new PointP(1, 2);
        wp instanceof PointP(int ap, double bp);
        assertEquals(2.0d, bp);
    }

    static Integer raiseExceptionTest() {
        PointEx pointEx = new PointEx(1, 2);
        pointEx instanceof PointEx(Integer a_ex, Integer b_noex);
        return a_ex;
    }

    sealed interface IPoint permits Point {}
    record Point(Integer x, Integer y) implements IPoint { }
    record PointP(int x, double y) { }
    record PointEx(Integer x, Integer y) {
        @Override
        public Integer x() {
            throw new TestPatternFailed(EXCEPTION_MESSAGE);
        }
    }
    static final String EXCEPTION_MESSAGE = "exception-message";
    public static class TestPatternFailed extends AssertionError {
        public TestPatternFailed(String message) {
            super(message);
        }
    }

    // error handling
    static void fail(String message) {
        throw new AssertionError(message);
    }

    static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected: " + expected + "," +
                    "got: " + actual);
        }
    }

    static <T> void assertMatchExceptionWithNested(Supplier<Integer> f, Class<?> nestedExceptionClass) {
        try {
            f.get();
            fail("Expected an exception, but none happened!");
        }
        catch(Exception ex) {
            assertEquals(MatchException.class, ex.getClass());

            MatchException me = (MatchException) ex;

            assertEquals(nestedExceptionClass, me.getCause().getClass());
        }
    }

    static <T> void assertEx(Supplier<Integer> f, Class<?> exceptionClass) {
        try {
            f.get();
            fail("Expected an exception, but none happened!");
        }
        catch(Exception ex) {
            assertEquals(exceptionClass, ex.getClass());
        }
    }
}