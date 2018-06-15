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

import java.lang.constant.*;
import java.lang.invoke.*;
import java.util.List;
import java.util.stream.Stream;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

/**
 * @test
 * @compile IntrinsicsTest.java
 * @run testng IntrinsicsTest
 * @compile -g IntrinsicsTest.java
 * @run testng IntrinsicsTest
 * @summary integration test for compiler support of java.lang.invoke.Intrinsics
 * @author Brian Goetz
 */
@Test
public class IntrinsicsTest {
    static final ClassDesc HELPER_CLASS = ClassDesc.of("IntrinsicsTest").inner("IntrinsicTestHelper");

    static final ClassDesc staticField = ClassDesc.of("java.lang.String");
    final ClassDesc instanceField = ClassDesc.of("java.lang.String");

    public void testPropagateThroughLocals() {
        boolean condition = true;
        String descriptor = "Ljava/lang/String;";
        String descriptorI = "Ljava/lang/Integer;";
        ClassDesc cc1 = ClassDesc.ofDescriptor(descriptor);
        ClassDesc ccI = ClassDesc.ofDescriptor(descriptorI);
        assertEquals(String.class, Intrinsics.ldc(cc1));
        assertEquals(String.class, condition ? Intrinsics.ldc(cc1) : Intrinsics.ldc(ccI));

        ClassDesc cc2 = ClassDesc.ofDescriptor("Ljava/lang/String;");
        assertEquals(String.class, Intrinsics.ldc(cc2));

        ConstantDesc<String> s = "foo";
        assertEquals("foo", Intrinsics.ldc(s));

        // Boxing should preserve IC-ness
        ConstantDesc<Integer> i = (Integer) 3;
        assertEquals(3, (int) Intrinsics.ldc(i));
    }

    public void testPropagateThroughField() {
        // Instance and static fields both

        assertEquals(String.class, Intrinsics.ldc(staticField));
        assertEquals(String.class, Intrinsics.ldc(instanceField));
    }

    public void testPropagateThroughLambda() {
        String name = "java.lang.String";
        ClassDesc cc = ClassDesc.of("java.lang.String");
        Runnable[] rs = new Runnable[] {
                () -> assertEquals(String.class, Intrinsics.ldc(cc)),
                () -> assertEquals(String.class, Intrinsics.ldc(ClassDesc.of(name))),
                new Runnable() {
                    @Override
                    public void run() {
                        assertEquals(String.class, Intrinsics.ldc(cc));
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        assertEquals(String.class, Intrinsics.ldc(ClassDesc.of(name)));
                    }
                }
        };
        Stream.of(rs).forEach(Runnable::run);
    }

    public void testPrimitiveClassRef() {
        assertEquals(int.class, Intrinsics.ldc(ClassDesc.ofDescriptor("I")));
    }

    public void testFoldedConstants() {
        assertEquals(String.class, Intrinsics.ldc(ClassDesc.ofDescriptor("Ljava/lang/String;" + "")));
        ClassDesc cc = ClassDesc.ofDescriptor("Ljava/lang/String;");
        assertEquals(String.class, Intrinsics.ldc(ClassDesc.ofDescriptor("" + cc.descriptorString())));

        assertEquals(2, (int) Intrinsics.ldc(1 + 1));
    }

    public void testPropagateThroughCombinator() {
        ClassDesc cc = ClassDesc.ofDescriptor("Ljava/lang/String;");
        assertEquals(String.class, Intrinsics.ldc(ClassDesc.ofDescriptor(cc.descriptorString())));
    }

    public void testClassCombinators() {
        ClassDesc cs = ClassDesc.ofDescriptor("Ljava/lang/String;");
        assertEquals(String.class, Intrinsics.ldc(cs));
        assertEquals(String[].class, Intrinsics.ldc(cs.arrayType()));

        assertEquals(String[][].class, Intrinsics.ldc(cs.arrayType().arrayType()));
        assertEquals(String.class, Intrinsics.ldc(cs.arrayType().componentType()));
        assertEquals(String[].class, Intrinsics.ldc(cs.arrayType().arrayType().componentType()));
    }

