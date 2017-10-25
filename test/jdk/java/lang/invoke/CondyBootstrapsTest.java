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
import java.lang.invoke.Bootstraps;
import java.lang.invoke.ClassRef;
import java.lang.invoke.DynamicConstantRef;
import java.lang.invoke.Intrinsics;
import java.lang.invoke.MethodHandleRef;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodTypeRef;
import java.lang.invoke.VarHandle;
import java.lang.invoke.WrongMethodTypeException;
import java.util.List;

import org.testng.annotations.Test;

import static java.lang.invoke.Intrinsics.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

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

    static final MethodHandleRef BSM_PRIMITIVE_CLASS
            = MethodHandleRef.ofCondyBootstrap(CLASS_CONDY, "primitiveClass", ClassRef.CR_Class);
    static final MethodHandleRef BSM_DEFAULT_VALUE
            = MethodHandleRef.ofCondyBootstrap(CLASS_CONDY, "defaultValue", ClassRef.CR_Object);
    static final MethodHandleRef BSM_GET_SATIC_FINAL_SELF
            = MethodHandleRef.ofCondyBootstrap(CLASS_CONDY, "getStaticFinal", ClassRef.CR_Object);
    static final MethodHandleRef BSM_GET_SATIC_FINAL_DECL
            = MethodHandleRef.ofCondyBootstrap(CLASS_CONDY, "getStaticFinal", ClassRef.CR_Object, ClassRef.CR_Class);
    static final MethodHandleRef BSM_INVOKE
            = MethodHandleRef.ofCondyBootstrap(CLASS_CONDY, "invoke", ClassRef.CR_Object, ClassRef.CR_MethodHandle, ClassRef.CR_Object.array());
    static final MethodHandleRef BSM_VARHANDLE_INSTANCE_FIELD
            = MethodHandleRef.ofCondyBootstrap(CLASS_CONDY, "varHandleInstanceField", ClassRef.CR_VarHandle, ClassRef.CR_Class, ClassRef.CR_Class);
    static final MethodHandleRef BSM_VARHANDLE_STATIC_FIELD
            = MethodHandleRef.ofCondyBootstrap(CLASS_CONDY, "varHandleStaticField", ClassRef.CR_VarHandle, ClassRef.CR_Class, ClassRef.CR_Class);
    static final MethodHandleRef BSM_VARHANDLE_ARRAY
            = MethodHandleRef.ofCondyBootstrap(CLASS_CONDY, "varHandleArray", ClassRef.CR_VarHandle, ClassRef.CR_Class);

    public void testDefaultValueBootstrap() {
        Object supposedlyNull = Intrinsics.ldc(DynamicConstantRef.of(BSM_DEFAULT_VALUE, ClassRef.CR_Object));
        assertNull(supposedlyNull);

        DynamicConstantRef<Integer> defaultInt = DynamicConstantRef.of(BSM_DEFAULT_VALUE, ClassRef.CR_int);
        int supposedlyZero = ldc(defaultInt);
        int supposedlyZeroToo = ldc(defaultInt);
        assertEquals(supposedlyZero, 0);
        assertEquals(supposedlyZeroToo, 0);

        DynamicConstantRef<Boolean> defaultBoolean = DynamicConstantRef.of(BSM_DEFAULT_VALUE, ClassRef.CR_boolean);
        boolean supposedlyFalse = ldc(defaultBoolean);
        boolean supposedlyFalseToo = ldc(defaultBoolean);
        assertTrue(!supposedlyFalse);
        assertTrue(!supposedlyFalseToo);
    }

    public void testPrimitiveClass() {
        assertEquals(ldc(DynamicConstantRef.of(BootstrapSpecifier.of(BSM_PRIMITIVE_CLASS), ClassRef.CR_int.descriptorString())),
                     int.class);
        assertEquals(ldc(DynamicConstantRef.of(BootstrapSpecifier.of(BSM_PRIMITIVE_CLASS), ClassRef.CR_long.descriptorString())),
                     long.class);
        assertEquals(ldc(DynamicConstantRef.of(BootstrapSpecifier.of(BSM_PRIMITIVE_CLASS), ClassRef.CR_short.descriptorString())),
                     short.class);
        assertEquals(ldc(DynamicConstantRef.of(BootstrapSpecifier.of(BSM_PRIMITIVE_CLASS), ClassRef.CR_byte.descriptorString())),
                     byte.class);
        assertEquals(ldc(DynamicConstantRef.of(BootstrapSpecifier.of(BSM_PRIMITIVE_CLASS), ClassRef.CR_char.descriptorString())),
                     char.class);
        assertEquals(ldc(DynamicConstantRef.of(BootstrapSpecifier.of(BSM_PRIMITIVE_CLASS), ClassRef.CR_float.descriptorString())),
                     float.class);
        assertEquals(ldc(DynamicConstantRef.of(BootstrapSpecifier.of(BSM_PRIMITIVE_CLASS), ClassRef.CR_double.descriptorString())),
                     double.class);
        assertEquals(ldc(DynamicConstantRef.of(BootstrapSpecifier.of(BSM_PRIMITIVE_CLASS), ClassRef.CR_boolean.descriptorString())),
                     boolean.class);
        assertEquals(ldc(DynamicConstantRef.of(BootstrapSpecifier.of(BSM_PRIMITIVE_CLASS), ClassRef.CR_void.descriptorString())),
                     void.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testPrimitiveClassNullName() {
        Bootstraps.primitiveClass(MethodHandles.lookup(), null, Class.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testPrimitiveClassEmptyName() {
        Bootstraps.primitiveClass(MethodHandles.lookup(), "", Class.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testPrimitiveClassWrongNameChar() {
        Bootstraps.primitiveClass(MethodHandles.lookup(), "L", Class.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testPrimitiveClassWrongNameString() {
        Bootstraps.primitiveClass(MethodHandles.lookup(), "Ljava/lang/Object;", Class.class);
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

    public void testInvoke() {
        DynamicConstantRef<List<Integer>> list = DynamicConstantRef.of(
                BootstrapSpecifier.of(BSM_INVOKE,
                                      MethodHandleRef.of(MethodHandleRef.Kind.STATIC, ClassRef.CR_List, "of", ClassRef.CR_List, ClassRef.CR_Object.array()),
                                      1, 2, 3, 4),
                ClassRef.CR_List);

        List<Integer> l = ldc(list);
        assertEquals(l, List.of(1, 2, 3, 4));
    }

    public void testInvokeAsType() {
        DynamicConstantRef<Integer> valueOf = DynamicConstantRef.of(
                BootstrapSpecifier.of(BSM_INVOKE,
                                      MethodHandleRef.of(MethodHandleRef.Kind.STATIC, ClassRef.CR_Integer, "valueOf", ClassRef.CR_Integer, ClassRef.CR_String),
                                      "42"),
                ClassRef.CR_int);

        int v = ldc(valueOf);
        assertEquals(v, 42);
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testInvokeAsTypeClassCast() throws Exception {
        Bootstraps.invoke(MethodHandles.lookup(), "_", String.class,
                          MethodHandles.lookup().findStatic(Integer.class, "valueOf", MethodType.methodType(Integer.class, String.class)),
                          "42");
    }

    @Test(expectedExceptions = WrongMethodTypeException.class)
    public void testInvokeAsTypeWrongReturnType() throws Exception {
        Bootstraps.invoke(MethodHandles.lookup(), "_", short.class,
                          MethodHandles.lookup().findStatic(Integer.class, "parseInt", MethodType.methodType(int.class, String.class)),
                          "42");
    }

    public void testVarHandleInstanceField() {
        VarHandle fh = Intrinsics.ldc(DynamicConstantRef.of(
                BootstrapSpecifier.of(BSM_VARHANDLE_INSTANCE_FIELD, ClassRef.of("CondyTestHelper"), ClassRef.CR_String), "f"));

        CondyTestHelper instance = new CondyTestHelper();
        assertEquals(null, fh.get(instance));
        fh.set(instance, "42");
        assertEquals(fh.get(instance), "42");
    }

    public void testVarHandleStaticField() {
        VarHandle sfh = Intrinsics.ldc(DynamicConstantRef.of(
                BootstrapSpecifier.of(BSM_VARHANDLE_STATIC_FIELD, ClassRef.of("CondyTestHelper"), ClassRef.CR_String), "sf"));

        assertEquals(null, sfh.get());
        sfh.set("42");
        assertEquals(sfh.get(), "42");
    }

    public void testVarHandleArray() {
        VarHandle ah = Intrinsics.ldc(DynamicConstantRef.of(
                BootstrapSpecifier.of(BSM_VARHANDLE_ARRAY, ClassRef.CR_String.array())));

        String[] sa = { "A" };
        assertEquals("A", ah.get(sa, 0));
        ah.set(sa, 0, "B");
        assertEquals(ah.get(sa, 0), "B");
    }
}

class CondyTestHelper {
    public static String sf;
    public String f;
}
