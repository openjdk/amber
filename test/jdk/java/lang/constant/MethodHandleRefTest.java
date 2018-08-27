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
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.testng.annotations.Test;

import static java.lang.constant.DirectMethodHandleDesc.Kind.GETTER;
import static java.lang.constant.DirectMethodHandleDesc.Kind.SETTER;
import static java.lang.constant.DirectMethodHandleDesc.Kind.STATIC_GETTER;
import static java.lang.constant.DirectMethodHandleDesc.Kind.STATIC_SETTER;
import static java.lang.constant.DirectMethodHandleDesc.Kind.VIRTUAL;
import static java.lang.constant.ConstantDescs.CR_Integer;
import static java.lang.constant.ConstantDescs.CR_List;
import static java.lang.constant.ConstantDescs.CR_Object;
import static java.lang.constant.ConstantDescs.CR_String;
import static java.lang.constant.ConstantDescs.CR_int;
import static java.lang.constant.ConstantDescs.CR_void;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @test
 * @compile MethodHandleRefTest.java
 * @run testng MethodHandleRefTest
 * @summary unit tests for java.lang.constant.MethodHandleDesc
 */
@Test
public class MethodHandleRefTest extends SymbolicRefTest {
    private static ClassDesc helperHolderClass = ClassDesc.of("TestHelpers");
    private static ClassDesc testClass = helperHolderClass.inner("TestClass");
    private static ClassDesc testInterface = helperHolderClass.inner("TestInterface");
    private static ClassDesc testSuperclass = helperHolderClass.inner("TestSuperclass");


    private static void assertMHEquals(MethodHandle a, MethodHandle b) {
        MethodHandleInfo ia = LOOKUP.revealDirect(a);
        MethodHandleInfo ib = LOOKUP.revealDirect(b);
        assertEquals(ia.getDeclaringClass(), ib.getDeclaringClass());
        assertEquals(ia.getName(), ib.getName());
        assertEquals(ia.getMethodType(), ib.getMethodType());
        assertEquals(ia.getReferenceKind(), ib.getReferenceKind());
    }

    private void testMethodHandleRef(MethodHandleDesc r) throws ReflectiveOperationException {
        if (r instanceof DirectMethodHandleDesc) {
            testSymbolicRef(r);

            DirectMethodHandleDesc rr = (DirectMethodHandleDesc) r;
            assertEquals(r, MethodHandleDesc.of(rr.kind(), rr.owner(), rr.methodName(), r.methodType()));
        }
        else {
            testSymbolicRefForwardOnly(r);
        }
    }

    private void testMethodHandleRef(MethodHandleDesc r, MethodHandle mh) throws ReflectiveOperationException {
        testMethodHandleRef(r);

        assertMHEquals(r.resolveConstantDesc(LOOKUP), mh);
        assertEquals(mh.describeConstable().orElseThrow(), r);

        // compare extractable properties: refKind, owner, name, type
        MethodHandleInfo mhi = LOOKUP.revealDirect(mh);
        DirectMethodHandleDesc rr = (DirectMethodHandleDesc) r;
        assertEquals(mhi.getDeclaringClass().descriptorString(), rr.owner().descriptorString());
        assertEquals(mhi.getName(), rr.methodName());
        assertEquals(mhi.getReferenceKind(), rr.kind().refKind);
        assertEquals(mhi.getMethodType().toMethodDescriptorString(), r.methodType().descriptorString());
    }

