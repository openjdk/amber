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
public class StringProcessorFactory implements IntrinsicProcessorFactory {
    @Override
    public void register() {
        Intrinsics.register(this,
                String.class, "align", String.class);
        Intrinsics.register(this,
                String.class, "align", String.class, int.class);
        Intrinsics.register(this,
                String.class, "indent", String.class, int.class);
        Intrinsics.register(this,
                String.class, "length", int.class);
        Intrinsics.register(this,
                String.class, "matches", boolean.class, String.class);
        Intrinsics.register(this,
                String.class, "repeat", String.class, int.class);
        Intrinsics.register(this,
                String.class, "replaceAll", String.class, String.class, String.class);
        Intrinsics.register(this,
                String.class, "replaceFirst", String.class, String.class, String.class);
        Intrinsics.register(this,
                String.class, "split", String.class);
        Intrinsics.register(this,
                String.class, "split", String.class, int.class);
        Intrinsics.register(this,
                String.class, "strip", String.class);
        Intrinsics.register(this,
                String.class, "stripLeading", String.class);
        Intrinsics.register(this,
                String.class, "stripTrailing", String.class);
    }

    private static StringProcessor instance;

    @Override
    public IntrinsicProcessor processor() {
        if (instance == null) {
            instance = new StringProcessor();
        }

        return instance;
    }

    static class StringProcessor implements IntrinsicProcessor {
        @Override
        public Result tryIntrinsify(IntrinsicContext intrinsicContext,
                                    ClassDesc ownerDesc,
                                    String methodName,
                                    MethodTypeDesc methodType,
                                    boolean isStatic,
                                    ClassDesc[] argClassDescs,
                                    ConstantDesc<?>[] constantArgs) {
            if (ClassDesc.of("java.lang.String").equals(ownerDesc)) {
                // Compile time checks
                switch (methodName) {
                    case "matches":
                    case "replaceAll":
                    case "replaceFirst":
                    case "split":
                        if (!Intrinsics.checkRegex(intrinsicContext, constantArgs, 1)) {
                            return new Result.None();
                        }
                        break;
                }

                // Fold when all arguments are constant
                if (Intrinsics.isAllConstants(constantArgs)) {
                    try {
                        String string = (String)constantArgs[0];

                        switch (methodName) {
                            case "align": {
                                if (constantArgs.length == 2) {
                                    return new Result.Ldc(string.align((Integer)constantArgs[1]));
                                } else {
                                    return new Result.Ldc(string.align());
                                }
                            }
                            case "indent": {
                                return new Result.Ldc(string.indent((Integer)constantArgs[1]));
                            }
                            case "length": {
                                 return new Result.Ldc(string.length());
                            }
                            case "repeat": {
                                return new Result.Ldc(string.repeat((Integer)constantArgs[1]));
                            }
                            case "strip": {
                                return new Result.Ldc(string.strip());
                            }
                            case "stripLeading": {
                                return new Result.Ldc(string.stripLeading());
                            }
                            case "stripTrailing": {
                                return new Result.Ldc(string.stripTrailing());
                            }
                        }
                    } catch (Exception ex) {
                        return new Result.None();
                    }
                }
            }

            return new Result.None();
        }
    }
}

