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
import java.lang.constant.MethodTypeDesc;

/**
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class ProgramProcessorFactory implements IntrinsicProcessorFactory {
    @Override
    public void register() {
        try {
            Class<?> program = Class.forName(Object.class.getModule(), "java.lang.Program");

            if (program != null) {
                Intrinsics.register(this, program, "getThisClass", Class.class);
                Intrinsics.register(this, program, "getOuterMostClass", Class.class);
                Intrinsics.register(this, program, "getMethodName", String.class);
                Intrinsics.register(this, program, "getSourceName", String.class);
                Intrinsics.register(this, program, "getLineNumber", int.class);
            }
        } catch (SecurityException | LinkageError ex) {
            // Older version of jdk
        }
    }

    private static ProgramProcessor instance;

    @Override
    public IntrinsicProcessor processor() {
        if (instance == null) {
            instance = new ProgramProcessor();
        }

        return instance;
    }

    static class ProgramProcessor implements IntrinsicProcessor {
        @Override
        public Result tryIntrinsify(IntrinsicContext intrinsicContext,
                                    ClassDesc ownerDesc,
                                    String methodName,
                                    MethodTypeDesc methodType,
                                    boolean isStatic,
                                    ClassDesc[] argClassDescs,
                                    ConstantDesc<?>[] constantArgs) {
            if (ClassDesc.of("java.lang.Program").equals(ownerDesc)) {
                switch (methodName) {
                    case "getThisClass": {
                        return new Result.Ldc(intrinsicContext.getThisClass());
                    }
                    case "getOuterMostClass": {
                        return new Result.Ldc(intrinsicContext.getOuterMostClass());
                    }
                    case "getMethodName": {
                        return new Result.Ldc(intrinsicContext.getMethod().methodName());
                    }
                    case "getSourceName": {
                        return new Result.Ldc(intrinsicContext.getSourceName());
                    }
                    case "getLineNumber": {
                        return new Result.Ldc(intrinsicContext.getLineNumber());
                    }
                }
            }

            return new Result.None();
        }
    }
}

