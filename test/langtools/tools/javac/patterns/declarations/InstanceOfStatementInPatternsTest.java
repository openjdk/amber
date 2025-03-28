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
 * @compile InstanceOfStatementInPatternsTest.java
 * @run main InstanceOfStatementInPatternsTest
 */
public class InstanceOfStatementInPatternsTest {

    public static void main(String... args) {
        assertEquals("B", testB(new B(new H("B"))));
        assertEquals("AB", testC(new C(new A("A"), new B(new H("B")))));
        try {
            testC(new C(new A("A"), new B(null)));
            throw new AssertionError("Expected an MatchException, but none thrown.");
        } catch (MatchException ex) {
            ;
        }
    }

    static String testB(B b) {
        return switch(b) {
            case B(String s) -> s;
        };
    }

    static String testC(C c) {
        return switch(c) {
            case C(String x, String y) -> x + y;
        };
    }

    // dependency to super class
    static class Base {
        String s;
        public Base(String s) {
            this.s = s;
        }
        pattern Base(String s) {
            match Base(this.s);
        }
    }
    static class A extends Base {
        public A(String s) {
            super(s);
        }
        pattern A(String s) {
            this instanceof Base(String ss);         // instanceof calls super
            match A(ss);
        }
    }

    // dependency to field
    record H<T>(T t) { }
    static class B {
        H<String> h;

        public B(H<String> s) { h = s; }

        pattern B(String s) {
            h instanceof H<String>(String ss);      // not calling in super, just a regulard instanceof
                                                    // unconditional apart from null in the remainder
            match B(ss);
        }
    }

    // multiple instanceof statements
    static class C {
        A a;
        B b;
        public C(A a, B b) {this.a = a; this.b = b;}
        public pattern C(String x, String y) {
            a instanceof A(var xx);                  // not calling in super, just a regulard instanceof
            b instanceof B(var yy);
            match C(xx, yy);
        }
    }

    private static <T> void assertEquals(T expected, T actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected: " + expected + ", but got: " + actual);
        }
    }
}