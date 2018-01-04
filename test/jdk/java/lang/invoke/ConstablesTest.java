/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.sym.EnumRef;
import java.lang.sym.SymbolicRef;
import java.lang.sym.DynamicConstantRef;
import java.lang.invoke.Intrinsics;
import java.lang.sym.MethodHandleRef;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.sym.ClassRef;
import java.lang.sym.MethodTypeRef;
import java.lang.invoke.VarHandle;
import java.lang.sym.SymbolicRefs;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.testng.annotations.Test;

import static java.lang.sym.MethodHandleRef.Kind.GETTER;
import static java.lang.sym.MethodHandleRef.Kind.SETTER;
import static java.lang.sym.MethodHandleRef.Kind.STATIC_GETTER;
import static java.lang.sym.MethodHandleRef.Kind.STATIC_SETTER;
import static java.util.stream.Collectors.joining;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @test
 * @compile -XDdoConstantFold ConstablesTest.java
 * @run testng ConstablesTest
 * @summary unit tests for Constables factories and symbolic reference classes (ClassRef, etc)
 * @author Brian Goetz
 */
@Test
public class ConstablesTest {

    public static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();


    public void testLdcClassRef() {
        assertEquals(String.class, Intrinsics.ldc(ClassRef.ofDescriptor("Ljava/lang/String;")));
//        assertEquals(String.class, Intrinsics.ldc(ClassRef.of(String.class)));
        assertEquals(String.class, Intrinsics.ldc(ClassRef.of("java.lang.String")));
        assertEquals(String.class, Intrinsics.ldc(ClassRef.of("java.lang", "String")));

        assertEquals(String[].class, Intrinsics.ldc(ClassRef.ofDescriptor("[Ljava/lang/String;")));
        assertEquals(String[].class, Intrinsics.ldc(ClassRef.of("java.lang.String").array()));
//        assertEquals(String[].class, Intrinsics.ldc(ClassRef.of(String.class).arrayOf()));
//        assertEquals(String[].class, Intrinsics.ldc(ClassRef.of(String[].class)));

        assertEquals(int.class, Intrinsics.ldc(ClassRef.ofDescriptor("I")));
        assertEquals(int.class, Intrinsics.ldc(SymbolicRefs.CR_int));
//        assertEquals(int.class, Intrinsics.ldc(ClassRef.of(int.class)));

        assertEquals(int[].class, Intrinsics.ldc(ClassRef.ofDescriptor("[I")));
        assertEquals(int[].class, Intrinsics.ldc(SymbolicRefs.CR_int.array()));
//        assertEquals(int[].class, Intrinsics.ldc(ClassRef.of(int.class).arrayOf()));
    }

    public void testLdcMethodType() {
        assertEquals(MethodType.methodType(void.class), Intrinsics.ldc(MethodTypeRef.ofDescriptor("()V")));
        assertEquals(MethodType.methodType(void.class), Intrinsics.ldc(MethodTypeRef.of(SymbolicRefs.CR_void)));

        assertEquals(MethodType.methodType(int.class, int.class),
                     Intrinsics.ldc(MethodTypeRef.ofDescriptor("(I)I")));
        assertEquals(MethodType.methodType(int.class, int.class),
                     Intrinsics.ldc(MethodTypeRef.of(SymbolicRefs.CR_int, SymbolicRefs.CR_int)));

        assertEquals(MethodType.methodType(String.class, String.class),
                     Intrinsics.ldc(MethodTypeRef.ofDescriptor("(Ljava/lang/String;)Ljava/lang/String;")));
        assertEquals(MethodType.methodType(String.class, String.class),
                     Intrinsics.ldc(MethodTypeRef.of(ClassRef.of("java.lang.String"), ClassRef.of("java.lang.String"))));

        assertEquals(MethodType.methodType(String[].class, int[].class),
                     Intrinsics.ldc(MethodTypeRef.ofDescriptor("([I)[Ljava/lang/String;")));
    }


    public void testLdcMethodHandle() throws Throwable {
        ldcMethodHandleTestsFromOuter();
        TestClass.ldcMethodHandleTestsFromClass();
    }
    
