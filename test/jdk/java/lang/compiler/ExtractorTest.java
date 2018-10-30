/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.compiler.Extractor;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/**
 * @test
 * @run testng ExtractorTest
 * @summary Smoke tests for java.lang.compiler.Extractor
 */
@Test
public class ExtractorTest {

    private enum MatchKind { CARRIER, SELF, FAIL, MATCH }

    private void assertMatch(MatchKind kind, Extractor e, Object target, Object... args) throws Throwable {
        int count = e.descriptor().parameterCount();
        Object[] bindings = new Object[count];
        Object carrier = Extractor.adapt(e, Object.class).tryMatch().invoke(target);
        if (carrier != null) {
            for (int i = 0; i < count; i++)
                bindings[i] = e.component(i).invoke(carrier);
        }

        if (kind == MatchKind.FAIL)
            assertNull(carrier);
        else {
            if (target != null)
                assertNotNull(carrier);
            assertEquals(bindings.length, args.length);
            for (int i = 0; i < args.length; i++)
                assertEquals(bindings[i], args[i]);

            if (kind == MatchKind.SELF)
                assertSame(carrier, target);
            else if (kind == MatchKind.CARRIER)
                assertNotSame(carrier, target);
        }
    }

    private static class TestClass {
        static MethodHandle MH_S, MH_I, MH_L, MH_B, MH_PRED;
        static MethodHandle CONSTRUCTOR;
        static MethodHandle DIGESTER;
        static MethodHandle DIGESTER_PARTIAL;
        static MethodType TYPE = MethodType.methodType(TestClass.class, String.class, int.class, long.class, byte.class);
        static {
            try {
                MH_B = MethodHandles.lookup().findGetter(TestClass.class, "b", byte.class);
                MH_S = MethodHandles.lookup().findGetter(TestClass.class, "s", String.class);
                MH_I = MethodHandles.lookup().findGetter(TestClass.class, "i", int.class);
                MH_L = MethodHandles.lookup().findGetter(TestClass.class, "l", long.class);
                MH_PRED = MethodHandles.lookup().findVirtual(TestClass.class, "matches", MethodType.methodType(boolean.class));
                CONSTRUCTOR = MethodHandles.lookup().findConstructor(TestClass.class, TYPE.changeReturnType(void.class));
                DIGESTER = MethodHandles.lookup().findVirtual(TestClass.class, "digest", MethodType.methodType(Object.class, MethodHandle.class));
                DIGESTER_PARTIAL = MethodHandles.lookup().findVirtual(TestClass.class, "digestPartial", MethodType.methodType(Object.class, MethodHandle.class));
            }
            catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        String s;
        int i;
        long l;
        byte b;

        TestClass(String s, int i, long l, byte b) {
            this.s = s;
            this.i = i;
            this.l = l;
            this.b = b;
        }

        TestClass copy() {
            return new TestClass(s, i, l, b);
        }

        boolean matches() { return s != null && s.length() == i; }

        Object digest(MethodHandle target) throws Throwable {
            return target.invoke(s, i, l, b);
        }

        Object digestPartial(MethodHandle target) throws Throwable {
            return matches() ? target.invoke(s, i, l, b) : null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestClass aClass = (TestClass) o;
            return i == aClass.i &&
                   l == aClass.l &&
                   b == aClass.b &&
                   Objects.equals(s, aClass.s);
        }

        @Override
        public int hashCode() {
            return Objects.hash(s, i, l, b);
        }
    }

    private static class TestClass2 {
        static MethodHandle MH_X;
        static MethodType TYPE = MethodType.methodType(TestClass2.class, Object.class);
        static {
            try {
                MH_X = MethodHandles.lookup().findGetter(TestClass2.class, "x", Object.class);
            }
            catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        Object x;

        public TestClass2(Object x) {
            this.x = x;
        }
    }

    private static final MethodHandle[] COMPONENTS = {TestClass.MH_S, TestClass.MH_I, TestClass.MH_L, TestClass.MH_B };

    public void testTotal() throws Throwable {
        Extractor e = Extractor.ofTotal(TestClass.class, COMPONENTS);
        assertMatch(MatchKind.CARRIER, e, new TestClass("foo", 3, 4L, (byte) 5),
                    "foo", 3, 4L, (byte) 5);
        assertMatch(MatchKind.CARRIER, e, new TestClass(null, 0, 0L, (byte) 0),
                    null, 0, 0L, (byte) 0);
    }

    public void testSelfTotal() throws Throwable {
        Extractor e = Extractor.ofSelfTotal(TestClass.class, COMPONENTS);
        assertMatch(MatchKind.SELF, e, new TestClass("foo", 3, 4L, (byte) 5),
                    "foo", 3, 4L, (byte) 5);
        assertMatch(MatchKind.SELF, e, new TestClass(null, 0, 0L, (byte) 0),
                    null, 0, 0L, (byte) 0);
    }

    public void testPartial() throws Throwable {
        Extractor e = Extractor.ofPartial(TestClass.class, TestClass.MH_PRED, COMPONENTS);
        assertMatch(MatchKind.CARRIER, e, new TestClass("foo", 3, 4L, (byte) 5),
                    "foo", 3, 4L, (byte) 5);
        assertMatch(MatchKind.FAIL, e, new TestClass("foo", 2, 4L, (byte) 5));
        assertMatch(MatchKind.FAIL, e, new TestClass(null, 0, 0L, (byte) 0));
    }

