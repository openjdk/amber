/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.runtime.PatternHandle;
import java.lang.runtime.PatternHandles;
import java.lang.runtime.SwitchBootstraps;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.testng.annotations.Test;

import static java.lang.invoke.MethodHandleInfo.REF_newInvokeSpecial;
import static org.testng.Assert.assertEquals;

/**
 * @test
 * @run testng RecordTest
 * @summary End-to-end test for record patterns
 */
@Test
public class RecordTest {
    record R(int a, String b, double c) { };
    record RR(R r1, R R2) { };

    private PatternHandle recordExtractor(Class<?> recordClass,
                                          Class<?>... paramTypes) throws Throwable {
        return PatternHandles.ofNamed(MethodHandles.lookup(), "_", PatternHandle.class,
                recordClass, MethodType.methodType(void.class, paramTypes), recordClass.getName(), REF_newInvokeSpecial);
    }

    public void testRecord() throws Throwable {
        R r = new R(1, "two", 3.14d);
        PatternHandle rExtract = recordExtractor(R.class, int.class, String.class, double.class);

        MethodHandle tryExtract = PatternHandles.tryMatch(MethodHandles.lookup(), "_", MethodHandle.class, rExtract);
        MethodHandle a = PatternHandles.component(MethodHandles.lookup(), "_", MethodHandle.class, rExtract, 0);
        MethodHandle b = PatternHandles.component(MethodHandles.lookup(), "_", MethodHandle.class, rExtract, 1);
        MethodHandle c = PatternHandles.component(MethodHandles.lookup(), "_", MethodHandle.class, rExtract, 2);

        Object o = tryExtract.invoke(r);
        assertEquals(1, a.invoke(o));
        assertEquals("two", b.invoke(o));
        assertEquals(3.14d, c.invoke(o));
    }

