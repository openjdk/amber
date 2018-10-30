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

package com.sun.tools.javac.intrinsics;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DynamicCallSiteDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.Arrays;
import java.util.Objects;

/**
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class HashProcessorFactory implements IntrinsicProcessorFactory {
    @Override
    public void register() {
        Intrinsics.register(this,
                Objects.class, "hash", int.class, Object[].class);
    }

    private static HashProcessor instance;

    @Override
    public IntrinsicProcessor processor() {
        if (instance == null) {
            instance = new HashProcessor();
        }

        return instance;
    }

    static class HashProcessor implements IntrinsicProcessor {
        @Override
        public Result tryIntrinsify(IntrinsicContext intrinsicContext,
                                    ClassDesc ownerDesc,
                                    String methodName,
                                    MethodTypeDesc methodType,
                                    boolean isStatic,
                                    ClassDesc[] argClassDescs,
                                    ConstantDesc<?>[] constantArgs) {
            if (ClassDesc.of("java.util.Objects").equals(ownerDesc)) {
                switch (methodName) {
                    case "hash":
                        if (Intrinsics.isAllConstants(constantArgs)) {
                            Object[] constants =
                                    Intrinsics.getConstants(argClassDescs, constantArgs);

                            return new Result.Ldc(Arrays.hashCode(constants));
                        } else {
                            if (Intrinsics.isArrayVarArg(argClassDescs, 0)) {
                                return new Result.None();
                            }

                            return new Result.Indy(
                                    DynamicCallSiteDesc.of(
                                            ConstantDescs.ofCallsiteBootstrap(
                                                    ClassDesc.of("java.lang.invoke.IntrinsicFactory"),
                                                    "objectsHashBootstrap",
                                                    ClassDesc.of("java.lang.invoke.CallSite")
                                            ),
                                            methodName,
                                            methodType,
                                            new ConstantDesc<?>[0]),
                                    argClassDescs.length
                            );
                        }
                }
            }

            return new Result.None();
        }
    }
}

