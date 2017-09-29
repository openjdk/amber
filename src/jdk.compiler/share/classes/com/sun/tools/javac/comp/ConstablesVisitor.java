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

package com.sun.tools.javac.comp;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.DynamicMethodSymbol;
import com.sun.tools.javac.code.Symbol.IntrinsicsLDCMethodSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.Resolve.MethodResolutionPhase;
import com.sun.tools.javac.jvm.Pool;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCConditional;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCTypeCast;
import com.sun.tools.javac.tree.JCTree.JCUnary;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Options;
import com.sun.tools.javac.util.SpecialConstantUtils;

import static com.sun.tools.javac.code.Flags.EFFECTIVELY_FINAL;
import static com.sun.tools.javac.code.Flags.FINAL;
import static com.sun.tools.javac.code.Kinds.Kind.VAR;
import static com.sun.tools.javac.code.TypeTag.ARRAY;
import static com.sun.tools.javac.code.TypeTag.NONE;
import static com.sun.tools.javac.tree.JCTree.Tag.SELECT;

/**
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class ConstablesVisitor extends TreeScanner {
    protected static final Context.Key<ConstablesVisitor> constablesVisitorKey = new Context.Key<>();

    public static ConstablesVisitor instance(Context context) {
        ConstablesVisitor instance = context.get(constablesVisitorKey);
        if (instance == null)
            instance = new ConstablesVisitor(context);
        return instance;
    }

    private final Symtab syms;
    private final Names names;
    private final Types types;
    private final Resolve rs;
    private final Log log;
    private final ConstFold cfolder;
    private final SpecialConstantUtils specialConstantUtils;

    protected ConstablesVisitor(Context context) {
        context.put(constablesVisitorKey, this);
        Options options = Options.instance(context);
        doConstantFold = options.isSet("doConstantFold");
        syms = Symtab.instance(context);
        names = Names.instance(context);
        types = Types.instance(context);
        rs = Resolve.instance(context);
        log = Log.instance(context);
        cfolder = ConstFold.instance(context);
        specialConstantUtils = new SpecialConstantUtils(context);
        elementToConstantMap = new HashMap<>();
        specialConstantsHelper = new SpecialConstantsHelper(types, names, syms, rs, log, specialConstantUtils, elementToConstantMap);
    }

    /**
     * Switch: fold special constants
     */
    private final boolean doConstantFold;

    private Env<AttrContext> attrEnv;

    public void analyzeTree(JCTree tree, Env<AttrContext> attrEnv) {
        if (!doConstantFold) {
            return;
        }
        this.attrEnv = attrEnv;
        scan(tree);
        elementToConstantMap.clear();
    }

    Map<Object, Object> elementToConstantMap;

    @Override
    public void visitVarDef(JCVariableDecl tree) {
        super.visitVarDef(tree);
        if (tree.init != null) {
            VarSymbol v = tree.sym;
            Object val = tree.init.type.constValue();
            Object constant = elementToConstantMap.get(tree.init);
            if ((val != null || constant != null) &&
                    ((v.flags_field & FINAL) != 0 ||
                    (v.flags_field & EFFECTIVELY_FINAL) != 0)) {
                if (val != null) {
                    v.setData(val);
                    tree.type = tree.type.constType(val);
                } else {
                    elementToConstantMap.remove(tree.init);
                    elementToConstantMap.put(v, constant);
                }
            }
        }
    }

    Object getConstant(JCTree tree) {
        return tree.type.constValue() != null ?
                tree.type.constValue() :
                elementToConstantMap.get(tree);
    }

    @Override
    public void visitBinary(JCBinary tree) {
        super.visitBinary(tree);
        if (tree.type.constValue() == null &&
                getConstant(tree.lhs) != null &&
                getConstant(tree.rhs) != null) {
            Type ctype = cfolder.fold2(tree.operator.opcode, tree.lhs.type, tree.rhs.type, getConstant(tree.lhs), getConstant(tree.rhs));
            if (ctype != null) {
                tree.type = cfolder.coerce(ctype, tree.type);
            }
        }
    }

    @Override
    public void visitUnary(JCUnary tree) {
        super.visitUnary(tree);
        Object constant;
        if (tree.type.constValue() == null &&
                (constant = getConstant(tree.arg)) != null &&
                constant instanceof Number) {
            Type ctype = cfolder.fold1(tree.operator.opcode, tree.arg.type, getConstant(tree.arg));
            if (ctype != null) {
                tree.type = cfolder.coerce(ctype, tree.type);
            }
        }
    }

    @Override
    public void visitConditional(JCConditional tree) {
        // missing
        super.visitConditional(tree);
        if (tree.type.constValue() == null &&
            tree.cond.type.constValue() != null &&
            tree.truepart.type.constValue() != null &&
            tree.falsepart.type.constValue() != null &&
            !tree.type.hasTag(NONE)) {
            //constant folding
            tree.type = cfolder.coerce(tree.cond.type.isTrue() ? tree.truepart.type : tree.falsepart.type, tree.type);
        }
    }

    @Override
    public void visitTypeCast(JCTypeCast tree) {
        // missing
        super.visitTypeCast(tree);
        if (tree.type.constValue() == null &&
                tree.expr.type.constValue() != null) {
            tree.type = coerce(tree.expr.type, tree.type);
        }
    }

    Type coerce(Type etype, Type ttype) {
        Type coercedType = cfolder.coerce(etype, ttype);
        if (coercedType.constValue() == null) {
            // constant value lost in the intent
            if (types.isConvertible(etype, ttype)) {
                Type unboxedType = types.unboxedType(ttype);
                if (unboxedType != Type.noType) {
                    coercedType = cfolder.coerce(etype, unboxedType);
                    return ttype.constType(coercedType.constValue());
                }
            }
        }
        return coercedType;
    }

    @Override
    public void visitIdent(JCIdent tree) {
        super.visitIdent(tree);
        Symbol sym = TreeInfo.symbol(tree);
        if (sym.kind == VAR) {
            VarSymbol v = (VarSymbol)sym;
            Object constant = v.getConstValue();
            if (constant != null && tree.type.constValue() == null) {
                // we are seeing an effectively final variable which has just being assigned a
                // legacy constant, we should update the type of the tree if we want Gen to generate
                // the right code for us
                tree.type = tree.type.constType(constant);
                return;
            } else {
                constant = elementToConstantMap.get(v);
                constant = constant != null ?
                        constant :
                        specialConstantsHelper.foldTrackableField(tree, attrEnv);
                if (constant != null) {
                    elementToConstantMap.put(tree, constant);
                }
            }
        }
    }

    @Override
    public void visitSelect(JCFieldAccess tree) {
        super.visitSelect(tree);
        Object constantType = specialConstantsHelper.foldTrackableField(tree, attrEnv);
        if (constantType != null) {
            elementToConstantMap.put(tree, constantType);
        }
    }

    @Override
    public void visitApply(JCMethodInvocation tree) {
        super.visitApply(tree);
        Name methName = TreeInfo.name(tree.meth);
        boolean isConstructorCall = methName == names._this || methName == names._super;
        if (!isConstructorCall) {
            Object constant = specialConstantsHelper.foldMethodInvocation(tree, attrEnv);
            Symbol msym = TreeInfo.symbol(tree.meth);
            boolean isLDC = specialConstantUtils.isIntrinsicsLDCInvocation(msym);
            if (constant == null && isLDC) {
                log.error(tree.pos(), Errors.IntrinsicsLdcMustHaveConstantArg);
            }
            if (constant != null) {
                elementToConstantMap.put(tree, constant);
                if (isLDC) {
                    // if condy
                    if (tree.args.head.type.tsym == syms.dynamicConstantRefType.tsym) {
                        Object classRef =
                                specialConstantUtils.invokeReflectiveMethod(specialConstantUtils.dynamicConstantClass, constant, "type");
                        String descriptor = (String)specialConstantUtils.invokeReflectiveMethod(specialConstantUtils.classRefClass, classRef, "descriptorString");
                        Type newType = specialConstantUtils.descriptorToType(descriptor, attrEnv.enclClass.sym.packge().modle, false);
                        constant = specialConstantUtils.convertConstant(tree, attrEnv,
                                constant, attrEnv.enclClass.sym.packge().modle);
                        ((Pool.ConstantDynamic)constant).updateType(newType);
                        MethodType oldMT = tree.meth.type.asMethodType();
                        MethodType newMT = new MethodType(oldMT.argtypes, newType, oldMT.thrown, syms.methodClass);
                        MethodSymbol newMS = new MethodSymbol(msym.flags_field, msym.name, newMT, msym.owner);
                        TreeInfo.updateSymbol(tree.meth, msym, newMS);
                        tree.meth.type = newMT;
                        tree.type = newType;
                    } else {
                        constant = specialConstantUtils.convertConstant(tree, attrEnv,
                                constant, attrEnv.enclClass.sym.packge().modle);
                    }
                    // lets update the field as condy could have changed it
                    msym = TreeInfo.symbol(tree.meth);
                    IntrinsicsLDCMethodSymbol ldcSymbol = new IntrinsicsLDCMethodSymbol(
                            msym.flags_field, msym.name, msym.type, msym.owner, constant);
                    TreeInfo.updateSymbol(tree, msym, ldcSymbol);
                }
            } else if (specialConstantUtils.isIntrinsicsIndy(tree)) {
                List<Object> constants = specialConstantsHelper.extractAllConstansOrNone(List.of(tree.args.head, tree.args.tail.head));
                if (constants.isEmpty()) {
                    log.error(tree.args.head.pos(), Errors.IntrinsicsIndyMustHaveConstantArg);
                } else {
                    Object bootstrapSpecifier = constants.head;
                    String invocationName = (String)constants.tail.head;
                    if (invocationName.isEmpty()) {
                        log.error(tree.args.tail.head.pos(), Errors.InvocationNameCannotBeEmpty);
                    }
                    Object mh = specialConstantUtils.invokeReflectiveMethod(specialConstantUtils.bootstrapSpecifierClass,
                            bootstrapSpecifier, "method");
                    Pool.MethodHandle mHandle = (Pool.MethodHandle)specialConstantUtils
                            .convertConstant(tree, attrEnv, mh, attrEnv.enclClass.sym.packge().modle);
                    boolean correct = false;
                    if (mHandle.refKind == 6 || mHandle.refKind == 8) {
                        MethodSymbol ms = (MethodSymbol)mHandle.refSym;
                        MethodType mt = (MethodType)ms.type;
                        correct = (mt.argtypes.size() >= 3 &&
                            mt.argtypes.head.tsym == syms.methodHandlesLookupType.tsym &&
                            mt.argtypes.tail.head.tsym == syms.stringType.tsym &&
                            mt.argtypes.tail.tail.head.tsym == syms.methodTypeType.tsym);
                    }
                    if (!correct) {
                        log.error(tree.args.head.pos(), Errors.MethodHandleNotSuitableIndy(mHandle.refSym.type));
                    }

                    ListBuffer<Type> arguments = new ListBuffer<>();
                    tree.args = tree.args.tail.tail;
                    tree.args.forEach(arg -> arguments.add(arg.type));
                    Object[] bsmArgs = (Object[])specialConstantUtils.invokeReflectiveMethod(specialConstantUtils.bootstrapSpecifierClass, bootstrapSpecifier, "arguments");
                    Object[] convertedBsmArgs = specialConstantUtils.convertConstants(tree, attrEnv, bsmArgs, attrEnv.enclClass.sym.packge().modle, true);
                    MethodType mType = new MethodType(arguments.toList(), tree.type, List.nil(), syms.methodClass);
                    DynamicMethodSymbol dynSym = new DynamicMethodSymbol(
                            names.fromString(invocationName),
                            syms.noSymbol,
                            mHandle.refKind,
                            (MethodSymbol)mHandle.refSym,
                            mType,
                            convertedBsmArgs);
                    TreeInfo.updateSymbol(tree, msym, dynSym);
                    tree.meth.type = mType;
                }
            }
        }
    }

    final SpecialConstantsHelper specialConstantsHelper;

    public static class SpecialConstantsHelper {
        final Types types;
        final Names names;
        final Symtab syms;
        final Resolve rs;
        final Log log;
        final SpecialConstantUtils specialConstantUtils;
        final Map<Object, Object> elementToConstantMap;

        public SpecialConstantsHelper(
                Types types,
                Names names,
                Symtab syms,
                Resolve rs,
                Log log,
                SpecialConstantUtils specialConstantUtils,
                Map<Object, Object> elementsToConstantsMap) {
            this.syms = syms;
            this.types = types;
            this.names = names;
            this.rs = rs;
            this.log = log;
            this.specialConstantUtils = specialConstantUtils;
            this.elementToConstantMap = elementsToConstantsMap;
        }

        /* This method doesnt verify that the annotated field is static it is assumed that
         * it has to be
         */
        Object foldTrackableField(final JCTree tree, final Env<AttrContext> env) {
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

        Object foldMethodInvocation(final JCMethodInvocation tree, final Env<AttrContext> env) {
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

        List<Object> extractAllConstansOrNone(List<JCExpression> args) {
            ListBuffer<Object> constantArgumentValues = new ListBuffer<>();
            for (JCExpression arg: args) {
                Object argConstant = arg.type.constValue();
                if (argConstant != null) {
                    constantArgumentValues.add(argConstant);
                } else {
                    argConstant = elementToConstantMap.get(arg) != null ?
                            elementToConstantMap.get(arg) :
                            elementToConstantMap.get(TreeInfo.symbol(arg));
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
                    Object instance = elementToConstantMap.get(qualifierTree);
                    className = msym.owner.type.tsym.flatName().toString();
                    methodName = msym.name;
                    Class<?> constablesClass = Class.forName(className, false, null);
                    MethodType mt = msym.type.asMethodType();
                    java.util.List<Class<?>> argumentTypes =
                            mt.argtypes.stream().map(t -> getClassForType(t)).collect(List.collector());
                    Method theMethod = constablesClass.getDeclaredMethod(methodName.toString(),
                            argumentTypes.toArray(new Class<?>[argumentTypes.size()]));
                    int modifiers = theMethod.getModifiers();
                    Type varTypeElement = mt.getParameterTypes().last();
                    if (varTypeElement != null) {
                        varTypeElement = types.elemtype(varTypeElement);
                    }
                    Object[] args = boxArgs(
                            mt.argtypes,
                            constantArgumentValues,
                            tree.resolutionPhase == MethodResolutionPhase.VARARITY ? varTypeElement : null);
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
}
