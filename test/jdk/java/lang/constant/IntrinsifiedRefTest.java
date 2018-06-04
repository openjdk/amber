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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.constant.ClassDesc;
import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.lang.constant.ConstantDescs;
import java.lang.Enum.EnumDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.function.Supplier;

import org.testng.annotations.Test;

import static java.lang.invoke.Intrinsics.ldc;
import static java.lang.constant.MethodHandleDesc.Kind.GETTER;
import static java.lang.constant.MethodHandleDesc.Kind.SETTER;
import static java.lang.constant.MethodHandleDesc.Kind.STATIC_GETTER;
import static java.lang.constant.MethodHandleDesc.Kind.STATIC_SETTER;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

/**
 * @test
 * @compile IntrinsifiedRefTest.java
 * @run testng IntrinsifiedRefTest
 * @summary Integration test for intrinsification of XxxRef
 */
@Test
public class IntrinsifiedRefTest {
    public static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static final ClassDesc CR_THIS = ClassDesc.of("IntrinsifiedRefTest");
    private static final ClassDesc CR_TESTCLASS = CR_THIS.inner("TestClass");
    private static final ClassDesc CR_TESTINTF = CR_THIS.inner("TestInterface");
    private static final ClassDesc CR_TESTSUPERCLASS = CR_THIS.inner("TestSuperclass");
    private static final ClassDesc CR_TESTENUM = CR_THIS.inner("TestEnum");
    private static final String NONEXISTENT_CLASS = "foo.Bar";
    private static final String INACCESSIBLE_CLASS = "java.lang.invoke.DirectMethodHandle";

    private static final MethodHandleDesc MHR_TESTCLASS_CTOR = MethodHandleDesc.of(MethodHandleDesc.Kind.CONSTRUCTOR, CR_TESTCLASS, "<ignored!>", MethodTypeDesc.ofDescriptor("()V"));
    private static final MethodHandleDesc MHR_TESTCLASS_SM = MethodHandleDesc.of(MethodHandleDesc.Kind.STATIC, CR_TESTCLASS, "sm", "(I)I");
    private static final MethodHandleDesc MHR_TESTINTF_SM = MethodHandleDesc.of(MethodHandleDesc.Kind.STATIC, CR_TESTINTF, "sm", "(I)I");
    private static final MethodHandleDesc MHR_TESTCLASS_M = MethodHandleDesc.of(MethodHandleDesc.Kind.VIRTUAL, CR_TESTCLASS, "m", MethodTypeDesc.ofDescriptor("(I)I"));
    private static final MethodHandleDesc MHR_TESTINTF_M = MethodHandleDesc.of(MethodHandleDesc.Kind.INTERFACE_VIRTUAL, CR_TESTINTF, "m", MethodTypeDesc.ofDescriptor("(I)I"));
    private static final MethodHandleDesc MHR_TESTCLASS_PM_SPECIAL = MethodHandleDesc.of(MethodHandleDesc.Kind.SPECIAL, CR_TESTCLASS, "pm", MethodTypeDesc.ofDescriptor("(I)I"));
    private static final MethodHandleDesc MHR_TESTINTF_PM_SPECIAL = MethodHandleDesc.of(MethodHandleDesc.Kind.SPECIAL, CR_TESTINTF, "pm", MethodTypeDesc.ofDescriptor("(I)I"));
    private static final MethodHandleDesc MHR_TESTCLASS_PSM = MethodHandleDesc.of(MethodHandleDesc.Kind.STATIC, CR_TESTCLASS, "psm", MethodTypeDesc.ofDescriptor("(I)I"));
    private static final MethodHandleDesc MHR_TESTINTF_PSM = MethodHandleDesc.of(MethodHandleDesc.Kind.STATIC, CR_TESTINTF, "psm", MethodTypeDesc.ofDescriptor("(I)I"));
    private static final MethodHandleDesc MHR_TESTSUPER_M_SPECIAL = MethodHandleDesc.of(MethodHandleDesc.Kind.SPECIAL, CR_TESTSUPERCLASS, "m", "(I)I");
    private static final MethodHandleDesc MHR_TESTINTF_M_SPECIAL = MethodHandleDesc.of(MethodHandleDesc.Kind.SPECIAL, CR_TESTINTF, "m", "(I)I");
    private static final MethodHandleDesc MHR_TESTCLASS_SF_SETTER = MethodHandleDesc.ofField(STATIC_SETTER, CR_TESTCLASS, "sf", ConstantDescs.CR_int);
    private static final MethodHandleDesc MHR_TESTCLASS_SF_GETTER = MethodHandleDesc.ofField(STATIC_GETTER, CR_TESTCLASS, "sf", ConstantDescs.CR_int);
    private static final MethodHandleDesc MHR_TESTINTF_SF_GETTER = MethodHandleDesc.ofField(STATIC_GETTER, CR_TESTINTF, "sf", ConstantDescs.CR_int);
    private static final MethodHandleDesc MHR_TESTCLASS_F_SETTER = MethodHandleDesc.ofField(SETTER, CR_TESTCLASS, "f", ConstantDescs.CR_int);
    private static final MethodHandleDesc MHR_TESTCLASS_F_GETTER = MethodHandleDesc.ofField(GETTER, CR_TESTCLASS, "f", ConstantDescs.CR_int);



