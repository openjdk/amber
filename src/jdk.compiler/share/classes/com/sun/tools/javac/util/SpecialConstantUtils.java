/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.util;

import java.lang.reflect.Field;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.ModuleSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.jvm.ClassFile;
import com.sun.tools.javac.jvm.Pool;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;

import static com.sun.tools.javac.code.Flags.STATIC;
import static com.sun.tools.javac.code.TypeTag.ARRAY;
import static com.sun.tools.javac.tree.JCTree.Tag.APPLY;

/** This class is a support tool to parse a method descriptor and obtain a list of the types
 *  represented in it.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class SpecialConstantUtils {

    public SpecialConstantUtils(Context context) {
        types = Types.instance(context);
        names = Names.instance(context);
        syms = Symtab.instance(context);
        rs = Resolve.instance(context);
        log = Log.instance(context);
        try {
            methodHandleRefClass = Class.forName("java.lang.invoke.MethodHandleRef", false, null);
            methodTypeRefClass = Class.forName("java.lang.invoke.MethodTypeRef", false, null);
            classRefClass = Class.forName("java.lang.invoke.ClassRef", false, null);
            constantRefClass = Class.forName("java.lang.invoke.ConstantRef", false, null);
            constablesClass = Class.forName("java.lang.invoke.Constables", false, null);
            bootstrapSpecifierClass = Class.forName("java.lang.invoke.BootstrapSpecifier", false, null);
            dynamicConstantClass = Class.forName("java.lang.invoke.DynamicConstantRef", false, null);
        } catch (ClassNotFoundException ex) {
            methodHandleRefClass = null;
            methodTypeRefClass = null;
            constantRefClass = null;
            classRefClass = null;
            bootstrapSpecifierClass = null;
            dynamicConstantClass = null;
            constablesClass = null;
        }
    }

    final Types types;
    final Names names;
    final Symtab syms;
    final Resolve rs;
    final Log log;
    ModuleSymbol currentModule;

    /** The unread portion of the currently read type is
     *  signature[sigp..siglimit-1].
     */
    byte[] signature;
    int sigp;
    int siglimit;
    boolean sigEnterPhase = false;
    byte[] signatureBuffer;
    int sbp;

    /** Convert signature to type, where signature is a byte array segment.
     */
    public Type descriptorToType(String descriptor, ModuleSymbol currentModule, boolean methodDescriptor) {
        byte[] sig = descriptor.getBytes(Charset.forName("UTF-8"));
        signature = sig;
        sigp = 0;
        siglimit = sig.length - 1;
        sbp = 0;
        signatureBuffer = new byte[sig.length];
        this.currentModule = currentModule;
        try {
            if (methodDescriptor) {
                return internalMethodDescriptorToType();
            } else { // type descriptor
                return sigToType();
            }
        } catch (AssertionError ae) {
            return Type.noType;
        }
    }

    private Type internalMethodDescriptorToType() {
        if (signature[sigp] != '(') {
            throw new AssertionError("bad descriptor");
        }
        sigp++;
        List<Type> argtypes = sigToTypes(')');
        Type restype = sigToType();
        return new MethodType(argtypes,
                              restype,
                              List.nil(),
                              syms.methodClass);
    }

    /** Convert signature to type, where signature is implicit.
     */
    Type sigToType() {
        switch ((char) signature[sigp]) {
        case 'B':
            sigp++;
            return syms.byteType;
        case 'C':
            sigp++;
            return syms.charType;
        case 'D':
            sigp++;
            return syms.doubleType;
        case 'F':
            sigp++;
            return syms.floatType;
        case 'I':
            sigp++;
            return syms.intType;
        case 'J':
            sigp++;
            return syms.longType;
        case 'L':
            {
                Type t = classSigToType();
                if (sigp < siglimit && signature[sigp] == '.') {
                    throw new AssertionError("deprecated inner class signature syntax");
                }
                return t;
            }
        case 'S':
            sigp++;
            return syms.shortType;
        case 'V':
            sigp++;
            return syms.voidType;
        case 'Z':
            sigp++;
            return syms.booleanType;
        case '[':
            sigp++;
            return new ArrayType(sigToType(), syms.arrayClass);
        default:
            throw new AssertionError("bad descriptor");
        }
    }

    /** Convert class signature to type, where signature is implicit.
     */
    Type classSigToType() {
        if (signature[sigp] != 'L') {
            throw new AssertionError("bad descriptor");
        }
        sigp++;
        Type outer = Type.noType;
        int startSbp = sbp;

        while (true) {
            final byte c = signature[sigp++];
            switch (c) {

            case ';': {         // end
                ClassSymbol t = enterClass(names.fromUtf(signatureBuffer,
                                                         startSbp,
                                                         sbp - startSbp));

                try {
                    return (outer == Type.noType) ?
                            t.erasure(types) :
                        new ClassType(outer, List.<Type>nil(), t);
                } finally {
                    sbp = startSbp;
                }
            }
            case '.':
                //we have seen an enclosing non-generic class
                if (outer != Type.noType) {
                    ClassSymbol t = enterClass(names.fromUtf(signatureBuffer,
                                                 startSbp,
                                                 sbp - startSbp));
                    outer = new ClassType(outer, List.<Type>nil(), t);
                }
                signatureBuffer[sbp++] = (byte)'$';
                continue;
            case '/':
                signatureBuffer[sbp++] = (byte)'.';
                continue;
            default:
                signatureBuffer[sbp++] = c;
                continue;
            }
        }
    }

    ClassSymbol enterClass(Name name) {
        return syms.enterClass(currentModule, name);
    }

    /** Convert (implicit) signature to list of types
     *  until `terminator' is encountered.
     */
    List<Type> sigToTypes(char terminator) {
        List<Type> head = List.of(null);
        List<Type> tail = head;
        while (signature[sigp] != terminator)
            tail = tail.setTail(List.of(sigToType()));
        sigp++;
        return head.tail;
    }

    public Object convertConstant(JCTree tree, Env<AttrContext> attrEnv, Object constant, ModuleSymbol currentModule) {
        return convertConstant(tree, attrEnv, constant, currentModule, false);
    }

    public Object convertConstant(JCTree tree, Env<AttrContext> attrEnv, Object constant, ModuleSymbol currentModule, boolean bsmArg) {
        if (methodHandleRefClass.isInstance(constant)) {
            String name = (String)invokeReflectiveMethod(methodHandleRefClass, constant, "name");
            int refKind = (int)invokeReflectiveMethod(methodHandleRefClass, constant, "refKind");
            Object owner = invokeReflectiveMethod(methodHandleRefClass, constant, "owner");
            String ownerDescriptor = (String)invokeReflectiveMethod(classRefClass, owner, "descriptorString");
            Type ownerType = descriptorToType(ownerDescriptor, currentModule, false);
            Object mtConstant = invokeReflectiveMethod(methodHandleRefClass, constant, "type");
            String methodTypeDesc = (String)invokeReflectiveMethod(methodTypeRefClass, mtConstant, "descriptorString");
            MethodType mType = (MethodType)descriptorToType(
                    methodTypeDesc, currentModule, true);
            Symbol refSymbol = getReferenceSymbol(refKind, ownerType.tsym, name, mType);
            return new Pool.MethodHandle(refKind, refSymbol, types);
        } else if (methodTypeRefClass.isInstance(constant)) {
            String descriptor = (String)invokeReflectiveMethod(methodTypeRefClass, constant, "descriptorString");
            return types.erasure(descriptorToType(descriptor, currentModule, true));
        } else if (classRefClass.isInstance(constant)) {
            String descriptor = (String)invokeReflectiveMethod(classRefClass, constant, "descriptorString");
            if ((boolean)invokeReflectiveMethod(classRefClass, constant, "isPrimitive")) {
                if (bsmArg) {
                    Object condy = invokeReflectiveMethod(constablesClass, null, "reduce", new Class<?>[]{constantRefClass}, new Object[]{constant});
                    return convertConstant(tree, attrEnv, condy, currentModule);
                } else {
                    return rs.resolveInternalField(tree, attrEnv, types.boxedClass(descriptor).type, names.TYPE);
                }
            }
            Type type = descriptorToType(descriptor, currentModule, false);
            return type.hasTag(ARRAY) ? type : type.tsym;
        } else if (dynamicConstantClass.isInstance(constant)) {
            String name = (String)invokeReflectiveMethod(dynamicConstantClass, constant, "name");
            Object mh = invokeReflectiveMethod(dynamicConstantClass, constant, "bootstrapMethod");
            Pool.MethodHandle methodHandle = (Pool.MethodHandle)convertConstant(tree, attrEnv, mh, currentModule);
            Object[] args = (Object[])invokeReflectiveMethod(dynamicConstantClass, constant, "bootstrapArgs");
            Object[] convertedArgs = convertConstants(tree, attrEnv, args, currentModule, true);
            return new Pool.ConstantDynamic(names.fromString(name), methodHandle, convertedArgs, types);
        }
        return constant;
    }

    public Object[] convertConstants(JCTree tree, Env<AttrContext> attrEnv, Object[] constants, ModuleSymbol currentModule, boolean bsmArgs) {
        if (constants == null || constants.length == 0) {
            return constants;
        }
        Object[] result = new Object[constants.length];
        int i = 0;
        for (Object constant : constants) {
            result[i] = convertConstant(tree, attrEnv, constant, currentModule, bsmArgs);
            i++;
        }
        return result;
    }

    public boolean isPrimitiveClassRef(Object constant) {
        return classRefClass.isInstance(constant) &&
                (boolean)invokeReflectiveMethod(classRefClass, constant, "isPrimitive");
    }

    public Class<?> methodHandleRefClass;
    public Class<?> methodTypeRefClass;
    public Class<?> classRefClass;
    public Class<?> constantRefClass;
    public Class<?> constablesClass;
    public Class<?> bootstrapSpecifierClass;
    public Class<?> dynamicConstantClass;

    private Symbol getReferenceSymbol(int refKind, Symbol owner, String name, MethodType methodType) {
        long flags = refKind == ClassFile.REF_getStatic ||
                refKind == ClassFile.REF_putStatic ||
                refKind == ClassFile.REF_invokeStatic ? STATIC : 0;
        Name symbolName = refKind == ClassFile.REF_newInvokeSpecial ? names.init : names.fromString(name);
        switch (refKind) {
            case ClassFile.REF_newInvokeSpecial :
            case ClassFile.REF_invokeVirtual:
            case ClassFile.REF_invokeStatic:
            case ClassFile.REF_invokeSpecial:
            case ClassFile.REF_invokeInterface:
                return new MethodSymbol(flags, symbolName, methodType, owner);
            case ClassFile.REF_putField:
                return new VarSymbol(flags, symbolName, methodType.argtypes.tail.head, owner);
            case ClassFile.REF_putStatic:
                return new VarSymbol(flags, symbolName, methodType.argtypes.head, owner);
            case ClassFile.REF_getField:
            case ClassFile.REF_getStatic:
                return new VarSymbol(flags, symbolName, methodType.restype, owner);
            default:
                throw new AssertionError("invalid refKind value " + refKind);
        }
    }

    public Object invokeReflectiveMethod(
            Class<?> hostClass,
            Object instance,
            String methodName) {
        return invokeReflectiveMethod(hostClass, instance, methodName, new Class<?>[0], new Object[0]);
    }

    public Object invokeReflectiveMethod(
            Class<?> hostClass,
            Object instance,
            String methodName,
            Class<?>[] argumentTypes,
            Object[] arguments) {
        Method theMethod;
        try {
            theMethod = hostClass.getDeclaredMethod(methodName, argumentTypes);
            return theMethod.invoke(instance, arguments);
        } catch (NoSuchMethodException |
                SecurityException |
                IllegalAccessException |
                IllegalArgumentException |
                InvocationTargetException ex) {
            log.error(Errors.ReflectiveError(methodName, hostClass.getCanonicalName()));
        }
        return null;
    }

    public boolean isIntrinsicsIndy(JCTree tree) {
        return isIntrinsicsIndy(TreeInfo.symbol(tree));
    }

    public boolean isIntrinsicsIndy(Symbol msym) {
        return (msym != null &&
                msym.owner != null &&
                msym.owner.type != null &&
                msym.owner.type.tsym == syms.intrinsicsType.tsym &&
                msym.name == names.invokedynamic);
    }

    public boolean isIntrinsicsLDCInvocation(Symbol msym) {
        return (msym != null &&
                msym.owner != null &&
                msym.owner.type != null &&
                msym.owner.type.tsym == syms.intrinsicsType.tsym &&
                msym.name == names.ldc);
    }

    public boolean isIntrinsicsLDCInvocation(JCTree tree) {
        Symbol msym = TreeInfo.symbol(tree);
        return (tree.hasTag(APPLY) &&
                msym != null &&
                msym.owner != null &&
                msym.owner.type != null &&
                msym.owner.type.tsym == syms.intrinsicsType.tsym &&
                msym.name == names.ldc);
    }
}
