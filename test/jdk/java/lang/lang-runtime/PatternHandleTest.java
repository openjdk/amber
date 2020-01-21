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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.PatternHandle;
import java.lang.runtime.PatternHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.testng.annotations.Test;

import static java.util.Map.entry;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @test
 * @run testng PatternHandleTest
 * @summary Smoke tests for java.lang.runtime.Extractor
 */
@Test
public class PatternHandleTest {

    enum MatchKind {
        /** Match succeeds, with a carrier object different form the target */
        MATCH_CARRIER,
        /** Match succeeds, with self-carrier */
        MATCH_SELF,
        /** Match succeeds, carrier provenance unknown */
        MATCH,
        /** Match fails */
        NO_MATCH,
        /** Match fails with a runtime exception */
        ERROR;
    }

    // We have to resort to the subterfuge of laundering the tryMatch invocation
    // through an ancillary object, because the only way to control the static
    // signature is through the static types.  So we have a bunch of invokers,
    // each of which embeds different assumptions about the target type.  This
    // way we can test mismatches between the expected and actual types.

    interface TryMatchInvoker<T> {
        Object tryMatch(MethodHandle mh, T target) throws Throwable;
    }

    static final TryMatchInvoker<Object> objectInvoker = (MethodHandle mh, Object x) -> mh.invokeExact(x);
    static final TryMatchInvoker<Number> numberInvoker = (MethodHandle mh, Number x) -> mh.invokeExact(x);
    static final TryMatchInvoker<Integer> integerInvoker = (MethodHandle mh, Integer x) -> mh.invokeExact(x);
    static final TryMatchInvoker<Integer> intInvoker = (MethodHandle mh, Integer x) -> mh.invokeExact((int) x);
    static final TryMatchInvoker<String> stringInvoker = (MethodHandle mh, String x) -> mh.invokeExact(x);
    static final TryMatchInvoker<List> listInvoker = (MethodHandle mh, List x) -> mh.invokeExact(x);
    static final TryMatchInvoker<TestClass> testClassInvoker = (MethodHandle mh, TestClass x) -> mh.invokeExact(x);
    static final TryMatchInvoker<TestClass2> testClass2Invoker = (MethodHandle mh, TestClass2 x) -> mh.invokeExact(x);

    static final Map<Class<?>, TryMatchInvoker<?>> invokers
            = Map.ofEntries(entry(Object.class, objectInvoker),
                            entry(Number.class, numberInvoker),
                            entry(Integer.class, integerInvoker),
                            entry(int.class, intInvoker),
                            entry(String.class, stringInvoker),
                            entry(List.class, listInvoker),
                            entry(TestClass.class, testClassInvoker),
                            entry(TestClass2.class, testClass2Invoker));

    interface Throwing {
        public void run() throws Throwable;
    }

    static void assertThrows(Class<? extends Throwable> exception, Throwing r) {
        try {
            r.run();
            fail("Expected exception: " + exception);
        }
        catch (Throwable t) {
            if (!exception.isAssignableFrom(t.getClass()))
                fail(String.format("Expected exception %s, got %s", exception, t.getClass()), t);
        }
    }

    static void assertMatch(MatchKind expected,
                            PatternHandle e,
                            Object target,
                            Object... expectedBindings) throws Throwable {
        int count = e.descriptor().parameterCount();
        Object[] bindings = new Object[count];
        Object carrier;
        try {
            TryMatchInvoker inv = invokers.get(e.descriptor().returnType());
            // @@@ temporary hack until we break out the assert-match machinery
            if (inv == null)
                inv = (MethodHandle mh, Object x) -> mh.invoke(x);
            // @@@ end temporary hack
            carrier = inv.tryMatch(e.tryMatch(), target);
        }
        catch (Throwable t) {
            carrier = null;
            if (expected == MatchKind.ERROR)
                return;
            else
                fail("Unexpected exception in tryMatch", t);
        }

        if (carrier != null) {
            for (int i = 0; i < count; i++)
                bindings[i] = e.component(i).invoke(carrier);
        }

        if (expected == MatchKind.NO_MATCH)
            assertNull(carrier);
        else {
            assertNotNull(carrier);
            assertEquals(bindings.length, expectedBindings.length);
            for (int i = 0; i < expectedBindings.length; i++)
                assertEquals(bindings[i], expectedBindings[i]);

            if (expected == MatchKind.MATCH_SELF)
                assertSame(carrier, target);
            else if (expected == MatchKind.MATCH_CARRIER)
                assertNotSame(carrier, target);
        }
    }

