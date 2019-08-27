/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * StaticMembersInInnerClassesTest
 *
 * @test
 * @compile -XDallowStaticMembersInInners StaticMembersInInnerClassesTest.java
 * @run testng StaticMembersInInnerClassesTest
 */

import org.testng.annotations.*;
import java.lang.reflect.Modifier;
import static org.testng.Assert.*;

@Test
public class StaticMembersInInnerClassesTest {
    class InstanceNestedHelper {
        interface I {
            String foo();
        }

        record R(int x) implements I {
            public String foo() { return "foo"; }
        }

        private R r;

        InstanceNestedHelper(int x) {
            this.r = new R(x);
            this.e = E.B;
        }

        int x() {
            return r.x();
        }

        static int ident(int i) {
            return i;
        }

        String foo() {
            return r.foo();
        }

        enum E { A, B }

        E e;
    }

    public void testNestedRecordsStatic() {
        assertTrue((InstanceNestedHelper.R.class.getModifiers() & Modifier.STATIC) != 0);
        InstanceNestedHelper inner = new InstanceNestedHelper(1);
        assertTrue(inner.x() == 1);
        assertTrue(inner.ident(1) == 1);
        assertTrue(InstanceNestedHelper.ident(1) == 1);
        assertTrue(inner.foo().equals("foo"));
        assertTrue(InstanceNestedHelper.E.A.toString().equals("A"));
        assertTrue(inner.e.toString().equals("B"));
    }

    Runnable r = new Runnable() {
        record R3(int x) {}
        public void run() {}
    };
}
