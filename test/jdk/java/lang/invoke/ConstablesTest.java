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
import java.lang.invoke.ConstantRef;
import java.lang.invoke.DynamicConstantRef;
import java.lang.invoke.Intrinsics;
import java.lang.invoke.MethodHandleRef;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.ClassRef;
import java.lang.invoke.MethodTypeRef;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.testng.annotations.Test;

import static java.lang.invoke.ClassRef.*;
import static java.lang.invoke.MethodHandleRef.Kind.GETTER;
import static java.lang.invoke.MethodHandleRef.Kind.SETTER;
import static java.lang.invoke.MethodHandleRef.Kind.STATIC_GETTER;
import static java.lang.invoke.MethodHandleRef.Kind.STATIC_SETTER;
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

    enum Primitives {
        INT("I", "int", int.class, int[].class, CR_int),
        LONG("J", "long", long.class, long[].class, CR_long),
        SHORT("S", "short", short.class, short[].class, CR_short),
        BYTE("B", "byte", byte.class, byte[].class, CR_byte),
        CHAR("C", "char", char.class, char[].class, CR_char),
        FLOAT("F", "float", float.class, float[].class, CR_float),
        DOUBLE("D", "double", double.class, double[].class, CR_double),
        BOOLEAN("Z", "boolean", boolean.class, boolean[].class, CR_boolean),
        VOID("V", "void", void.class, null, CR_void);

        public final String descriptor;
        public final String name;
        public final Class<?> clazz;
        public final Class<?> arrayClass;
        public final ClassRef classRef;

        Primitives(String descriptor, String name, Class<?> clazz, Class<?> arrayClass, ClassRef ref) {
            this.descriptor = descriptor;
            this.name = name;
            this.clazz = clazz;
            this.arrayClass = arrayClass;
            classRef = ref;
        }
    }

    static List<String> someRefs = List.of("Ljava/lang/String;", "Ljava/util/List;");
    static String[] basicDescs = Stream.concat(Stream.of(Primitives.values()).filter(p -> p != Primitives.VOID).map(p -> p.descriptor),
                                               someRefs.stream())
                                       .toArray(String[]::new);
    static String[] paramDescs = Stream.of(basicDescs).flatMap(d -> Stream.of(d, "[" + d))
                                       .toArray(String[]::new);
    static String[] returnDescs = Stream.concat(Stream.of(paramDescs), Stream.of("V")).toArray(String[]::new);


    public void testPrimitiveClassRef() throws ReflectiveOperationException {
        for (Primitives p : Primitives.values()) {
            ClassRef c1 = ClassRef.ofDescriptor(p.descriptor);
            ClassRef c2 = p.classRef;
            assertTrue(c1.isPrimitive());
            assertTrue(c2.isPrimitive());
            assertEquals(p.descriptor, c1.descriptorString());
            assertEquals(p.descriptor, c2.descriptorString());
            assertEquals(p.name, c1.canonicalName());
            assertEquals(c1, c2);
            assertEquals(c1.resolve(LOOKUP), p.clazz);
            assertEquals(c2.resolve(LOOKUP), p.clazz);
            if (p != Primitives.VOID) {
                assertEquals(c1.array().resolve(LOOKUP), p.arrayClass);
                assertEquals(c2.array().resolve(LOOKUP), p.arrayClass);
            }

            for (Primitives other : Primitives.values()) {
                ClassRef otherDescr = ClassRef.ofDescriptor(other.descriptor);
                if (p != other)
                    assertNotEquals(c1, otherDescr);
                else
                    assertEquals(c1, otherDescr);
            }
        }
    }

    public void testSimpleClassRef() throws ReflectiveOperationException {
        String stringDesc = "Ljava/lang/String;";
        ClassRef stringRef1 = ClassRef.ofDescriptor(stringDesc);
        ClassRef stringRef2 = ClassRef.of("java.lang", "String");
        ClassRef stringRef3 = ClassRef.of("java.lang.String");

        List<ClassRef> refs = Arrays.asList(stringRef1, stringRef2, stringRef3);

        refs.forEach(s -> assertFalse(s.isPrimitive()));
        refs.forEach(s -> assertEquals(stringDesc, s.descriptorString()));
        refs.forEach(s -> assertEquals("java.lang.String", s.canonicalName()));
        for (ClassRef r : refs)
            assertEquals(r.resolve(LOOKUP), String.class);

        assertEquals(stringRef1, stringRef2);
        assertEquals(stringRef2, stringRef3);
        assertEquals(stringRef3, stringRef1);

        ClassRef thisClassRef = ClassRef.ofDescriptor("LConstablesTest;");
        assertEquals(thisClassRef, ClassRef.of("", "ConstablesTest"));
        assertEquals(thisClassRef, ClassRef.of("ConstablesTest"));
        assertEquals(thisClassRef.canonicalName(), "ConstablesTest");

    }

    public void testArrayClassRef() throws ReflectiveOperationException {
        for (String d : basicDescs) {
            ClassRef a0 = ClassRef.ofDescriptor(d);
            ClassRef a1 = a0.array();
            ClassRef a2 = a1.array();

            assertFalse(a0.isArray());
            assertTrue(a1.isArray());
            assertTrue(a2.isArray());
            assertFalse(a1.isPrimitive());
            assertFalse(a2.isPrimitive());
            assertEquals(a0.descriptorString(), d);
            assertEquals(a1.descriptorString(), "[" + a0.descriptorString());
            assertEquals(a2.descriptorString(), "[[" + a0.descriptorString());

            try {
                assertEquals(a0, a0.componentType());
                fail("Didn't throw ISE");
            }
            catch (IllegalStateException expected) {
                // succeed
            }
            assertEquals(a0, a1.componentType());
            assertEquals(a1, a2.componentType());

            assertNotEquals(a0, a1);
            assertNotEquals(a1, a2);

            assertEquals(a1, ClassRef.ofDescriptor("[" + d));
            assertEquals(a2, ClassRef.ofDescriptor("[[" + d));
            assertEquals(classToDescriptor(a0.resolve(LOOKUP)), a0.descriptorString());
            assertEquals(classToDescriptor(a1.resolve(LOOKUP)), a1.descriptorString());
            assertEquals(classToDescriptor(a2.resolve(LOOKUP)), a2.descriptorString());
        }
    }

    public void testBadClassRefs() {
        List<String> badDescriptors = List.of("II", "I;", "Q", "L",
                                              "java.lang.String", "[]", "Ljava/lang/String",
                                              "Ljava.lang.String;", "java/lang/String");

        for (String d : badDescriptors) {
            try {
                ClassRef constant = ClassRef.ofDescriptor(d);
                fail(d);
            }
            catch (IllegalArgumentException e) {
                // good
            }
        }
    }

    private void assertMethodType(String returnDesc,
                                  String... paramDescs) throws ReflectiveOperationException {
        String descriptor = Stream.of(paramDescs).collect(joining("", "(", ")")) + returnDesc;
        ClassRef ret = ClassRef.ofDescriptor(returnDesc);
        ClassRef[] params = Stream.of(paramDescs).map(ClassRef::ofDescriptor).toArray(ClassRef[]::new);

        MethodTypeRef mtRef = MethodTypeRef.of(ret, params);
        MethodType mt = mtRef.resolve(LOOKUP);

        assertEquals(mtRef, MethodTypeRef.ofDescriptor(descriptor));
        assertEquals(mt.toMethodDescriptorString(), descriptor);

        assertEquals(descriptor, mtRef.descriptorString());
        assertEquals(returnDesc, mtRef.returnType().descriptorString());
        assertEquals(paramDescs.length, mtRef.parameterCount());
        for (int i=0; i<paramDescs.length; i++) {
            assertEquals(params[i], mtRef.parameterType(i));
            assertEquals(paramDescs[i], mtRef.parameterType(i).descriptorString());
        }

        // changeReturnType
        for (String r : returnDescs) {
            ClassRef rc = ClassRef.ofDescriptor(r);
            MethodTypeRef changed = mtRef.changeReturnType(rc);
            assertEquals(changed, MethodTypeRef.of(rc, params));
            assertEquals(mt.changeReturnType(rc.resolve(LOOKUP)).toMethodDescriptorString(), changed.descriptorString());
        }

        // changeParamType
        for (int i=0; i<paramDescs.length; i++) {
            for (String p : paramDescs) {
                ClassRef pc = ClassRef.ofDescriptor(p);
                ClassRef[] ps = params.clone();
                ps[i] = pc;
                MethodTypeRef changed = mtRef.changeParameterType(i, pc);
                assertEquals(changed, MethodTypeRef.of(ret, ps));
                assertEquals(mt.changeParameterType(i, pc.resolve(LOOKUP)).toMethodDescriptorString(), changed.descriptorString());
            }
        }

        // dropParamType
        for (int i=0; i<paramDescs.length; i++) {
            int k = i;
            ClassRef[] ps = IntStream.range(0, paramDescs.length)
                                     .filter(j -> j != k)
                                     .mapToObj(j -> params[j])
                                     .toArray(ClassRef[]::new);
            MethodTypeRef changed = mtRef.dropParameterTypes(i, i + 1);
            assertEquals(changed, MethodTypeRef.of(ret, ps));
            assertEquals(mt.dropParameterTypes(i, i+1).toMethodDescriptorString(), changed.descriptorString());
        }

        // addParam
        for (int i=0; i <= paramDescs.length; i++) {
            for (String p : paramDescs) {
                int k = i;
                ClassRef pc = ClassRef.ofDescriptor(p);
                ClassRef[] ps = IntStream.range(0, paramDescs.length + 1)
                                         .mapToObj(j -> (j < k) ? params[j] : (j == k) ? pc : params[j-1])
                                         .toArray(ClassRef[]::new);
                MethodTypeRef changed = mtRef.insertParameterTypes(i, pc);
                assertEquals(changed, MethodTypeRef.of(ret, ps));
                assertEquals(mt.insertParameterTypes(i, pc.resolve(LOOKUP)).toMethodDescriptorString(), changed.descriptorString());
            }
        }
    }

    public void testMethodTypeRef() throws ReflectiveOperationException {
        for (String r : returnDescs) {
            assertMethodType(r);
            for (String p1 : paramDescs) {
                assertMethodType(r, p1);
                for (String p2 : paramDescs) {
                    assertMethodType(r, p1, p2);
                }
            }
        }
    }

    public void testMethodHandleRef() throws Throwable {
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

        TestClass instance = (TestClass) ctorRef.resolve(LOOKUP).invokeExact();
        instance = (TestClass) ctorRef.resolve(TestClass.LOOKUP).invokeExact();
        TestInterface instanceI = instance;
        
        assertEquals(5, (int) staticMethodRef.resolve(LOOKUP).invokeExact(5));
        assertEquals(5, (int) staticMethodRef.resolve(TestClass.LOOKUP).invokeExact(5));
        assertEquals(0, (int) staticIMethodRef.resolve(LOOKUP).invokeExact(5));
        assertEquals(0, (int) staticIMethodRef.resolve(TestClass.LOOKUP).invokeExact(5));
        
        assertEquals(5, (int) instanceMethodRef.resolve(LOOKUP).invokeExact(instance, 5));
        assertEquals(5, (int) instanceMethodRef.resolve(TestClass.LOOKUP).invokeExact(instance, 5));
        assertEquals(5, (int) instanceIMethodRef.resolve(LOOKUP).invokeExact(instanceI, 5));
        assertEquals(5, (int) instanceIMethodRef.resolve(TestClass.LOOKUP).invokeExact(instanceI, 5));

        try { superMethodRef.resolve(LOOKUP); fail(); }
        catch (IllegalAccessException e) { /* expected */ }
        assertEquals(-1, (int) superMethodRef.resolve(TestClass.LOOKUP).invokeExact(instance, 5));
        try { superIMethodRef.resolve(LOOKUP); fail(); }
        catch (IllegalAccessException e) { /* expected */ }
        assertEquals(0, (int) superIMethodRef.resolve(TestClass.LOOKUP).invokeExact(instance, 5));
        
        try { privateMethodRef.resolve(LOOKUP); fail(); }
        catch (IllegalAccessException e) { /* expected */ }
        assertEquals(5, (int) privateMethodRef.resolve(TestClass.LOOKUP).invokeExact(instance, 5));
        try { privateIMethodRef.resolve(LOOKUP); fail(); }
        catch (IllegalAccessException e) { /* expected */ }
        try { privateIMethodRef.resolve(TestClass.LOOKUP); fail(); }
        catch (IllegalAccessException e) { /* expected */ }
        assertEquals(0, (int) privateIMethodRef.resolve(TestInterface.LOOKUP).invokeExact(instanceI, 5));
        
        try { privateStaticMethodRef.resolve(LOOKUP); fail(); }
        catch (IllegalAccessException e) { /* expected */ }
        assertEquals(5, (int) privateStaticMethodRef.resolve(TestClass.LOOKUP).invokeExact(5));
        try { privateStaticIMethodRef.resolve(LOOKUP); fail(); }
        catch (IllegalAccessException e) { /* expected */ }
        try { privateStaticIMethodRef.resolve(TestClass.LOOKUP); fail(); }
        catch (IllegalAccessException e) { /* expected */ }
        assertEquals(0, (int) privateStaticIMethodRef.resolve(TestInterface.LOOKUP).invokeExact(5));

        MethodHandleRef staticSetterRef = MethodHandleRef.ofField(STATIC_SETTER, testClass, "sf", ClassRef.CR_int);
        MethodHandleRef staticGetterRef = MethodHandleRef.ofField(STATIC_GETTER, testClass, "sf", ClassRef.CR_int);
        MethodHandleRef staticGetterIRef = MethodHandleRef.ofField(STATIC_GETTER, testInterface, "sf", ClassRef.CR_int);
        MethodHandleRef setterRef = MethodHandleRef.ofField(SETTER, testClass, "f", ClassRef.CR_int);
        MethodHandleRef getterRef = MethodHandleRef.ofField(GETTER, testClass, "f", ClassRef.CR_int);

        staticSetterRef.resolve(LOOKUP).invokeExact(6); assertEquals(TestClass.sf, 6);
        assertEquals(6, (int) staticGetterRef.resolve(LOOKUP).invokeExact());
        assertEquals(6, (int) staticGetterRef.resolve(TestClass.LOOKUP).invokeExact());
        staticSetterRef.resolve(TestClass.LOOKUP).invokeExact(7); assertEquals(TestClass.sf, 7);
        assertEquals(7, (int) staticGetterRef.resolve(LOOKUP).invokeExact());
        assertEquals(7, (int) staticGetterRef.resolve(TestClass.LOOKUP).invokeExact());

        assertEquals(3, (int) staticGetterIRef.resolve(LOOKUP).invokeExact());
        assertEquals(3, (int) staticGetterIRef.resolve(TestClass.LOOKUP).invokeExact());

        setterRef.resolve(LOOKUP).invokeExact(instance, 6); assertEquals(instance.f, 6);
        assertEquals(6, (int) getterRef.resolve(LOOKUP).invokeExact(instance));
        assertEquals(6, (int) getterRef.resolve(TestClass.LOOKUP).invokeExact(instance));
        setterRef.resolve(TestClass.LOOKUP).invokeExact(instance, 7); assertEquals(instance.f, 7);
        assertEquals(7, (int) getterRef.resolve(LOOKUP).invokeExact(instance));
        assertEquals(7, (int) getterRef.resolve(TestClass.LOOKUP).invokeExact(instance));
    }

    public void testVarHandles() throws ReflectiveOperationException {
        ClassRef testClass = ClassRef.of("ConstablesTest").inner("TestClass");
        TestClass instance = new TestClass();
        int[] ints = new int[3];

        // static varHandle
        DynamicConstantRef<VarHandle> vhc = (DynamicConstantRef<VarHandle>) ConstantRef.staticFieldVarHandle(testClass, "sf", ClassRef.CR_int);
        MethodTypeRef methodTypeRef = vhc.bootstrapMethod().type();
        System.out.println(vhc.name() + " " + methodTypeRef.returnType() + " " + methodTypeRef.parameterList());
        VarHandle varHandle = vhc.resolve(LOOKUP);
        assertEquals(varHandle.varType(), int.class);
        varHandle.set(8);
        assertEquals(8, (int) varHandle.get());
        assertEquals(TestClass.sf, 8);

        // static varHandle
        vhc = (DynamicConstantRef<VarHandle>) ConstantRef.fieldVarHandle(testClass, "f", ClassRef.CR_int);
        varHandle = vhc.resolve(LOOKUP);
        assertEquals(varHandle.varType(), int.class);
        varHandle.set(instance, 9);
        assertEquals(9, (int) varHandle.get(instance));
        assertEquals(instance.f, 9);

        vhc = (DynamicConstantRef<VarHandle>) ConstantRef.arrayVarHandle(ClassRef.CR_int.array());
        varHandle = vhc.resolve(LOOKUP);
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

    public void testMiscConstablesFactories() throws ReflectiveOperationException {
        DynamicConstantRef<?> ofNull = (DynamicConstantRef<?>) ConstantRef.ofNull();

        assertNull(ofNull.resolve(LOOKUP));
    }

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
        assertEquals(int.class, Intrinsics.ldc(ClassRef.CR_int));
//        assertEquals(int.class, Intrinsics.ldc(ClassRef.of(int.class)));

        assertEquals(int[].class, Intrinsics.ldc(ClassRef.ofDescriptor("[I")));
        assertEquals(int[].class, Intrinsics.ldc(ClassRef.CR_int.array()));
//        assertEquals(int[].class, Intrinsics.ldc(ClassRef.of(int.class).arrayOf()));
    }

    public void testLdcMethodType() {
        assertEquals(MethodType.methodType(void.class), Intrinsics.ldc(MethodTypeRef.ofDescriptor("()V")));
        assertEquals(MethodType.methodType(void.class), Intrinsics.ldc(MethodTypeRef.of(ClassRef.CR_void)));

        assertEquals(MethodType.methodType(int.class, int.class),
                     Intrinsics.ldc(MethodTypeRef.ofDescriptor("(I)I")));
        assertEquals(MethodType.methodType(int.class, int.class),
                     Intrinsics.ldc(MethodTypeRef.of(ClassRef.CR_int, ClassRef.CR_int)));

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

        MethodHandleRef staticSetterRef = MethodHandleRef.ofField(STATIC_SETTER, testClass, "sf", ClassRef.CR_int);
        MethodHandleRef staticGetterRef = MethodHandleRef.ofField(STATIC_GETTER, testClass, "sf", ClassRef.CR_int);
        MethodHandleRef staticGetterIRef = MethodHandleRef.ofField(STATIC_GETTER, testInterface, "sf", ClassRef.CR_int);
        MethodHandleRef setterRef = MethodHandleRef.ofField(SETTER, testClass, "f", ClassRef.CR_int);
        MethodHandleRef getterRef = MethodHandleRef.ofField(GETTER, testClass, "f", ClassRef.CR_int);

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

    private static interface TestInterface {
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
        
        private static void ldcMethodHandleTestsFromClass() throws Throwable {
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

            assertEquals(-1, (int) Intrinsics.ldc(superMethodRef).invokeExact(instance, 5));
            assertEquals(0, (int) Intrinsics.ldc(superIMethodRef).invokeExact(instance, 5));
        
            assertEquals(5, (int) Intrinsics.ldc(privateMethodRef).invokeExact(instance, 5));
            try { Intrinsics.ldc(privateIMethodRef); fail(); }
            catch (IllegalAccessError e) { /* expected */ }
        
            assertEquals(5, (int) Intrinsics.ldc(privateStaticMethodRef).invokeExact(5));
            try { Intrinsics.ldc(privateStaticIMethodRef); fail(); }
            catch (IllegalAccessError e) { /* expected */ }

            MethodHandleRef staticSetterRef = MethodHandleRef.ofField(STATIC_SETTER, testClass, "sf", ClassRef.CR_int);
            MethodHandleRef staticGetterRef = MethodHandleRef.ofField(STATIC_GETTER, testClass, "sf", ClassRef.CR_int);
            MethodHandleRef staticGetterIRef = MethodHandleRef.ofField(STATIC_GETTER, testInterface, "sf", ClassRef.CR_int);
            MethodHandleRef setterRef = MethodHandleRef.ofField(SETTER, testClass, "f", ClassRef.CR_int);
            MethodHandleRef getterRef = MethodHandleRef.ofField(GETTER, testClass, "f", ClassRef.CR_int);

            Intrinsics.ldc(staticSetterRef).invokeExact(10); assertEquals(TestClass.sf, 10);
            assertEquals(10, (int) Intrinsics.ldc(staticGetterRef).invokeExact());

            //assertEquals(3, (int) Intrinsics.ldc(staticGetterIRef).invokeExact());

            Intrinsics.ldc(setterRef).invokeExact(instance, 11); assertEquals(instance.f, 11);
            assertEquals(11, (int) Intrinsics.ldc(getterRef).invokeExact(instance));
        }
        
    }
}