    public void testMethodTypeCombinators() {
        MethodTypeDesc mtc = MethodTypeDesc.of(ConstantDescs.CR_String, ConstantDescs.CR_int);
        assertEquals(String.class, Intrinsics.ldc(mtc.returnType()));
        assertEquals(int.class, Intrinsics.ldc(mtc.parameterType(0)));
        assertEquals(String.class, Intrinsics.ldc(mtc.returnType()));
        assertEquals(MethodType.methodType(long.class, int.class),
                     Intrinsics.ldc(mtc.changeReturnType(ConstantDescs.CR_long)));
    }

    public void testMethodHandleCombinators() {
        MethodHandleDesc mhc = MethodHandleDesc.of(DirectMethodHandleDesc.Kind.STATIC, ConstantDescs.CR_String, "valueOf",
                                                   MethodTypeDesc.of(ConstantDescs.CR_String, ConstantDescs.CR_Object));
        assertEquals(MethodType.methodType(String.class, Object.class),
                     Intrinsics.ldc(mhc.methodType()));
        assertEquals(String.class, Intrinsics.ldc(mhc.methodType().returnType()));
        assertEquals(String.class, Intrinsics.ldc(ClassDesc.ofDescriptor(mhc.methodType().returnType().descriptorString())));
    }

    public void testInterfaceSpecial() throws Throwable {
        MethodHandleDesc mhr = MethodHandleDesc.of(DirectMethodHandleDesc.Kind.STATIC, ConstantDescs.CR_List, "of",
                                                   MethodTypeDesc.of(ConstantDescs.CR_List, ConstantDescs.CR_Object.arrayType()));
        MethodHandle mh = Intrinsics.ldc(mhr);
        assertEquals(List.of("a", "b"), (List<String>) mh.invoke("a", "b"));
    }

    public void testSimpleIndy() throws Throwable {
        final MethodTypeDesc Str_MT = MethodTypeDesc.of(ConstantDescs.CR_String);
        DirectMethodHandleDesc simpleBSM = ConstantDescs.ofCallsiteBootstrap(HELPER_CLASS, "simpleBSM", ConstantDescs.CR_CallSite);
        DynamicCallSiteDesc bsm = DynamicCallSiteDesc.of(simpleBSM, "foo", Str_MT);
        String result = (String) Intrinsics.invokedynamic(bsm);
        assertEquals("foo", result);

        DynamicCallSiteDesc bsm2 = DynamicCallSiteDesc.of(simpleBSM, "bar", Str_MT);
        assertEquals("bar", (String) Intrinsics.invokedynamic(bsm2));

        DirectMethodHandleDesc staticArgBSM = ConstantDescs.ofCallsiteBootstrap(HELPER_CLASS, "staticArgBSM", ConstantDescs.CR_CallSite, ConstantDescs.CR_String);
        DynamicCallSiteDesc bsm3 = DynamicCallSiteDesc.of(staticArgBSM, "ignored", Str_MT, "bark");
        assertEquals("bark", (String) Intrinsics.invokedynamic(bsm3));

        final MethodTypeDesc Str_Str_MT = MethodTypeDesc.of(ConstantDescs.CR_String, ConstantDescs.CR_String);
        DirectMethodHandleDesc dynArgBSM = ConstantDescs.ofCallsiteBootstrap(HELPER_CLASS, "dynArgBSM", ConstantDescs.CR_CallSite, ConstantDescs.CR_String);
        DynamicCallSiteDesc bsm4 = DynamicCallSiteDesc.of(dynArgBSM, "ignored", Str_Str_MT, "bargle");
        assertEquals("barglefoo", (String) Intrinsics.invokedynamic(bsm4, "foo"));
        assertEquals("barglebar", (String) Intrinsics.invokedynamic(bsm4, "bar"));
    }

