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
 * @compile -XDdoConstantFold IndyPositiveTest01.java
 * @run main IndyPositiveTest01
 */

import java.lang.sym.*;

import com.sun.tools.javac.util.Assert;
import static java.lang.invoke.Intrinsics.*;

public class IndyPositiveTest01 {
    public static void main(String... args) throws Throwable {
        Assert.check(new IndyPositiveTest01().test("1", "2").equals("12"));
    }

    String test(String x, String y) throws Throwable {
        MethodTypeRef methodTypeForMethodHandle = MethodTypeRef.of(
                SymbolicRefs.CR_CallSite,
                SymbolicRefs.CR_Lookup,
                SymbolicRefs.CR_String,
                SymbolicRefs.CR_MethodType,
                SymbolicRefs.CR_String,
                SymbolicRefs.CR_Object.array()
        );
        MethodHandleRef mh = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, ClassRef.ofDescriptor("Ljava/lang/invoke/StringConcatFactory;"),
                                                "makeConcatWithConstants", methodTypeForMethodHandle);
        MethodTypeRef methodTypeForIndy = MethodTypeRef.of(
                SymbolicRefs.CR_String,
                SymbolicRefs.CR_String,
                SymbolicRefs.CR_String
        );
        final String param = "" + '\u0001' + '\u0001';
        IndyRef indyDescr = IndyRef.of(mh, "makeConcatWithConstants", methodTypeForIndy, param);
        return (String)invokedynamic(indyDescr, x, y);
    }
}
