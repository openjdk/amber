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

import java.lang.Enum.EnumDesc;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.invoke.VarHandle.VarHandleDesc;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;

import org.testng.annotations.Test;

import static java.lang.constant.ConstantDescs.CD_MethodHandle;
import static java.lang.constant.ConstantDescs.CD_Object;
import static java.lang.constant.ConstantDescs.CD_String;
import static java.lang.constant.ConstantDescs.CD_VarHandle;
import static java.lang.constant.ConstantDescs.CD_int;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/**
 * @test
 * @compile CondyDescTest.java
 * @run testng CondyDescTest
 * @summary unit tests for java.lang.constant.CondyDescTest
 */
@Test
public class CondyDescTest extends SymbolicDescTest {
    private final static ConstantDesc<?>[] EMPTY_ARGS = new ConstantDesc<?>[0];
    private final static ClassDesc CD_ConstantBootstraps = ClassDesc.of("java.lang.invoke.ConstantBootstraps");

    private static<T> void testDCR(DynamicConstantDesc<T> r, T c) throws ReflectiveOperationException {
        assertEquals(r, DynamicConstantDesc.ofNamed(r.bootstrapMethod(), r.constantName(), r.constantType(), r.bootstrapArgs()));
        assertEquals(r.resolveConstantDesc(LOOKUP), c);
    }

    private void testVarHandleRef(DynamicConstantDesc<VarHandle> r, VarHandle vh) throws ReflectiveOperationException  {
        testSymbolicDesc(r);
        assertEquals(r.resolveConstantDesc(LOOKUP), vh);
        assertEquals(vh.describeConstable().orElseThrow(), r);
    }

    private static<E extends Enum<E>> void testEnumRef(EnumDesc<E> r, E e) throws ReflectiveOperationException {
        testSymbolicDesc(r);

        assertEquals(r, EnumDesc.of(r.constantType(), r.constantName()));
        assertEquals(r.resolveConstantDesc(LOOKUP), e);
    }

    public void testNullConstant() throws ReflectiveOperationException {
        DynamicConstantDesc<?> r = (DynamicConstantDesc<?>) ConstantDescs.NULL;
        assertEquals(r, DynamicConstantDesc.ofNamed(r.bootstrapMethod(), r.constantName(), r.constantType(), r.bootstrapArgs()));
        assertNull(r.resolveConstantDesc(LOOKUP));
    }

    static String concatBSM(MethodHandles.Lookup lookup, String name, Class<?> type, String a, String b) {
        return a + b;
    }

    public void testDynamicConstant() throws ReflectiveOperationException {
        DirectMethodHandleDesc bsmRef = ConstantDescs.ofConstantBootstrap(ClassDesc.of("CondyDescTest"), "concatBSM",
                                                                            CD_String, CD_String, CD_String);
        DynamicConstantDesc<String> r = DynamicConstantDesc.of(bsmRef, "foo", "bar");
        testDCR(r, "foobar");
    }

    public void testNested() throws Throwable {
        DirectMethodHandleDesc invoker = ConstantDescs.ofConstantBootstrap(CD_ConstantBootstraps, "invoke", CD_Object, CD_MethodHandle, CD_Object.arrayType());
        DirectMethodHandleDesc format = MethodHandleDesc.of(DirectMethodHandleDesc.Kind.STATIC, CD_String, "format", CD_String, CD_String, CD_Object.arrayType());

        String s = (String) invoker.resolveConstantDesc(LOOKUP)
                                   .invoke(LOOKUP, "", String.class,
                                           format.resolveConstantDesc(LOOKUP), "%s%s", "moo", "cow");
        assertEquals(s, "moocow");

        DynamicConstantDesc<String> ref = DynamicConstantDesc.of(invoker, format, "%s%s", "moo", "cow");
        testDCR(ref, "moocow");

        DynamicConstantDesc<String> ref2 = DynamicConstantDesc.of(invoker, format, "%s%s", ref, "cow");
        testDCR(ref2, "moocowcow");
    }

    enum MyEnum { A, B, C }

    public void testEnumRef() throws ReflectiveOperationException {
        ClassDesc enumClass = ClassDesc.of("CondyDescTest").inner("MyEnum");

        testEnumRef(EnumDesc.of(enumClass, "A"), MyEnum.A);
        testEnumRef(EnumDesc.of(enumClass, "B"), MyEnum.B);
        testEnumRef(EnumDesc.of(enumClass, "C"), MyEnum.C);
    }

    static class MyClass {
        static int sf;
        int f;
    }

