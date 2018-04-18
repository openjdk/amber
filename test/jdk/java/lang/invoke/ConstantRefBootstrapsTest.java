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

import java.lang.invoke.Intrinsics;
import java.lang.invoke.VarHandle;
import java.lang.invoke.constant.ClassDesc;
import java.lang.invoke.constant.ConstantDescs;
import java.lang.invoke.constant.ConstantMethodHandleDesc;
import java.lang.invoke.constant.DynamicConstantDesc;
import java.lang.invoke.constant.MethodHandleDesc;
import java.util.List;

import org.testng.annotations.Test;

import static java.lang.invoke.Intrinsics.ldc;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @test
 * @compile -XDdoConstantFold ConstantRefBootstrapsTest.java
 * @run testng ConstantRefBootstrapsTest
 * @summary integration tests for dynamic constant bootstraps and Intrinsics.ldc()
 */
@Test
public class ConstantRefBootstrapsTest {
    static final ClassDesc CLASS_CONDY = ClassDesc.of("java.lang.invoke.ConstantBootstraps");

    static final ConstantMethodHandleDesc BSM_NULL_CONSTANT
            = ConstantDescs.ofConstantBootstrap(CLASS_CONDY, "nullConstant", ConstantDescs.CR_Object);
    static final ConstantMethodHandleDesc BSM_PRIMITIVE_CLASS
            = ConstantDescs.ofConstantBootstrap(CLASS_CONDY, "primitiveClass", ConstantDescs.CR_Class);
    static final ConstantMethodHandleDesc BSM_ENUM_CONSTANT
            = ConstantDescs.ofConstantBootstrap(CLASS_CONDY, "enumConstant", ConstantDescs.CR_Enum);
    static final ConstantMethodHandleDesc BSM_GET_STATIC_FINAL_SELF
            = ConstantDescs.ofConstantBootstrap(CLASS_CONDY, "getStaticFinal", ConstantDescs.CR_Object);
    static final ConstantMethodHandleDesc BSM_GET_STATIC_FINAL_DECL
            = ConstantDescs.ofConstantBootstrap(CLASS_CONDY, "getStaticFinal", ConstantDescs.CR_Object, ConstantDescs.CR_Class);
    static final ConstantMethodHandleDesc BSM_INVOKE
            = ConstantDescs.ofConstantBootstrap(CLASS_CONDY, "invoke", ConstantDescs.CR_Object, ConstantDescs.CR_MethodHandle, ConstantDescs.CR_Object.array());
    static final ConstantMethodHandleDesc BSM_VARHANDLE_FIELD
            = ConstantDescs.ofConstantBootstrap(CLASS_CONDY, "fieldVarHandle", ConstantDescs.CR_VarHandle, ConstantDescs.CR_Class, ConstantDescs.CR_Class);
    static final ConstantMethodHandleDesc BSM_VARHANDLE_STATIC_FIELD
            = ConstantDescs.ofConstantBootstrap(CLASS_CONDY, "staticFieldVarHandle", ConstantDescs.CR_VarHandle, ConstantDescs.CR_Class, ConstantDescs.CR_Class);
    static final ConstantMethodHandleDesc BSM_VARHANDLE_ARRAY
            = ConstantDescs.ofConstantBootstrap(CLASS_CONDY, "arrayVarHandle", ConstantDescs.CR_VarHandle, ConstantDescs.CR_Class);


    public void testNullConstant() {
        Object supposedlyNull = Intrinsics.ldc(DynamicConstantDesc.of(BSM_NULL_CONSTANT, ConstantDescs.CR_Object));
        assertNull(supposedlyNull);

        supposedlyNull = Intrinsics.ldc(DynamicConstantDesc.of(BSM_NULL_CONSTANT, ConstantDescs.CR_MethodType));
        assertNull(supposedlyNull);
    }


