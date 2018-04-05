/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @summary smoke tests for CallSite
 * @library /lib/testlibrary/bytecode /java/lang/invoke/common
 * @build jdk.experimental.bytecode.BasicClassBuilder test.java.lang.invoke.lib.InstructionHelper
 * @run main test.java.lang.invoke.CallSiteTest
 */

package test.java.lang.invoke;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.VolatileCallSite;

import static java.lang.invoke.MethodHandles.Lookup;
import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodType.methodType;
import static test.java.lang.invoke.lib.InstructionHelper.invokedynamic;

public class CallSiteTest {
    private static final Class<?> CLASS = CallSiteTest.class;

    private static final CallSite mcs;
    private static final CallSite vcs;
    private static final MethodHandle mh_foo;
    private static final MethodHandle mh_bar;

    private static final MethodHandle indy_mcs;
    private static final MethodHandle indy_vcs;
    static {
        try {
            MethodHandles.Lookup l = lookup();
            mh_foo = l.findStatic(CLASS, "foo", methodType(int.class, int.class, int.class));
            mh_bar = l.findStatic(CLASS, "bar", methodType(int.class, int.class, int.class));
            mcs = new MutableCallSite(mh_foo);
            vcs = new VolatileCallSite(mh_foo);

            MethodType bsmType = methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class);
            indy_mcs = invokedynamic(l, "mcs", methodType(int.class, int.class, int.class), "bsm_mcs", bsmType, S -> {});
            indy_vcs = invokedynamic(l, "vcs", methodType(int.class, int.class, int.class), "bsm_vcs", bsmType, S -> {});
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public static void main(String... av) throws Throwable {
        testMutableCallSite();
        testVolatileCallSite();
    }

    private static final int N = Integer.MAX_VALUE / 100;
    private static final int RESULT1 = 762786192;
    private static final int RESULT2 = -21474836;

    private static void assertEquals(int expected, int actual) {
        if (expected != actual)
            throw new AssertionError("expected: " + expected + ", actual: " + actual);
    }

    private static void testMutableCallSite() throws Throwable {
        // warm-up
        for (int i = 0; i < 20000; i++) {
            mcs.setTarget(mh_foo);
        }
        // run
        for (int n = 0; n < 2; n++) {
            mcs.setTarget(mh_foo);
            for (int i = 0; i < 5; i++) {
                assertEquals(RESULT1, runMutableCallSite());
            }
            mcs.setTarget(mh_bar);
            for (int i = 0; i < 5; i++) {
                assertEquals(RESULT2, runMutableCallSite());
            }
        }
    }
    private static void testVolatileCallSite() throws Throwable {
        // warm-up
        for (int i = 0; i < 20000; i++) {
            vcs.setTarget(mh_foo);
        }
        // run
        for (int n = 0; n < 2; n++) {
            vcs.setTarget(mh_foo);
            for (int i = 0; i < 5; i++) {
                assertEquals(RESULT1, runVolatileCallSite());
            }
            vcs.setTarget(mh_bar);
            for (int i = 0; i < 5; i++) {
                assertEquals(RESULT2, runVolatileCallSite());
            }
        }
    }

    private static int runMutableCallSite() throws Throwable {
        int sum = 0;
        for (int i = 0; i < N; i++) {
            sum += (int) indy_mcs.invokeExact(i, i+1);
        }
        return sum;
    }
    private static int runVolatileCallSite() throws Throwable {
        int sum = 0;
        for (int i = 0; i < N; i++) {
            sum += (int) indy_vcs.invokeExact(i, i+1);
        }
        return sum;
    }

    static int foo(int a, int b) { return a + b; }
    static int bar(int a, int b) { return a - b; }

    static CallSite bsm_mcs(Lookup caller, String name, MethodType type) throws ReflectiveOperationException {
        return mcs;
    }

    static CallSite bsm_vcs(Lookup caller, String name, MethodType type) throws ReflectiveOperationException {
        return vcs;
    }
}