    public void testVarHandles() throws ReflectiveOperationException {
        ClassDesc testClass = ClassDesc.of("CondyDescTest").inner("MyClass");
        MyClass instance = new MyClass();

        // static varHandle
        VarHandleDesc vhc = VarHandleDesc.ofStaticField(testClass, "sf", CD_int);
        VarHandle varHandle = LOOKUP.findStaticVarHandle(MyClass.class, "sf", int.class);
        testVarHandleRef(vhc, varHandle);

        assertEquals(varHandle.varType(), int.class);
        varHandle.set(8);
        assertEquals(8, (int) varHandle.get());
        assertEquals(MyClass.sf, 8);

        // static varHandle
        vhc = VarHandleDesc.ofField(testClass, "f", CD_int);
        varHandle = LOOKUP.findVarHandle(MyClass.class, "f", int.class);
        testVarHandleRef(vhc, varHandle);

        assertEquals(varHandle.varType(), int.class);
        varHandle.set(instance, 9);
        assertEquals(9, (int) varHandle.get(instance));
        assertEquals(instance.f, 9);

        vhc = VarHandleDesc.ofArray(CD_int.arrayType());
        varHandle = MethodHandles.arrayElementVarHandle(int[].class);
        testVarHandleRef(vhc, varHandle);

        int[] ints = new int[3];
        varHandle.set(ints, 0, 1);
        varHandle.set(ints, 1, 2);
        varHandle.set(ints, 2, 3);

        assertEquals(1, varHandle.get(ints, 0));
        assertEquals(2, varHandle.get(ints, 1));
        assertEquals(3, varHandle.get(ints, 2));
        assertEquals(1, ints[0]);
        assertEquals(2, ints[1]);
        assertEquals(3, ints[2]);
    }

    private<T> void assertLifted(ConstantDesc<T> prototype,
                                 DynamicConstantDesc<T> nonCanonical,
                                 ConstantDesc<T> canonical) {
        Class<?> clazz = prototype.getClass();

        assertNotSame(canonical, nonCanonical);
        assertTrue(clazz.isAssignableFrom(canonical.getClass()));
        assertFalse(clazz.isAssignableFrom(nonCanonical.getClass()));
        assertEquals(prototype, canonical);
        assertEquals(canonical, prototype);
        if (prototype instanceof DynamicConstantDesc) {
            assertEquals(canonical, nonCanonical);
            assertEquals(nonCanonical, canonical);
            assertEquals(prototype, nonCanonical);
            assertEquals(nonCanonical, prototype);
        }
    }

    public void testLifting() {
        DynamicConstantDesc<Object> unliftedNull = DynamicConstantDesc.ofNamed(ConstantDescs.BSM_NULL_CONSTANT, "_", CD_Object, EMPTY_ARGS);
        assertEquals(ConstantDescs.NULL, unliftedNull);
        assertNotSame(ConstantDescs.NULL, unliftedNull);
        assertSame(ConstantDescs.NULL, DynamicConstantDesc.ofCanonical(ConstantDescs.BSM_NULL_CONSTANT, "_", CD_Object, EMPTY_ARGS));
        assertSame(ConstantDescs.NULL, DynamicConstantDesc.ofCanonical(ConstantDescs.BSM_NULL_CONSTANT, "_", CD_String, EMPTY_ARGS));
        assertSame(ConstantDescs.NULL, DynamicConstantDesc.ofCanonical(ConstantDescs.BSM_NULL_CONSTANT, "wahoo", CD_Object, EMPTY_ARGS));

        assertLifted(CD_int,
                     DynamicConstantDesc.ofNamed(ConstantDescs.BSM_PRIMITIVE_CLASS, "I", ConstantDescs.CD_Class, EMPTY_ARGS),
                     DynamicConstantDesc.ofCanonical(ConstantDescs.BSM_PRIMITIVE_CLASS, "I", ConstantDescs.CD_Class, EMPTY_ARGS));

        ClassDesc enumClass = ClassDesc.of("CondyDescTest").inner("MyEnum");
        assertLifted(EnumDesc.of(enumClass, "A"),
                     DynamicConstantDesc.ofNamed(ConstantDescs.BSM_ENUM_CONSTANT, "A", enumClass, EMPTY_ARGS),
                     DynamicConstantDesc.<MyEnum>ofCanonical(ConstantDescs.BSM_ENUM_CONSTANT, "A", enumClass, EMPTY_ARGS));

        ClassDesc testClass = ClassDesc.of("CondyDescTest").inner("MyClass");
        assertLifted(VarHandleDesc.ofStaticField(testClass, "sf", CD_int),
                     DynamicConstantDesc.ofNamed(ConstantDescs.BSM_VARHANDLE_STATIC_FIELD, "sf", CD_VarHandle, new ConstantDesc<?>[] {testClass, "sf", CD_int }),
                     DynamicConstantDesc.ofCanonical(ConstantDescs.BSM_VARHANDLE_STATIC_FIELD, "sf", CD_VarHandle, new ConstantDesc<?>[] {testClass, "sf", CD_int }));
        assertLifted(VarHandleDesc.ofField(testClass, "f", CD_int),
                     DynamicConstantDesc.ofNamed(ConstantDescs.BSM_VARHANDLE_FIELD, "f", CD_VarHandle, new ConstantDesc<?>[] {testClass, "f", CD_int }),
                     DynamicConstantDesc.ofCanonical(ConstantDescs.BSM_VARHANDLE_FIELD, "f", CD_VarHandle, new ConstantDesc<?>[] {testClass, "f", CD_int }));
        assertLifted(VarHandleDesc.ofArray(CD_int.arrayType()),
                     DynamicConstantDesc.ofNamed(ConstantDescs.BSM_VARHANDLE_ARRAY, "_", CD_VarHandle, new ConstantDesc<?>[] {CD_int.arrayType() }),
                     DynamicConstantDesc.ofCanonical(ConstantDescs.BSM_VARHANDLE_ARRAY, "_", CD_VarHandle, new ConstantDesc<?>[] {CD_int.arrayType() }));
    }

}
