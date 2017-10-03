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

package com.sun.tools.javac.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.Map;

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
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.ConstablesVisitor;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.jvm.ClassFile;
import com.sun.tools.javac.jvm.Pool;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.TreeInfo;

import static com.sun.tools.javac.code.Flags.STATIC;
import static com.sun.tools.javac.code.TypeTag.ARRAY;
import static com.sun.tools.javac.tree.JCTree.Tag.APPLY;
import static com.sun.tools.javac.tree.JCTree.Tag.SELECT;

/** This class is a support tool to parse a method descriptor and obtain a list of the types
 *  represented in it.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Constables {

    public Constables(Context context) {
        types = Types.instance(context);
        names = Names.instance(context);
        syms = Symtab.instance(context);
        rs = Resolve.instance(context);
        log = Log.instance(context);
        constablesVisitor = ConstablesVisitor.instance(context);
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

    private final Types types;
    private final Names names;
    private final Symtab syms;
    private final Resolve rs;
    private final Log log;
    private ModuleSymbol currentModule;
    private final ConstablesVisitor constablesVisitor;

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
                    return rs.resolveInternalField(tree, attrEnv, boxedClass(descriptor).type, names.TYPE);
                }
            }
            Type type = descriptorToType(descriptor, currentModule, false);
            return type.hasTag(ARRAY) ? type : type.tsym;
        } else if (dynamicConstantClass.isInstance(constant)) {
            Object classRef =
                    invokeReflectiveMethod(dynamicConstantClass, constant, "type");
            String descriptor = (String)invokeReflectiveMethod(classRefClass, classRef, "descriptorString");
            Type type = descriptorToType(descriptor, attrEnv.enclClass.sym.packge().modle, false);
            String name = (String)invokeReflectiveMethod(dynamicConstantClass, constant, "name");
            Object mh = invokeReflectiveMethod(dynamicConstantClass, constant, "bootstrapMethod");
            Pool.MethodHandle methodHandle = (Pool.MethodHandle)convertConstant(tree, attrEnv, mh, currentModule);
            Object[] args = (Object[])invokeReflectiveMethod(dynamicConstantClass, constant, "bootstrapArgs");
            Object[] convertedArgs = convertConstants(tree, attrEnv, args, currentModule, true);
            return new Pool.ConstantDynamic(names.fromString(name), methodHandle, type, convertedArgs, types);
        }
        return constant;
    }
    // where
        private ClassSymbol boxedClass(String descriptor) {
            switch (descriptor) {
                case "I": return syms.enterClass(syms.java_base, syms.boxedName[TypeTag.INT.ordinal()]);
                case "J": return syms.enterClass(syms.java_base, syms.boxedName[TypeTag.LONG.ordinal()]);
                case "S": return syms.enterClass(syms.java_base, syms.boxedName[TypeTag.SHORT.ordinal()]);
                case "B": return syms.enterClass(syms.java_base, syms.boxedName[TypeTag.BYTE.ordinal()]);
                case "C": return syms.enterClass(syms.java_base, syms.boxedName[TypeTag.CHAR.ordinal()]);
                case "F": return syms.enterClass(syms.java_base, syms.boxedName[TypeTag.FLOAT.ordinal()]);
                case "D": return syms.enterClass(syms.java_base, syms.boxedName[TypeTag.DOUBLE.ordinal()]);
                case "Z": return syms.enterClass(syms.java_base, syms.boxedName[TypeTag.BOOLEAN.ordinal()]);
                case "V": return syms.enterClass(syms.java_base, syms.boxedName[TypeTag.VOID.ordinal()]);
                default:
                    throw new AssertionError("invalid primitive descriptor " + descriptor);
            }
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

    /* This method doesnt verify that the annotated field is static it is assumed that
     * it has to be
     */
    public Object foldTrackableField(final JCTree tree, final Env<AttrContext> env) {
        Symbol sym = TreeInfo.symbol(tree);
        boolean trackableConstant = sym.attribute(syms.trackableConstantType.tsym) != null &&
                sym.packge().modle == syms.java_base;
        if (trackableConstant) {
            String className = sym.owner.type.tsym.flatName().toString();
            try {
                Class<?> constablesClass = Class.forName(className, false, null);
                MemberKind mKind = getMemberKind(constablesClass, sym.name.toString());
                if (mKind == MemberKind.METHOD) {
                    // we are in the middle of a method invocation bail out
                    return null;
                }
                Field theField = constablesClass.getField(sym.name.toString());
                Object value = theField.get(null);
                if (value != null) {
                    return value;
                }
            } catch (ClassNotFoundException |
                    NoSuchFieldException |
                    IllegalAccessException ex) {
                log.error(tree, Errors.ReflectiveError(sym.name.toString(), className));
            }
        }
        return null;
    }

    enum MemberKind {
        FIELD,
        METHOD
    }

    MemberKind getMemberKind(Class<?> aClass, String name) {
        try {
            aClass.getField(name);
            return MemberKind.FIELD;
        } catch (NoSuchFieldException ex) {
            return MemberKind.METHOD;
        }
    }

    public Object foldMethodInvocation(final JCMethodInvocation tree, final Env<AttrContext> env) {
        Symbol msym = TreeInfo.symbol(tree.meth);
        Object constant = null;
        boolean trackableConstant = msym.attribute(syms.trackableConstantType.tsym) != null &&
                msym.packge().modle == syms.java_base;
        boolean isLDC = msym.owner.type.tsym == syms.intrinsicsType.tsym && msym.name == names.ldc;
        if (trackableConstant || isLDC) {
            List<Object> constantArgumentValues = extractAllConstansOrNone(tree.args);
            boolean allConstants = tree.args.isEmpty() == constantArgumentValues.isEmpty();
            if (allConstants) {
                if (trackableConstant) {
                    constant = invokeConstablesMethod(tree, env, constantArgumentValues);
                } else if (isLDC) {
                    constant = constantArgumentValues.head;
                }
            }
            if (constant != null) {
                return constant;
            }
        }
        return null;
    }

    public List<Object> extractAllConstansOrNone(List<JCExpression> args) {
        ListBuffer<Object> constantArgumentValues = new ListBuffer<>();
        for (JCExpression arg: args) {
            Object argConstant = arg.type.constValue();
            if (argConstant != null) {
                constantArgumentValues.add(argConstant);
            } else {
                argConstant = constablesVisitor.elementToConstantMap.get(arg) != null ?
                        constablesVisitor.elementToConstantMap.get(arg) :
                        constablesVisitor.elementToConstantMap.get(TreeInfo.symbol(arg));
                if (argConstant != null) {
                    constantArgumentValues.add(argConstant);
                } else {
                    return List.nil();
                }
            }
        }
        return constantArgumentValues.toList();
    }

    // where
        Object invokeConstablesMethod(
                final JCMethodInvocation tree,
                final Env<AttrContext> env,
                List<Object> constantArgumentValues) {
            String className = "";
            Name methodName = names.empty;
            try {
                Symbol msym = TreeInfo.symbol(tree.meth);
                JCTree qualifierTree = (tree.meth.hasTag(SELECT))
                    ? ((JCFieldAccess) tree.meth).selected
                    : null;
                Object instance = constablesVisitor.elementToConstantMap.get(qualifierTree);
                className = msym.owner.type.tsym.flatName().toString();
                methodName = msym.name;
                Class<?> constablesClass = Class.forName(className, false, null);
                MethodType mt = msym.type.asMethodType();
                java.util.List<Class<?>> argumentTypes =
                        mt.argtypes.stream().map(t -> getClassForType(t)).collect(List.collector());
                Method theMethod = constablesClass.getDeclaredMethod(methodName.toString(),
                        argumentTypes.toArray(new Class<?>[argumentTypes.size()]));
                int modifiers = theMethod.getModifiers();
                Object[] args = boxArgs(
                        mt.argtypes,
                        constantArgumentValues,
                        tree.varargsElement);
                if ((modifiers & Modifier.STATIC) == 0) {
                    return (instance != null) ? theMethod.invoke(instance, args) : null;
                }
                return theMethod.invoke(null, args);
            } catch (ClassNotFoundException |
                    SecurityException |
                    NoSuchMethodException |
                    IllegalAccessException |
                    IllegalArgumentException |
                    InvocationTargetException ex) {
                log.error(tree, Errors.ReflectiveError(methodName.toString(), className));
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
                    ArrayType at = (ArrayType)t;
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
