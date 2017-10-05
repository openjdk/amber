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

import java.lang.invoke.BootstrapSpecifier;
import java.lang.invoke.CallSite;
import java.lang.invoke.ClassRef;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.ConstantRef;
import java.lang.invoke.DynamicConstantRef;
import java.lang.invoke.Intrinsics;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleRef;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodTypeRef;
import java.lang.invoke.MutableCallSite;
import java.util.List;
import java.util.stream.Stream;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @test
 * @compile -XDdoConstantFold IntrinsicsTest.java
 * @run testng IntrinsicsTest
 * @summary integration test for compiler support of java.lang.invoke.Intrinsics
 * @author Brian Goetz
 */
@Test
public class IntrinsicsTest {
    static final ClassRef HELPER_CLASS = ClassRef.of("IntrinsicsTest$IntrinsicTestHelper");

    static final ClassRef staticField = ClassRef.of("java.lang.String");
    final ClassRef instanceField = ClassRef.of("java.lang.String");


    public void testPropagateThroughLocals() {
        boolean condition = true;
        String descriptor = "Ljava/lang/String;";
        String descriptorI = "Ljava/lang/Integer;";
        ClassRef cc1 = ClassRef.ofDescriptor(descriptor);
        ClassRef ccI = ClassRef.ofDescriptor(descriptorI);
        assertEquals(String.class, Intrinsics.ldc(cc1));
        assertEquals(String.class, condition ? Intrinsics.ldc(cc1) : Intrinsics.ldc(ccI));

        ClassRef cc2 = ClassRef.ofDescriptor("Ljava/lang/String;");
        assertEquals(String.class, Intrinsics.ldc(cc2));

        ConstantRef<String> s = "foo";
        assertEquals("foo", Intrinsics.ldc(s));

        // @@@ Boxing should preserve IC-ness
        ConstantRef<Integer> i = (Integer) 3;
        assertEquals(3, (int) Intrinsics.ldc(i));
    }

    public void testPropagateThroughField() {
        // @@@ Do we want instance fields treated as constants in this story?

        assertEquals(String.class, Intrinsics.ldc(staticField));
        assertEquals(String.class, Intrinsics.ldc(instanceField));
    }

    public void testPropagateThroughLambda() {
        String name = "java.lang.String";
        ClassRef cc = ClassRef.of("java.lang.String");
        Runnable[] rs = new Runnable[] {
                () -> assertEquals(String.class, Intrinsics.ldc(cc)),
                () -> assertEquals(String.class, Intrinsics.ldc(ClassRef.of(name))),
                new Runnable() {
                    @Override
                    public void run() {
                        assertEquals(String.class, Intrinsics.ldc(cc));
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        assertEquals(String.class, Intrinsics.ldc(ClassRef.of(name)));
                    }
                }
        };
        Stream.of(rs).forEach(Runnable::run);
    }

    public void testPrimitiveClassRef() {
        assertEquals(int.class, Intrinsics.ldc(ClassRef.ofDescriptor("I")));
    }

    public void testFoldedConstants() {
        assertEquals(String.class, Intrinsics.ldc(ClassRef.ofDescriptor("Ljava/lang/String;" + "")));
        ClassRef cc = ClassRef.ofDescriptor("Ljava/lang/String;");
        assertEquals(String.class, Intrinsics.ldc(ClassRef.ofDescriptor("" + cc.descriptorString())));

        assertEquals(2, (int) Intrinsics.ldc(1 + 1));
    }

    public void testPropagateThroughCombinator() {
        ClassRef cc = ClassRef.ofDescriptor("Ljava/lang/String;");
        assertEquals(String.class, Intrinsics.ldc(ClassRef.ofDescriptor(cc.descriptorString())));
    }

    public void testClassCombinators() {
        ClassRef cs = ClassRef.ofDescriptor("Ljava/lang/String;");
        assertEquals(String.class, Intrinsics.ldc(cs));
        assertEquals(String[].class, Intrinsics.ldc(cs.array()));

        assertEquals(String[][].class, Intrinsics.ldc(cs.array().array()));
        assertEquals(String.class, Intrinsics.ldc(cs.array().componentType()));
        assertEquals(String[].class, Intrinsics.ldc(cs.array().array().componentType()));
    }

    public void testMethodTypeCombinators() {
        MethodTypeRef mtc = MethodTypeRef.of(ClassRef.CR_String, ClassRef.CR_int);
        assertEquals(String.class, Intrinsics.ldc(mtc.returnType()));
        assertEquals(int.class, Intrinsics.ldc(mtc.parameterType(0)));
        assertEquals(String.class, Intrinsics.ldc(mtc.returnType()));
        assertEquals(MethodType.methodType(long.class, int.class),
                     Intrinsics.ldc(mtc.changeReturnType(ClassRef.CR_long)));
    }

