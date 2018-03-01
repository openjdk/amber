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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.sym.ClassRef;
import java.lang.sym.ConstantMethodHandleRef;
import java.lang.sym.MethodHandleRef;
import java.lang.sym.MethodTypeRef;
import java.lang.sym.ConstantRefs;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.testng.annotations.Test;

import static java.lang.sym.MethodHandleRef.Kind.GETTER;
import static java.lang.sym.MethodHandleRef.Kind.SETTER;
import static java.lang.sym.MethodHandleRef.Kind.STATIC_GETTER;
import static java.lang.sym.MethodHandleRef.Kind.STATIC_SETTER;
import static java.lang.sym.ConstantRefs.CR_Integer;
import static java.lang.sym.ConstantRefs.CR_List;
import static java.lang.sym.ConstantRefs.CR_Object;
import static java.lang.sym.ConstantRefs.CR_String;
import static java.lang.sym.ConstantRefs.CR_int;
import static java.lang.sym.ConstantRefs.CR_void;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @test
 * @run testng MethodHandleRefTest
 * @summary unit tests for java.lang.sym.MethodHandleRefTest
 */
@Test
public class MethodHandleRefTest extends SymbolicRefTest {
    private static ClassRef thisClass = ClassRef.of("MethodHandleRefTest");
    private static ClassRef testClass = thisClass.inner("TestClass");
    private static ClassRef testInterface = thisClass.inner("TestInterface");
    private static ClassRef testSuperclass = thisClass.inner("TestSuperclass");


    private static void assertMHEquals(MethodHandle a, MethodHandle b) {
        MethodHandleInfo ia = LOOKUP.revealDirect(a);
        MethodHandleInfo ib = LOOKUP.revealDirect(b);
        assertEquals(ia.getDeclaringClass(), ib.getDeclaringClass());
        assertEquals(ia.getName(), ib.getName());
        assertEquals(ia.getMethodType(), ib.getMethodType());
        assertEquals(ia.getReferenceKind(), ib.getReferenceKind());
    }

    private void testMethodHandleRef(MethodHandleRef r) throws ReflectiveOperationException {
        if (r instanceof ConstantMethodHandleRef) {
            testSymbolicRef(r);

            ConstantMethodHandleRef rr = (ConstantMethodHandleRef) r;
            assertEquals(r, MethodHandleRef.of(rr.kind(), rr.owner(), rr.methodName(), r.methodType()));
        }
        else {
            testSymbolicRefForwardOnly(r);
        }
    }

    private void testMethodHandleRef(MethodHandleRef r, MethodHandle mh) throws ReflectiveOperationException {
        testMethodHandleRef(r);

        assertMHEquals(r.resolveConstantRef(LOOKUP), mh);
        assertEquals(mh.toConstantRef(LOOKUP).orElseThrow(), r);

        // compare extractable properties: refKind, owner, name, type
        MethodHandleInfo mhi = LOOKUP.revealDirect(mh);
        ConstantMethodHandleRef rr = (ConstantMethodHandleRef) r;
        assertEquals(mhi.getDeclaringClass().toDescriptorString(), rr.owner().descriptorString());
        assertEquals(mhi.getName(), rr.methodName());
        assertEquals(mhi.getReferenceKind(), rr.kind().refKind);
        assertEquals(mhi.getMethodType().toMethodDescriptorString(), r.methodType().descriptorString());
    }

    public void testSimpleMHs() throws ReflectiveOperationException {
        testMethodHandleRef(MethodHandleRef.of(MethodHandleRef.Kind.VIRTUAL, CR_String, "isEmpty", "()Z"),
                            LOOKUP.findVirtual(String.class, "isEmpty", MethodType.fromMethodDescriptorString("()Z", null)));
        testMethodHandleRef(MethodHandleRef.of(MethodHandleRef.Kind.STATIC, CR_String, "format", CR_String, CR_String, CR_Object.array()),
                            LOOKUP.findStatic(String.class, "format", MethodType.methodType(String.class, String.class, Object[].class)));
        testMethodHandleRef(MethodHandleRef.of(MethodHandleRef.Kind.INTERFACE_VIRTUAL, CR_List, "isEmpty", "()Z"),
                            LOOKUP.findVirtual(List.class, "isEmpty", MethodType.fromMethodDescriptorString("()Z", null)));
        testMethodHandleRef(MethodHandleRef.of(MethodHandleRef.Kind.CONSTRUCTOR, ClassRef.of("java.util.ArrayList"), "<init>", CR_void),
                            LOOKUP.findConstructor(ArrayList.class, MethodType.methodType(void.class)));
    }