    private void ldcMethodHandleTestsFromOuter() throws Throwable {
        ClassRef thisClass = ClassRef.of("ConstablesTest");
        ClassRef testClass = thisClass.inner("TestClass");
        ClassRef testInterface = thisClass.inner("TestInterface");
        ClassRef testSuperclass = thisClass.inner("TestSuperclass");

        MethodHandleRef ctorRef = MethodHandleRef.of(MethodHandleRef.Kind.CONSTRUCTOR, testClass, "<ignored!>", MethodTypeRef.ofDescriptor("()V"));
        MethodHandleRef staticMethodRef = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, testClass, "sm", "(I)I");
        MethodHandleRef staticIMethodRef = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, testInterface, "sm", "(I)I");
        MethodHandleRef instanceMethodRef = MethodHandleRef.of(MethodHandleRef.Kind.VIRTUAL, testClass, "m", MethodTypeRef.ofDescriptor("(I)I"));
        MethodHandleRef instanceIMethodRef = MethodHandleRef.of(MethodHandleRef.Kind.INTERFACE_VIRTUAL, testInterface, "m", MethodTypeRef.ofDescriptor("(I)I"));
        MethodHandleRef superMethodRef = MethodHandleRef.of(MethodHandleRef.Kind.SPECIAL, testSuperclass, "m", "(I)I");
        MethodHandleRef superIMethodRef = MethodHandleRef.of(MethodHandleRef.Kind.SPECIAL, testInterface, "m", "(I)I");
        MethodHandleRef privateMethodRef = MethodHandleRef.of(MethodHandleRef.Kind.SPECIAL, testClass, "pm", MethodTypeRef.ofDescriptor("(I)I"));
        MethodHandleRef privateIMethodRef = MethodHandleRef.of(MethodHandleRef.Kind.SPECIAL, testInterface, "pm", MethodTypeRef.ofDescriptor("(I)I"));
        MethodHandleRef privateStaticMethodRef = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, testClass, "psm", MethodTypeRef.ofDescriptor("(I)I"));
        MethodHandleRef privateStaticIMethodRef = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, testInterface, "psm", MethodTypeRef.ofDescriptor("(I)I"));

        TestClass instance = (TestClass) Intrinsics.ldc(ctorRef).invokeExact();
        TestInterface instanceI = instance;
        
        assertEquals(5, (int) Intrinsics.ldc(staticMethodRef).invokeExact(5));
        assertEquals(0, (int) Intrinsics.ldc(staticIMethodRef).invokeExact(5));
        
        assertEquals(5, (int) Intrinsics.ldc(instanceMethodRef).invokeExact(instance, 5));
        assertEquals(5, (int) Intrinsics.ldc(instanceIMethodRef).invokeExact(instanceI, 5));

        try { Intrinsics.ldc(superMethodRef); fail(); }
        catch (IllegalAccessError e) { /* expected */ }
        try { Intrinsics.ldc(superIMethodRef); fail(); }
        catch (IllegalAccessError e) { /* expected */ }
        
        try { Intrinsics.ldc(privateMethodRef); fail(); }
        catch (IllegalAccessError e) { /* expected */ }
        try { Intrinsics.ldc(privateIMethodRef); fail(); }
        catch (IllegalAccessError e) { /* expected */ }
        
        try { Intrinsics.ldc(privateStaticMethodRef); fail(); }
        catch (IllegalAccessError e) { /* expected */ }
        try { Intrinsics.ldc(privateStaticIMethodRef); fail(); }
        catch (IllegalAccessError e) { /* expected */ }

        MethodHandleRef staticSetterRef = MethodHandleRef.ofField(STATIC_SETTER, testClass, "sf", SymbolicRefs.CR_int);
        MethodHandleRef staticGetterRef = MethodHandleRef.ofField(STATIC_GETTER, testClass, "sf", SymbolicRefs.CR_int);
        MethodHandleRef staticGetterIRef = MethodHandleRef.ofField(STATIC_GETTER, testInterface, "sf", SymbolicRefs.CR_int);
        MethodHandleRef setterRef = MethodHandleRef.ofField(SETTER, testClass, "f", SymbolicRefs.CR_int);
        MethodHandleRef getterRef = MethodHandleRef.ofField(GETTER, testClass, "f", SymbolicRefs.CR_int);

        Intrinsics.ldc(staticSetterRef).invokeExact(8); assertEquals(TestClass.sf, 8);
        assertEquals(8, (int) Intrinsics.ldc(staticGetterRef).invokeExact());

        //assertEquals(3, (int) Intrinsics.ldc(staticGetterIRef).invokeExact());

        Intrinsics.ldc(setterRef).invokeExact(instance, 9); assertEquals(instance.f, 9);
        assertEquals(9, (int) Intrinsics.ldc(getterRef).invokeExact(instance));
    }


    static String classToDescriptor(Class<?> clazz) {
        return MethodType.methodType(clazz).toMethodDescriptorString().substring(2);
    }

    private static int privateStaticMethod(int i) {
        return i;
    }

    private int privateMethod(int i) {
        return i;
    }


}