    public void testMethodHandleCombinators() {
        MethodHandleRef mhc = MethodHandleRef.ofStatic(ClassRef.CR_String, "valueOf",
                                                       MethodTypeRef.of(ClassRef.CR_String, ClassRef.CR_Object));
        assertEquals(MethodType.methodType(String.class, Object.class),
                     Intrinsics.ldc(mhc.type()));
        assertEquals(String.class, Intrinsics.ldc(mhc.type().returnType()));
        assertEquals(String.class, Intrinsics.ldc(ClassRef.ofDescriptor(mhc.type().returnType().descriptorString())));
    }

    public void testInterfaceSpecial() throws Throwable {
        MethodHandleRef mhr = MethodHandleRef.ofStatic(ClassRef.CR_List,
                                                       "of", MethodTypeRef.of(ClassRef.CR_List, ClassRef.CR_Object.array()));
        MethodHandle mh = Intrinsics.ldc(mhr);
        assertEquals(List.of("a", "b"), (List<String>) mh.invoke("a", "b"));
    }

    public void testSimpleIndy() throws Throwable {
        MethodHandleRef simpleBSM = MethodHandleRef.ofIndyBootstrap(HELPER_CLASS, "simpleBSM", ClassRef.CR_CallSite);
        BootstrapSpecifier bsm = BootstrapSpecifier.of(simpleBSM);
        String result = (String) Intrinsics.invokedynamic(bsm, "foo");
        assertEquals("foo", result);

        BootstrapSpecifier bsm2 = BootstrapSpecifier.of(simpleBSM);
        assertEquals("bar", (String) Intrinsics.invokedynamic(bsm2, "bar"));

        MethodHandleRef staticArgBSM = MethodHandleRef.ofIndyBootstrap(HELPER_CLASS, "staticArgBSM", ClassRef.CR_CallSite, ClassRef.CR_String);
        BootstrapSpecifier bsm3 = BootstrapSpecifier.of(staticArgBSM, "bark");
        assertEquals("bark", (String) Intrinsics.invokedynamic(bsm3, "ignored"));

        MethodHandleRef dynArgBSM = MethodHandleRef.ofIndyBootstrap(HELPER_CLASS, "dynArgBSM", ClassRef.CR_CallSite, ClassRef.CR_String);
        BootstrapSpecifier bsm4 = BootstrapSpecifier.of(dynArgBSM, "bargle");
        assertEquals("barglefoo", (String) Intrinsics.invokedynamic(bsm4, "ignored", "foo"));
        assertEquals("barglebar", (String) Intrinsics.invokedynamic(bsm4, "ignored", "bar"));
    }

    public void testStatefulBSM() throws Throwable {
        MethodHandleRef statefulBSM = MethodHandleRef.ofIndyBootstrap(HELPER_CLASS, "statefulBSM", ClassRef.CR_CallSite);
        BootstrapSpecifier bsm = BootstrapSpecifier.of(statefulBSM);
        for (int i=0; i<10; i++) {
            assertEquals(i, (int) Intrinsics.invokedynamic(bsm, "ignored"));
        }
    }

    public void testCondyInIndy() throws Throwable {
        MethodHandleRef bsm = MethodHandleRef.ofIndyBootstrap(HELPER_CLASS, "staticArgBSM",
                                                              ClassRef.CR_CallSite, ClassRef.CR_Class);
        assertEquals(String.class, (Class) Intrinsics.invokedynamic(BootstrapSpecifier.of(bsm, ClassRef.CR_String), "ignored"));
        assertEquals(int.class, (Class) Intrinsics.invokedynamic(BootstrapSpecifier.of(bsm, ClassRef.CR_int), "ignored"));
    }

    public void testCondyInCondy() throws Throwable {
        MethodHandleRef bsm = MethodHandleRef.ofCondyBootstrap(HELPER_CLASS, "identityCondy", ClassRef.CR_Object, ClassRef.CR_Object);
        assertEquals(String.class, Intrinsics.ldc(DynamicConstantRef.of(BootstrapSpecifier.of(bsm, ClassRef.CR_String), ClassRef.CR_Class)));
        assertEquals(String.class, Intrinsics.ldc(DynamicConstantRef.of(BootstrapSpecifier.of(bsm, ClassRef.CR_String))));
        assertEquals(int.class, Intrinsics.ldc(DynamicConstantRef.of(BootstrapSpecifier.of(bsm, ClassRef.CR_int))));
        assertEquals("foo", Intrinsics.ldc(DynamicConstantRef.of(BootstrapSpecifier.of(bsm, "foo"))));
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
