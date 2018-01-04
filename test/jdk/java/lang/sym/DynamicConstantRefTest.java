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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.sym.ClassRef;
import java.lang.sym.DynamicConstantRef;
import java.lang.sym.EnumRef;
import java.lang.sym.MethodHandleRef;
import java.lang.sym.MethodTypeRef;
import java.lang.sym.SymbolicRef;
import java.lang.sym.SymbolicRefs;
import java.util.List;

import org.testng.annotations.Test;

import static java.lang.sym.SymbolicRefs.CR_MethodHandle;
import static java.lang.sym.SymbolicRefs.CR_Object;
import static java.lang.sym.SymbolicRefs.CR_String;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @test
 * @run testng DynamicConstantRefTest
 * @summary unit tests for java.lang.sym.DynamicConstantRefTest
 */
@Test
public class DynamicConstantRefTest extends SymbolicRefTest {
    static ClassRef CR_ConstantBootstraps = ClassRef.of("java.lang.invoke.ConstantBootstraps");

    private static<T> void testDCR(DynamicConstantRef<T> r, T c) throws ReflectiveOperationException {
        assertEquals(r, DynamicConstantRef.of(r.bootstrapMethod(), r.name(), r.type(), r.bootstrapArgs()));
        assertEquals(r.resolveRef(LOOKUP), c);
    }

    private static<E extends Enum<E>> void testEnumRef(EnumRef<E> r, E e) throws ReflectiveOperationException {
        testSymbolicRef(r);

        assertEquals(r, EnumRef.of(r.enumClass(), r.constantName()));
        assertEquals(r.resolveRef(LOOKUP), e);
    }

    public void testNullConstant() throws ReflectiveOperationException {
        DynamicConstantRef<?> r = (DynamicConstantRef<?>) SymbolicRefs.NULL;
        assertEquals(r, DynamicConstantRef.of(r.bootstrapMethod(), r.name(), r.type(), r.bootstrapArgs()));
        assertNull(r.resolveRef(LOOKUP));
    }

    static String concatBSM(MethodHandles.Lookup lookup, String name, Class<?> type, String a, String b) {
        return a + b;
    }

    public void testDynamicConstant() throws ReflectiveOperationException {
        MethodHandleRef bsmRef = MethodHandleRef.ofCondyBootstrap(ClassRef.of("DynamicConstantRefTest"), "concatBSM",
                                                                  CR_String, CR_String, CR_String);
        DynamicConstantRef<String> r = DynamicConstantRef.<String>of(bsmRef).withArgs("foo", "bar");
        testDCR(r, "foobar");
    }

    public void testNested() throws Throwable {
        MethodHandleRef invoker = MethodHandleRef.ofCondyBootstrap(CR_ConstantBootstraps, "invoke", CR_Object, CR_MethodHandle, CR_Object.array());
        MethodHandleRef format = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, CR_String, "format", CR_String, CR_String, CR_Object.array());

        String s = (String) invoker.resolveRef(LOOKUP)
                                   .invoke(LOOKUP, "", String.class,
                                           format.resolveRef(LOOKUP), "%s%s", "moo", "cow");
        assertEquals(s, "moocow");

        DynamicConstantRef<String> ref = DynamicConstantRef.<String>of(invoker).withArgs(format, "%s%s", "moo", "cow");
        testDCR(ref, "moocow");

        DynamicConstantRef<String> ref2 = DynamicConstantRef.<String>of(invoker).withArgs(format, "%s%s", ref, "cow");
        testDCR(ref2, "moocowcow");
    }

    enum MyEnum { A, B, C }

    public void testEnumRef() throws ReflectiveOperationException {
        ClassRef enumClass = ClassRef.of("DynamicConstantRefTest").inner("MyEnum");

        testEnumRef(EnumRef.of(enumClass, "A"), MyEnum.A);
        testEnumRef(EnumRef.of(enumClass, "B"), MyEnum.B);
        testEnumRef(EnumRef.of(enumClass, "C"), MyEnum.C);
    }

    public void testLifting() {
        MethodHandleRef BSM_NULL_CONSTANT
                = MethodHandleRef.ofCondyBootstrap(CR_ConstantBootstraps, "nullConstant", CR_Object);
        MethodHandleRef BSM_PRIMITIVE_CLASS
                = MethodHandleRef.ofCondyBootstrap(CR_ConstantBootstraps, "primitiveClass", SymbolicRefs.CR_Class);
        MethodHandleRef BSM_ENUM_CONSTANT
                = MethodHandleRef.ofCondyBootstrap(CR_ConstantBootstraps, "enumConstant", SymbolicRefs.CR_Enum);

        assertEquals(SymbolicRefs.NULL, DynamicConstantRef.of(BSM_NULL_CONSTANT, "_", CR_Object, new SymbolicRef[0]));
        assertTrue(SymbolicRefs.NULL != DynamicConstantRef.of(BSM_NULL_CONSTANT, "_", CR_Object, new SymbolicRef[0]));
        assertTrue(SymbolicRefs.NULL == DynamicConstantRef.ofCanonical(BSM_NULL_CONSTANT, "_", CR_Object, new SymbolicRef[0]));
        assertTrue(SymbolicRefs.NULL == DynamicConstantRef.ofCanonical(BSM_NULL_CONSTANT, "_", CR_String, new SymbolicRef[0]));
        assertTrue(SymbolicRefs.NULL == DynamicConstantRef.ofCanonical(BSM_NULL_CONSTANT, "wahoo", CR_Object, new SymbolicRef[0]));

        assertNotEquals(SymbolicRefs.CR_int, DynamicConstantRef.of(BSM_PRIMITIVE_CLASS, "I", SymbolicRefs.CR_Class, new SymbolicRef[0]));
        assertEquals(SymbolicRefs.CR_int, DynamicConstantRef.<Class<?>>ofCanonical(BSM_PRIMITIVE_CLASS, "I", SymbolicRefs.CR_Class, new SymbolicRef[0]));

        ClassRef enumClass = ClassRef.of("DynamicConstantRefTest").inner("MyEnum");
        assertNotEquals(EnumRef.of(enumClass, "A"),
                        DynamicConstantRef.of(BSM_ENUM_CONSTANT, "A", enumClass, new SymbolicRef[0]));
        assertEquals(EnumRef.of(enumClass, "A"),
                     DynamicConstantRef.ofCanonical(BSM_ENUM_CONSTANT, "A", enumClass, new SymbolicRef[0]));
    }

}
