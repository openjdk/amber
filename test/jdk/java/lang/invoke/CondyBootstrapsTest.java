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
    static final ClassRef CLASS_CONDY = ClassRef.of("java.lang.invoke.Bootstraps");

    static final MethodHandleRef BSM_GET_SATIC_FINAL_SELF
            = MethodHandleRef.ofCondyBootstrap(CLASS_CONDY, "getStaticFinal", ClassRef.CR_Object);
    static final MethodHandleRef BSM_GET_SATIC_FINAL_DECL
            = MethodHandleRef.ofCondyBootstrap(CLASS_CONDY, "getStaticFinal", ClassRef.CR_Object, ClassRef.CR_Class);
    static final MethodHandleRef BSM_DEFAULT_VALUE
            = MethodHandleRef.ofCondyBootstrap(CLASS_CONDY, "defaultValue", ClassRef.CR_Object);
    static final MethodHandleRef BSM_VARHANDLE
            = MethodHandleRef.ofCondyBootstrap(CLASS_CONDY, "varHandle", ClassRef.CR_VarHandle, ClassRef.CR_MethodType, ClassRef.CR_Object.array());

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

    public void testStaticFinalDecl() {
        DynamicConstantRef<Class<Integer>> intClass =
                DynamicConstantRef.of(BootstrapSpecifier.of(BSM_GET_SATIC_FINAL_DECL, ClassRef.CR_Integer),
                                      "TYPE", ClassRef.CR_Class);
        Class<Integer> c = Intrinsics.ldc(intClass);
        assertEquals(c, int.class);
    }

    public void testStaticFinalSelf() {
        DynamicConstantRef<Integer> integerMaxValue = DynamicConstantRef.of(BootstrapSpecifier.of(BSM_GET_SATIC_FINAL_SELF),
                                                                            "MAX_VALUE", ClassRef.CR_int);
        int v = Intrinsics.ldc(integerMaxValue);
        assertEquals(v, Integer.MAX_VALUE);
    }

    public void testVarHandleBootstrap() {
        ClassRef helperClass = ClassRef.of("CondyTestHelper");
        ClassRef stringClass = ClassRef.CR_String;
        VarHandle fh = Intrinsics.ldc(DynamicConstantRef.of(BootstrapSpecifier.of(BSM_VARHANDLE, MethodTypeRef.of(stringClass, helperClass)), "f"));
        VarHandle sfh = Intrinsics.ldc(DynamicConstantRef.of(BootstrapSpecifier.of(BSM_VARHANDLE, MethodTypeRef.of(stringClass), helperClass), "sf"));

        assertEquals(null, sfh.get());
        sfh.set("42");
        assertEquals(sfh.get(), "42");

        CondyTestHelper instance = new CondyTestHelper();
        assertEquals(null, fh.get(instance));
        fh.set(instance, "42");
        assertEquals(fh.get(instance), "42");
    }
}

class CondyTestHelper {
    public static String sf;
    public String f;
}
