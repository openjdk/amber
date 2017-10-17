/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 * @test 8168964
 * @summary introducing indy
 * @modules jdk.compiler/com.sun.tools.javac.util
 * @compile -XDdoConstantFold IndyCodeGenerationTest.java
 * @run main IndyCodeGenerationTest
 */

import java.lang.invoke.BootstrapSpecifier;
import java.lang.invoke.CallSite;
import java.lang.invoke.ClassRef;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleRef;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodTypeRef;
import java.util.StringJoiner;

import static java.lang.invoke.Intrinsics.invokedynamic;

public class IndyCodeGenerationTest {
    public static void main(String[] args) throws Throwable {
        String v;
        if (!(v = new IndyCodeGenerationTest().testWithStaticArgs()).equals("name-()String-1-2-3.0-4.0-int-something-(int,long,float,double)void-11")) {
            throw new AssertionError("unexpected value: " + v);
        }
    }

    String testWithStaticArgs() throws Throwable {
        MethodTypeRef methodTypeForMethodHandle = MethodTypeRef.of(
                ClassRef.CR_CallSite,
                ClassRef.CR_Lookup,
                ClassRef.CR_String,
                ClassRef.CR_MethodType,
                ClassRef.CR_int,
                ClassRef.CR_long,
                ClassRef.CR_float,
                ClassRef.CR_double,
                ClassRef.CR_Class,
                ClassRef.CR_String,
                ClassRef.CR_MethodType,
                ClassRef.CR_MethodHandle
        );
        MethodHandleRef mh = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, ClassRef.ofDescriptor("LIndyCodeGenerationTest;"),
                                                "testWithStaticArgsBSM", methodTypeForMethodHandle);
        BootstrapSpecifier bs = BootstrapSpecifier.of(mh,
                                                      1, 2L, 3.0f, 4.0d,
                                                      ClassRef.CR_int,
                                                      "something",
                                                      MethodTypeRef.ofDescriptor("(IJFD)V"), mh);
        return (String) invokedynamic(bs, "name");
    }

    public static CallSite testWithStaticArgsBSM(MethodHandles.Lookup l, String name, MethodType type,
                                                 int i, long j, float f, double d,
                                                 Class<?> c, String s,
                                                 MethodType mt, MethodHandle mh) {
        String v = new StringJoiner("-")
                .add(name)
                .add(type.toString())
                .add(Integer.toString(i))
                .add(Long.toString(j))
                .add(Float.toString(f))
                .add(Double.toString(d))
                .add(c.getSimpleName())
                .add(s)
                .add(mt.toString())
                .add(Integer.toString(mh.type().parameterCount()))
                .toString();
        return new ConstantCallSite(MethodHandles.constant(String.class, v));
    }

}