    public void testFakeNested() throws Throwable {
        R r1 = new R(1, "two", 3.14d);
        R r2 = new R(2, "four", 6.0d);
        RR rr = new RR(r1, r2);

        PatternHandle rExtract = recordExtractor(R.class, int.class, String.class, double.class);
        PatternHandle rrExtract = recordExtractor(RR.class, R.class, R.class);

        MethodHandle tryExtractR = PatternHandles.tryMatch(MethodHandles.lookup(), "_", MethodHandle.class, rExtract);
        MethodHandle ra = PatternHandles.component(MethodHandles.lookup(), "_", MethodHandle.class, rExtract, 0);
        MethodHandle rb = PatternHandles.component(MethodHandles.lookup(), "_", MethodHandle.class, rExtract, 1);
        MethodHandle rc = PatternHandles.component(MethodHandles.lookup(), "_", MethodHandle.class, rExtract, 2);

        MethodHandle tryExtractRr = PatternHandles.tryMatch(MethodHandles.lookup(), "_", MethodHandle.class, rrExtract);
        MethodHandle r1c = PatternHandles.component(MethodHandles.lookup(), "_", MethodHandle.class, rrExtract, 0);
        MethodHandle r2c = PatternHandles.component(MethodHandles.lookup(), "_", MethodHandle.class, rrExtract, 1);

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
        PatternHandle rExtract = recordExtractor(R.class, int.class, String.class, double.class);
        PatternHandle rrExtract = recordExtractor(RR.class, R.class, R.class);

        PatternHandle e = PatternHandles.nested(rrExtract, rExtract);

        R r1 = new R(1, "two", 3.14d);
        R r2 = new R(2, "four", 6.0d);
        RR rr = new RR(r1, r2);

        Object o = e.tryMatch().invoke(rr);

        assertEquals(e.component(0).invoke(o), new R(1, "two", 3.14d));
        assertEquals(e.component(1).invoke(o), new R(2, "four", 6.0d));
        assertEquals(e.component(2).invoke(o), 1);
        assertEquals(e.component(3).invoke(o), "two");
        assertEquals(e.component(4).invoke(o), 3.14d);

        PatternHandle ee = PatternHandles.nested(rrExtract, rExtract, rExtract);
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

    record A(int a) { };
    record B(int a, int b) { };
    record S(String s) { };
    record T(String s, String t) { };
    record U() { };

    private Object component(PatternHandle e, int num, Object carrier) throws Throwable {
        return PatternHandles.component(MethodHandles.lookup(), "_", MethodHandle.class,
                e, num).invoke(carrier);
    }

    public void testRecordSwitch() throws Throwable {
        PatternHandle[] extractors = {
                recordExtractor(A.class, int.class),
                recordExtractor(B.class, int.class, int.class),
                recordExtractor(S.class, String.class),
                recordExtractor(T.class, String.class, String.class),
                recordExtractor(U.class)
        };

        Object[] exemplars = {
                new A(1),
                new B(2, 3),
                new S("four"),
                new T("five", "six"),
                new U()
        };

        CallSite cs = SwitchBootstraps.patternSwitch(MethodHandles.lookup(), "_",
                MethodType.methodType(SwitchBootstraps.PatternSwitchResult.class, Object.class),
                extractors);
        MethodHandle mh = cs.dynamicInvoker();
        for (int i = 0; i < exemplars.length; i++) {
            Object exemplar = exemplars[i];
            SwitchBootstraps.PatternSwitchResult result = (SwitchBootstraps.PatternSwitchResult) mh.invoke(exemplar);
            assertEquals(result.index, i);
            switch (result.index) {
                case 0:
                    assertEquals(component(extractors[i], 0, result.carrier), 1);
                    break;
                case 1:
                    assertEquals(component(extractors[i], 0, result.carrier), 2);
                    assertEquals(component(extractors[i], 1, result.carrier), 3);
                    break;
                case 2:
                    assertEquals(component(extractors[i], 0, result.carrier), "four");
                    break;
                case 3:
                    assertEquals(component(extractors[i], 0, result.carrier), "five");
                    assertEquals(component(extractors[i], 1, result.carrier), "six");
                    break;
            };

            result = (SwitchBootstraps.PatternSwitchResult) mh.invoke(null);
            assertEquals(result.index, -1);

            result = (SwitchBootstraps.PatternSwitchResult) mh.invoke("foo");
            assertEquals(result.index, 5);
        }
    }

    record Box(Object o1) { };

    public void testNestedRecord() throws Throwable {
        PatternHandle boxA = PatternHandles.nested(recordExtractor(Box.class, Object.class),
                recordExtractor(A.class, int.class));
        PatternHandle boxB = PatternHandles.nested(recordExtractor(Box.class, Object.class),
                recordExtractor(B.class, int.class, int.class));

        CallSite cs = SwitchBootstraps.patternSwitch(MethodHandles.lookup(), "_",
                MethodType.methodType(SwitchBootstraps.PatternSwitchResult.class, Object.class),
                boxA, boxB);
        MethodHandle mh = cs.dynamicInvoker();

        assertEquals(((SwitchBootstraps.PatternSwitchResult) mh.invoke(new Box(new A(1)))).index, 0);
        assertEquals(((SwitchBootstraps.PatternSwitchResult) mh.invoke(new Box(new B(2, 3)))).index, 1);
        assertEquals(((SwitchBootstraps.PatternSwitchResult) mh.invoke(new Box("foo"))).index, 2);
        assertEquals(((SwitchBootstraps.PatternSwitchResult) mh.invoke(new Box(null))).index, 2);
        assertEquals(((SwitchBootstraps.PatternSwitchResult) mh.invoke("foo")).index, 2);
        assertEquals(((SwitchBootstraps.PatternSwitchResult) mh.invoke(null)).index, -1);
    }

    record RString(String i) { }
    record RObject(Object i) { }
    record Rint(int i) { }


    public void testNestedWithConstant() throws Throwable {
        PatternHandle rb = recordExtractor(Box.class, Object.class);
        PatternHandle rs = recordExtractor(RString.class, String.class);
        PatternHandle ro = recordExtractor(RObject.class, Object.class);
        PatternHandle ri = recordExtractor(Rint.class, int.class);
        PatternHandle cs = PatternHandles.ofConstant("foo");
        PatternHandle cn = PatternHandles.ofConstant(null);
        PatternHandle ci = PatternHandles.ofConstant(3);

        PatternHandleTest.assertMatch(PatternHandleTest.MatchKind.MATCH, PatternHandles.nested(rs, cs), new RString("foo"), "foo");
        PatternHandleTest.assertMatch(PatternHandleTest.MatchKind.NO_MATCH, PatternHandles.nested(rs, cs), new RString("bar"));
        PatternHandleTest.assertMatch(PatternHandleTest.MatchKind.NO_MATCH, PatternHandles.nested(rs, cs), new RString(null));

        PatternHandleTest.assertMatch(PatternHandleTest.MatchKind.MATCH, PatternHandles.nested(ro, cs), new RObject("foo"), "foo");
        PatternHandleTest.assertMatch(PatternHandleTest.MatchKind.NO_MATCH, PatternHandles.nested(ro, cs), new RObject("bar"));
        PatternHandleTest.assertMatch(PatternHandleTest.MatchKind.NO_MATCH, PatternHandles.nested(ro, cs), new RObject(3));
        PatternHandleTest.assertMatch(PatternHandleTest.MatchKind.NO_MATCH, PatternHandles.nested(ro, cs), new RObject(null));

        // @@@ These are waiting for support for either unboxing in adapt() or primitive constant patterns
//        PatternHandleTest.assertMatch(PatternHandleTest.MatchKind.MATCH, PatternHandles.nested(ri, ci), new Rint(3), 3);
//        PatternHandleTest.assertMatch(PatternHandleTest.MatchKind.NO_MATCH, PatternHandles.nested(ri, ci), new Rint(2));

        PatternHandleTest.assertMatch(PatternHandleTest.MatchKind.MATCH,
                PatternHandles.nested(rb, PatternHandles.nested(rs, cs)),
                new Box(new RString("foo")), new RString("foo"), "foo");
        PatternHandleTest.assertMatch(PatternHandleTest.MatchKind.NO_MATCH,
                PatternHandles.nested(rb, PatternHandles.nested(rs, cs)),
                new Box(new RString("bar")));
        PatternHandleTest.assertMatch(PatternHandleTest.MatchKind.NO_MATCH,
                PatternHandles.nested(rb, PatternHandles.nested(rs, cs)),
                new Box("foo"));
    }
}
