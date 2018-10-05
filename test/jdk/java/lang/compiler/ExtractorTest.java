/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.compiler.PatternCarriers;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

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
 * @summary unit tests for java.lang.compiler.Extractor
 */
@Test
public class ExtractorTest {

    private Object[] extract(Extractor extractor, Object target) throws Throwable {
        int count = extractor.descriptor().parameterCount();
        Object[] result = new Object[count + 1];
        Object carrier = extractor.tryMatch().invoke(target);
        if (carrier == null)
            return null;
        for (int i=0; i<count; i++)
            result[i] = extractor.component(i).invoke(carrier);
        result[count] = carrier;
        return result;
    }

    private void assertExtracted(Object[] result, Object... args) {
        assertNotNull(result);
        assertEquals(result.length - 1, args.length);
        for (int i = 0; i < args.length; i++) {
            assertEquals(result[i], args[i]);
        }
    }

    private void testExtractLazy(Extractor e, Object target, Object... args) throws Throwable {
        Object[] result = extract(e, target);
        assertExtracted(result, args);
        assertSame(carrier(result), target);
    }

    private void testExtractEager(Extractor e, Object target, Object... args) throws Throwable {
        Object[] result = extract(e, target);
        assertExtracted(result, args);
        assertNotSame(carrier(result), target);
    }

    private void assertExtractFail(Extractor e, Object target) throws Throwable {
        Object[] result = extract(e, target);
        assertNull(result);
    }

    private Object carrier(Object[] result) {
        return result[result.length-1];
    }

    private static class TestClass {
        static MethodHandle MH_S, MH_I, MH_L, MH_B;
        static MethodHandle CONSTRUCTOR;
        static MethodHandle COPIER;
        static MethodHandle DIGESTER;
        static MethodHandle NON_NULL;
        static MethodHandle NON_NULL_EXPLODED;
        static MethodType TYPE = MethodType.methodType(TestClass.class, String.class, int.class, long.class, byte.class);
        static {
            try {
                MH_B = MethodHandles.lookup().findGetter(TestClass.class, "b", byte.class);
                MH_S = MethodHandles.lookup().findGetter(TestClass.class, "s", String.class);
                MH_I = MethodHandles.lookup().findGetter(TestClass.class, "i", int.class);
                MH_L = MethodHandles.lookup().findGetter(TestClass.class, "l", long.class);
                CONSTRUCTOR = MethodHandles.lookup().findConstructor(TestClass.class, TYPE.changeReturnType(void.class));
                COPIER = MethodHandles.lookup().findVirtual(TestClass.class, "copy", MethodType.methodType(TestClass.class));
                NON_NULL = MethodHandles.lookup().findVirtual(TestClass.class, "test", MethodType.methodType(boolean.class));
                NON_NULL_EXPLODED = MethodHandles.lookup().findStatic(TestClass.class, "testExploded", MethodType.methodType(boolean.class, String.class, int.class, long.class, byte.class));
                DIGESTER = MethodHandles.lookup().findVirtual(TestClass.class, "digest", MethodType.methodType(Object.class, MethodHandle.class));
            }
            catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        String s;
        int i;
        long l;
        byte b;

        public TestClass(String s, int i, long l, byte b) {
            this.s = s;
            this.i = i;
            this.l = l;
            this.b = b;
        }

        TestClass copy() {
            return new TestClass(s, i, l, b);
        }

        boolean test() {
            return s != null;
        }

        static boolean testExploded(String s, int i, long l, byte b) {
            return s != null;
        }

        Object digest(MethodHandle target) throws Throwable {
            return target.invoke(s, i, l, b);
        }
    }

    static final MethodHandle[] COMPONENTS = { TestClass.MH_S, TestClass.MH_I, TestClass.MH_L, TestClass.MH_B };
    static final MethodHandle CARRIER_FACTORY = PatternCarriers.carrierFactory(TestClass.TYPE);
    static final MethodHandle[] CARRIER_COMPONENTS = { PatternCarriers.carrierComponent(TestClass.TYPE, 0),
                                                       PatternCarriers.carrierComponent(TestClass.TYPE, 1),
                                                       PatternCarriers.carrierComponent(TestClass.TYPE, 2),
                                                       PatternCarriers.carrierComponent(TestClass.TYPE, 3) };

    public void testLazySelfTotal() throws Throwable {
        Extractor e = Extractor.ofLazy(TestClass.TYPE, COMPONENTS);
        testExtractLazy(e, new TestClass("foo", 3, 4L, (byte) 5),
                        "foo", 3, 4L, (byte) 5);
        testExtractLazy(e, new TestClass(null, 0, 0L, (byte) 0),
                        null, 0, 0L, (byte) 0);
    }

    public void testEagerSelfTotal() throws Throwable {
        Extractor e = Extractor.of(TestClass.TYPE, TestClass.COPIER, COMPONENTS);
        testExtractEager(e, new TestClass("foo", 3, 4L, (byte) 5),
                        "foo", 3, 4L, (byte) 5);
        testExtractEager(e, new TestClass(null, 0, 0L, (byte) 0),
                        null, 0, 0L, (byte) 0);
    }

    public void testLazySelfPartial() throws Throwable {
        Extractor e = Extractor.ofLazyPartial(TestClass.TYPE, TestClass.NON_NULL, COMPONENTS);
        testExtractLazy(e, new TestClass("foo", 3, 4L, (byte) 5),
                        "foo", 3, 4L, (byte) 5);
        assertExtractFail(e, new TestClass(null, 0, 0L, (byte) 0));
    }

    public void testEagerSelfPartial() throws Throwable {
        Extractor e = Extractor.ofPartial(TestClass.TYPE, TestClass.COPIER, TestClass.NON_NULL, COMPONENTS);
        testExtractEager(e, new TestClass("foo", 3, 4L, (byte) 5),
                        "foo", 3, 4L, (byte) 5);
        assertExtractFail(e, new TestClass(null, 0, 0L, (byte) 0));
    }

    public void testCarrierTotal() throws Throwable {
        Extractor e = Extractor.ofCarrier(TestClass.TYPE, CARRIER_FACTORY, TestClass.DIGESTER, CARRIER_COMPONENTS);
        testExtractEager(e, new TestClass("foo", 3, 4L, (byte) 5),
                        "foo", 3, 4L, (byte) 5);
        testExtractEager(e, new TestClass(null, 0, 0L, (byte) 0),
                        null, 0, 0L, (byte) 0);
    }

    public void testCarrierPartial() throws Throwable {
        Extractor e = Extractor.ofCarrierPartial(TestClass.TYPE, CARRIER_FACTORY, TestClass.DIGESTER, TestClass.NON_NULL_EXPLODED, CARRIER_COMPONENTS);
        testExtractEager(e, new TestClass("foo", 3, 4L, (byte) 5),
                        "foo", 3, 4L, (byte) 5);
        assertExtractFail(e, new TestClass(null, 0, 0L, (byte) 0));
    }

    public void testClamshell() throws Throwable {
        Extractor e = Extractor.ofCarrier(TestClass.TYPE, CARRIER_FACTORY, TestClass.DIGESTER, CARRIER_COMPONENTS);
        MethodHandle mh = e.compose(TestClass.CONSTRUCTOR);
        TestClass target = new TestClass("foo", 3, 4L, (byte) 5);
        Object o = mh.invoke(target);
        assertTrue(o instanceof TestClass);
        TestClass copy = (TestClass) o;
        assertEquals(target.s, copy.s);
        assertEquals(target.i, copy.i);
        assertEquals(target.l, copy.l);
        assertEquals(target.b, copy.b);
    }
}