    private static class TestClass {
        static TestClass INSTANCE_A = new TestClass("foo", 3, 4L, (byte) 5);
        static TestClass INSTANCE_B = new TestClass(null, 0, 0L, (byte) 0);
        static TestClass INSTANCE_C = new TestClass("foo", 2, 4L, (byte) 5);
        static Object[] COMPONENTS_A = new Object[] { "foo", 3, 4L, (byte) 5 };
        static Object[] COMPONENTS_B = new Object[] { null, 0, 0L, (byte) 0 };
        static Object[] COMPONENTS_C = new Object[] { "foo", 2, 4L, (byte) 5 };

        static Map<TestClass, Object[]> INSTANCES = Map.of(INSTANCE_A, COMPONENTS_A,
                                                           INSTANCE_B, COMPONENTS_B,
                                                           INSTANCE_C, COMPONENTS_C);

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
        static MethodHandle[] COMPONENT_MHS = {TestClass.MH_S, TestClass.MH_I, TestClass.MH_L, TestClass.MH_B };

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

    PatternHandle TYPE_STRING = PatternHandles.ofType(String.class);
    PatternHandle TYPE_LIST = PatternHandles.ofType(List.class);
    PatternHandle TYPE_INTEGER = PatternHandles.ofType(Integer.class);
    PatternHandle TYPE_NUMBER = PatternHandles.ofType(Number.class);
    PatternHandle TYPE_OBJECT = PatternHandles.ofType(Object.class);
    PatternHandle TYPE_INT = PatternHandles.ofType(int.class);
    PatternHandle TYPE_STRING_NULLABLE = PatternHandles.ofTypeNullable(String.class);

    public void testType() throws Throwable {
        assertMatch(MatchKind.MATCH_SELF, TYPE_STRING, "Foo", "Foo");
        assertMatch(MatchKind.NO_MATCH, TYPE_STRING, null);
        assertMatch(MatchKind.ERROR, TYPE_STRING, List.of());
        assertMatch(MatchKind.ERROR, TYPE_STRING, 3);

        assertMatch(MatchKind.MATCH_SELF, TYPE_LIST, List.of(3), List.of(3));
        assertMatch(MatchKind.MATCH_SELF, TYPE_LIST, List.of(), List.of());
        assertMatch(MatchKind.MATCH_SELF, TYPE_LIST, new ArrayList<>(), List.of());
        assertMatch(MatchKind.NO_MATCH, TYPE_LIST, null);

        assertMatch(MatchKind.MATCH_SELF, TYPE_INTEGER, 3, 3);
        assertMatch(MatchKind.MATCH_SELF, TYPE_NUMBER, 3, 3);
        assertMatch(MatchKind.MATCH_SELF, TYPE_OBJECT, 3, 3);
        assertMatch(MatchKind.NO_MATCH, TYPE_OBJECT, null);

        assertMatch(MatchKind.ERROR, TYPE_INTEGER, 3.14f);
        assertMatch(MatchKind.ERROR, TYPE_INTEGER, "foo");
    }

    public void testPrimitiveType() throws Throwable {
        assertMatch(MatchKind.MATCH_SELF, TYPE_INT, 3, 3);
        assertMatch(MatchKind.ERROR, TYPE_INT, 3.14f);

        PatternHandle asObject = PatternHandles.adaptTarget(TYPE_INT, Object.class);
        assertMatch(MatchKind.MATCH_SELF, asObject, 3, 3);
        assertMatch(MatchKind.NO_MATCH, asObject, 3.14f);
        assertMatch(MatchKind.NO_MATCH, asObject, null);

        PatternHandle asInteger = PatternHandles.adaptTarget(TYPE_INT, Integer.class);
        assertMatch(MatchKind.MATCH_SELF, asInteger, 3, 3);
        assertMatch(MatchKind.NO_MATCH, asInteger, null);
        assertMatch(MatchKind.ERROR, asInteger, 3.14f);
    }

    public void testNullableType() throws Throwable {
        assertMatch(MatchKind.MATCH_SELF, TYPE_STRING_NULLABLE, "Foo", "Foo");
        assertMatch(MatchKind.MATCH, TYPE_STRING_NULLABLE, null, (Object) null);
        assertMatch(MatchKind.ERROR, TYPE_STRING_NULLABLE, 3);

        PatternHandle asObjectNullable = PatternHandles.adaptTarget(TYPE_STRING_NULLABLE, Object.class);
        assertMatch(MatchKind.MATCH_SELF, asObjectNullable, "Foo", "Foo");
        assertMatch(MatchKind.MATCH, asObjectNullable, null, (Object) null);
        assertMatch(MatchKind.NO_MATCH, asObjectNullable, 3);
    }