    public void testStatefulBSM() throws Throwable {
        final MethodTypeDesc Int_MT = MethodTypeDesc.of(ConstantDescs.CR_int);
        DirectMethodHandleDesc statefulBSM = ConstantDescs.ofCallsiteBootstrap(HELPER_CLASS, "statefulBSM", ConstantDescs.CR_CallSite);
        DynamicCallSiteDesc bsm = DynamicCallSiteDesc.of(statefulBSM, "ignored", Int_MT);
        for (int i=0; i<10; i++) {
            assertEquals(i, (int) Intrinsics.invokedynamic(bsm));
        }
    }

    public void testCondyInIndy() throws Throwable {
        final MethodTypeDesc Class_MT = MethodTypeDesc.of(ConstantDescs.CR_Class);
        DirectMethodHandleDesc bsm = ConstantDescs.ofCallsiteBootstrap(HELPER_CLASS, "staticArgBSM",
                                                                         ConstantDescs.CR_CallSite, ConstantDescs.CR_Class);
        assertEquals(String.class, (Class) Intrinsics.invokedynamic(DynamicCallSiteDesc.of(bsm, "ignored", Class_MT, ConstantDescs.CR_String)));
        assertEquals(int.class, (Class) Intrinsics.invokedynamic(DynamicCallSiteDesc.of(bsm, "ignored", Class_MT, ConstantDescs.CR_int)));
    }

    public void testCondyInCondy() throws Throwable {
        DirectMethodHandleDesc bsm = ConstantDescs.ofConstantBootstrap(HELPER_CLASS, "identityCondy", ConstantDescs.CR_Object, ConstantDescs.CR_Object);
        assertEquals(String.class, Intrinsics.ldc(DynamicConstantDesc.of(bsm, ConstantDescs.CR_Class).withArgs(ConstantDescs.CR_String)));
        assertEquals(String.class, Intrinsics.ldc(DynamicConstantDesc.of(bsm).withArgs(ConstantDescs.CR_String)));
        assertEquals(int.class, Intrinsics.ldc(DynamicConstantDesc.of(bsm).withArgs(ConstantDescs.CR_int)));
        assertEquals("foo", Intrinsics.ldc(DynamicConstantDesc.of(bsm).withArgs("foo")));
    }

    static class IntrinsicTestHelper {
        public static int sf;
        public int f;

        public static CallSite simpleBSM(MethodHandles.Lookup lookup,
                                         String invocationName,
                                         MethodType invocationType) {
            return new ConstantCallSite(MethodHandles.constant(String.class, invocationName));
        }

        public static CallSite staticArgBSM(MethodHandles.Lookup lookup,
                                            String invocationName,
                                            MethodType invocationType,
                                            String arg) {
            return new ConstantCallSite(MethodHandles.constant(String.class, arg));
        }

        public static CallSite staticArgBSM(MethodHandles.Lookup lookup,
                                            String invocationName,
                                            MethodType invocationType,
                                            Class arg) {
            return new ConstantCallSite(MethodHandles.constant(Class.class, arg));
        }

        public static Object identityCondy(MethodHandles.Lookup lookup,
                                           String invocationName,
                                           Class invocationType,
                                           Object arg) {
            return arg;
        }

        public static CallSite dynArgBSM(MethodHandles.Lookup lookup,
                                         String invocationName,
                                         MethodType invocationType,
                                         String arg) throws NoSuchMethodException, IllegalAccessException {
            MethodHandle mh = lookup.findVirtual(String.class, "concat", MethodType.methodType(String.class, String.class))
                                    .bindTo(arg);
            return new ConstantCallSite(mh);
        }

        public static CallSite statefulBSM(MethodHandles.Lookup lookup,
                                           String invocationName,
                                           MethodType invocationType) throws NoSuchMethodException, IllegalAccessException {
            MethodHandle target = lookup.findVirtual(MCS.class, "target", MethodType.methodType(int.class));
            MCS mcs = new MCS();
            mcs.setTarget(target.bindTo(mcs));
            return mcs;
        }

        static class MCS extends MutableCallSite {
            public int counter = 0;

            public MCS() {
                super(MethodType.methodType(int.class));
            }

            public int target() {
                return counter++;
            }
        }

    }
}
