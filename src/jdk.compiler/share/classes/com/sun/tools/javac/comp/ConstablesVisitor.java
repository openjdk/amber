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

import java.util.HashMap;
import java.util.Map;

import com.sun.tools.javac.code.Symbol;
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
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Constables;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Options;

import static com.sun.tools.javac.code.Flags.EFFECTIVELY_FINAL;
import static com.sun.tools.javac.code.Flags.FINAL;
import static com.sun.tools.javac.code.Kinds.Kind.VAR;
import static com.sun.tools.javac.code.TypeTag.NONE;

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
    private final Constables constables;

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
        constables = new Constables(context);
        elementToConstantMap = new HashMap<>();
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

    public Map<Object, Object> elementToConstantMap;

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
        Object constantType = constables.foldTrackableField(tree, attrEnv);
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
            Object constant = constables.foldMethodInvocation(tree, attrEnv);
            Symbol msym = TreeInfo.symbol(tree.meth);
            boolean isLDC = constables.isIntrinsicsLDCInvocation(msym);
            if (constant == null && isLDC) {
                log.error(tree.pos(), Errors.IntrinsicsLdcMustHaveConstantArg);
            }
            if (constant != null) {
                elementToConstantMap.put(tree, constant);
                if (isLDC) {
                    // if condy
                    if (tree.args.head.type.tsym == syms.dynamicConstantRefType.tsym) {
                        constant = constables.convertConstant(tree, attrEnv,
                                constant, attrEnv.enclClass.sym.packge().modle);
                        MethodType oldMT = tree.meth.type.asMethodType();
                        MethodType newMT = new MethodType(oldMT.argtypes,
                                ((Pool.ConstantDynamic)constant).type, oldMT.thrown, syms.methodClass);
                        MethodSymbol newMS = new MethodSymbol(msym.flags_field, msym.name, newMT, msym.owner);
                        TreeInfo.setSymbol(tree.meth, newMS);
                        tree.meth.type = newMT;
                        tree.type = newMT.restype;
                    } else {
                        constant = constables.convertConstant(tree, attrEnv,
                                constant, attrEnv.enclClass.sym.packge().modle);
                    }
                    // lets update the field as condy could have changed it
                    msym = TreeInfo.symbol(tree.meth);
                    IntrinsicsLDCMethodSymbol ldcSymbol = new IntrinsicsLDCMethodSymbol(
                            msym.flags_field, msym.name, msym.type, msym.owner, constant);
                    TreeInfo.setSymbol(tree.meth, ldcSymbol);
                }
            } else if (constables.isIntrinsicsIndy(tree.meth)) {
                List<Object> constants = constables.extractAllConstansOrNone(List.of(tree.args.head, tree.args.tail.head));
                if (constants.isEmpty()) {
                    log.error(tree.args.head.pos(), Errors.IntrinsicsIndyMustHaveConstantArg);
                } else {
                    Object bootstrapSpecifier = constants.head;
                    String invocationName = (String)constants.tail.head;
                    if (invocationName.isEmpty()) {
                        log.error(tree.args.tail.head.pos(), Errors.InvocationNameCannotBeEmpty);
                    }
                    Object mh = constables.invokeReflectiveMethod(constables.bootstrapSpecifierClass,
                            bootstrapSpecifier, "method");
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
                    tree.args = tree.args.tail.tail;
                    tree.args.forEach(arg -> arguments.add(arg.type));
                    Object[] bsmArgs = (Object[])constables.invokeReflectiveMethod(constables.bootstrapSpecifierClass, bootstrapSpecifier, "arguments");
                    Object[] convertedBsmArgs = constables.convertConstants(tree, attrEnv, bsmArgs, attrEnv.enclClass.sym.packge().modle, true);
                    MethodType mType = new MethodType(arguments.toList(), tree.type, List.nil(), syms.methodClass);
                    DynamicMethodSymbol dynSym = new DynamicMethodSymbol(
                            names.fromString(invocationName),
                            syms.noSymbol,
                            mHandle.refKind,
                            (MethodSymbol)mHandle.refSym,
                            mType,
                            convertedBsmArgs);
                    TreeInfo.setSymbol(tree.meth, dynSym);
                    tree.meth.type = mType;
                }
            }
        }
    }
}