    public void testAsType() throws Throwable {
        MethodHandleRef mhr = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, ClassRef.of("java.lang.Integer"), "valueOf",
                                                 MethodTypeRef.of(CR_Integer, CR_int));
            MethodHandleRef takesInteger = mhr.asType(MethodTypeRef.of(CR_Integer, CR_Integer));
        testMethodHandleRef(takesInteger);
        MethodHandle mh1 = takesInteger.resolveConstantRef(LOOKUP);
        assertEquals((Integer) 3, (Integer) mh1.invokeExact((Integer) 3));

        try {
            Integer i = (Integer) mh1.invokeExact(3);
            fail("Expected WMTE");
        }
        catch (WrongMethodTypeException ignored) { }

        MethodHandleRef takesInt = takesInteger.asType(MethodTypeRef.of(CR_Integer, CR_int));
        testMethodHandleRef(takesInt);
        MethodHandle mh2 = takesInt.resolveConstantRef(LOOKUP);
        assertEquals((Integer) 3, (Integer) mh2.invokeExact(3));

        try {
            Integer i = (Integer) mh2.invokeExact((Integer) 3);
            fail("Expected WMTE");
        }
        catch (WrongMethodTypeException ignored) { }

        // @@@ Test short-circuit optimization
        // @@@ Test varargs adaptation
        // @@@ Test bad adaptations and assert runtime error on resolution
        // @@@ Test intrinsification of adapted MH
    }

    public void testMethodHandleRef() throws Throwable {
        MethodHandleRef ctorRef = MethodHandleRef.of(MethodHandleRef.Kind.CONSTRUCTOR, testClass, "<ignored!>", CR_void);
        MethodHandleRef staticMethodRef = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, testClass, "sm", "(I)I");
        MethodHandleRef staticIMethodRef = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, testInterface, "sm", "(I)I");
        MethodHandleRef instanceMethodRef = MethodHandleRef.of(MethodHandleRef.Kind.VIRTUAL, testClass, "m", "(I)I");
        MethodHandleRef instanceIMethodRef = MethodHandleRef.of(MethodHandleRef.Kind.INTERFACE_VIRTUAL, testInterface, "m", "(I)I");
        MethodHandleRef superMethodRef = MethodHandleRef.of(MethodHandleRef.Kind.SPECIAL, testSuperclass, "m", "(I)I");
        MethodHandleRef superIMethodRef = MethodHandleRef.of(MethodHandleRef.Kind.SPECIAL, testInterface, "m", "(I)I");
        MethodHandleRef privateMethodRef = MethodHandleRef.of(MethodHandleRef.Kind.SPECIAL, testClass, "pm", "(I)I");
        MethodHandleRef privateIMethodRef = MethodHandleRef.of(MethodHandleRef.Kind.SPECIAL, testInterface, "pm", "(I)I");
        MethodHandleRef privateStaticMethodRef = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, testClass, "psm", "(I)I");
        MethodHandleRef privateStaticIMethodRef = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, testInterface, "psm", "(I)I");

        for (MethodHandleRef r : List.of(ctorRef, staticMethodRef, staticIMethodRef, instanceMethodRef, instanceIMethodRef))
            testMethodHandleRef(r);

        TestClass instance = (TestClass) ctorRef.resolveConstantRef(LOOKUP).invokeExact();
        TestClass instance2 = (TestClass) ctorRef.resolveConstantRef(TestClass.LOOKUP).invokeExact();
        TestInterface instanceI = instance;

        assertTrue(instance != instance2);

        assertEquals(5, (int) staticMethodRef.resolveConstantRef(LOOKUP).invokeExact(5));
        assertEquals(5, (int) staticMethodRef.resolveConstantRef(TestClass.LOOKUP).invokeExact(5));
        assertEquals(0, (int) staticIMethodRef.resolveConstantRef(LOOKUP).invokeExact(5));
        assertEquals(0, (int) staticIMethodRef.resolveConstantRef(TestClass.LOOKUP).invokeExact(5));

        assertEquals(5, (int) instanceMethodRef.resolveConstantRef(LOOKUP).invokeExact(instance, 5));
        assertEquals(5, (int) instanceMethodRef.resolveConstantRef(TestClass.LOOKUP).invokeExact(instance, 5));
        assertEquals(5, (int) instanceIMethodRef.resolveConstantRef(LOOKUP).invokeExact(instanceI, 5));
        assertEquals(5, (int) instanceIMethodRef.resolveConstantRef(TestClass.LOOKUP).invokeExact(instanceI, 5));

        try { superMethodRef.resolveConstantRef(LOOKUP); fail(); }
        catch (IllegalAccessException e) { /* expected */ }
        assertEquals(-1, (int) superMethodRef.resolveConstantRef(TestClass.LOOKUP).invokeExact(instance, 5));

        try { superIMethodRef.resolveConstantRef(LOOKUP); fail(); }
        catch (IllegalAccessException e) { /* expected */ }
        assertEquals(0, (int) superIMethodRef.resolveConstantRef(TestClass.LOOKUP).invokeExact(instance, 5));

        try { privateMethodRef.resolveConstantRef(LOOKUP); fail(); }
        catch (IllegalAccessException e) { /* expected */ }
        assertEquals(5, (int) privateMethodRef.resolveConstantRef(TestClass.LOOKUP).invokeExact(instance, 5));

        try { privateIMethodRef.resolveConstantRef(LOOKUP); fail(); }
        catch (IllegalAccessException e) { /* expected */ }
        try { privateIMethodRef.resolveConstantRef(TestClass.LOOKUP); fail(); }
        catch (IllegalAccessException e) { /* expected */ }
        assertEquals(0, (int) privateIMethodRef.resolveConstantRef(TestInterface.LOOKUP).invokeExact(instanceI, 5));

        try { privateStaticMethodRef.resolveConstantRef(LOOKUP); fail(); }
        catch (IllegalAccessException e) { /* expected */ }
        assertEquals(5, (int) privateStaticMethodRef.resolveConstantRef(TestClass.LOOKUP).invokeExact(5));

        try { privateStaticIMethodRef.resolveConstantRef(LOOKUP); fail(); }
        catch (IllegalAccessException e) { /* expected */ }
        try { privateStaticIMethodRef.resolveConstantRef(TestClass.LOOKUP); fail(); }
        catch (IllegalAccessException e) { /* expected */ }
        assertEquals(0, (int) privateStaticIMethodRef.resolveConstantRef(TestInterface.LOOKUP).invokeExact(5));

        MethodHandleRef staticSetterRef = MethodHandleRef.ofField(STATIC_SETTER, testClass, "sf", CR_int);
        MethodHandleRef staticGetterRef = MethodHandleRef.ofField(STATIC_GETTER, testClass, "sf", CR_int);
        MethodHandleRef staticGetterIRef = MethodHandleRef.ofField(STATIC_GETTER, testInterface, "sf", CR_int);
        MethodHandleRef setterRef = MethodHandleRef.ofField(SETTER, testClass, "f", CR_int);
        MethodHandleRef getterRef = MethodHandleRef.ofField(GETTER, testClass, "f", CR_int);

        for (MethodHandleRef r : List.of(staticSetterRef, staticGetterRef, staticGetterIRef, setterRef, getterRef))
            testMethodHandleRef(r);

        staticSetterRef.resolveConstantRef(LOOKUP).invokeExact(6); assertEquals(TestClass.sf, 6);
        assertEquals(6, (int) staticGetterRef.resolveConstantRef(LOOKUP).invokeExact());
        assertEquals(6, (int) staticGetterRef.resolveConstantRef(TestClass.LOOKUP).invokeExact());
        staticSetterRef.resolveConstantRef(TestClass.LOOKUP).invokeExact(7); assertEquals(TestClass.sf, 7);
        assertEquals(7, (int) staticGetterRef.resolveConstantRef(LOOKUP).invokeExact());
        assertEquals(7, (int) staticGetterRef.resolveConstantRef(TestClass.LOOKUP).invokeExact());

        assertEquals(3, (int) staticGetterIRef.resolveConstantRef(LOOKUP).invokeExact());
        assertEquals(3, (int) staticGetterIRef.resolveConstantRef(TestClass.LOOKUP).invokeExact());

        setterRef.resolveConstantRef(LOOKUP).invokeExact(instance, 6); assertEquals(instance.f, 6);
        assertEquals(6, (int) getterRef.resolveConstantRef(LOOKUP).invokeExact(instance));
        assertEquals(6, (int) getterRef.resolveConstantRef(TestClass.LOOKUP).invokeExact(instance));
        setterRef.resolveConstantRef(TestClass.LOOKUP).invokeExact(instance, 7); assertEquals(instance.f, 7);
        assertEquals(7, (int) getterRef.resolveConstantRef(LOOKUP).invokeExact(instance));
        assertEquals(7, (int) getterRef.resolveConstantRef(TestClass.LOOKUP).invokeExact(instance));
    }

    private void assertBadArgs(Supplier<MethodHandleRef> supplier, String s) {
        try {
            MethodHandleRef r = supplier.get();
            fail("Expected failure for " + s);
        }
        catch (IllegalArgumentException e) {
            // succeed
        }
    }

    public void testBadFieldMHs() {
        List<String> badGetterDescs = List.of("()V", "(Ljava/lang/Object;)V", "(I)I", "(Ljava/lang/Object;I)I");
        List<String> badStaticGetterDescs = List.of("()V", "(Ljava/lang/Object;)I", "(I)I", "(Ljava/lang/Object;I)I");
        List<String> badSetterDescs = List.of("()V", "(I)V", "(Ljava/lang/Object;)V", "(Ljava/lang/Object;I)I", "(Ljava/lang/Object;II)V");
        List<String> badStaticSetterDescs = List.of("()V", "(II)V", "()I");

        badGetterDescs.forEach(s -> assertBadArgs(() -> MethodHandleRef.of(GETTER, thisClass, "x", s), s));
        badSetterDescs.forEach(s -> assertBadArgs(() -> MethodHandleRef.of(SETTER, thisClass, "x", s), s));
        badStaticGetterDescs.forEach(s -> assertBadArgs(() -> MethodHandleRef.of(STATIC_GETTER, thisClass, "x", s), s));
        badStaticSetterDescs.forEach(s -> assertBadArgs(() -> MethodHandleRef.of(STATIC_SETTER, thisClass, "x", s), s));
    }

    public void testSymbolicRefsConstants() throws ReflectiveOperationException {
        int tested = 0;
        Field[] fields = ConstantRefs.class.getDeclaredFields();
        for (Field f : fields) {
            try {
                if (f.getType().equals(MethodHandleRef.class)
                    && ((f.getModifiers() & Modifier.STATIC) != 0)
                    && ((f.getModifiers() & Modifier.PUBLIC) != 0)) {
                    MethodHandleRef r = (MethodHandleRef) f.get(null);
                    MethodHandle m = r.resolveConstantRef(MethodHandles.lookup());
                    testMethodHandleRef(r, m);
                    ++tested;
                }
            }
            catch (Throwable e) {
                fail("Error testing field " + f.getName(), e);
            }
        }

        assertTrue(tested > 0);
    }

    private interface TestInterface {
        public static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

        public static final int sf = 3;

        static int sm(int  x) { return 0; }
        default int m(int x) { return 0; }
        private int pm(int x) { return 0; }
        private static int psm(int x) { return 0; }
    }

    private static class TestSuperclass {
        public int m(int x) { return -1; }
    }

    private static class TestClass extends TestSuperclass implements TestInterface {
        public static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

        static int sf;
        int f;

        public TestClass()  {}

        public static int sm(int x) { return x; }
        public int m(int x) { return x; }
        private static int psm(int x) { return x; }
        private int pm(int x) { return x; }
    }
}
