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

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.DynamicVarSymbol;
import com.sun.tools.javac.code.Symbol.DynamicMethodSymbol;
import com.sun.tools.javac.code.Symbol.IntrinsicsLDCMethodSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.jvm.Pool;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCConditional;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCTypeCast;
import com.sun.tools.javac.tree.JCTree.JCUnary;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Constables;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Options;

import static com.sun.tools.javac.code.Kinds.Kind.VAR;
import static com.sun.tools.javac.code.TypeTag.NONE;
import static com.sun.tools.javac.main.Option.G;
import static com.sun.tools.javac.main.Option.G_CUSTOM;

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
    private final Log log;
    private final ConstFold cfolder;
    private final Constables constables;
    private final boolean varDebugInfo;
    private final TreeMaker make;

    protected ConstablesVisitor(Context context) {
        context.put(constablesVisitorKey, this);
        Options options = Options.instance(context);
        doConstantFold = options.isSet("doConstantFold");
        syms = Symtab.instance(context);
        names = Names.instance(context);
        types = Types.instance(context);
        log = Log.instance(context);
        cfolder = ConstFold.instance(context);
        make = TreeMaker.instance(context);
        constables = new Constables(context);
        varDebugInfo =
            options.isUnset(G_CUSTOM)
            ? options.isSet(G)
            : options.isSet(G_CUSTOM, "vars");
        elementToConstantMap = new HashMap<>();
    }

    /**
     * Switch: fold special constants
     */
    private final boolean doConstantFold;

    private Env<AttrContext> attrEnv;

    public Map<Object, Object> elementToConstantMap;

    public JCTree analyzeTree(JCTree tree, Env<AttrContext> attrEnv) {
        try {
            if (!doConstantFold) {
                return tree;
            }
            this.attrEnv = attrEnv;
            errorForLDCAndIndy = false;
            scan(tree);
            errorForLDCAndIndy = true;
            scan(tree);
            if (!varDebugInfo) {
                tree = constablesSetter.translate(tree);
            }
            return tree;
        } finally {
            elementToConstantMap.clear();
        }
    }

    boolean errorForLDCAndIndy = false;

    @Override
    public void visitVarDef(JCVariableDecl tree) {
        super.visitVarDef(tree);
        if (tree.init != null) {
            VarSymbol v = tree.sym;
            if (elementToConstantMap.get(v) != null) {
                return;
            }
            Object constant = getConstant(tree.init);
            if (constant != null &&
                    (v.isFinal() || v.isEffectivelyFinal())) {
                elementToConstantMap.remove(tree.init);
                elementToConstantMap.put(v, constant);
            }
        }
    }

    Object getConstant(JCTree tree) {
        Symbol sym = TreeInfo.symbol(tree);
        Object result = tree.type.constValue() != null ?
                tree.type.constValue() :
                elementToConstantMap.get(tree);
        return result == null ? elementToConstantMap.get(sym) : result;
    }

    @Override
    public void visitBinary(JCBinary tree) {
        super.visitBinary(tree);
        if (elementToConstantMap.get(tree) == null &&
                tree.type.constValue() == null &&
                getConstant(tree.lhs) != null &&
                getConstant(tree.rhs) != null) {
            Object constant = cfolder.fold2(tree.operator, getConstant(tree.lhs), getConstant(tree.rhs));
            if (constant != null) {
                elementToConstantMap.put(tree, constant);
            }
        }
    }

    @Override
    public void visitUnary(JCUnary tree) {
        super.visitUnary(tree);
        Object constant;
        if (elementToConstantMap.get(tree) == null &&
                tree.type.constValue() == null &&
                (constant = getConstant(tree.arg)) != null &&
                constant instanceof Number) {
            constant = cfolder.fold1(tree.operator, constant);
            if (constant != null) {
                elementToConstantMap.put(tree, constant);
            }
        }
    }

    @Override
    public void visitConditional(JCConditional tree) {
        super.visitConditional(tree);
        if (elementToConstantMap.get(tree) != null) {
            return;
        }
        Object condConstant = getConstant(tree.cond);
        Object truePartConstant = getConstant(tree.truepart);
        Object falsePartConstant = getConstant(tree.falsepart);
        if (tree.type.constValue() == null &&
            condConstant != null &&
            truePartConstant != null &&
            falsePartConstant != null &&
            !tree.type.hasTag(NONE)) {
            Object constant = ConstFold.isTrue(tree.cond.type.getTag(), condConstant) ? truePartConstant : falsePartConstant;
            elementToConstantMap.put(tree, constant);
        }
        if (condConstant != null) {
            elementToConstantMap.put(tree.cond, condConstant);
        }
    }

    @Override
    public void visitTypeCast(JCTypeCast tree) {
        super.visitTypeCast(tree);
        if (elementToConstantMap.get(tree) == null &&
                tree.type.constValue() == null &&
                getConstant(tree.expr) != null) {
            elementToConstantMap.put(tree, getConstant(tree.expr));
        }
    }

    @Override
    public void visitIdent(JCIdent tree) {
        super.visitIdent(tree);
        checkForSymbolConstant(tree);
    }

    void checkForSymbolConstant(JCTree tree) {
        if (elementToConstantMap.get(tree) != null) {
            return;
        }
        Symbol sym = TreeInfo.symbol(tree);
        if (sym.kind == VAR) {
            VarSymbol v = (VarSymbol)sym;
            Object constant = v.getConstValue();
            if (constant != null && tree.type.constValue() == null) {
                // we are seeing an effectively final variable which has just being assigned a
                // legacy constant, we should update the type of the tree if we want Gen to generate
                // the right code for us
                tree.type = tree.type.constType(constant);
            } else {
                constant = elementToConstantMap.get(v);
                constant = constant != null ?
                        constant :
                        constables.foldTrackableField(tree, attrEnv);
                if (constant != null) {
                    elementToConstantMap.put(tree, constant);
                }
            }
        }
    }

    @Override
    public void visitSelect(JCFieldAccess tree) {
        super.visitSelect(tree);
        checkForSymbolConstant(tree);
    }

    @Override
    public void visitApply(JCMethodInvocation tree) {
        super.visitApply(tree);
        if (elementToConstantMap.get(tree) != null) {
            return;
        }
        Name methName = TreeInfo.name(tree.meth);
        boolean isConstructorCall = methName == names._this || methName == names._super;
        if (!isConstructorCall) {
            Object constant = constables.foldMethodInvocation(tree, attrEnv);
            Symbol msym = TreeInfo.symbol(tree.meth);
            boolean isLDC = constables.isIntrinsicsLDCInvocation(msym);
            if (constant == null && isLDC) {
                if (errorForLDCAndIndy) {
                    log.error(tree.pos(), Errors.IntrinsicsLdcMustHaveConstantArg);
                } else {
                    return;
                }
            }
            if (constant != null) {
                if (!isLDC) {
                    elementToConstantMap.put(tree, constant);
                }
                if (isLDC) {
                    Symbol sym = TreeInfo.symbol(tree.meth);
                    if (sym instanceof IntrinsicsLDCMethodSymbol) {
                        return;
                    }
                    Type newType;
                    // if condy
                    if (constables.dynamicConstantClass.isInstance(constant)) {
                        constant = constables.convertConstant(tree, attrEnv,
                                constant, attrEnv.enclClass.sym.packge().modle);
                        newType = ((Pool.DynamicVariable)constant).type;
                    } else {
                        newType = tree.meth.type.asMethodType().restype;
                        Type unboxed = types.unboxedType(newType);
                        newType = unboxed != Type.noType ? unboxed : newType;
                        constant = constables.convertConstant(tree, attrEnv,
                                constant, attrEnv.enclClass.sym.packge().modle);
                    }
                    MethodType oldMT = tree.meth.type.asMethodType();
                    MethodType newMT = new MethodType(oldMT.argtypes, newType, oldMT.thrown, syms.methodClass);
                    IntrinsicsLDCMethodSymbol ldcSymbol = new IntrinsicsLDCMethodSymbol(
                            msym.flags_field, msym.name, newMT, msym.owner, constant);
                    TreeInfo.setSymbol(tree.meth, ldcSymbol);
                    tree.meth.type = newMT;
                    tree.type = newMT.restype;
                }
            } else if (constables.isIntrinsicsIndy(tree.meth)) {
                List<Object> constants = constables.extractAllConstansOrNone(List.of(tree.args.head));
                if (constants.isEmpty()) {
                    if (errorForLDCAndIndy) {
                        log.error(tree.args.head.pos(), Errors.IntrinsicsIndyMustHaveConstantArg);
                    }
                } else {
                    Symbol sym = TreeInfo.symbol(tree.meth);
                    if (sym instanceof DynamicMethodSymbol) {
                        return;
                    }
                    Object indyRef = constants.head;
                    String invocationName = (String)constables.invokeMethodReflectively(constables.dynamicCallsiteRefClass,
                            indyRef, "name");
                    if (invocationName.isEmpty()) {
                        log.error(tree.args.tail.head.pos(), Errors.InvocationNameCannotBeEmpty);
                    }
                    Object mh = constables.invokeMethodReflectively(constables.dynamicCallsiteRefClass,
                            indyRef, "bootstrapMethod");
                    Pool.MethodHandle mHandle = (Pool.MethodHandle)constables
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
                    tree.args = tree.args.tail;
                    tree.args.forEach(arg -> arguments.add(arg.type));
                    Object[] bsmArgs = (Object[])constables.invokeMethodReflectively(constables.dynamicCallsiteRefClass, indyRef, "bootstrapArgs");
                    Object[] convertedBsmArgs = constables.convertConstants(tree, attrEnv, bsmArgs, attrEnv.enclClass.sym.packge().modle, true);
                    Object mt = constables.invokeMethodReflectively(constables.dynamicCallsiteRefClass, indyRef, "type");
                    String methodTypeDesc = (String)constables.invokeMethodReflectively(
                            constables.methodTypeRefClass, mt, "descriptorString");
                    MethodType mType = (MethodType)constables.descriptorToType(methodTypeDesc,
                            attrEnv.enclClass.sym.packge().modle, true);
                    DynamicMethodSymbol dynSym = new DynamicMethodSymbol(
                            names.fromString(invocationName),
                            syms.noSymbol,
                            mHandle.refKind,
                            (MethodSymbol)mHandle.refSym,
                            mType,
                            convertedBsmArgs);
                    TreeInfo.setSymbol(tree.meth, dynSym);
                    tree.meth.type = mType;
                    // we need to issue a warning if the type of the indy is not assignable to the type of the
                    // tree, same for condy
                    tree.type = mType.restype;
                }
            }
        }
    }

    ConstablesSetter constablesSetter = new ConstablesSetter();
    class ConstablesSetter extends TreeTranslator {
        @Override
        public void visitVarDef(JCVariableDecl tree) {
            super.visitVarDef(tree);
            tree = (JCVariableDecl)result;
            if (tree.init != null) {
                VarSymbol v = tree.sym;
                Object constant = elementToConstantMap.get(v);
                if (constant != null) {
                    v.setData(constant);
                }
            }
            result = tree;
        }

        Set<JCTree.Tag> treesToCheck = EnumSet.of(
                JCTree.Tag.SELECT,
                JCTree.Tag.APPLY,
                JCTree.Tag.IDENT,
                JCTree.Tag.CONDEXPR,
                JCTree.Tag.TYPECAST
        );

        @SuppressWarnings("unchecked")
        @Override
        public <T extends JCTree> T translate(T tree) {
            tree = super.translate(tree);
            Object constant = elementToConstantMap.get(tree);
            if (tree != null &&
                    treesToCheck.contains(tree.getTag()) &&
                    constant != null) {
                Optional<DynamicVarSymbol> opDynSym = constables.getDynamicFieldSymbol(tree, constant, attrEnv);
                if (opDynSym.isPresent()) {
                    DynamicVarSymbol dynSym = opDynSym.get();
                    JCTree ident = make.at(tree.pos()).Ident(dynSym);
                    ident.type = dynSym.type.constType(constant);
                    return (T)ident;
                } else {
                    tree.type = tree.type.constType(constant);
                }
            }
            return tree;
        }
    }
}