    public void testSimpleMHs() throws ReflectiveOperationException {
        testMethodHandleRef(MethodHandleDesc.of(DirectMethodHandleDesc.Kind.VIRTUAL, CR_String, "isEmpty", "()Z"),
                            LOOKUP.findVirtual(String.class, "isEmpty", MethodType.fromMethodDescriptorString("()Z", null)));
        testMethodHandleRef(MethodHandleDesc.of(DirectMethodHandleDesc.Kind.STATIC, CR_String, "format", CR_String, CR_String, CR_Object.arrayType()),
                            LOOKUP.findStatic(String.class, "format", MethodType.methodType(String.class, String.class, Object[].class)));
        testMethodHandleRef(MethodHandleDesc.of(DirectMethodHandleDesc.Kind.INTERFACE_VIRTUAL, CR_List, "isEmpty", "()Z"),
                            LOOKUP.findVirtual(List.class, "isEmpty", MethodType.fromMethodDescriptorString("()Z", null)));
        testMethodHandleRef(MethodHandleDesc.of(DirectMethodHandleDesc.Kind.CONSTRUCTOR, ClassDesc.of("java.util.ArrayList"), "<init>", CR_void),
                            LOOKUP.findConstructor(ArrayList.class, MethodType.methodType(void.class)));
        testMethodHandleRef(MethodHandleDesc.ofConstructor(ClassDesc.of("java.util.ArrayList")),
                            LOOKUP.findConstructor(ArrayList.class, MethodType.methodType(void.class)));
        // bad constructor non void return type
        try {
            MethodHandleDesc.of(DirectMethodHandleDesc.Kind.CONSTRUCTOR, ClassDesc.of("java.util.ArrayList"), "<init>", CR_int);
            fail("should have failed: non void return type for constructor");
        } catch (IllegalArgumentException ex) {
            // good
        }
    }