    public void testAdapt() throws Throwable {
        PatternHandle e = PatternHandles.ofTypeNullable(Number.class);
        PatternHandle n = PatternHandles.adaptTarget(e, Integer.class);
        PatternHandle w = PatternHandles.adaptTarget(e, Object.class);

        assertEquals(e.descriptor().returnType(), Number.class);
        assertEquals(n.descriptor().returnType(), Integer.class);
        assertEquals(w.descriptor().returnType(), Object.class);

        assertMatch(MatchKind.MATCH_SELF, e, 1, 1);
        assertMatch(MatchKind.MATCH_SELF, n, 1, 1);
        assertMatch(MatchKind.MATCH_SELF, w, 1, 1);

        assertMatch(MatchKind.MATCH_SELF, e, 3.14f, 3.14f);
        assertMatch(MatchKind.ERROR, n, 3.14f);
        assertMatch(MatchKind.MATCH_SELF, w, 3.14f, 3.14f);

        assertMatch(MatchKind.MATCH, e, null, (Object) null);
        assertMatch(MatchKind.MATCH, n, null, (Object) null);
        assertMatch(MatchKind.MATCH, w, null, (Object) null);

        e = PatternHandles.ofType(Number.class);
        n = PatternHandles.adaptTarget(e, Integer.class);
        w = PatternHandles.adaptTarget(e, Object.class);

        assertMatch(MatchKind.MATCH_SELF, e, 1, 1);
        assertMatch(MatchKind.MATCH_SELF, n, 1, 1);
        assertMatch(MatchKind.MATCH_SELF, w, 1, 1);
        assertMatch(MatchKind.NO_MATCH, e, null);
        assertMatch(MatchKind.NO_MATCH, n, null);
        assertMatch(MatchKind.NO_MATCH, w, null);

        PatternHandle widenNarrow = PatternHandles.adaptTarget(PatternHandles.adaptTarget(TYPE_STRING, Object.class), String.class);
        assertMatch(MatchKind.MATCH_SELF, widenNarrow, "Foo", "Foo");
        assertMatch(MatchKind.NO_MATCH, widenNarrow, null);
        assertMatch(MatchKind.ERROR, widenNarrow, List.of());
        assertMatch(MatchKind.ERROR, widenNarrow, 3);

        PatternHandle widenNarrowNullable = PatternHandles.adaptTarget(PatternHandles.adaptTarget(TYPE_STRING_NULLABLE, Object.class), String.class);
        assertMatch(MatchKind.MATCH_SELF, widenNarrowNullable, "Foo", "Foo");
        assertMatch(MatchKind.MATCH, widenNarrowNullable, null, (Object) null);
        assertMatch(MatchKind.ERROR, widenNarrowNullable, List.of());
        assertMatch(MatchKind.ERROR, widenNarrowNullable, 3);
    }

    public void testConstant() throws Throwable {
        PatternHandle constantFoo = PatternHandles.ofConstant("foo");
        assertMatch(MatchKind.MATCH, constantFoo, "foo");
        assertMatch(MatchKind.NO_MATCH, constantFoo, "bar");
        assertMatch(MatchKind.ERROR, constantFoo, 3);
        assertMatch(MatchKind.NO_MATCH, constantFoo, null);

        PatternHandle constantThree = PatternHandles.ofConstant(3);
        assertMatch(MatchKind.MATCH, constantThree, 3);
        assertMatch(MatchKind.NO_MATCH, constantThree, 4);
        assertMatch(MatchKind.NO_MATCH, constantThree, null);
    }

    public void testNullConstant() throws Throwable {
        PatternHandle constantNull = PatternHandles.ofConstant(null);
        assertMatch(MatchKind.MATCH, constantNull, null);
        assertMatch(MatchKind.NO_MATCH, constantNull, "foo");
        assertMatch(MatchKind.NO_MATCH, constantNull, 3);
    }

    public void testProjections() throws Throwable {
        Map<PatternHandle, MatchKind> m
                = Map.of(PatternHandles.ofLazyProjection(TestClass.class, TestClass.COMPONENT_MHS), MatchKind.MATCH_SELF,
                         PatternHandles.ofEagerProjection(TestClass.class, TestClass.COMPONENT_MHS), MatchKind.MATCH_CARRIER);
        for (var ps : m.entrySet()) {
            for (var entry : TestClass.INSTANCES.entrySet()) {
                assertMatch(ps.getValue(), ps.getKey(), entry.getKey(), entry.getValue());
            }
            assertMatch(MatchKind.NO_MATCH, ps.getKey(), null);

            PatternHandle asObject = PatternHandles.adaptTarget(ps.getKey(), Object.class);
            for (var entry : TestClass.INSTANCES.entrySet())
                assertMatch(ps.getValue(), asObject, entry.getKey(), entry.getValue());
            assertMatch(MatchKind.NO_MATCH, asObject, null);

            PatternHandle asTestClassAgain = PatternHandles.adaptTarget(asObject, TestClass.class);
            for (var entry : TestClass.INSTANCES.entrySet())
                assertMatch(ps.getValue(), asTestClassAgain, entry.getKey(), entry.getValue());
            assertMatch(MatchKind.NO_MATCH, asTestClassAgain, null);
        }
    }