    private static <T extends Constable> void assertIntrinsic(ConstantDesc<T> ref, T intrinsified, T target) throws ReflectiveOperationException {
        assertEquals(target, intrinsified);
        assertEquals(ref.resolveConstantDesc(LOOKUP), intrinsified);
        assertEquals(intrinsified.describeConstable().orElseThrow(), ref);
    }

    private static<T extends Constable> void assertIntrinsicFail(ConstantDesc<T> ref, Supplier<T> supplier, Class<? extends Throwable> exception) {
        try {
            T t = supplier.get();
            fail("Expected failure resolving " + ref);
        } catch (Throwable e) {
            if (exception.isAssignableFrom(e.getClass()))
                return;
            else if (e instanceof BootstrapMethodError) {
                Throwable cause = e.getCause();
                if (cause != null && exception.isAssignableFrom(cause.getClass()))
                    return;
            }
            fail(String.format("Expected %s, found %s for %s", exception, e.getClass(), ref));
        }
    }

    public void testLdcClass() throws ReflectiveOperationException {
        ClassDesc cr1 = ClassDesc.ofDescriptor("Ljava/lang/String;");
        ClassDesc cr2 = ClassDesc.of("java.lang.String");
        ClassDesc cr3 = ClassDesc.of("java.lang", "String");

        assertIntrinsic(cr1, ldc(cr1), String.class);
        assertIntrinsic(cr2, ldc(cr2), String.class);
        assertIntrinsic(cr3, ldc(cr3), String.class);

        ClassDesc cr4 = ClassDesc.ofDescriptor("[Ljava/lang/String;");
        ClassDesc cr5 = cr2.arrayType();
        assertIntrinsic(cr4, ldc(cr4), String[].class);
        assertIntrinsic(cr5, ldc(cr5), String[].class);

        ClassDesc cr6 = ClassDesc.ofDescriptor("I");
        assertIntrinsic(cr6, ldc(cr6), int.class);
        assertIntrinsic(ConstantDescs.CR_int, ldc(ConstantDescs.CR_int), int.class);

        ClassDesc cr7 = ClassDesc.ofDescriptor("[I");
        ClassDesc cr8 = ConstantDescs.CR_int.arrayType();
        assertIntrinsic(cr7, ldc(cr7), int[].class);
        assertIntrinsic(cr8, ldc(cr8), int[].class);
    }

    public void negLdcClass() {
        ClassDesc cr = ClassDesc.of(NONEXISTENT_CLASS);
        assertIntrinsicFail(cr, () -> ldc(cr), NoClassDefFoundError.class);

        ClassDesc cr2 = ClassDesc.of(INACCESSIBLE_CLASS);
        assertIntrinsicFail(cr2, () -> ldc(cr2), IllegalAccessError.class);
    }

