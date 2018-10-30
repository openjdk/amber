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

import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DynamicCallSiteDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.Locale;

/**
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class FormatterProcessorFactory implements IntrinsicProcessorFactory {
    @Override
    public void register() {
        // static String methods
        Intrinsics.register(this,
                String.class, "format", String.class, String.class, Object[].class);
        Intrinsics.register(this,
                String.class, "format", String.class, Locale.class, String.class, Object[].class);

        // instance String methods
        Intrinsics.register(this,
                String.class, "format", String.class, Object[].class);
        Intrinsics.register(this,
                String.class, "format", String.class, Locale.class, Object[].class);

        // others
        Intrinsics.register(this,
                PrintStream.class, "printf", PrintStream.class, String.class, Object[].class);
        Intrinsics.register(this,
                PrintStream.class, "printf", PrintStream.class, Locale.class, String.class, Object[].class);
        Intrinsics.register(this,
                PrintStream.class, "format", PrintStream.class, String.class, Object[].class);
        Intrinsics.register(this,
                PrintStream.class, "format", PrintStream.class, Locale.class, String.class, Object[].class);
        Intrinsics.register(this,
                PrintWriter.class, "printf", PrintWriter.class, String.class, Object[].class);
        Intrinsics.register(this,
                PrintWriter.class, "printf", PrintWriter.class, Locale.class, String.class, Object[].class);
        Intrinsics.register(this,
                PrintWriter.class, "format", PrintWriter.class, String.class, Object[].class);
        Intrinsics.register(this,
                PrintWriter.class, "format", PrintWriter.class, Locale.class, String.class, Object[].class);
    }

    private static FormatterProcessor instance;

    @Override
    public IntrinsicProcessor processor() {
        if (instance == null) {
            instance = new FormatterProcessor();
        }

        return instance;
    }

    static class FormatterProcessor implements IntrinsicProcessor {
        static String lowerFirst(String string) {
            return string.substring(0, 1).toLowerCase() + string.substring(1);
        }

        static String upperFirst(String string) {
            return string.substring(0, 1).toUpperCase() + string.substring(1);
        }

        private String getBSMName(ClassDesc ownerDesc, String methodName, boolean isStatic, boolean hasLocale) {
            StringBuffer sb = new StringBuffer(32);

            if (isStatic) {
                sb.append("static");
                sb.append(ownerDesc.displayName());
            } else {
                sb.append(lowerFirst(ownerDesc.displayName()));
            }

            if (hasLocale) {
                sb.append("Locale");
            }

            sb.append(upperFirst(methodName));
            sb.append("Bootstrap");

            return sb.toString();
        }

        @Override
        public Result tryIntrinsify(IntrinsicContext intrinsicContext,
                                    ClassDesc ownerDesc,
                                    String methodName,
                                    MethodTypeDesc methodType,
                                    boolean isStatic,
                                    ClassDesc[] argClassDescs,
                                    ConstantDesc<?>[] constantArgs) {
            // Don't bother in array vararg case.
            if (Intrinsics.isArrayVarArg(argClassDescs, methodType.parameterCount())) {
                return new Result.None();
            }

            boolean hasLocale;
            int formatArg;

            if (!ClassDesc.of("java.lang.String").equals(ownerDesc)) {
                hasLocale = ClassDesc.of("java.util.Locale").equals(methodType.parameterType(1));
                formatArg = hasLocale ? 2 : 1;
            } else  if (isStatic) {
                hasLocale = ClassDesc.of("java.util.Locale").equals(methodType.parameterType(0));
                formatArg = hasLocale ? 1 : 0;
            } else {
                hasLocale = ClassDesc.of("java.util.Locale").equals(methodType.parameterType(1));
                formatArg = 0;
            }

            ConstantDesc<?> constantFormat = constantArgs[formatArg];

            if (constantFormat == null) {
                return new Result.None();
            }

            String bsmName = getBSMName(ownerDesc, methodName, isStatic, hasLocale);

            MethodTypeDesc methodTypeLessFormat =
                    methodType.dropParameterTypes(formatArg,  formatArg + 1);

            return new Result.Indy(
                    DynamicCallSiteDesc.of(
                            ConstantDescs.ofCallsiteBootstrap(
                                    ClassDesc.of("java.lang.invoke.IntrinsicFactory"),
                                    bsmName,
                                    ClassDesc.of("java.lang.invoke.CallSite")
                            ),
                            methodName,
                            methodTypeLessFormat,
                            new ConstantDesc<?>[]{constantFormat}),
                    Intrinsics.dropArg(argClassDescs.length, formatArg)
            );
        }
    }

 }

