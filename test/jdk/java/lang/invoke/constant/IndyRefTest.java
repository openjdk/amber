/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.constant.ClassDesc;
import java.lang.invoke.constant.ConstantMethodHandleDesc;
import java.lang.invoke.constant.DynamicCallSiteDesc;
import java.lang.invoke.constant.MethodHandleDesc;
import java.lang.invoke.constant.MethodTypeDesc;

import org.testng.annotations.Test;

import static java.lang.invoke.constant.ConstantDescs.*;
import static org.testng.Assert.assertEquals;

/**
 * @test
 * @compile -XDfolding=false IndyRefTest.java
 * @run testng IndyRefTest
 * @summary unit tests for java.lang.invoke.constant.IndyRefTest
 */
@Test
public class IndyRefTest {
    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type,
                                     Object... args) {
        if (args.length == 0)
            return new ConstantCallSite(MethodHandles.constant(String.class, "Foo"));
        else
            return new ConstantCallSite(MethodHandles.constant(String.class, (String) args[0]));
    }

    public void testIndyRef() throws Throwable {
        ClassDesc c = ClassDesc.of("IndyRefTest");
        MethodTypeDesc mt = MethodTypeDesc.of(CR_CallSite, CR_MethodHandles_Lookup, CR_String, CR_MethodType, CR_Object.arrayType());
        ConstantMethodHandleDesc mh = MethodHandleDesc.of(MethodHandleDesc.Kind.STATIC, c, "bootstrap", mt);
        DynamicCallSiteDesc csd = DynamicCallSiteDesc.of(mh, "wooga", MethodTypeDesc.of(CR_String));
        CallSite cs = csd.resolveCallSiteDesc(MethodHandles.lookup());
        MethodHandle target = cs.getTarget();
        assertEquals("Foo", target.invoke());

        DynamicCallSiteDesc csd2 = DynamicCallSiteDesc.of(mh, "wooga", MethodTypeDesc.of(CR_String), "Bar");
        CallSite cs2 = csd2.resolveCallSiteDesc(MethodHandles.lookup());
        MethodHandle target2 = cs2.getTarget();
        assertEquals("Bar", target2.invoke());
    }
}