    public void testLdcMethodType() throws ReflectiveOperationException {
        MethodTypeDesc mtr1 = MethodTypeDesc.ofDescriptor("()V");
        MethodTypeDesc mtr2 = MethodTypeDesc.of(ConstantDescs.CR_void);
        assertIntrinsic(mtr1, ldc(mtr1), MethodType.methodType(void.class));
        assertIntrinsic(mtr2, ldc(mtr2), MethodType.methodType(void.class));

        MethodTypeDesc mtr3 = MethodTypeDesc.ofDescriptor("(I)I");
        MethodTypeDesc mtr4 = MethodTypeDesc.of(ConstantDescs.CR_int, ConstantDescs.CR_int);
        assertIntrinsic(mtr3, ldc(mtr3), MethodType.methodType(int.class, int.class));
        assertIntrinsic(mtr4, ldc(mtr4), MethodType.methodType(int.class, int.class));

        MethodTypeDesc mtr5 = MethodTypeDesc.ofDescriptor("(Ljava/lang/String;)Ljava/lang/String;");
        MethodTypeDesc mtr6 = MethodTypeDesc.of(ConstantDescs.CR_String, ConstantDescs.CR_String);
        assertIntrinsic(mtr5, ldc(mtr5), MethodType.methodType(String.class, String.class));
        assertIntrinsic(mtr6, ldc(mtr6), MethodType.methodType(String.class, String.class));

        MethodTypeDesc mtr7 = MethodTypeDesc.ofDescriptor("([I)[Ljava/lang/String;");
        assertIntrinsic(mtr7, ldc(mtr7), MethodType.methodType(String[].class, int[].class));
    }

    public void negLdcMethodType() {
        MethodTypeDesc mtr1 = MethodTypeDesc.of(ClassDesc.of(NONEXISTENT_CLASS));
        assertIntrinsicFail(mtr1, () -> ldc(mtr1), NoClassDefFoundError.class);

        MethodTypeDesc mtr2 = MethodTypeDesc.of(ClassDesc.of(INACCESSIBLE_CLASS));
        assertIntrinsicFail(mtr2, () -> ldc(mtr2), IllegalAccessError.class);
    }

    public void testLdcEnum() throws ReflectiveOperationException {
        EnumDesc<TestEnum> enr1 = EnumDesc.of(CR_TESTENUM, "A");
        assertIntrinsic(enr1, ldc(enr1), TestEnum.A);

        EnumDesc<TestEnum> enr2 = EnumDesc.of(CR_TESTENUM, "B");
        assertIntrinsic(enr2, ldc(enr2), TestEnum.B);
    }

    public void negLdcEnum() {
        EnumDesc<TestEnum> enr1 = EnumDesc.of(CR_TESTENUM, "C");
        assertIntrinsicFail(enr1, () -> ldc(enr1), IllegalArgumentException.class);

        EnumDesc<TestEnum> enr2 = EnumDesc.of(ClassDesc.of(NONEXISTENT_CLASS), "A");
        assertIntrinsicFail(enr2, () -> ldc(enr2), NoClassDefFoundError.class);

        EnumDesc<TestEnum> enr3 = EnumDesc.of(CR_THIS, "A");
        assertIntrinsicFail(enr3, () -> ldc(enr3), IllegalArgumentException.class);

        EnumDesc<TestEnum> enr4 = EnumDesc.of(ClassDesc.of(INACCESSIBLE_CLASS), "A");
        assertIntrinsicFail(enr4, () -> ldc(enr4), IllegalAccessError.class);
    }

    public void testLdcSelfConstants() throws ReflectiveOperationException {
        assertIntrinsic("Foo", ldc("Foo"), "Foo");
        assertIntrinsic(1, ldc(1), 1);
        assertIntrinsic(1L, ldc(1L), 1L);
        assertIntrinsic(2.0f, ldc(2.0f), 2.0f);
        assertIntrinsic(3.0d, ldc(3.0d), 3.0d);
    }

