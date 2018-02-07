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
import java.lang.sym.ClassRef;
import java.lang.sym.Constable;
import java.lang.sym.EnumRef;
import java.lang.sym.MethodHandleRef;
import java.lang.sym.MethodTypeRef;
import java.lang.sym.SymbolicRef;
import java.lang.sym.SymbolicRefs;
import java.util.function.Supplier;

import org.testng.annotations.Test;

import static java.lang.invoke.Intrinsics.ldc;
import static java.lang.sym.MethodHandleRef.Kind.GETTER;
import static java.lang.sym.MethodHandleRef.Kind.SETTER;
import static java.lang.sym.MethodHandleRef.Kind.STATIC_GETTER;
import static java.lang.sym.MethodHandleRef.Kind.STATIC_SETTER;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

/**
 * @test
 * @compile -XDdoConstantFold IntrinsifiedRefTest.java
 * @run testng IntrinsifiedRefTest
 * @summary Integration test for intrinsification of XxxRef
 */
@Test
public class IntrinsifiedRefTest {
    public static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static final ClassRef CR_THIS = ClassRef.of("IntrinsifiedRefTest");
    private static final ClassRef CR_TESTCLASS = CR_THIS.inner("TestClass");
    private static final ClassRef CR_TESTINTF = CR_THIS.inner("TestInterface");
    private static final ClassRef CR_TESTSUPERCLASS = CR_THIS.inner("TestSuperclass");
    private static final ClassRef CR_TESTENUM = CR_THIS.inner("TestEnum");
    private static final String NONEXISTENT_CLASS = "foo.Bar";
    private static final String INACCESSIBLE_CLASS = "java.lang.invoke.DirectMethodHandle";

