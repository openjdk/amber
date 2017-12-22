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
 * @compile -XDdoConstantFold CondyCodeGenerationTest.java
 * @run main CondyCodeGenerationTest
 */

import java.lang.sym.BootstrapSpecifier;
import java.lang.sym.ClassRef;
import java.lang.sym.DynamicConstantRef;
import java.lang.invoke.MethodHandle;
import java.lang.sym.MethodHandleRef;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.sym.MethodTypeRef;
import java.lang.sym.SymbolicRefs;
import java.util.StringJoiner;

import static java.lang.invoke.Intrinsics.ldc;
import static java.lang.sym.MethodHandleRef.Kind.STATIC;

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
        MethodTypeRef methodTypeForMethodHandle = MethodTypeRef.of(
                SymbolicRefs.CR_String, SymbolicRefs.CR_Lookup, SymbolicRefs.CR_String, SymbolicRefs.CR_Class);
        MethodHandleRef mh = MethodHandleRef.of(STATIC, ClassRef.ofDescriptor("LCondyCodeGenerationTest;"),
                                                "testNoStaticArgsBSM", methodTypeForMethodHandle);

        DynamicConstantRef<String> condyDescr = DynamicConstantRef.<String>of(mh, "constant-name");
        return (String)ldc(condyDescr);
    }

    public static String testNoStaticArgsBSM(MethodHandles.Lookup l, String name, Class<?> type) {
        return name + "-" + type.getSimpleName();
    }


    String testWithStaticArgs() throws Throwable {
        MethodTypeRef methodTypeForMethodHandle = MethodTypeRef.of(
                SymbolicRefs.CR_String,
                SymbolicRefs.CR_Lookup,
                SymbolicRefs.CR_String,
                SymbolicRefs.CR_Class,
                SymbolicRefs.CR_int,
                SymbolicRefs.CR_long,
                SymbolicRefs.CR_float,
                SymbolicRefs.CR_double,
                SymbolicRefs.CR_Class,
                SymbolicRefs.CR_String,
                SymbolicRefs.CR_MethodType,
                SymbolicRefs.CR_MethodHandle);
        MethodHandleRef mh = MethodHandleRef.of(STATIC, ClassRef.ofDescriptor("LCondyCodeGenerationTest;"),
                                                "testWithStaticArgsBSM", methodTypeForMethodHandle);
        DynamicConstantRef<String> condyDescr = DynamicConstantRef.<String>of(mh, "constant-name").withArgs(1, 2L, 3.0f, 4.0d,
                                                                                                            SymbolicRefs.CR_Number,
                                                                                                            "something",
                                                                                                            MethodTypeRef.ofDescriptor("(IJFD)V"),
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
        MethodTypeRef c_primitiveClassBSM_MT = MethodTypeRef.of(
                SymbolicRefs.CR_Class,
                SymbolicRefs.CR_Lookup,
                SymbolicRefs.CR_String,
                SymbolicRefs.CR_Class
        );
        MethodHandleRef c_primitiveClassBSM_MH =
                MethodHandleRef.of(STATIC, ClassRef.ofDescriptor("LCondyCodeGenerationTest;"),
                                   "primitiveClassBSM", c_primitiveClassBSM_MT);
        DynamicConstantRef<Class> c_primitiveClassBSM_CD = DynamicConstantRef.of(c_primitiveClassBSM_MH, "I");

        MethodTypeRef methodTypeForMethodHandle = MethodTypeRef.of(
                SymbolicRefs.CR_String,
                SymbolicRefs.CR_Lookup,
                SymbolicRefs.CR_String,
                SymbolicRefs.CR_Class,
                SymbolicRefs.CR_Class
        );
        MethodHandleRef mh = MethodHandleRef.of(STATIC, ClassRef.ofDescriptor("LCondyCodeGenerationTest;"),
                                                "testWithNestedArgBSM", methodTypeForMethodHandle);

        DynamicConstantRef<String> condyDescr = DynamicConstantRef.<String>of(mh, "constant-name").withArgs(c_primitiveClassBSM_CD);
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