    public void testLdcMethodHandleFromInner() throws Throwable {
        TestClass.ldcMethodHandleFromInner();
        TestClass.negLdcMethodHandleFromInner();
        TestInterface.testLdcMethodHandleFromIntf(new TestInterface(){});
    }

    public void testLdcMethodHandle() throws Throwable {
        TestClass instance = (TestClass) ldc(MHR_TESTCLASS_CTOR).invokeExact();

        assertEquals(5, (int) ldc(MHR_TESTCLASS_SM).invokeExact(5));
        assertEquals(0, (int) ldc(MHR_TESTINTF_SM).invokeExact(5));

        assertEquals(5, (int) ldc(MHR_TESTCLASS_M).invokeExact(instance, 5));
        assertEquals(5, (int) ldc(MHR_TESTINTF_M).invoke(instance, 5));

        ldc(MHR_TESTCLASS_SF_SETTER).invokeExact(8);
        assertEquals(TestClass.sf, 8);
        assertEquals(8, (int) ldc(MHR_TESTCLASS_SF_GETTER).invokeExact());

        assertEquals(3, (int) ldc(MHR_TESTINTF_SF_GETTER).invokeExact());

        ldc(MHR_TESTCLASS_F_SETTER).invokeExact(instance, 9); assertEquals(instance.f, 9);
        assertEquals(9, (int) ldc(MHR_TESTCLASS_F_GETTER).invokeExact(instance));
    }