    private static final MethodHandleRef MHR_TESTCLASS_CTOR = MethodHandleRef.of(MethodHandleRef.Kind.CONSTRUCTOR, CR_TESTCLASS, "<ignored!>", MethodTypeRef.ofDescriptor("()V"));
    private static final MethodHandleRef MHR_TESTCLASS_SM = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, CR_TESTCLASS, "sm", "(I)I");
    private static final MethodHandleRef MHR_TESTINTF_SM = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, CR_TESTINTF, "sm", "(I)I");
    private static final MethodHandleRef MHR_TESTCLASS_M = MethodHandleRef.of(MethodHandleRef.Kind.VIRTUAL, CR_TESTCLASS, "m", MethodTypeRef.ofDescriptor("(I)I"));
    private static final MethodHandleRef MHR_TESTINTF_M = MethodHandleRef.of(MethodHandleRef.Kind.INTERFACE_VIRTUAL, CR_TESTINTF, "m", MethodTypeRef.ofDescriptor("(I)I"));
    private static final MethodHandleRef MHR_TESTCLASS_PM_SPECIAL = MethodHandleRef.of(MethodHandleRef.Kind.SPECIAL, CR_TESTCLASS, "pm", MethodTypeRef.ofDescriptor("(I)I"));
    private static final MethodHandleRef MHR_TESTINTF_PM_SPECIAL = MethodHandleRef.of(MethodHandleRef.Kind.SPECIAL, CR_TESTINTF, "pm", MethodTypeRef.ofDescriptor("(I)I"));
    private static final MethodHandleRef MHR_TESTCLASS_PSM = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, CR_TESTCLASS, "psm", MethodTypeRef.ofDescriptor("(I)I"));
    private static final MethodHandleRef MHR_TESTINTF_PSM = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, CR_TESTINTF, "psm", MethodTypeRef.ofDescriptor("(I)I"));
    private static final MethodHandleRef MHR_TESTSUPER_M_SPECIAL = MethodHandleRef.of(MethodHandleRef.Kind.SPECIAL, CR_TESTSUPERCLASS, "m", "(I)I");
    private static final MethodHandleRef MHR_TESTINTF_M_SPECIAL = MethodHandleRef.of(MethodHandleRef.Kind.SPECIAL, CR_TESTINTF, "m", "(I)I");
    private static final MethodHandleRef MHR_TESTCLASS_SF_SETTER = MethodHandleRef.ofField(STATIC_SETTER, CR_TESTCLASS, "sf", SymbolicRefs.CR_int);
    private static final MethodHandleRef MHR_TESTCLASS_SF_GETTER = MethodHandleRef.ofField(STATIC_GETTER, CR_TESTCLASS, "sf", SymbolicRefs.CR_int);
    private static final MethodHandleRef MHR_TESTINTF_SF_GETTER = MethodHandleRef.ofField(STATIC_GETTER, CR_TESTINTF, "sf", SymbolicRefs.CR_int);
    private static final MethodHandleRef MHR_TESTCLASS_F_SETTER = MethodHandleRef.ofField(SETTER, CR_TESTCLASS, "f", SymbolicRefs.CR_int);
    private static final MethodHandleRef MHR_TESTCLASS_F_GETTER = MethodHandleRef.ofField(GETTER, CR_TESTCLASS, "f", SymbolicRefs.CR_int);



    private static <T extends Constable> void assertIntrinsic(SymbolicRef<T> ref, T intrinsified, T target) throws ReflectiveOperationException {
        assertEquals(target, intrinsified);
        assertEquals(ref.resolveRef(LOOKUP), intrinsified);
        assertEquals(intrinsified.toSymbolicRef(LOOKUP).orElseThrow(), ref);
    }

    private static<T extends Constable> void assertIntrinsicFail(SymbolicRef<T> ref, Supplier<T> supplier, Class<? extends Throwable> exception) {
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
        ClassRef cr1 = ClassRef.ofDescriptor("Ljava/lang/String;");
        ClassRef cr2 = ClassRef.of("java.lang.String");
        ClassRef cr3 = ClassRef.of("java.lang", "String");

        assertIntrinsic(cr1, ldc(cr1), String.class);
        assertIntrinsic(cr2, ldc(cr2), String.class);
        assertIntrinsic(cr3, ldc(cr3), String.class);

        ClassRef cr4 = ClassRef.ofDescriptor("[Ljava/lang/String;");
        ClassRef cr5 = cr2.array();
        assertIntrinsic(cr4, ldc(cr4), String[].class);
        assertIntrinsic(cr5, ldc(cr5), String[].class);

        ClassRef cr6 = ClassRef.ofDescriptor("I");
        assertIntrinsic(cr6, ldc(cr6), int.class);
        assertIntrinsic(SymbolicRefs.CR_int, ldc(SymbolicRefs.CR_int), int.class);

        ClassRef cr7 = ClassRef.ofDescriptor("[I");
        ClassRef cr8 = SymbolicRefs.CR_int.array();
        assertIntrinsic(cr7, ldc(cr7), int[].class);
        assertIntrinsic(cr8, ldc(cr8), int[].class);
    }

    public void negLdcClass() {
        ClassRef cr = ClassRef.of(NONEXISTENT_CLASS);
        assertIntrinsicFail(cr, () -> ldc(cr), NoClassDefFoundError.class);

        ClassRef cr2 = ClassRef.of(INACCESSIBLE_CLASS);
        assertIntrinsicFail(cr2, () -> ldc(cr2), IllegalAccessError.class);
    }

    public void testLdcMethodType() throws ReflectiveOperationException {
        MethodTypeRef mtr1 = MethodTypeRef.ofDescriptor("()V");
        MethodTypeRef mtr2 = MethodTypeRef.of(SymbolicRefs.CR_void);
        assertIntrinsic(mtr1, ldc(mtr1), MethodType.methodType(void.class));
        assertIntrinsic(mtr2, ldc(mtr2), MethodType.methodType(void.class));

        MethodTypeRef mtr3 = MethodTypeRef.ofDescriptor("(I)I");
        MethodTypeRef mtr4 = MethodTypeRef.of(SymbolicRefs.CR_int, SymbolicRefs.CR_int);
        assertIntrinsic(mtr3, ldc(mtr3), MethodType.methodType(int.class, int.class));
        assertIntrinsic(mtr4, ldc(mtr4), MethodType.methodType(int.class, int.class));

        MethodTypeRef mtr5 = MethodTypeRef.ofDescriptor("(Ljava/lang/String;)Ljava/lang/String;");
        MethodTypeRef mtr6 = MethodTypeRef.of(SymbolicRefs.CR_String, SymbolicRefs.CR_String);
        assertIntrinsic(mtr5, ldc(mtr5), MethodType.methodType(String.class, String.class));
        assertIntrinsic(mtr6, ldc(mtr6), MethodType.methodType(String.class, String.class));

        MethodTypeRef mtr7 = MethodTypeRef.ofDescriptor("([I)[Ljava/lang/String;");
        assertIntrinsic(mtr7, ldc(mtr7), MethodType.methodType(String[].class, int[].class));
    }

    public void negLdcMethodType() {
        MethodTypeRef mtr1 = MethodTypeRef.of(ClassRef.of(NONEXISTENT_CLASS));
        assertIntrinsicFail(mtr1, () -> ldc(mtr1), NoClassDefFoundError.class);

        MethodTypeRef mtr2 = MethodTypeRef.of(ClassRef.of(INACCESSIBLE_CLASS));
        assertIntrinsicFail(mtr2, () -> ldc(mtr2), IllegalAccessError.class);
    }

    public void testLdcEnum() throws ReflectiveOperationException {
        EnumRef<TestEnum> enr1 = EnumRef.of(CR_TESTENUM, "A");
        assertIntrinsic(enr1, ldc(enr1), TestEnum.A);

        EnumRef<TestEnum> enr2 = EnumRef.of(CR_TESTENUM, "B");
        assertIntrinsic(enr2, ldc(enr2), TestEnum.B);
    }

    public void negLdcEnum() {
        EnumRef<TestEnum> enr1 = EnumRef.of(CR_TESTENUM, "C");
        assertIntrinsicFail(enr1, () -> ldc(enr1), IllegalArgumentException.class);

        EnumRef<TestEnum> enr2 = EnumRef.of(ClassRef.of(NONEXISTENT_CLASS), "A");
        assertIntrinsicFail(enr2, () -> ldc(enr2), NoClassDefFoundError.class);

        EnumRef<TestEnum> enr3 = EnumRef.of(CR_THIS, "A");
        assertIntrinsicFail(enr3, () -> ldc(enr3), IllegalArgumentException.class);

        EnumRef<TestEnum> enr4 = EnumRef.of(ClassRef.of(INACCESSIBLE_CLASS), "A");
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
        MethodHandleRef intfMethodAsVirtual = MethodHandleRef.of(MethodHandleRef.Kind.VIRTUAL, CR_TESTINTF, "m", MethodTypeRef.ofDescriptor("(I)I"));
        MethodHandleRef intfMethodAsStatic = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, CR_TESTINTF, "m", MethodTypeRef.ofDescriptor("(I)I"));
        MethodHandleRef virtualMethodAsIntf = MethodHandleRef.of(MethodHandleRef.Kind.INTERFACE_VIRTUAL, CR_TESTCLASS, "m", MethodTypeRef.ofDescriptor("(I)I"));
        MethodHandleRef virtualMethodAsStatic = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, CR_TESTCLASS, "m", MethodTypeRef.ofDescriptor("(I)I"));
        MethodHandleRef staticMethodAsVirtual = MethodHandleRef.of(MethodHandleRef.Kind.VIRTUAL, CR_TESTCLASS, "sm", MethodTypeRef.ofDescriptor("(I)I"));
        MethodHandleRef staticMethodAsIntf = MethodHandleRef.of(MethodHandleRef.Kind.INTERFACE_VIRTUAL, CR_TESTINTF, "sm", MethodTypeRef.ofDescriptor("(I)I"));

        assertIntrinsicFail(intfMethodAsVirtual, () -> ldc(intfMethodAsVirtual), IncompatibleClassChangeError.class);
        assertIntrinsicFail(intfMethodAsStatic, () -> ldc(intfMethodAsStatic), IncompatibleClassChangeError.class);
        assertIntrinsicFail(virtualMethodAsIntf, () -> ldc(virtualMethodAsIntf), IncompatibleClassChangeError.class);
        assertIntrinsicFail(virtualMethodAsStatic, () -> ldc(virtualMethodAsStatic), IncompatibleClassChangeError.class);
        assertIntrinsicFail(staticMethodAsVirtual, () -> ldc(staticMethodAsVirtual), IncompatibleClassChangeError.class);
        assertIntrinsicFail(staticMethodAsIntf, () -> ldc(staticMethodAsIntf), IncompatibleClassChangeError.class);

        // Field kind mismatch -- instance/static
        MethodHandleRef staticFieldAsInstance = MethodHandleRef.ofField(MethodHandleRef.Kind.GETTER, CR_TESTCLASS, "sf", SymbolicRefs.CR_int);
        MethodHandleRef instanceFieldAsStatic = MethodHandleRef.of(MethodHandleRef.Kind.STATIC_GETTER, CR_TESTCLASS, "f", SymbolicRefs.CR_int);

        assertIntrinsicFail(staticFieldAsInstance, () -> ldc(staticFieldAsInstance), IncompatibleClassChangeError.class);
        assertIntrinsicFail(instanceFieldAsStatic, () -> ldc(instanceFieldAsStatic), IncompatibleClassChangeError.class);

        // Setter for final field
        MethodHandleRef finalStaticSetter = MethodHandleRef.ofField(MethodHandleRef.Kind.STATIC_SETTER, CR_TESTCLASS, "sff", SymbolicRefs.CR_int);
        MethodHandleRef finalSetter = MethodHandleRef.ofField(MethodHandleRef.Kind.SETTER, CR_TESTCLASS, "ff", SymbolicRefs.CR_int);

        assertIntrinsicFail(finalStaticSetter, () -> ldc(finalStaticSetter), IllegalAccessError.class);
        assertIntrinsicFail(finalSetter, () -> ldc(finalSetter), IllegalAccessError.class);

        // Nonexistent owner
        MethodHandleRef r1 = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, ClassRef.of(NONEXISTENT_CLASS), "m", "()V");
        assertIntrinsicFail(r1, () -> ldc(r1), NoClassDefFoundError.class);

        // Inaccessible owner
        MethodHandleRef r2 = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, ClassRef.of(INACCESSIBLE_CLASS), "m", "()V");
        assertIntrinsicFail(r2, () -> ldc(r2), IllegalAccessError.class);

        // Nonexistent method, ctor, field
        MethodHandleRef r3 = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, CR_TESTCLASS, "nonexistent", "()V");
        MethodHandleRef r4 = MethodHandleRef.of(MethodHandleRef.Kind.VIRTUAL, CR_TESTCLASS, "nonexistent", "()V");
        MethodHandleRef r5 = MethodHandleRef.of(MethodHandleRef.Kind.CONSTRUCTOR, CR_TESTCLASS, "<ignored>", "(I)V");
        MethodHandleRef r6 = MethodHandleRef.ofField(MethodHandleRef.Kind.GETTER, CR_TESTCLASS, "nonexistent", SymbolicRefs.CR_int);
        MethodHandleRef r7 = MethodHandleRef.ofField(MethodHandleRef.Kind.SETTER, CR_TESTCLASS, "nonexistent", SymbolicRefs.CR_int);
        MethodHandleRef r8 = MethodHandleRef.ofField(MethodHandleRef.Kind.STATIC_GETTER, CR_TESTCLASS, "nonexistent", SymbolicRefs.CR_int);
        MethodHandleRef r9 = MethodHandleRef.ofField(MethodHandleRef.Kind.STATIC_SETTER, CR_TESTCLASS, "nonexistent", SymbolicRefs.CR_int);

        assertIntrinsicFail(r3, () -> ldc(r3), NoSuchMethodError.class);
        assertIntrinsicFail(r4, () -> ldc(r4), NoSuchMethodError.class);
        assertIntrinsicFail(r5, () -> ldc(r5), NoSuchMethodError.class);
        assertIntrinsicFail(r6, () -> ldc(r6), NoSuchFieldError.class);
        assertIntrinsicFail(r7, () -> ldc(r7), NoSuchFieldError.class);
        assertIntrinsicFail(r8, () -> ldc(r8), NoSuchFieldError.class);
        assertIntrinsicFail(r9, () -> ldc(r9), NoSuchFieldError.class);
    }

    public void testLdcDynamicConstants() throws ReflectiveOperationException {
        assertNull(ldc(SymbolicRefs.NULL));
        assertIntrinsic(SymbolicRefs.CR_int, ldc(SymbolicRefs.CR_int), int.class);
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
