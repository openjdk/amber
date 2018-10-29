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
import java.lang.compiler.ExtractorCarriers;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.testng.annotations.Test;

import static java.lang.invoke.MethodHandleInfo.REF_newInvokeSpecial;
import static java.util.Collections.nCopies;
import static org.testng.Assert.assertEquals;

@Test
/**
 * @test
 * @run testng RecordTest
 * @summary End-to-end test for record patterns
 */
public class RecordTest {
    record R(int a, String b, double c);
    record RR(R r1, R R2);

    public void testRecord() throws Throwable {
        R r = new R(1, "two", 3.14d);
        Extractor ex = Extractor.findExtractor(MethodHandles.lookup(), "_", Extractor.class,
                                               R.class, MethodType.methodType(void.class, int.class, String.class, double.class), "Foo", REF_newInvokeSpecial);

        MethodHandle tryExtract = Extractor.extractorTryMatch(MethodHandles.lookup(), "_", MethodHandle.class, ex);
        MethodHandle a = Extractor.extractorComponent(MethodHandles.lookup(), "_", MethodHandle.class, ex, 0);
        MethodHandle b = Extractor.extractorComponent(MethodHandles.lookup(), "_", MethodHandle.class, ex, 1);
        MethodHandle c = Extractor.extractorComponent(MethodHandles.lookup(), "_", MethodHandle.class, ex, 2);

        Object o = tryExtract.invoke(r);
        assertEquals(1, a.invoke(o));
        assertEquals("two", b.invoke(o));
        assertEquals(3.14d, c.invoke(o));
    }

    public void testFakeNested() throws Throwable {
        R r1 = new R(1, "two", 3.14d);
        R r2 = new R(2, "four", 6.0d);
        RR rr = new RR(r1, r2);

        Extractor rExtract = Extractor.findExtractor(MethodHandles.lookup(), "_", Extractor.class,
                                                     R.class, MethodType.methodType(void.class, int.class, String.class, double.class), "Foo", REF_newInvokeSpecial);
        Extractor rrExtract = Extractor.findExtractor(MethodHandles.lookup(), "_", Extractor.class,
                                                      RR.class, MethodType.methodType(void.class, R.class, R.class), "Foo", REF_newInvokeSpecial);

        MethodHandle tryExtractR = Extractor.extractorTryMatch(MethodHandles.lookup(), "_", MethodHandle.class, rExtract);
        MethodHandle ra = Extractor.extractorComponent(MethodHandles.lookup(), "_", MethodHandle.class, rExtract, 0);
        MethodHandle rb = Extractor.extractorComponent(MethodHandles.lookup(), "_", MethodHandle.class, rExtract, 1);
        MethodHandle rc = Extractor.extractorComponent(MethodHandles.lookup(), "_", MethodHandle.class, rExtract, 2);

        MethodHandle tryExtractRr = Extractor.extractorTryMatch(MethodHandles.lookup(), "_", MethodHandle.class, rrExtract);
        MethodHandle r1c = Extractor.extractorComponent(MethodHandles.lookup(), "_", MethodHandle.class, rrExtract, 0);
        MethodHandle r2c = Extractor.extractorComponent(MethodHandles.lookup(), "_", MethodHandle.class, rrExtract, 1);

        Object o = tryExtractRr.invoke(rr);
        R o1 = (R) r1c.invoke(o);
        R o2 = (R) r2c.invoke(o);

        assertEquals(1, ra.invoke(o1));
        assertEquals("two", rb.invoke(o1));
        assertEquals(3.14d, rc.invoke(o1));

        assertEquals(2, ra.invoke(o2));
        assertEquals("four", rb.invoke(o2));
        assertEquals(6.0d, rc.invoke(o2));
    }

    public void testNested() throws Throwable {
        Extractor rExtract = Extractor.findExtractor(MethodHandles.lookup(), "_", Extractor.class,
                                                     R.class, MethodType.methodType(void.class, int.class, String.class, double.class), "Foo", REF_newInvokeSpecial);
        Extractor rrExtract = Extractor.findExtractor(MethodHandles.lookup(), "_", Extractor.class,
                                                      RR.class, MethodType.methodType(void.class, R.class, R.class), "Foo", REF_newInvokeSpecial);

        Extractor e = Extractor.nested(rrExtract, rExtract);

        R r1 = new R(1, "two", 3.14d);
        R r2 = new R(2, "four", 6.0d);
        RR rr = new RR(r1, r2);

        Object o = e.tryMatch().invoke(rr);

        assertEquals(e.component(0).invoke(o), new R(1, "two", 3.14d));
        assertEquals(e.component(1).invoke(o), new R(2, "four", 6.0d));
        assertEquals(e.component(2).invoke(o), 1);
        assertEquals(e.component(3).invoke(o), "two");
        assertEquals(e.component(4).invoke(o), 3.14d);

        Extractor ee = Extractor.nested(rrExtract, rExtract, rExtract);
        o = ee.tryMatch().invoke(rr);

        assertEquals(ee.component(0).invoke(o), new R(1, "two", 3.14d));
        assertEquals(ee.component(1).invoke(o), new R(2, "four", 6.0d));
        assertEquals(ee.component(2).invoke(o), 1);
        assertEquals(ee.component(3).invoke(o), "two");
        assertEquals(ee.component(4).invoke(o), 3.14d);
        assertEquals(ee.component(5).invoke(o), 2);
        assertEquals(ee.component(6).invoke(o), "four");
        assertEquals(ee.component(7).invoke(o), 6.0d);
    }
}
