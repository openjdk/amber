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
import java.lang.invoke.VarHandle;
import java.lang.sym.ClassRef;
import java.lang.sym.DynamicConstantRef;
import java.lang.sym.MethodHandleRef;
import java.lang.sym.MethodTypeRef;
import java.lang.sym.SymbolicRef;
import java.lang.sym.SymbolicRefs;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import static java.lang.sym.MethodHandleRef.Kind.GETTER;
import static java.lang.sym.MethodHandleRef.Kind.SETTER;
import static java.lang.sym.MethodHandleRef.Kind.STATIC_GETTER;
import static java.lang.sym.MethodHandleRef.Kind.STATIC_SETTER;
import static java.lang.sym.SymbolicRefs.CR_List;
import static java.lang.sym.SymbolicRefs.CR_Object;
import static java.lang.sym.SymbolicRefs.CR_String;
import static java.lang.sym.SymbolicRefs.CR_void;
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
        testSymbolicRef(r);

        assertEquals(r, MethodHandleRef.of(r.kind(), r.owner(), r.name(), r.type()));
    }

    private void testMethodHandleRef(MethodHandleRef r, MethodHandle mh) throws ReflectiveOperationException {
        testMethodHandleRef(r);

        assertMHEquals(r.resolveRef(LOOKUP), mh);
        assertEquals(mh.toSymbolicRef(LOOKUP).get(), r);

        // compare extractable properties: refKind, owner, name, type
        MethodHandleInfo mhi = LOOKUP.revealDirect(mh);
        assertEquals(mhi.getDeclaringClass().toDescriptorString(), r.owner().descriptorString());
        assertEquals(mhi.getName(), r.name());
        assertEquals(mhi.getReferenceKind(), r.kind().refKind);
        assertEquals(mhi.getMethodType().toMethodDescriptorString(), r.type().descriptorString());
    }

    private void testVarHandleRef(DynamicConstantRef<VarHandle> r, VarHandle vh) throws ReflectiveOperationException {
//        testSymbolicRef(r);
//        assertEquals(r.resolveRef(LOOKUP), vh);

        // @@@ VarHandle not yet Constable
//        assertEquals(vh.toSymbolicRef(LOOKUP).get(), r);
        // @@@ Test other assertable properties
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

        TestClass instance = (TestClass) ctorRef.resolveRef(LOOKUP).invokeExact();
        TestClass instance2 = (TestClass) ctorRef.resolveRef(TestClass.LOOKUP).invokeExact();
        TestInterface instanceI = instance;

        assertTrue(instance != instance2);

        assertEquals(5, (int) staticMethodRef.resolveRef(LOOKUP).invokeExact(5));
        assertEquals(5, (int) staticMethodRef.resolveRef(TestClass.LOOKUP).invokeExact(5));
        assertEquals(0, (int) staticIMethodRef.resolveRef(LOOKUP).invokeExact(5));
        assertEquals(0, (int) staticIMethodRef.resolveRef(TestClass.LOOKUP).invokeExact(5));

        assertEquals(5, (int) instanceMethodRef.resolveRef(LOOKUP).invokeExact(instance, 5));
        assertEquals(5, (int) instanceMethodRef.resolveRef(TestClass.LOOKUP).invokeExact(instance, 5));
        assertEquals(5, (int) instanceIMethodRef.resolveRef(LOOKUP).invokeExact(instanceI, 5));
        assertEquals(5, (int) instanceIMethodRef.resolveRef(TestClass.LOOKUP).invokeExact(instanceI, 5));

        try { superMethodRef.resolveRef(LOOKUP); fail(); }
        catch (IllegalAccessException e) { /* expected */ }
        assertEquals(-1, (int) superMethodRef.resolveRef(TestClass.LOOKUP).invokeExact(instance, 5));

        try { superIMethodRef.resolveRef(LOOKUP); fail(); }
        catch (IllegalAccessException e) { /* expected */ }
        assertEquals(0, (int) superIMethodRef.resolveRef(TestClass.LOOKUP).invokeExact(instance, 5));

        try { privateMethodRef.resolveRef(LOOKUP); fail(); }
        catch (IllegalAccessException e) { /* expected */ }
        assertEquals(5, (int) privateMethodRef.resolveRef(TestClass.LOOKUP).invokeExact(instance, 5));

        try { privateIMethodRef.resolveRef(LOOKUP); fail(); }
        catch (IllegalAccessException e) { /* expected */ }
        try { privateIMethodRef.resolveRef(TestClass.LOOKUP); fail(); }
        catch (IllegalAccessException e) { /* expected */ }
        assertEquals(0, (int) privateIMethodRef.resolveRef(TestInterface.LOOKUP).invokeExact(instanceI, 5));

        try { privateStaticMethodRef.resolveRef(LOOKUP); fail(); }
        catch (IllegalAccessException e) { /* expected */ }
        assertEquals(5, (int) privateStaticMethodRef.resolveRef(TestClass.LOOKUP).invokeExact(5));

        try { privateStaticIMethodRef.resolveRef(LOOKUP); fail(); }
        catch (IllegalAccessException e) { /* expected */ }
        try { privateStaticIMethodRef.resolveRef(TestClass.LOOKUP); fail(); }
        catch (IllegalAccessException e) { /* expected */ }
        assertEquals(0, (int) privateStaticIMethodRef.resolveRef(TestInterface.LOOKUP).invokeExact(5));

        MethodHandleRef staticSetterRef = MethodHandleRef.ofField(STATIC_SETTER, testClass, "sf", SymbolicRefs.CR_int);
        MethodHandleRef staticGetterRef = MethodHandleRef.ofField(STATIC_GETTER, testClass, "sf", SymbolicRefs.CR_int);
        MethodHandleRef staticGetterIRef = MethodHandleRef.ofField(STATIC_GETTER, testInterface, "sf", SymbolicRefs.CR_int);
        MethodHandleRef setterRef = MethodHandleRef.ofField(SETTER, testClass, "f", SymbolicRefs.CR_int);
        MethodHandleRef getterRef = MethodHandleRef.ofField(GETTER, testClass, "f", SymbolicRefs.CR_int);

        for (MethodHandleRef r : List.of(staticSetterRef, staticGetterRef, staticGetterIRef, setterRef, getterRef))
            testMethodHandleRef(r);

        staticSetterRef.resolveRef(LOOKUP).invokeExact(6); assertEquals(TestClass.sf, 6);
        assertEquals(6, (int) staticGetterRef.resolveRef(LOOKUP).invokeExact());
        assertEquals(6, (int) staticGetterRef.resolveRef(TestClass.LOOKUP).invokeExact());
        staticSetterRef.resolveRef(TestClass.LOOKUP).invokeExact(7); assertEquals(TestClass.sf, 7);
        assertEquals(7, (int) staticGetterRef.resolveRef(LOOKUP).invokeExact());
        assertEquals(7, (int) staticGetterRef.resolveRef(TestClass.LOOKUP).invokeExact());

        assertEquals(3, (int) staticGetterIRef.resolveRef(LOOKUP).invokeExact());
        assertEquals(3, (int) staticGetterIRef.resolveRef(TestClass.LOOKUP).invokeExact());

        setterRef.resolveRef(LOOKUP).invokeExact(instance, 6); assertEquals(instance.f, 6);
        assertEquals(6, (int) getterRef.resolveRef(LOOKUP).invokeExact(instance));
        assertEquals(6, (int) getterRef.resolveRef(TestClass.LOOKUP).invokeExact(instance));
        setterRef.resolveRef(TestClass.LOOKUP).invokeExact(instance, 7); assertEquals(instance.f, 7);
        assertEquals(7, (int) getterRef.resolveRef(LOOKUP).invokeExact(instance));
        assertEquals(7, (int) getterRef.resolveRef(TestClass.LOOKUP).invokeExact(instance));
    }

    // This test method belongs elsewhere; move when we revamp VarHandleRef API
    public void testVarHandles() throws ReflectiveOperationException {
        TestClass instance = new TestClass();
        int[] ints = new int[3];

        // static varHandle
        DynamicConstantRef<VarHandle> vhc = (DynamicConstantRef<VarHandle>) SymbolicRef.staticFieldVarHandle(testClass, "sf", SymbolicRefs.CR_int);
        MethodTypeRef methodTypeRef = vhc.bootstrapMethod().type();
        System.out.println(vhc.name() + " " + methodTypeRef.returnType() + " " + methodTypeRef.parameterList());
        VarHandle varHandle = vhc.resolveRef(LOOKUP);
        testVarHandleRef(vhc, varHandle);
        assertEquals(varHandle.varType(), int.class);
        varHandle.set(8);
        assertEquals(8, (int) varHandle.get());
        assertEquals(TestClass.sf, 8);

        // static varHandle
        vhc = (DynamicConstantRef<VarHandle>) SymbolicRef.fieldVarHandle(testClass, "f", SymbolicRefs.CR_int);
        varHandle = vhc.resolveRef(LOOKUP);
        testVarHandleRef(vhc, varHandle);
        assertEquals(varHandle.varType(), int.class);
        varHandle.set(instance, 9);
        assertEquals(9, (int) varHandle.get(instance));
        assertEquals(instance.f, 9);

        vhc = (DynamicConstantRef<VarHandle>) SymbolicRef.arrayVarHandle(SymbolicRefs.CR_int.array());
        varHandle = vhc.resolveRef(LOOKUP);
        testVarHandleRef(vhc, varHandle);
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