    public void testAsType() throws Throwable {
        MethodHandleDesc mhr = MethodHandleDesc.of(DirectMethodHandleDesc.Kind.STATIC, ClassDesc.of("java.lang.Integer"), "valueOf",
                                                   MethodTypeDesc.of(CR_Integer, CR_int));
        MethodHandleDesc takesInteger = mhr.asType(MethodTypeDesc.of(CR_Integer, CR_Integer));
        testMethodHandleRef(takesInteger);
        MethodHandle mh1 = takesInteger.resolveConstantDesc(LOOKUP);
        assertEquals((Integer) 3, (Integer) mh1.invokeExact((Integer) 3));
        assertEquals(takesInteger.toString(), "MethodHandleDesc[STATIC/Integer::valueOf(int)Integer].asType(Integer)Integer");

        try {
            Integer i = (Integer) mh1.invokeExact(3);
            fail("Expected WMTE");
        }
        catch (WrongMethodTypeException ignored) { }

        MethodHandleDesc takesInt = takesInteger.asType(MethodTypeDesc.of(CR_Integer, CR_int));
        testMethodHandleRef(takesInt);
        MethodHandle mh2 = takesInt.resolveConstantDesc(LOOKUP);
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
        MethodHandleDesc ctorRef = MethodHandleDesc.of(DirectMethodHandleDesc.Kind.CONSTRUCTOR, testClass, "<ignored!>", CR_void);
        MethodHandleDesc staticMethodRef = MethodHandleDesc.of(DirectMethodHandleDesc.Kind.STATIC, testClass, "sm", "(I)I");
        MethodHandleDesc staticIMethodRef = MethodHandleDesc.of(DirectMethodHandleDesc.Kind.INTERFACE_STATIC, testInterface, "sm", "(I)I");
        MethodHandleDesc instanceMethodRef = MethodHandleDesc.of(DirectMethodHandleDesc.Kind.VIRTUAL, testClass, "m", "(I)I");
        MethodHandleDesc instanceIMethodRef = MethodHandleDesc.of(DirectMethodHandleDesc.Kind.INTERFACE_VIRTUAL, testInterface, "m", "(I)I");
        MethodHandleDesc superMethodRef = MethodHandleDesc.of(DirectMethodHandleDesc.Kind.SPECIAL, testSuperclass, "m", "(I)I");
        MethodHandleDesc superIMethodRef = MethodHandleDesc.of(DirectMethodHandleDesc.Kind.INTERFACE_SPECIAL, testInterface, "m", "(I)I");
        MethodHandleDesc privateMethodRef = MethodHandleDesc.of(DirectMethodHandleDesc.Kind.SPECIAL, testClass, "pm", "(I)I");
        MethodHandleDesc privateIMethodRef = MethodHandleDesc.of(DirectMethodHandleDesc.Kind.INTERFACE_SPECIAL, testInterface, "pm", "(I)I");
        MethodHandleDesc privateStaticMethodRef = MethodHandleDesc.of(DirectMethodHandleDesc.Kind.STATIC, testClass, "psm", "(I)I");
        MethodHandleDesc privateStaticIMethodRef = MethodHandleDesc.of(DirectMethodHandleDesc.Kind.INTERFACE_STATIC, testInterface, "psm", "(I)I");

        for (MethodHandleDesc r : List.of(ctorRef, staticMethodRef, staticIMethodRef, instanceMethodRef, instanceIMethodRef))
            testMethodHandleRef(r);

        TestHelpers.TestClass instance = (TestHelpers.TestClass) ctorRef.resolveConstantDesc(LOOKUP).invokeExact();
        TestHelpers.TestClass instance2 = (TestHelpers.TestClass) ctorRef.resolveConstantDesc(TestHelpers.TestClass.LOOKUP).invokeExact();
        TestHelpers.TestInterface instanceI = instance;

        assertNotSame(instance, instance2);

        assertEquals(5, (int) staticMethodRef.resolveConstantDesc(LOOKUP).invokeExact(5));
        assertEquals(5, (int) staticMethodRef.resolveConstantDesc(TestHelpers.TestClass.LOOKUP).invokeExact(5));
        assertEquals(0, (int) staticIMethodRef.resolveConstantDesc(LOOKUP).invokeExact(5));
        assertEquals(0, (int) staticIMethodRef.resolveConstantDesc(TestHelpers.TestClass.LOOKUP).invokeExact(5));

        assertEquals(5, (int) instanceMethodRef.resolveConstantDesc(LOOKUP).invokeExact(instance, 5));
        assertEquals(5, (int) instanceMethodRef.resolveConstantDesc(TestHelpers.TestClass.LOOKUP).invokeExact(instance, 5));
        assertEquals(5, (int) instanceIMethodRef.resolveConstantDesc(LOOKUP).invokeExact(instanceI, 5));
        assertEquals(5, (int) instanceIMethodRef.resolveConstantDesc(TestHelpers.TestClass.LOOKUP).invokeExact(instanceI, 5));

        try { superMethodRef.resolveConstantDesc(LOOKUP); fail(); }
        catch (IllegalAccessException e) { /* expected */ }
        assertEquals(-1, (int) superMethodRef.resolveConstantDesc(TestHelpers.TestClass.LOOKUP).invokeExact(instance, 5));

        try { superIMethodRef.resolveConstantDesc(LOOKUP); fail(); }
        catch (IllegalAccessException e) { /* expected */ }
        assertEquals(0, (int) superIMethodRef.resolveConstantDesc(TestHelpers.TestClass.LOOKUP).invokeExact(instance, 5));

        try { privateMethodRef.resolveConstantDesc(LOOKUP); fail(); }
        catch (IllegalAccessException e) { /* expected */ }
        assertEquals(5, (int) privateMethodRef.resolveConstantDesc(TestHelpers.TestClass.LOOKUP).invokeExact(instance, 5));

        try { privateIMethodRef.resolveConstantDesc(LOOKUP); fail(); }
        catch (IllegalAccessException e) { /* expected */ }
        assertEquals(0, (int) privateIMethodRef.resolveConstantDesc(TestHelpers.TestInterface.LOOKUP).invokeExact(instanceI, 5));
        assertEquals(0, (int) privateIMethodRef.resolveConstantDesc(TestHelpers.TestClass.LOOKUP).invoke(instanceI, 5));

        try { privateStaticMethodRef.resolveConstantDesc(LOOKUP); fail(); }
        catch (IllegalAccessException e) { /* expected */ }
        assertEquals(5, (int) privateStaticMethodRef.resolveConstantDesc(TestHelpers.TestClass.LOOKUP).invokeExact(5));

        try { privateStaticIMethodRef.resolveConstantDesc(LOOKUP); fail(); }
        catch (IllegalAccessException e) { /* expected */ }
        assertEquals(0, (int) privateStaticIMethodRef.resolveConstantDesc(TestHelpers.TestInterface.LOOKUP).invokeExact(5));
        assertEquals(0, (int) privateStaticIMethodRef.resolveConstantDesc(TestHelpers.TestClass.LOOKUP).invokeExact(5));

        MethodHandleDesc staticSetterRef = MethodHandleDesc.ofField(STATIC_SETTER, testClass, "sf", CR_int);
        MethodHandleDesc staticGetterRef = MethodHandleDesc.ofField(STATIC_GETTER, testClass, "sf", CR_int);
        MethodHandleDesc staticGetterIRef = MethodHandleDesc.ofField(STATIC_GETTER, testInterface, "sf", CR_int);
        MethodHandleDesc setterRef = MethodHandleDesc.ofField(SETTER, testClass, "f", CR_int);
        MethodHandleDesc getterRef = MethodHandleDesc.ofField(GETTER, testClass, "f", CR_int);

        for (MethodHandleDesc r : List.of(staticSetterRef, staticGetterRef, staticGetterIRef, setterRef, getterRef))
            testMethodHandleRef(r);

        staticSetterRef.resolveConstantDesc(LOOKUP).invokeExact(6); assertEquals(TestHelpers.TestClass.sf, 6);
        assertEquals(6, (int) staticGetterRef.resolveConstantDesc(LOOKUP).invokeExact());
        assertEquals(6, (int) staticGetterRef.resolveConstantDesc(TestHelpers.TestClass.LOOKUP).invokeExact());
        staticSetterRef.resolveConstantDesc(TestHelpers.TestClass.LOOKUP).invokeExact(7); assertEquals(TestHelpers.TestClass.sf, 7);
        assertEquals(7, (int) staticGetterRef.resolveConstantDesc(LOOKUP).invokeExact());
        assertEquals(7, (int) staticGetterRef.resolveConstantDesc(TestHelpers.TestClass.LOOKUP).invokeExact());

        assertEquals(3, (int) staticGetterIRef.resolveConstantDesc(LOOKUP).invokeExact());
        assertEquals(3, (int) staticGetterIRef.resolveConstantDesc(TestHelpers.TestClass.LOOKUP).invokeExact());

        setterRef.resolveConstantDesc(LOOKUP).invokeExact(instance, 6); assertEquals(instance.f, 6);
        assertEquals(6, (int) getterRef.resolveConstantDesc(LOOKUP).invokeExact(instance));
        assertEquals(6, (int) getterRef.resolveConstantDesc(TestHelpers.TestClass.LOOKUP).invokeExact(instance));
        setterRef.resolveConstantDesc(TestHelpers.TestClass.LOOKUP).invokeExact(instance, 7); assertEquals(instance.f, 7);
        assertEquals(7, (int) getterRef.resolveConstantDesc(LOOKUP).invokeExact(instance));
        assertEquals(7, (int) getterRef.resolveConstantDesc(TestHelpers.TestClass.LOOKUP).invokeExact(instance));
    }