    public void negLdcMethodHandle() {
        // Accessible classes, inaccessible methods
        assertIntrinsicFail(MHR_TESTCLASS_PM_SPECIAL, () -> ldc(MHR_TESTCLASS_PM_SPECIAL), IllegalAccessError.class);
        assertIntrinsicFail(MHR_TESTINTF_PM_SPECIAL, () -> ldc(MHR_TESTINTF_PM_SPECIAL), IllegalAccessError.class);
        assertIntrinsicFail(MHR_TESTCLASS_PSM, () -> ldc(MHR_TESTCLASS_PSM), IllegalAccessError.class);
        assertIntrinsicFail(MHR_TESTINTF_PSM, () -> ldc(MHR_TESTINTF_PSM), IllegalAccessError.class);

        // Accessible class and method, but illegal super access
        assertIntrinsicFail(MHR_TESTSUPER_M_SPECIAL, () -> ldc(MHR_TESTSUPER_M_SPECIAL), IllegalAccessError.class);
        assertIntrinsicFail(MHR_TESTINTF_M_SPECIAL, () -> ldc(MHR_TESTINTF_M_SPECIAL), IncompatibleClassChangeError.class);

        // Method kind mismatches -- intf, virtual, static
        MethodHandleDesc intfMethodAsVirtual = MethodHandleDesc.of(MethodHandleDesc.Kind.VIRTUAL, CR_TESTINTF, "m", MethodTypeDesc.ofDescriptor("(I)I"));
        MethodHandleDesc intfMethodAsStatic = MethodHandleDesc.of(MethodHandleDesc.Kind.STATIC, CR_TESTINTF, "m", MethodTypeDesc.ofDescriptor("(I)I"));
        MethodHandleDesc virtualMethodAsIntf = MethodHandleDesc.of(MethodHandleDesc.Kind.INTERFACE_VIRTUAL, CR_TESTCLASS, "m", MethodTypeDesc.ofDescriptor("(I)I"));
        MethodHandleDesc virtualMethodAsStatic = MethodHandleDesc.of(MethodHandleDesc.Kind.STATIC, CR_TESTCLASS, "m", MethodTypeDesc.ofDescriptor("(I)I"));
        MethodHandleDesc staticMethodAsVirtual = MethodHandleDesc.of(MethodHandleDesc.Kind.VIRTUAL, CR_TESTCLASS, "sm", MethodTypeDesc.ofDescriptor("(I)I"));
        MethodHandleDesc staticMethodAsIntf = MethodHandleDesc.of(MethodHandleDesc.Kind.INTERFACE_VIRTUAL, CR_TESTINTF, "sm", MethodTypeDesc.ofDescriptor("(I)I"));

        assertIntrinsicFail(intfMethodAsVirtual, () -> ldc(intfMethodAsVirtual), IncompatibleClassChangeError.class);
        assertIntrinsicFail(intfMethodAsStatic, () -> ldc(intfMethodAsStatic), IncompatibleClassChangeError.class);
        assertIntrinsicFail(virtualMethodAsIntf, () -> ldc(virtualMethodAsIntf), IncompatibleClassChangeError.class);
        assertIntrinsicFail(virtualMethodAsStatic, () -> ldc(virtualMethodAsStatic), IncompatibleClassChangeError.class);
        assertIntrinsicFail(staticMethodAsVirtual, () -> ldc(staticMethodAsVirtual), IncompatibleClassChangeError.class);
        assertIntrinsicFail(staticMethodAsIntf, () -> ldc(staticMethodAsIntf), IncompatibleClassChangeError.class);

        // Field kind mismatch -- instance/static
        MethodHandleDesc staticFieldAsInstance = MethodHandleDesc.ofField(MethodHandleDesc.Kind.GETTER, CR_TESTCLASS, "sf", ConstantDescs.CR_int);
        MethodHandleDesc instanceFieldAsStatic = MethodHandleDesc.of(MethodHandleDesc.Kind.STATIC_GETTER, CR_TESTCLASS, "f", ConstantDescs.CR_int);

        assertIntrinsicFail(staticFieldAsInstance, () -> ldc(staticFieldAsInstance), IncompatibleClassChangeError.class);
        assertIntrinsicFail(instanceFieldAsStatic, () -> ldc(instanceFieldAsStatic), IncompatibleClassChangeError.class);

        // Setter for final field
        MethodHandleDesc finalStaticSetter = MethodHandleDesc.ofField(MethodHandleDesc.Kind.STATIC_SETTER, CR_TESTCLASS, "sff", ConstantDescs.CR_int);
        MethodHandleDesc finalSetter = MethodHandleDesc.ofField(MethodHandleDesc.Kind.SETTER, CR_TESTCLASS, "ff", ConstantDescs.CR_int);

        assertIntrinsicFail(finalStaticSetter, () -> ldc(finalStaticSetter), IllegalAccessError.class);
        assertIntrinsicFail(finalSetter, () -> ldc(finalSetter), IllegalAccessError.class);

        // Nonexistent owner
        MethodHandleDesc r1 = MethodHandleDesc.of(MethodHandleDesc.Kind.STATIC, ClassDesc.of(NONEXISTENT_CLASS), "m", "()V");
        assertIntrinsicFail(r1, () -> ldc(r1), NoClassDefFoundError.class);

        // Inaccessible owner
        MethodHandleDesc r2 = MethodHandleDesc.of(MethodHandleDesc.Kind.STATIC, ClassDesc.of(INACCESSIBLE_CLASS), "m", "()V");
        assertIntrinsicFail(r2, () -> ldc(r2), IllegalAccessError.class);

        // Nonexistent method, ctor, field
        MethodHandleDesc r3 = MethodHandleDesc.of(MethodHandleDesc.Kind.STATIC, CR_TESTCLASS, "nonexistent", "()V");
        MethodHandleDesc r4 = MethodHandleDesc.of(MethodHandleDesc.Kind.VIRTUAL, CR_TESTCLASS, "nonexistent", "()V");
        MethodHandleDesc r5 = MethodHandleDesc.of(MethodHandleDesc.Kind.CONSTRUCTOR, CR_TESTCLASS, "<ignored>", "(I)V");
        MethodHandleDesc r6 = MethodHandleDesc.ofField(MethodHandleDesc.Kind.GETTER, CR_TESTCLASS, "nonexistent", ConstantDescs.CR_int);
        MethodHandleDesc r7 = MethodHandleDesc.ofField(MethodHandleDesc.Kind.SETTER, CR_TESTCLASS, "nonexistent", ConstantDescs.CR_int);
        MethodHandleDesc r8 = MethodHandleDesc.ofField(MethodHandleDesc.Kind.STATIC_GETTER, CR_TESTCLASS, "nonexistent", ConstantDescs.CR_int);
        MethodHandleDesc r9 = MethodHandleDesc.ofField(MethodHandleDesc.Kind.STATIC_SETTER, CR_TESTCLASS, "nonexistent", ConstantDescs.CR_int);

        assertIntrinsicFail(r3, () -> ldc(r3), NoSuchMethodError.class);
        assertIntrinsicFail(r4, () -> ldc(r4), NoSuchMethodError.class);
        assertIntrinsicFail(r5, () -> ldc(r5), NoSuchMethodError.class);
        assertIntrinsicFail(r6, () -> ldc(r6), NoSuchFieldError.class);
        assertIntrinsicFail(r7, () -> ldc(r7), NoSuchFieldError.class);
        assertIntrinsicFail(r8, () -> ldc(r8), NoSuchFieldError.class);
        assertIntrinsicFail(r9, () -> ldc(r9), NoSuchFieldError.class);
    }

