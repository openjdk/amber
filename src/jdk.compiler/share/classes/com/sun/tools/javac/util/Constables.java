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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.Optional;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Kinds.Kind;
import com.sun.tools.javac.code.Scope.WriteableScope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import com.sun.tools.javac.code.Symbol.DynamicFieldSymbol;
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
import com.sun.tools.javac.comp.Resolve.*;
import com.sun.tools.javac.jvm.ClassFile;
import com.sun.tools.javac.jvm.Pool;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.resources.CompilerProperties.Warnings;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

import static com.sun.tools.javac.code.Flags.INTERFACE;
import static com.sun.tools.javac.code.Flags.STATIC;
import static com.sun.tools.javac.code.TypeTag.ARRAY;
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
            methodHandleRefClass = Class.forName("java.lang.sym.MethodHandleRef", false, null);
            methodTypeRefClass = Class.forName("java.lang.sym.MethodTypeRef", false, null);
            classRefClass = Class.forName("java.lang.sym.ClassRef", false, null);
            constantRefClass = Class.forName("java.lang.sym.SymbolicRef", false, null);
            indyRefClass = Class.forName("java.lang.sym.IndyRef", false, null);
            dynamicConstantClass = Class.forName("java.lang.sym.DynamicConstantRef", false, null);
            symbolicRefClass = Class.forName("java.lang.sym.SymbolicRef", false, null);
            symRefs = Class.forName("java.lang.sym.SymbolicRefs", false, null);
        } catch (ClassNotFoundException ex) {
            methodHandleRefClass = null;
            methodTypeRefClass = null;
            constantRefClass = null;
            classRefClass = null;
            indyRefClass = null;
            dynamicConstantClass = null;
            symRefs = null;
            symbolicRefClass = null;
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

    public Optional<DynamicFieldSymbol> getDynamicFieldSymbol(JCTree tree, Object constant, Env<AttrContext> attrEnv) {
        if (constant != null) {
            if (!canMakeItToConstantValue(tree.type) &&
                symbolicRefClass.isInstance(constant)) {
                constant = ((Optional<?>)invokeMethodReflectively(symbolicRefClass, constant, "toSymbolicRef")).get();
                // now this should be a condy that the compiler can understand
                // a Pool.ConstantDynamic
                Object condyOb = convertConstant(tree, attrEnv, constant, attrEnv.enclClass.sym.packge().modle);
                if (condyOb instanceof Pool.ConstantDynamic) {
                    Pool.ConstantDynamic condy = (Pool.ConstantDynamic)condyOb;
                    DynamicFieldSymbol dynSym = new DynamicFieldSymbol(condy.name,
                            syms.noSymbol,
                            condy.bsm.refKind,
                            (MethodSymbol)condy.bsm.refSym,
                            condy.type,
                            condy.args);
                    return Optional.of(dynSym);
                }
            }
        }
        return Optional.empty();
    }

    public Object convertConstant(JCTree tree, Env<AttrContext> attrEnv, Object constant, ModuleSymbol currentModule) {
        return convertConstant(tree, attrEnv, constant, currentModule, false);
    }

    public Object convertConstant(JCTree tree, Env<AttrContext> attrEnv, Object constant, ModuleSymbol currentModule, boolean bsmArg) {
        if (methodHandleRefClass.isInstance(constant)) {
            String name = (String)invokeMethodReflectively(methodHandleRefClass, constant, "name");
            int refKind = (int)invokeMethodReflectively(methodHandleRefClass, constant, "refKind");
            Object owner = invokeMethodReflectively(methodHandleRefClass, constant, "owner");
            String ownerDescriptor = (String)invokeMethodReflectively(classRefClass, owner, "descriptorString");
            Type ownerType = descriptorToType(ownerDescriptor, currentModule, false);
            Object mtConstant = invokeMethodReflectively(methodHandleRefClass, constant, "type");
            String methodTypeDesc = (String)invokeMethodReflectively(methodTypeRefClass, mtConstant, "descriptorString");
            MethodType mType = (MethodType)descriptorToType(methodTypeDesc, currentModule, true);
            // this method generates fake symbols as needed
            Symbol refSymbol = getReferenceSymbol(tree, refKind, ownerType.tsym, name, mType);
            boolean ownerFound = true;
            try {
                refSymbol.owner.complete();
            } catch (CompletionFailure ex) {
                log.warning(tree, Warnings.ClassNotFound(refSymbol.owner));
                ownerFound = false;
            }
            if (ownerFound) {
                ownerType = refSymbol.owner.type;
                checkIfMemberExists(tree, attrEnv, names.fromString(name), ownerType, mType.argtypes, refKind);
            }
            Pool.MethodHandle mHandle = new Pool.MethodHandle(refKind, refSymbol, types,
                    new Pool.MethodHandle.DumbMethodHandleCheckHelper(refKind, refSymbol));
            return mHandle;
        } else if (methodTypeRefClass.isInstance(constant)) {
            String descriptor = (String)invokeMethodReflectively(methodTypeRefClass, constant, "descriptorString");
            return types.erasure(descriptorToType(descriptor, currentModule, true));
        } else if (classRefClass.isInstance(constant)) {
            String descriptor = (String)invokeMethodReflectively(classRefClass, constant, "descriptorString");
            if (descriptor.length() == 1) {
                Object BSM_PRIMITIVE_CLASS = getFieldValueReflectively(symRefs, null, "BSM_PRIMITIVE_CLASS");
                Pool.MethodHandle methodHandle = (Pool.MethodHandle)convertConstant(tree, attrEnv,
                        BSM_PRIMITIVE_CLASS, currentModule);
                return new Pool.ConstantDynamic(names.fromString(descriptor), methodHandle, new Object[0], types);
            }
            Type type = descriptorToType(descriptor, currentModule, false);
            Symbol symToLoad;
            if (!type.hasTag(ARRAY)) {
                symToLoad = type.tsym;
            } else {
                Type elt = type;
                while (elt.hasTag(ARRAY)) {
                    elt = ((ArrayType)elt).elemtype;
                }
                symToLoad = elt.tsym;
            }
            try {
                symToLoad.complete();
            } catch (CompletionFailure ex) {
                log.warning(tree, Warnings.ClassNotFound(symToLoad));
            }
            return type.hasTag(ARRAY) ? type : type.tsym;
        } else if (dynamicConstantClass.isInstance(constant)) {
            Object classRef =
                    invokeMethodReflectively(dynamicConstantClass, constant, "type");
            String descriptor = (String)invokeMethodReflectively(classRefClass, classRef, "descriptorString");
            Type type = descriptorToType(descriptor, attrEnv.enclClass.sym.packge().modle, false);
            String name = (String)invokeMethodReflectively(dynamicConstantClass, constant, "name");
            Object mh = invokeMethodReflectively(dynamicConstantClass, constant, "bootstrapMethod");
            Pool.MethodHandle methodHandle = (Pool.MethodHandle)convertConstant(tree, attrEnv, mh, currentModule);
            Object[] args = (Object[])invokeMethodReflectively(dynamicConstantClass, constant, "bootstrapArgs");
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

        private void checkIfMemberExists(DiagnosticPosition pos, Env<AttrContext> attrEnv, Name name, Type qual, List<Type> args, int refKind) {
            Symbol refSym = resolveConstableMethod(pos, attrEnv, qual, name, args, List.nil());
            if (refSym.kind.isResolutionError()) {
                try {
                    refSym = rs.resolveInternalField(pos, attrEnv, qual, name);
                } catch (Throwable t) {
                    log.warning(pos, Warnings.MemberNotFoundAtClass(name,
                            (qual.tsym.flags_field & INTERFACE) == 0 ? "class" : "interface", qual.tsym));
                    return;
                }
            }
            Pool.MethodHandle.WarnMethodHandleCheckHelper mhCheckHelper = new Pool.MethodHandle.WarnMethodHandleCheckHelper(log, pos, refKind, refSym);
            mhCheckHelper.check();
        }

        public Symbol resolveConstableMethod(DiagnosticPosition pos, Env<AttrContext> env,
                                    Type site, Name name,
                                    List<Type> argtypes,
                                    List<Type> typeargtypes) {
            MethodResolutionContext resolveContext = rs.new MethodResolutionContext();
            resolveContext.internalResolution = true;
            resolveContext.silentFail = true;
            Symbol sym = rs.resolveQualifiedMethod(resolveContext, pos, env, site.tsym,
                    site, name, argtypes, typeargtypes);
            return sym;
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

    public Class<?> methodHandleRefClass;
    public Class<?> methodTypeRefClass;
    public Class<?> classRefClass;
    public Class<?> constantRefClass;
    public Class<?> indyRefClass;
    public Class<?> dynamicConstantClass;
    public Class<?> symbolicRefClass;
    public Class<?> symRefs;

    public boolean canMakeItToConstantValue(Type t) {
        return t.isPrimitive() || t.tsym == syms.stringType.tsym;
    }

    public boolean skipCodeGeneration(JCVariableDecl tree) {
        if (tree.init != null) {
            VarSymbol v = tree.sym;
            return (v.isLocal() &&
                v.owner.kind == Kind.MTH &&
                (v.isFinal() || v.isEffectivelyFinal()));
        }
        return false;
    }

    boolean canHaveInterfaceOwner(int refKind) {
        switch (refKind) {
            case ClassFile.REF_invokeStatic:
            case ClassFile.REF_invokeSpecial:
            case ClassFile.REF_invokeInterface:
            case ClassFile.REF_getStatic:
            case ClassFile.REF_putStatic:
                return true;
            default:
                return false;
        }
    }

    String refKindToString(int refKind) {
        switch (refKind) {
            case ClassFile.REF_newInvokeSpecial :
                return "REF_newInvokeSpecial";
            case ClassFile.REF_invokeVirtual:
                return "REF_invokeVirtual";
            case ClassFile.REF_invokeStatic:
                return "REF_invokeStatic";
            case ClassFile.REF_invokeSpecial:
                return "REF_invokeSpecial";
            case ClassFile.REF_invokeInterface:
                return "REF_invokeInterface";
            case ClassFile.REF_putField:
                return "REF_putField";
            case ClassFile.REF_putStatic:
                return "REF_putStatic";
            case ClassFile.REF_getField:
                return "REF_getField";
            case ClassFile.REF_getStatic:
                return "REF_getStatic";
            default:
                throw new AssertionError("invalid refKind value " + refKind);
        }
    }

    boolean checkMethodTypeShape(JCTree tree, int refKind, MethodType methodType) {
        boolean error;
        switch (refKind) {
            case ClassFile.REF_newInvokeSpecial :
                error = !methodType.restype.hasTag(TypeTag.VOID);
                break;
            case ClassFile.REF_invokeVirtual:
            case ClassFile.REF_invokeStatic:
            case ClassFile.REF_invokeSpecial:
            case ClassFile.REF_invokeInterface:
                error = false;
                break;
            case ClassFile.REF_putField:
                error = methodType.argtypes.size() != 2 || !methodType.restype.hasTag(TypeTag.VOID);
                break;
            case ClassFile.REF_putStatic:
                error = methodType.argtypes.size() != 1 || !methodType.restype.hasTag(TypeTag.VOID);
                break;
            case ClassFile.REF_getField:
                error = methodType.argtypes.size() != 1 || methodType.restype.hasTag(TypeTag.VOID);
                break;
            case ClassFile.REF_getStatic:
                error = methodType.argtypes.size() > 0 || methodType.restype.hasTag(TypeTag.VOID);
                break;
            default:
                throw new AssertionError("invalid refKind value " + refKind);
        }
        if (error) {
            log.error(tree, Errors.BadMethodTypeShape(methodType, refKindToString(refKind)));
        }
        if (methodType.argtypes.stream().filter(t -> t.hasTag(TypeTag.VOID)).findAny().isPresent()) {
            log.error(tree, Errors.BadMethodTypeShapeArgWithTypeVoid(methodType));
        }
        return !error;
    }

    private Symbol getReferenceSymbol(JCTree tree, int refKind, Symbol owner, String name, MethodType methodType) {
        if (!checkMethodTypeShape(tree, refKind, methodType)) {
            return syms.noSymbol;
        }
        long flags = refKind == ClassFile.REF_getStatic ||
                refKind == ClassFile.REF_putStatic ||
                refKind == ClassFile.REF_invokeStatic ? STATIC : 0;
        flags |= Flags.PUBLIC;
        Name symbolName = refKind == ClassFile.REF_newInvokeSpecial ? names.init : names.fromString(name);
        boolean canHaveInterfaceOwner = canHaveInterfaceOwner(refKind);
        switch (refKind) {
            case ClassFile.REF_newInvokeSpecial :
            case ClassFile.REF_invokeVirtual:
            case ClassFile.REF_invokeStatic:
            case ClassFile.REF_invokeSpecial:
            case ClassFile.REF_invokeInterface:
                if (refKind == ClassFile.REF_invokeInterface && (owner.flags_field & INTERFACE) == 0) {
                    Symbol result = generateMethodSymbolHelper(owner, symbolName, methodType, flags, true);
                    log.warning(tree, Warnings.MemberNotFoundAtClass(symbolName, "interface", result.owner));
                    return result;
                }
                if (!canHaveInterfaceOwner && (owner.flags_field & INTERFACE) != 0) {
                    Symbol result = generateMethodSymbolHelper(owner, symbolName, methodType, flags, false);
                    log.warning(tree, Warnings.MemberNotFoundAtClass(symbolName, "class", result.owner));
                    return result;
                }
                return new MethodSymbol(flags, symbolName, methodType, owner);
            case ClassFile.REF_putField:
                if ((owner.flags_field & INTERFACE) != 0) {
                    Symbol result = generateVarSymbolHelper(owner, symbolName, methodType, flags, false);
                    log.warning(tree, Warnings.MemberNotFoundAtClass(symbolName, "class", result.owner));
                    return result;
                }
                return new VarSymbol(flags, symbolName, methodType.argtypes.tail.head, owner);
            case ClassFile.REF_putStatic:
                return new VarSymbol(flags, symbolName, methodType.argtypes.head, owner);
            case ClassFile.REF_getField:
            case ClassFile.REF_getStatic:
                if (refKind == ClassFile.REF_getField && (owner.flags_field & INTERFACE) != 0) {
                    Symbol result = generateVarSymbolHelper(owner, symbolName, methodType, flags, false);
                    log.warning(tree, Warnings.MemberNotFoundAtClass(symbolName, "class", result.owner));
                    return result;
                }
                return new VarSymbol(flags, symbolName, methodType.restype, owner);
            default:
                throw new AssertionError("invalid refKind value " + refKind);
        }
    }

    private Symbol generateMethodSymbolHelper(
            Symbol currentOwner,
            Name symbolName,
            MethodType methodType,
            long flags,
            boolean shouldBeInterface) {
        ClassSymbol newOwner = createNewOwner(currentOwner, shouldBeInterface);
        Symbol newMS = new MethodSymbol(flags, symbolName, methodType, newOwner);
        newOwner.members_field.enter(newMS);
        return newMS;
    }

    private ClassSymbol createNewOwner(Symbol currentOwner,
            boolean shouldBeInterface) {
        long newFlags = shouldBeInterface ?
                currentOwner.flags_field | Flags.INTERFACE :
                currentOwner.flags_field & ~Flags.INTERFACE;
        ClassSymbol newOwner = new ClassSymbol(newFlags,
                currentOwner.name, currentOwner.owner);
        newOwner.members_field = WriteableScope.create(newOwner);
        return newOwner;
    }

    private Symbol generateVarSymbolHelper(
            Symbol currentOwner,
            Name symbolName,
            MethodType methodType,
            long flags,
            boolean shouldBeInterface) {
        ClassSymbol newOwner = createNewOwner(currentOwner, shouldBeInterface);
        Symbol newVS = new VarSymbol(flags, symbolName, methodType.restype, newOwner);
        newOwner.members_field.enter(newVS);
        return newVS;
    }

    public Object invokeMethodReflectively(
            Class<?> hostClass,
            Object instance,
            String methodName) {
        return invokeMethodReflectively(hostClass, instance, methodName, new Class<?>[0], new Object[0]);
    }

    public Object invokeMethodReflectively(
            Class<?> hostClass,
            Object instance,
            String methodName,
            Class<?>[] argumentTypes,
            Object[] arguments) {
        Method theMethod;
        try {
            theMethod = hostClass.getMethod(methodName, argumentTypes);
            return theMethod.invoke(instance, arguments);
        } catch (NoSuchMethodException |
                SecurityException |
                IllegalAccessException |
                IllegalArgumentException |
                InvocationTargetException ex) {
            Throwable e = (ex.getCause() == null) ? ex : ex.getCause();
            String msg = e.getLocalizedMessage();
            if (msg == null)
                msg = e.toString();
            log.error(Errors.ReflectiveError(methodName, hostClass.getCanonicalName(), msg));
            e.printStackTrace(System.err);
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
        boolean trackableConstant = sym.attribute(syms.foldableType.tsym) != null &&
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
                return getFieldValueReflectively(constablesClass, null, sym.name.toString());
            } catch (ClassNotFoundException ex) {
                log.error(tree, Errors.ReflectiveError(sym.name.toString(), className, ex.getCause().getLocalizedMessage()));
            }
        }
        return null;
    }

    Object getFieldValueReflectively(Class<?> hostClass, Object instance, String fieldName) {
        try {
            return hostClass.getField(fieldName).get(instance);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            Throwable e = (ex.getCause() == null) ? ex : ex.getCause();
            String msg = e.getLocalizedMessage();
            if (msg == null)
                msg = e.toString();
            log.error(Errors.ReflectiveError(fieldName, hostClass.getCanonicalName(), msg));
            e.printStackTrace(System.err);
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
        boolean trackableConstant = msym.attribute(syms.foldableType.tsym) != null &&
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
                log.error(tree, Errors.ReflectiveError(methodName.toString(), className, ex.getCause().getLocalizedMessage()));
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