    public void testPrimitiveClass() {
        assertEquals(ldc(DynamicConstantDesc.of(BSM_PRIMITIVE_CLASS, ConstantDescs.CR_int.descriptorString())), int.class);
        assertEquals(ldc(DynamicConstantDesc.of(BSM_PRIMITIVE_CLASS, ConstantDescs.CR_long.descriptorString())), long.class);
        assertEquals(ldc(DynamicConstantDesc.of(BSM_PRIMITIVE_CLASS, ConstantDescs.CR_short.descriptorString())), short.class);
        assertEquals(ldc(DynamicConstantDesc.of(BSM_PRIMITIVE_CLASS, ConstantDescs.CR_byte.descriptorString())), byte.class);
        assertEquals(ldc(DynamicConstantDesc.of(BSM_PRIMITIVE_CLASS, ConstantDescs.CR_char.descriptorString())), char.class);
        assertEquals(ldc(DynamicConstantDesc.of(BSM_PRIMITIVE_CLASS, ConstantDescs.CR_float.descriptorString())), float.class);
        assertEquals(ldc(DynamicConstantDesc.of(BSM_PRIMITIVE_CLASS, ConstantDescs.CR_double.descriptorString())), double.class);
        assertEquals(ldc(DynamicConstantDesc.of(BSM_PRIMITIVE_CLASS, ConstantDescs.CR_boolean.descriptorString())), boolean.class);
        assertEquals(ldc(DynamicConstantDesc.of(BSM_PRIMITIVE_CLASS, ConstantDescs.CR_void.descriptorString())), void.class);
    }


    public void testEnumConstant() {
        MethodHandleDesc.Kind k = Intrinsics.ldc(DynamicConstantDesc.of(
                BSM_ENUM_CONSTANT, "STATIC",
                ClassDesc.of("java.lang.invoke.constant.MethodHandleRef$Kind")));
        assertEquals(k, MethodHandleDesc.Kind.STATIC);
    }


    public void testGetStaticFinalDecl() {
        DynamicConstantDesc<Class<Integer>> intClass =
                DynamicConstantDesc.<Class<Integer>>of(BSM_GET_STATIC_FINAL_DECL, "TYPE", ConstantDescs.CR_Class).withArgs(ConstantDescs.CR_Integer);
        Class<Integer> c = Intrinsics.ldc(intClass);
        assertEquals(c, int.class);
    }

    public void testGetStaticFinalSelf() {
        DynamicConstantDesc<Integer> integerMaxValue = DynamicConstantDesc.of(BSM_GET_STATIC_FINAL_SELF, "MAX_VALUE", ConstantDescs.CR_int);
        int v = Intrinsics.ldc(integerMaxValue);
        assertEquals(v, Integer.MAX_VALUE);
    }


    public void testInvoke() {
        DynamicConstantDesc<List<Integer>> list
                = DynamicConstantDesc.<List<Integer>>of(BSM_INVOKE, ConstantDescs.CR_List)
                .withArgs(MethodHandleDesc.of(MethodHandleDesc.Kind.STATIC, ConstantDescs.CR_List, "of", ConstantDescs.CR_List, ConstantDescs.CR_Object.array()), 1, 2, 3, 4);

        List<Integer> l = ldc(list);
        assertEquals(l, List.of(1, 2, 3, 4));
    }

    public void testInvokeAsType() {
        DynamicConstantDesc<Integer> valueOf = DynamicConstantDesc.<Integer>of(BSM_INVOKE, ConstantDescs.CR_int)
                .withArgs(MethodHandleDesc.of(MethodHandleDesc.Kind.STATIC, ConstantDescs.CR_Integer, "valueOf", ConstantDescs.CR_Integer, ConstantDescs.CR_String),
                          "42");

        int v = ldc(valueOf);
        assertEquals(v, 42);
    }


    public void testVarHandleField() {
        VarHandle fh = Intrinsics.ldc(DynamicConstantDesc.<VarHandle>of(BSM_VARHANDLE_FIELD, "f")
                                              .withArgs(ClassDesc.of("CondyTestHelper"), ConstantDescs.CR_String));

        CondyTestHelper instance = new CondyTestHelper();
        assertEquals(null, fh.get(instance));
        fh.set(instance, "42");
        assertEquals(fh.get(instance), "42");
    }

    public void testVarHandleStaticField() {
        VarHandle sfh = Intrinsics.ldc(DynamicConstantDesc.<VarHandle>of(BSM_VARHANDLE_STATIC_FIELD, "sf")
                                       .withArgs(ClassDesc.of("CondyTestHelper"), ConstantDescs.CR_String));

        assertEquals(null, sfh.get());
        sfh.set("42");
        assertEquals(sfh.get(), "42");
    }

    public void testVarHandleArray() {
        VarHandle ah = Intrinsics.ldc(DynamicConstantDesc.<VarHandle>of(BSM_VARHANDLE_ARRAY).withArgs(ConstantDescs.CR_String.array()));

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
