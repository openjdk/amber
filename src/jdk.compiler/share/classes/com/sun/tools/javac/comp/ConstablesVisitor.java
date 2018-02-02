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

import java.util.Optional;

import com.sun.tools.javac.code.Kinds.Kind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.DynamicFieldSymbol;
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

/**
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class ConstablesVisitor extends TreeTranslator {
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
    }

    /**
     * Switch: fold special constants
     */
    private final boolean doConstantFold;

    private Env<AttrContext> attrEnv;

    public JCTree analyzeTree(JCTree tree, Env<AttrContext> attrEnv) {
        if (!doConstantFold) {
            return tree;
        }
        this.attrEnv = attrEnv;
        return translate(tree);
    }

    @Override
    public void visitVarDef(JCVariableDecl tree) {
        super.visitVarDef(tree);
        tree = (JCVariableDecl)result;
        if (tree.init != null) {
            VarSymbol v = tree.sym;
            Object constant = tree.init.type.constValue();
            if (constant != null &&
                    (v.isFinal() || v.isEffectivelyFinal())) {
                v.setData(constant);
            }
            if (v.isLocal() &&
                v.owner.kind == Kind.MTH &&
                (v.isFinal() || v.isEffectivelyFinal())) {
                tree.skip = true;
            }
        }
    }

    @Override
    public void visitBinary(JCBinary tree) {
        super.visitBinary(tree);
        tree = (JCBinary)result;
        if (tree.type.constValue() == null &&
                tree.lhs.type.constValue() != null &&
                tree.rhs.type.constValue() != null) {

            Object constant = cfolder.fold2(tree.operator, tree.lhs.type.constValue(), tree.rhs.type.constValue());
            if (constant != null) {
                Type foldType = cfolder.foldType(tree.operator);
                if (foldType != null) {
                    tree.type = foldType.constType(constant);
                }
            }
        }
    }

    @Override
    public void visitUnary(JCUnary tree) {
        super.visitUnary(tree);
        tree = (JCUnary)result;
        Object constant;
        if (tree.type.constValue() == null &&
                (constant = tree.arg.type.constValue()) != null &&
                constant instanceof Number) {
            constant = cfolder.fold1(tree.operator, constant);
            if (constant != null) {
                Type foldType = cfolder.foldType(tree.operator);
                if (foldType != null) {
                    tree.type = foldType.constType(constant);
                }
            }
        }
    }

    @Override
    public void visitConditional(JCConditional tree) {
        super.visitConditional(tree);
        tree = (JCConditional)result;
        Object condConstant = tree.cond.type.constValue();
        Object truePartConstant = tree.truepart.type.constValue();
        Object falsePartConstant = tree.falsepart.type.constValue();
        if (tree.type.constValue() == null &&
            condConstant != null &&
            truePartConstant != null &&
            falsePartConstant != null &&
            !tree.type.hasTag(NONE)) {
            Object constant = ConstFold.isTrue(tree.cond.type.getTag(), condConstant) ? truePartConstant : falsePartConstant;
            tree.type = tree.type.constType(constant);
        }
        result = rewriteCondy(tree);
    }

    @Override
    public void visitTypeCast(JCTypeCast tree) {
        super.visitTypeCast(tree);
        tree = (JCTypeCast)result;
        if (tree.type.constValue() == null && tree.expr.type.constValue() != null) {
            tree.type = tree.type.constType(tree.expr.type.constValue());
        }
        result = rewriteCondy(tree);
    }

    @Override
    public void visitIdent(JCIdent tree) {
        super.visitIdent(tree);
        checkForSymbolConstant(tree);
        result = rewriteCondy(tree);
    }

    void checkForSymbolConstant(JCTree tree) {
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
                constant = constant != null ?
                        constant :
                        constables.foldTrackableField(tree, attrEnv);
                if (constant != null) {
                    tree.type = tree.type.constType(constant);
                }
            }
        }
    }

    @Override
    public void visitSelect(JCFieldAccess tree) {
        super.visitSelect(tree);
        tree = (JCFieldAccess)result;
        checkForSymbolConstant(tree);
        result = rewriteCondy(tree);
    }

    @Override
    public void visitApply(JCMethodInvocation tree) {
        super.visitApply(tree);
        tree = (JCMethodInvocation)result;
        Name methName = TreeInfo.name(tree.meth);
        boolean isConstructorCall = methName == names._this || methName == names._super;
        if (!isConstructorCall) {
            Object constant = constables.foldMethodInvocation(tree, attrEnv);
            Symbol msym = TreeInfo.symbol(tree.meth);
            boolean isLDC = constables.isIntrinsicsLDCInvocation(msym);
            if (constant == null && isLDC) {
                log.error(tree.pos(), Errors.IntrinsicsLdcMustHaveConstantArg);
            }
            if (constant != null) {
                if (isLDC) {
                    Type newType;
                    // if condy
                    if (constables.dynamicConstantClass.isInstance(constant)) {
                        constant = constables.convertConstant(tree, attrEnv,
                                constant, attrEnv.enclClass.sym.packge().modle);
                        newType = ((Pool.ConstantDynamic)constant).type;
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
                } else {
                    tree.type = tree.type.constType(constant);
                }
            } else if (constables.isIntrinsicsIndy(tree.meth)) {
                List<Object> constants = constables.extractAllConstansOrNone(List.of(tree.args.head));
                if (constants.isEmpty()) {
                    log.error(tree.args.head.pos(), Errors.IntrinsicsIndyMustHaveConstantArg);
                } else {
                    Object indyRef = constants.head;
                    String invocationName = (String)constables.invokeMethodReflectively(constables.indyRefClass,
                            indyRef, "name");
                    if (invocationName.isEmpty()) {
                        log.error(tree.args.tail.head.pos(), Errors.InvocationNameCannotBeEmpty);
                    }
                    Object mh = constables.invokeMethodReflectively(constables.indyRefClass,
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
                    Object[] bsmArgs = (Object[])constables.invokeMethodReflectively(constables.indyRefClass, indyRef, "bootstrapArgs");
                    Object[] convertedBsmArgs = constables.convertConstants(tree, attrEnv, bsmArgs, attrEnv.enclClass.sym.packge().modle, true);
                    Object mt = constables.invokeMethodReflectively(constables.indyRefClass, indyRef, "type");
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
            result = rewriteCondy(tree);
        }
    }

    JCTree rewriteCondy(JCTree tree) {
        Object constant = tree.type.constValue();
        Optional<DynamicFieldSymbol> opDynSym = constables.getDynamicFieldSymbol(tree, tree.type.constValue(), attrEnv);
        if (opDynSym.isPresent()) {
            DynamicFieldSymbol dynSym = opDynSym.get();
            JCIdent ident = make.at(tree.pos()).Ident(dynSym);
            ident.type = dynSym.type.constType(constant);
            return ident;
        }
        return tree;
    }
}