    public void testSelfPartial() throws Throwable {
        Extractor e = Extractor.ofSelfPartial(TestClass.class, TestClass.MH_PRED, COMPONENTS);
        assertMatch(MatchKind.SELF, e, new TestClass("foo", 3, 4L, (byte) 5),
                    "foo", 3, 4L, (byte) 5);
        assertMatch(MatchKind.FAIL, e, new TestClass("foo", 2, 4L, (byte) 5));
        assertMatch(MatchKind.FAIL, e, new TestClass(null, 0, 0L, (byte) 0));
    }

    public void testDigest() throws Throwable {
        Extractor e = Extractor.of(TestClass.TYPE, TestClass.DIGESTER);
        assertMatch(MatchKind.CARRIER, e, new TestClass("foo", 3, 4L, (byte) 5),
                    "foo", 3, 4L, (byte) 5);
        assertMatch(MatchKind.CARRIER, e, new TestClass("foo", 2, 4L, (byte) 5),
                    "foo", 2, 4L, (byte) 5);
        assertMatch(MatchKind.CARRIER, e, new TestClass(null, 0, 0L, (byte) 0),
                    null, 0, 0L, (byte) 0);
    }

    public void testDigestPartial() throws Throwable {
        Extractor e = Extractor.of(TestClass.TYPE, TestClass.DIGESTER_PARTIAL);
        assertMatch(MatchKind.CARRIER, e, new TestClass("foo", 3, 4L, (byte) 5),
                    "foo", 3, 4L, (byte) 5);
        assertMatch(MatchKind.FAIL, e, new TestClass("foo", 2, 4L, (byte) 5));
    }

    public void testCompose() throws Throwable {
        Extractor e = Extractor.ofTotal(TestClass.class, COMPONENTS);
        MethodHandle mh = e.compose(TestClass.CONSTRUCTOR, null);
        TestClass target = new TestClass("foo", 3, 4L, (byte) 5);
        Object o = mh.invoke(target);
        assertTrue(o instanceof TestClass);
        assertNotSame(target, o);
        assertEquals(target, o);
    }

    public void testDropBindings() throws Throwable {
        Extractor e = Extractor.ofTotal(TestClass.class, COMPONENTS);
        assertMatch(MatchKind.CARRIER, e, new TestClass("foo", 3, 4L, (byte) 5),
                    "foo", 3, 4L, (byte) 5);
        assertMatch(MatchKind.CARRIER, Extractor.dropBindings(e, 0), new TestClass("foo", 3, 4L, (byte) 5),
                    3, 4L, (byte) 5);
        assertMatch(MatchKind.CARRIER, Extractor.dropBindings(e, 0, 0), new TestClass("foo", 3, 4L, (byte) 5),
                    3, 4L, (byte) 5);
        assertMatch(MatchKind.CARRIER, Extractor.dropBindings(e, 3), new TestClass("foo", 3, 4L, (byte) 5),
                    "foo", 3, 4L);
        assertMatch(MatchKind.CARRIER, Extractor.dropBindings(e, 0, 1, 2, 3), new TestClass("foo", 3, 4L, (byte) 5));
    }

    public void testAsType() throws Throwable {
        assertMatch(MatchKind.SELF, Extractor.ofType(String.class), "Foo", "Foo");
        assertMatch(MatchKind.FAIL, Extractor.ofType(String.class), 3);
        assertMatch(MatchKind.FAIL, Extractor.ofType(String.class), null);

        assertMatch(MatchKind.SELF, Extractor.ofType(List.class), List.of(3), List.of(3));
        assertMatch(MatchKind.SELF, Extractor.ofType(List.class), List.of(), List.of());
        assertMatch(MatchKind.SELF, Extractor.ofType(List.class), new ArrayList<>(), List.of());
    }

    public void testAsNullableType() throws Throwable {
        assertMatch(MatchKind.SELF, Extractor.ofTypeNullable(String.class), "Foo", "Foo");
        assertMatch(MatchKind.FAIL, Extractor.ofTypeNullable(String.class), 3);
        assertMatch(MatchKind.MATCH, Extractor.ofTypeNullable(String.class), null, (Object) null);
    }

    public void testNested() throws Throwable {
        Extractor TC2 = Extractor.ofTotal(TestClass2.class, TestClass2.MH_X);
        Extractor STRING = Extractor.ofType(String.class);
        Extractor OBJECT  = Extractor.ofType(Object.class);

        assertMatch(MatchKind.CARRIER, Extractor.dropBindings(Extractor.ofNested(TC2, STRING), 0), new TestClass2("foo"),
                    "foo");
        assertMatch(MatchKind.CARRIER, Extractor.dropBindings(Extractor.ofNested(TC2, OBJECT), 0), new TestClass2("foo"),
                    "foo");
        assertMatch(MatchKind.FAIL, Extractor.dropBindings(Extractor.ofNested(TC2, STRING), 0), new TestClass2(List.of(3)),
                    "foo");

        assertMatch(MatchKind.CARRIER, Extractor.dropBindings(Extractor.ofNested(TC2, Extractor.ofNested(TC2, STRING)), 0, 1), new TestClass2(new TestClass2("foo")),
                    "foo");

    }
}
