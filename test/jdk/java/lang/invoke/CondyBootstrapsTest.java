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

import java.lang.invoke.BootstrapSpecifier;
import java.lang.invoke.ClassRef;
import java.lang.invoke.Bootstraps;
import java.lang.invoke.DynamicConstantRef;
import java.lang.invoke.Intrinsics;
import java.lang.invoke.MethodHandleRef;
import java.lang.invoke.MethodTypeRef;
import java.lang.invoke.VarHandle;

import org.testng.annotations.Test;

import static java.lang.invoke.Intrinsics.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @test
 * @compile -XDdoConstantFold CondyBootstrapsTest.java
 * @run testng CondyBootstrapsTest
 * @summary integration tests for Condy bootstraps and Intrinsics.ldc()
 * @author Brian Goetz
 */
@Test
public class CondyBootstrapsTest {
    public static final ClassRef BOOTSTRAP_CLASS = ClassRef.of("java.lang.invoke.Bootstraps");

    private static final MethodHandleRef BSM_DEFAULT_VALUE
            = MethodHandleRef.ofCondyBootstrap(BOOTSTRAP_CLASS, "defaultValue", ClassRef.CR_Object);

    private static final MethodHandleRef BSM_PRIMITIVE_CLASS
            = MethodHandleRef.ofCondyBootstrap(BOOTSTRAP_CLASS, "primitiveClass", ClassRef.CR_Class);

    private static final MethodHandleRef varHandleBSM
            = MethodHandleRef.ofCondyBootstrap(BOOTSTRAP_CLASS, "varHandle",
                                               ClassRef.CR_VarHandle, ClassRef.CR_int, ClassRef.CR_Class, ClassRef.CR_String, ClassRef.CR_Class);

    public void testDefaultValueBootstrap() {
        Object supposedlyNull = Intrinsics.ldc(DynamicConstantRef.of(BSM_DEFAULT_VALUE, ClassRef.CR_Object));
        assertNull(supposedlyNull);

        DynamicConstantRef<Integer> defaultInt = DynamicConstantRef.of(BSM_DEFAULT_VALUE, ClassRef.CR_int);
        DynamicConstantRef<Boolean> defaultBoolean = DynamicConstantRef.of(BSM_DEFAULT_VALUE, ClassRef.CR_boolean);

        int supposedlyZero = Intrinsics.ldc(defaultInt);
        int supposedlyZeroToo = (int) ldc(defaultInt);
        boolean supposedlyFalse = Intrinsics.ldc(defaultBoolean);
        boolean supposedlyFalseToo = (boolean) ldc(defaultBoolean);

        assertEquals(supposedlyZero, 0);
        assertEquals(supposedlyZeroToo, 0);
        assertTrue(!supposedlyFalse);
        assertTrue(!supposedlyFalseToo);
    }

    public void testPrimitiveClassBootstrap() {
        assertEquals(int.class, Intrinsics.ldc(DynamicConstantRef.<Class<?>>of(BSM_PRIMITIVE_CLASS, "I")));
        assertEquals(long.class, Intrinsics.ldc(DynamicConstantRef.<Class<?>>of(BSM_PRIMITIVE_CLASS, "J")));
        assertEquals(short.class, Intrinsics.ldc(DynamicConstantRef.<Class<?>>of(BSM_PRIMITIVE_CLASS, "S")));
        assertEquals(byte.class, Intrinsics.ldc(DynamicConstantRef.<Class<?>>of(BSM_PRIMITIVE_CLASS, "B")));
        assertEquals(char.class, Intrinsics.ldc(DynamicConstantRef.<Class<?>>of(BSM_PRIMITIVE_CLASS, "C")));
        assertEquals(float.class, Intrinsics.ldc(DynamicConstantRef.<Class<?>>of(BSM_PRIMITIVE_CLASS, "F")));
        assertEquals(double.class, Intrinsics.ldc(DynamicConstantRef.<Class<?>>of(BSM_PRIMITIVE_CLASS, "D")));
        assertEquals(boolean.class, Intrinsics.ldc(DynamicConstantRef.<Class<?>>of(BSM_PRIMITIVE_CLASS, "Z")));
        assertEquals(void.class, Intrinsics.ldc(DynamicConstantRef.<Class<?>>of(BSM_PRIMITIVE_CLASS, "V")));
    }

    public void testVarHandleBootstrap() {
        ClassRef helperClass = ClassRef.of("CondyTestHelper");
        ClassRef stringClass = ClassRef.CR_String;
        VarHandle fh = Intrinsics.ldc(DynamicConstantRef.of(BootstrapSpecifier.of(varHandleBSM, Bootstraps.VH_instanceField, helperClass, "f", stringClass)));
        VarHandle sfh = Intrinsics.ldc(DynamicConstantRef.of(BootstrapSpecifier.of(varHandleBSM, Bootstraps.VH_staticField, helperClass, "sf", stringClass)));

        assertEquals(null, sfh.get());
        sfh.set("42");
        assertEquals("42", sfh.get());

        CondyTestHelper instance = new CondyTestHelper();
        assertEquals(null, fh.get(instance));
        fh.set(instance, "42");
        assertEquals("42", fh.get(instance));
    }
}

class CondyTestHelper {
    public static String sf;
    public String f;
}