    public void testLdcDynamicConstants() throws ReflectiveOperationException {
        assertNull(ldc(ConstantDescs.NULL));
        assertIntrinsic(ConstantDescs.CR_int, ldc(ConstantDescs.CR_int), int.class);
        // @@@ VarHandle
        // @@@ invoke (including multiple deep)
    }

    public void negLdcDynamicConstants() {
        // @@@ negative tests for nonexistent/inaccessible bootstrap
        // @@@ negative tests for bootstrap parameter mismatch
    }

    private enum TestEnum {
        A, B;
    }

    private interface TestInterface {
        public static final int sf = 3;

        static int sm(int x) { return 0; }
        default int m(int x) { return 0; }
        private int pm(int x) { return 1; }
        private static int psm(int x) { return 2; }

        static void testLdcMethodHandleFromIntf(TestInterface testInterface) throws Throwable {
            assertEquals(1, ldc(MHR_TESTINTF_PM_SPECIAL).invoke(testInterface, 1));
            assertEquals(2, ldc(MHR_TESTINTF_PSM).invoke(1));
        }
    }

    private static class TestSuperclass {
        public int m(int x) { return -1; }
    }

    private static class TestClass extends TestSuperclass implements TestInterface {

        static final int sff = 7;
        static final int ff = 8;

        static int sf;
        int f;

        public TestClass()  {}

        public static int sm(int x) { return x; }
        public int m(int x) { return x; }
        private static int psm(int x) { return x; }
        private int pm(int x) { return x; }

        private static void negLdcMethodHandleFromInner() {
            // When we have nestmates, these will probably start succeeding,
            // at which point we will need to find new negative tests for super-access.
            assertIntrinsicFail(MHR_TESTINTF_PM_SPECIAL, () -> ldc(MHR_TESTINTF_PM_SPECIAL), IllegalAccessError.class);
            assertIntrinsicFail(MHR_TESTINTF_PSM, () -> ldc(MHR_TESTINTF_PSM), IllegalAccessError.class);
        }

        private static void ldcMethodHandleFromInner() throws Throwable {
            TestClass instance = (TestClass) ldc(MHR_TESTCLASS_CTOR).invokeExact();

            assertEquals(-1, (int) ldc(MHR_TESTSUPER_M_SPECIAL).invokeExact(instance, 5));
            assertEquals(0, (int) ldc(MHR_TESTINTF_M_SPECIAL).invokeExact(instance, 5));

            assertEquals(5, (int) ldc(MHR_TESTCLASS_PM_SPECIAL).invokeExact(instance, 5));

            assertEquals(5, (int) ldc(MHR_TESTCLASS_PSM).invokeExact(5));
        }

    }

    private static int privateStaticMethod(int i) {
        return i;
    }

    private int privateMethod(int i) {
        return i;
    }
}