    public void testDigest() throws Throwable {
        PatternHandle e = PatternHandles.ofImperative(TestClass.TYPE, TestClass.DIGESTER);
        for (var entry : TestClass.INSTANCES.entrySet())
            assertMatch(MatchKind.MATCH_CARRIER, e, entry.getKey(), entry.getValue());
        assertMatch(MatchKind.NO_MATCH, e, null);
    }

    public void testDigestPartial() throws Throwable {
        PatternHandle e = PatternHandles.ofImperative(TestClass.TYPE, TestClass.DIGESTER_PARTIAL);
        for (var entry : TestClass.INSTANCES.entrySet()) {
            if (entry.getKey().matches())
                assertMatch(MatchKind.MATCH_CARRIER, e, entry.getKey(), entry.getValue());
            else
                assertMatch(MatchKind.NO_MATCH, e, entry.getKey());
        }
        assertMatch(MatchKind.NO_MATCH, e, null);
    }

    public void testCompose() throws Throwable {
        PatternHandle e = PatternHandles.ofLazyProjection(TestClass.class, TestClass.COMPONENT_MHS);
        MethodHandle mh = PatternHandles.compose(e, TestClass.CONSTRUCTOR);
        TestClass target = TestClass.INSTANCE_A;
        Object o = mh.invoke(target);
        assertTrue(o instanceof TestClass);
        assertNotSame(target, o);
        assertEquals(target, o);

        assertNull(mh.invoke((Object) null));
    }

    public void testDropBindings() throws Throwable {
        PatternHandle e = PatternHandles.ofEagerProjection(TestClass.class, TestClass.COMPONENT_MHS);
        assertMatch(MatchKind.MATCH_CARRIER, e, TestClass.INSTANCE_A,
                    TestClass.COMPONENTS_A);
        assertMatch(MatchKind.MATCH_CARRIER, PatternHandles.dropBindings(e, 0), TestClass.INSTANCE_A,
                    3, 4L, (byte) 5);
        assertMatch(MatchKind.MATCH_CARRIER, PatternHandles.dropBindings(e, 0, 0), TestClass.INSTANCE_A,
                    3, 4L, (byte) 5);
        assertMatch(MatchKind.MATCH_CARRIER, PatternHandles.dropBindings(e, 3), TestClass.INSTANCE_A,
                    "foo", 3, 4L);
        assertMatch(MatchKind.MATCH_CARRIER, PatternHandles.dropBindings(e, 0, 1, 2, 3), TestClass.INSTANCE_A);

        assertThrows(IndexOutOfBoundsException.class,
                     () -> assertMatch(MatchKind.MATCH_CARRIER, PatternHandles.dropBindings(e, -1), TestClass.INSTANCE_A,
                                       3, 4L, (byte) 5));
        assertThrows(IndexOutOfBoundsException.class,
                     () -> assertMatch(MatchKind.MATCH_CARRIER, PatternHandles.dropBindings(e, 4), TestClass.INSTANCE_A,
                                       3, 4L, (byte) 5));
    }

    public void testNested() throws Throwable {
        PatternHandle TC2 = PatternHandles.ofLazyProjection(TestClass2.class, TestClass2.MH_X);
        PatternHandle TC2_STRING = PatternHandles.nested(TC2, TYPE_STRING);
        PatternHandle TC2_OBJECT = PatternHandles.nested(TC2, TYPE_OBJECT);

        assertMatch(MatchKind.MATCH_CARRIER, PatternHandles.dropBindings(TC2_STRING, 0), new TestClass2("foo"),
                    "foo");
        assertMatch(MatchKind.MATCH_CARRIER, PatternHandles.dropBindings(TC2_OBJECT, 0), new TestClass2("foo"),
                    "foo");
        assertMatch(MatchKind.NO_MATCH, PatternHandles.dropBindings(TC2_STRING, 0), new TestClass2(List.of(3)),
                    "foo");

        assertMatch(MatchKind.MATCH_CARRIER, PatternHandles.dropBindings(PatternHandles.nested(TC2, TC2_STRING), 0, 1), new TestClass2(new TestClass2("foo")),
                    "foo");
    }
}
