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

/*
 * @test
 * @modules jdk.compiler/com.sun.tools.javac.util
 * @compile CondyCodeGenerationTest.java
 * @run main CondyCodeGenerationTest
 */

import java.lang.invoke.constant.*;
import java.lang.invoke.constant.ClassDesc;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.StringJoiner;

import static java.lang.invoke.Intrinsics.ldc;
import static java.lang.invoke.constant.MethodHandleDesc.Kind.STATIC;

public class CondyCodeGenerationTest {
    public static void main(String[] args) throws Throwable {
        String v;
        if (!(v = new CondyCodeGenerationTest().testNoStaticArgs()).equals("constant-name-String")) {
            throw new AssertionError("unexpected value: " + v);
        }
        if (!(v = new CondyCodeGenerationTest().testWithStaticArgs()).equals("constant-name-String-1-2-3.0-4.0-Number-something-(int,long,float,double)void-11")) {
            throw new AssertionError("unexpected value: " + v);
        }
        if (!(v = new CondyCodeGenerationTest().testWithNestedArg()).equals("constant-name-String-int")) {
            throw new AssertionError("unexpected value: " + v);
        }
    }

    String testNoStaticArgs() throws Throwable {
        MethodTypeDesc methodTypeForMethodHandle = MethodTypeDesc.of(
                ConstantDescs.CR_String, ConstantDescs.CR_MethodHandles_Lookup, ConstantDescs.CR_String, ConstantDescs.CR_Class);
        ConstantMethodHandleDesc mh = MethodHandleDesc.of(STATIC, ClassDesc.ofDescriptor("LCondyCodeGenerationTest;"),
                                                          "testNoStaticArgsBSM", methodTypeForMethodHandle);

        DynamicConstantDesc<String> condyDescr = DynamicConstantDesc.<String>of(mh, "constant-name");
        return (String)ldc(condyDescr);
    }

    public static String testNoStaticArgsBSM(MethodHandles.Lookup l, String name, Class<?> type) {
        return name + "-" + type.getSimpleName();
    }


    String testWithStaticArgs() throws Throwable {
        MethodTypeDesc methodTypeForMethodHandle = MethodTypeDesc.of(
                ConstantDescs.CR_String,
                ConstantDescs.CR_MethodHandles_Lookup,
                ConstantDescs.CR_String,
                ConstantDescs.CR_Class,
                ConstantDescs.CR_int,
                ConstantDescs.CR_long,
                ConstantDescs.CR_float,
                ConstantDescs.CR_double,
                ConstantDescs.CR_Class,
                ConstantDescs.CR_String,
                ConstantDescs.CR_MethodType,
                ConstantDescs.CR_MethodHandle);
        ConstantMethodHandleDesc mh = MethodHandleDesc.of(STATIC, ClassDesc.ofDescriptor("LCondyCodeGenerationTest;"),
                                                          "testWithStaticArgsBSM", methodTypeForMethodHandle);
        DynamicConstantDesc<String> condyDescr = DynamicConstantDesc.<String>of(mh, "constant-name").withArgs(1, 2L, 3.0f, 4.0d,
                                                                                                              ConstantDescs.CR_Number,
                                                                                                              "something",
                                                                                                              MethodTypeDesc.ofDescriptor("(IJFD)V"),
                                                                                                              mh);
        return (String)ldc(condyDescr);
    }

    public static String testWithStaticArgsBSM(MethodHandles.Lookup l, String name, Class<?> type,
                                               int i, long j, float f, double d,
                                               Class<?> c, String s,
                                               MethodType mt, MethodHandle mh) {
        return new StringJoiner("-")
                .add(name)
                .add(type.getSimpleName())
                .add(Integer.toString(i))
                .add(Long.toString(j))
                .add(Float.toString(f))
                .add(Double.toString(d))
                .add(c.getSimpleName())
                .add(s)
                .add(mt.toString())
                .add(Integer.toString(mh.type().parameterCount()))
                .toString();
    }

    String testWithNestedArg() throws Throwable {
        MethodTypeDesc c_primitiveClassBSM_MT = MethodTypeDesc.of(
                ConstantDescs.CR_Class,
                ConstantDescs.CR_MethodHandles_Lookup,
                ConstantDescs.CR_String,
                ConstantDescs.CR_Class
        );
        ConstantMethodHandleDesc c_primitiveClassBSM_MH =
                MethodHandleDesc.of(STATIC, ClassDesc.ofDescriptor("LCondyCodeGenerationTest;"),
                                    "primitiveClassBSM", c_primitiveClassBSM_MT);
        DynamicConstantDesc<Class> c_primitiveClassBSM_CD = DynamicConstantDesc.of(c_primitiveClassBSM_MH, "I");

        MethodTypeDesc methodTypeForMethodHandle = MethodTypeDesc.of(
                ConstantDescs.CR_String,
                ConstantDescs.CR_MethodHandles_Lookup,
                ConstantDescs.CR_String,
                ConstantDescs.CR_Class,
                ConstantDescs.CR_Class
        );
        ConstantMethodHandleDesc mh = MethodHandleDesc.of(STATIC, ClassDesc.ofDescriptor("LCondyCodeGenerationTest;"),
                                                          "testWithNestedArgBSM", methodTypeForMethodHandle);

        DynamicConstantDesc<String> condyDescr = DynamicConstantDesc.<String>of(mh, "constant-name").withArgs(c_primitiveClassBSM_CD);
        return (String)ldc(condyDescr);
    }

    public static Class<?> primitiveClassBSM(MethodHandles.Lookup l, String name, Class<?> type) {
        return Integer.TYPE;
    }

    public static String testWithNestedArgBSM(MethodHandles.Lookup l, String name, Class<?> type,
                                              Class<?> c) {
        return new StringJoiner("-")
                .add(name)
                .add(type.getSimpleName())
                .add(c.getSimpleName())
                .toString();
    }
}
