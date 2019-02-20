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

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DynamicCallSiteDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Locale;

import static com.sun.tools.javac.code.TypeTag.ARRAY;
import static com.sun.tools.javac.tree.JCTree.Tag.SELECT;
import static java.lang.constant.ConstantDescs.CD_CallSite;
import static java.lang.constant.ConstantDescs.CD_String;

/**
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class FormatterProcessor implements IntrinsicProcessor {
    @Override
    public void register(Intrinsics intrinsics) {
        this.intrinsics = intrinsics;
        intrinsics.register(this,
                PrintStream.class, "printf", PrintStream.class, String.class, Object[].class);
        intrinsics.register(this,
                PrintStream.class, "printf", PrintStream.class, Locale.class, String.class, Object[].class);
        intrinsics.register(this,
                PrintStream.class, "format", PrintStream.class, String.class, Object[].class);
        intrinsics.register(this,
                PrintStream.class, "format", PrintStream.class, Locale.class, String.class, Object[].class);
        intrinsics.register(this,
                PrintWriter.class, "printf", PrintWriter.class, String.class, Object[].class);
        intrinsics.register(this,
                PrintWriter.class, "printf", PrintWriter.class, Locale.class, String.class, Object[].class);
        intrinsics.register(this,
                PrintWriter.class, "format", PrintWriter.class, String.class, Object[].class);
        intrinsics.register(this,
                PrintWriter.class, "format", PrintWriter.class, Locale.class, String.class, Object[].class);
        intrinsics.register(this,
                String.class, "format", String.class, String.class, Object[].class);
        intrinsics.register(this,
                String.class, "format", String.class, Locale.class, String.class, Object[].class);
        intrinsics.register(this,
                String.class, "format", String.class, Object[].class);
        intrinsics.register(this,
                String.class, "format", String.class, Locale.class, Object[].class);
        intrinsics.register(this,
                Formatter.class, "format", Formatter.class, String.class, Object[].class);
        intrinsics.register(this,
                Formatter.class, "format", Formatter.class, Locale.class, String.class, Object[].class);
    }

    Intrinsics intrinsics;

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

    private static final ClassDesc CD_Locale = ClassDesc.of("java.util.Locale");
    private static final ClassDesc CD_IntrinsicFactory = ClassDesc.of("java.lang.invoke.IntrinsicFactory");

    @Override
    public Result tryIntrinsify(JCTree.JCMethodInvocation invocation,
                                ClassDesc ownerDesc,
                                String methodName,
                                MethodTypeDesc methodType,
                                boolean isStatic,
                                ClassDesc[] argClassDescs,
                                ConstantDesc[] constantArgs) {
        if (intrinsics.isArrayVarArg(argClassDescs, methodType.parameterCount())) {
            return new Result.None();
        }

        boolean hasLocale = CD_Locale.equals(methodType.parameterType(0));
        int formatArgPos = hasLocale ? 2 : 1;

        if (CD_String.equals(ownerDesc)) {
            formatArgPos = isStatic && hasLocale ? 1 : 0;
        }

        ConstantDesc constantFormat = constantArgs[formatArgPos];

        if (constantFormat == null) {
            return new Result.None();
        }

        String strFormat = (String)constantFormat;
        strFormat = strFormat.replaceAll("%%", "");
        int numberOfConversionChars = strFormat.length() - strFormat.replaceAll("%", "").length();
        if (numberOfConversionChars == 0) {
            // just LDC the format str
            return new Result.Ldc(((String)constantFormat).replaceAll("%%", "%"));
        }

        boolean allConstants = Arrays.stream(constantArgs).allMatch(c -> c != null);
        if (allConstants && isStatic && !hasLocale) {
            String formatted = (String)invokeMethodReflectively(invocation, Arrays.stream(constantArgs).collect(List.collector()));
            if (formatted != null) {
                return new Result.Ldc(formatted);
            }
        }

        String bsmName = getBSMName(ownerDesc, methodName, isStatic, hasLocale);

        MethodTypeDesc methodTypeLessFormat = methodType.dropParameterTypes(formatArgPos, formatArgPos + 1);

        return new Result.Indy(
                DynamicCallSiteDesc.of(
                        ConstantDescs.ofCallsiteBootstrap(
                                CD_IntrinsicFactory,
                                bsmName,
                                CD_CallSite
                        ),
                        methodName,
                        methodTypeLessFormat,
                        new ConstantDesc[] { constantFormat }),
                        intrinsics.dropArg(argClassDescs.length, formatArgPos)
        );
    }

    Object invokeMethodReflectively(
            final JCTree.JCMethodInvocation tree,
            List<Object> constantArgumentValues) {
        try {
            Symbol msym = TreeInfo.symbol(tree.meth);
            JCTree qualifierTree = (tree.meth.hasTag(SELECT))
                    ? ((JCTree.JCFieldAccess) tree.meth).selected
                    : null;
            String className = msym.owner.type.tsym.flatName().toString();
            Name methodName = msym.name;
            Class<?> ownerClass = Class.forName(className, false, null);
            Type.MethodType mt = msym.type.asMethodType();
            java.util.List<Class<?>> argumentTypes =
                    mt.argtypes.stream().map(t -> getClassForType(t)).collect(List.collector());
            Method theMethod = ownerClass.getDeclaredMethod(methodName.toString(),
                    argumentTypes.toArray(new Class<?>[argumentTypes.size()]));
            int modifiers = theMethod.getModifiers();
            Object[] args = boxArgs(
                    mt.argtypes,
                    constantArgumentValues,
                    tree.varargsElement);
            return theMethod.invoke(null, args);
        } catch (ClassNotFoundException |
                SecurityException |
                NoSuchMethodException |
                IllegalAccessException |
                IllegalArgumentException |
                InvocationTargetException ex) {
            return null;
        }
    }

    Class<?> getClassForType(Type t) {
        try {
            if (t.isPrimitiveOrVoid()) {
                return t.getTag().theClass;
            } else {
                return Class.forName(getFlatName(t), false, null);
            }
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }

    String getFlatName(Type t) {
        String flatName = t.tsym.flatName().toString();
        if (t.hasTag(ARRAY)) {
            flatName = "";
            while (t.hasTag(ARRAY)) {
                Type.ArrayType at = (Type.ArrayType)t;
                flatName += "[";
                t = at.elemtype;
            }
            flatName += "L" + t.tsym.flatName().toString() + ';';
        }
        return flatName;
    }

    Object[] boxArgs(List<Type> parameters, List<Object> _args, Type varargsElement) {
        java.util.List<Object> result = new java.util.ArrayList<>();
        List<Object> args = _args;
        if (parameters.isEmpty()) return new Object[0];
        while (parameters.tail.nonEmpty()) {
            result.add(args.head);
            args = args.tail;
            parameters = parameters.tail;
        }
        if (varargsElement != null) {
            java.util.List<Object> elems = new java.util.ArrayList<>();
            while (args.nonEmpty()) {
                elems.add(args.head);
                args = args.tail;
            }
            Class<?> arrayClass = null;
            try {
                arrayClass = Class.forName(getFlatName(varargsElement), false, null);
            } catch (ClassNotFoundException ex) {}
            Object arr = Array.newInstance(arrayClass, elems.size());
            for (int i = 0; i < elems.size(); i++) {
                Array.set(arr, i, elems.get(i));
            }
            result.add(arr);
        } else {
            if (args.length() != 1) throw new AssertionError(args);
            result.add(args.head);
        }
        return result.toArray();
    }
}