    private void assertBadArgs(Supplier<MethodHandleDesc> supplier, String s) {
        try {
            MethodHandleDesc r = supplier.get();
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

        badGetterDescs.forEach(s -> assertBadArgs(() -> MethodHandleDesc.of(GETTER, helperHolderClass, "x", s), s));
        badSetterDescs.forEach(s -> assertBadArgs(() -> MethodHandleDesc.of(SETTER, helperHolderClass, "x", s), s));
        badStaticGetterDescs.forEach(s -> assertBadArgs(() -> MethodHandleDesc.of(STATIC_GETTER, helperHolderClass, "x", s), s));
        badStaticSetterDescs.forEach(s -> assertBadArgs(() -> MethodHandleDesc.of(STATIC_SETTER, helperHolderClass, "x", s), s));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBadOwners() {
        MethodHandleDesc.of(VIRTUAL, ClassDesc.ofDescriptor("I"), "x", MethodTypeDesc.ofDescriptor("()I"));
    }

    public void testSymbolicRefsConstants() throws ReflectiveOperationException {
        int tested = 0;
        Field[] fields = ConstantDescs.class.getDeclaredFields();
        for (Field f : fields) {
            try {
                if (f.getType().equals(DirectMethodHandleDesc.class)
                    && ((f.getModifiers() & Modifier.STATIC) != 0)
                    && ((f.getModifiers() & Modifier.PUBLIC) != 0)) {
                    MethodHandleDesc r = (MethodHandleDesc) f.get(null);
                    MethodHandle m = r.resolveConstantDesc(MethodHandles.lookup());
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
}
