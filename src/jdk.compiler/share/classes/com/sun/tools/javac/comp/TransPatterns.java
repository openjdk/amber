/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol.DynamicVarSymbol;
import com.sun.tools.javac.code.Symbol.MethodHandleSymbol;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.BindingSymbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCConditional;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCIf;
import com.sun.tools.javac.tree.JCTree.JCInstanceOf;
import com.sun.tools.javac.tree.JCTree.JCLabeledStatement;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.JCBindingPattern;
import com.sun.tools.javac.tree.JCTree.JCWhileLoop;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Options;

import java.util.Map;
import java.util.Map.Entry;
import java.util.LinkedHashMap;

import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.MethodType;
import static com.sun.tools.javac.code.TypeTag.BOOLEAN;
import static com.sun.tools.javac.code.TypeTag.BOT;
import com.sun.tools.javac.jvm.ClassFile;
import com.sun.tools.javac.jvm.PoolConstant.LoadableConstant;
import com.sun.tools.javac.jvm.Target;
import com.sun.tools.javac.tree.JCTree.GenericSwitch.SwitchKind;
import com.sun.tools.javac.tree.JCTree.JCAssignOp;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCBreak;
import com.sun.tools.javac.tree.JCTree.JCCase;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCDeconstructionPattern;
import com.sun.tools.javac.tree.JCTree.JCDoWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCLiteralPattern;
import com.sun.tools.javac.tree.JCTree.JCPattern;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCSwitch;
import com.sun.tools.javac.tree.JCTree.LetExpr;
import static com.sun.tools.javac.tree.JCTree.Tag.SWITCH;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.List;

/**
 * This pass translates pattern-matching constructs, such as instanceof <pattern>.
 */
public class TransPatterns extends TreeTranslator {

    protected static final Context.Key<TransPatterns> transPatternsKey = new Context.Key<>();

    public static TransPatterns instance(Context context) {
        TransPatterns instance = context.get(transPatternsKey);
        if (instance == null)
            instance = new TransPatterns(context);
        return instance;
    }

    private final Symtab syms;
    private final Types types;
    private final Operators operators;
    private final Log log;
    private final ConstFold constFold;
    private final Names names;
    private final Resolve rs;
    private final Target target;
    private TreeMaker make;
    private Env<AttrContext> env;

    BindingContext bindingContext = new BindingContext() {
        @Override
        VarSymbol bindingDeclared(BindingSymbol varSymbol) {
            return null;
        }

        @Override
        VarSymbol getBindingFor(BindingSymbol varSymbol) {
            return null;
        }

        @Override
        JCStatement decorateStatement(JCStatement stat) {
            return stat;
        }

        @Override
        JCExpression decorateExpression(JCExpression expr) {
            return expr;
        }

        @Override
        BindingContext pop() {
            //do nothing
            return this;
        }

        @Override
        boolean tryPrepend(BindingSymbol binding, JCVariableDecl var) {
            return false;
        }
    };

    JCLabeledStatement pendingMatchLabel = null;

    boolean debugTransPatterns;

    private JCClassDecl currentClass;
    private Symbol currentOwnerSym = null;
    private List<JCTree> condyableMethods = List.nil();
    private MethodSymbol nullBootstrap; //hack: for ofConstant(null).
    private JCMethodDecl nullBootstrapTree;
    private MethodSymbol currentMethodSym = null;

    protected TransPatterns(Context context) {
        context.put(transPatternsKey, this);
        syms = Symtab.instance(context);
        make = TreeMaker.instance(context);
        types = Types.instance(context);
        operators = Operators.instance(context);
        log = Log.instance(context);
        constFold = ConstFold.instance(context);
        names = Names.instance(context);
        rs = Resolve.instance(context);
        target = Target.instance(context);
        debugTransPatterns = Options.instance(context).isSet("debug.patterns");
    }

    @Override
    public void visitTypeTest(JCInstanceOf tree) {
        if (tree.pattern.hasTag(Tag.BINDINGPATTERN) || tree.pattern.hasTag(Tag.DECONSTRUCTIONPATTERN) || tree.pattern.hasTag(Tag.LITERALPATTERN)) {
            //E instanceof T N
            //E instanceof T(PATT1, PATT2, ...)
            //=>
            //(let T' N$temp = E; N$temp instanceof T && (N = (T) N$temp == (T) N$temp))
            JCPattern patt = (JCPattern) tree.pattern;
            ListBuffer<JCStatement> statements = new ListBuffer<>();
            Type tempType = tree.expr.type.hasTag(BOT) ?
                    syms.objectType
                    : tree.expr.type;
            VarSymbol temp = new VarSymbol(Flags.SYNTHETIC,
                    names.fromString(target.syntheticNameChar() + "e" + target.syntheticNameChar()),
                    tempType,
                    currentOwnerSym);
            JCExpression translatedExpr = translate(tree.expr);
            statements.append(make.at(tree.pos).VarDef(temp, translatedExpr));
            ListBuffer<VarSymbol> bindingVars = new ListBuffer<>();
            Symbol.DynamicVarSymbol extractor = preparePatternExtractor(patt, tree.expr.type, bindingVars);
            JCIdent qualifier = make.Ident(patt.type.tsym);
            qualifier.sym = extractor;
            qualifier.type = extractor.type;
            VarSymbol e = new VarSymbol(0,
                    names.fromString("$e$" + tree.pos),
                    syms.patternHandleType,
                    currentOwnerSym);
            statements.add(make.VarDef(e, qualifier));

            VarSymbol tryMatch = new VarSymbol(0,
                    names.fromString("$tryMatch$" + tree.pos),
                    syms.methodHandleType,
                    currentOwnerSym);
            MethodSymbol tryMatchMethod = rs.resolveInternalMethod(patt.pos(), env, syms.patternHandleType, names.fromString("tryMatch"), List.nil(), List.nil());
            statements.append(make.VarDef(tryMatch, makeApply(make.Ident(e), tryMatchMethod, List.nil())));
            VarSymbol carrierMatch = new VarSymbol(0,
                    names.fromString("$carrier$" + tree.pos),
                    syms.objectType,
                    currentOwnerSym);
            MethodSymbol invokeMethodObject = rs.resolveInternalMethod(patt.pos(), env, syms.methodHandleType, names.fromString("invoke"), List.of(syms.objectType), List.nil());
            statements.append(make.VarDef(carrierMatch, makeApply(make.Ident(tryMatch), invokeMethodObject, List.of(translate(tree.expr)))));
            result = makeBinary(Tag.NE, make.Ident(carrierMatch), makeNull());

            int idx = 0;
            for (VarSymbol bindingVar : bindingVars) {
                if (bindingVar != syms.lengthVar) {
                    VarSymbol component = new VarSymbol(0,
                            names.fromString("$component$" + tree.pos + "$" + idx),
                            syms.methodHandleType,
                            currentOwnerSym);
                    MethodSymbol componentMethod = rs.resolveInternalMethod(patt.pos(), env, syms.patternHandleType, names.fromString("component"), List.of(syms.intType), List.nil());
                    statements.append(make.VarDef(component, makeApply(make.Ident(e), componentMethod, List.of(make.Literal(idx)))));
                    Type componentType = types.erasure(bindingVar.type.baseType());
                    JCTree oldNextTree = env.next.tree;
                    JCTree oldTree = env.tree;
                    MethodSymbol invokeMethodForComponent;
                    try {
                        env.next.tree = make.TypeCast(componentType, (JCExpression) (env.tree = make.Erroneous()));
                        invokeMethodForComponent = rs.resolveInternalMethod(patt.pos(), env, syms.methodHandleType, names.fromString("invoke"), List.of(syms.objectType), List.nil());
                    } finally {
                        env.next.tree = oldNextTree;
                        env.tree = oldTree;
                    }
                    Type castTargetType = bindingVar.erasure(types);
                    JCAssign bindingInit = (JCAssign)make.at(tree.pos).Assign(
                            make.Ident(bindingVar), convert(makeApply(make.Ident(component), invokeMethodForComponent, List.of(make.Ident(carrierMatch))), castTargetType)).setType(bindingVar.erasure(types));
                    JCExpression assignBoolExpr = make.at(tree.pos).LetExpr(List.of(make.Exec(bindingInit)), make.Literal(true)).setType(syms.booleanType);
                    result = makeBinary(Tag.AND, (JCExpression)result, assignBoolExpr);
                }
                idx++;
            }
            result = make.at(tree.pos).LetExpr(statements.toList(), (JCExpression)result).setType(syms.booleanType);
            ((LetExpr) result).needsCond = true;
        } else {
            super.visitTypeTest(tree);
        }
    }

    private Symbol.DynamicVarSymbol preparePatternExtractor(JCPattern patt, Type target, ListBuffer<VarSymbol> bindingVars) {
        if (target == syms.botType) {
            target = syms.objectType;
        }
        if (patt.hasTag(Tag.BINDINGPATTERN)) {
            Type tempType = patt.type.hasTag(BOT) ?
                    syms.objectType
                    : patt.type;
            List<Type> bsm_staticArgs = List.of(new ClassType(syms.classType.getEnclosingType(),
                                                              List.of(tempType),
                                                              syms.classType.tsym));

            if (!tempType.isPrimitive()) {
                bsm_staticArgs = bsm_staticArgs.append(new ClassType(syms.classType.getEnclosingType(),
                                                                     List.of(target),
                                                                     syms.classType.tsym));
            }

            MethodSymbol ofType = rs.resolveInternalMethod(patt.pos(), env, syms.patternHandlesType,
                    names.fromString("ofType"), bsm_staticArgs, List.nil());

            VarSymbol binding = bindingContext.bindingDeclared(((JCBindingPattern) patt).symbol);

            if (binding != null) {
                bindingVars.append(binding);
            }

            if (tempType.isPrimitive()) {
                return makeCondyable(patt.pos(), ofType, new LoadableConstant[] {loadPrimitiveClass(patt.pos(), tempType)});
            } else {
                return makeCondyable(patt.pos(), ofType, new LoadableConstant[] {(ClassType) tempType, (ClassType) target});
            }
        } else if (patt.hasTag(Tag.DECONSTRUCTIONPATTERN)) {
            //type test already done, finish handling of deconstruction patterns ("T(PATT1, PATT2, ...)")
            //=>
            //<PATT1-handling> && <PATT2-handling> && ...
            JCDeconstructionPattern dpatt = (JCDeconstructionPattern) patt;
            Type tempType = patt.type.hasTag(BOT) ?
                    syms.objectType
                    : patt.type;
            Type indyType = syms.objectType;
            List<Type> bsm_staticArgs = List.of(syms.methodHandleLookupType,
                                                syms.stringType,
                                                new ClassType(syms.classType.getEnclosingType(),
                                                              List.of(syms.patternHandleType),
                                                              syms.classType.tsym),
                                                new ClassType(syms.classType.getEnclosingType(),
                                                              List.of(tempType),
                                                              syms.classType.tsym),
                                                syms.methodTypeType,
                                                syms.stringType,
                                                syms.intType);

            Symbol ofType = rs.resolveInternalMethod(patt.pos(), env, syms.patternHandlesType,
                    names.fromString("ofNamed"), bsm_staticArgs, List.nil());

            Symbol.DynamicVarSymbol outter = new Symbol.DynamicVarSymbol(names.fromString("ofNamed"),
                    syms.noSymbol,
                    new Symbol.MethodHandleSymbol(ofType),
                    indyType,
                    new LoadableConstant[] {(ClassType) tempType,
                                            new MethodType(dpatt.innerTypes, syms.voidType, List.nil(), syms.methodClass),
                                            LoadableConstant.String(dpatt.extractorResolver.name.toString()),
                                            LoadableConstant.Int(ClassFile.REF_newInvokeSpecial)});

            DynamicVarSymbol[] params = new DynamicVarSymbol[((JCDeconstructionPattern) patt).getNestedPatterns().size() + 1];
            params[0] = outter;
            @SuppressWarnings({"rawtypes", "unchecked"})
            ListBuffer<VarSymbol>[] nestedBindings = new ListBuffer[((JCDeconstructionPattern) patt).getNestedPatterns().size()];

            for (int i = 0; i < ((JCDeconstructionPattern) patt).getNestedPatterns().size(); i++) {
                JCPattern nested = ((JCDeconstructionPattern) patt).getNestedPatterns().get(i);
                params[i + 1] = preparePatternExtractor(nested, nested.type, nestedBindings[i] = new ListBuffer<>());
                if (nested.hasTag(Tag.BINDINGPATTERN)) {
                    bindingVars.appendList(nestedBindings[i].toList());
                    nestedBindings[i].clear();
                } else {
                    bindingVars.append(syms.lengthVar);
                }
            }

            for (ListBuffer<VarSymbol> nested : nestedBindings) {
                if (nested.isEmpty())
                    continue;
                bindingVars.appendList(nested.toList());
            }

            List<Type> bsm_staticArgsNested = List.of(syms.patternHandleType,
                                                      types.makeArrayType(syms.patternHandleType));

            MethodSymbol ofNested = rs.resolveInternalMethod(patt.pos(), env, syms.patternHandlesType,
                    names.fromString("nested"), bsm_staticArgsNested, List.nil());

            return makeCondyable(patt.pos(), ofNested, params);
        } else if (patt.hasTag(Tag.ANYPATTERN)) {
            Type tempType = patt.type.hasTag(BOT) ?
                    syms.objectType
                    : patt.type;
            List<Type> bsm_staticArgs = List.of(new ClassType(syms.classType.getEnclosingType(),
                                                              List.of(tempType),
                                                              syms.classType.tsym));

            if (!tempType.isPrimitive()) {
                bsm_staticArgs = bsm_staticArgs.append(new ClassType(syms.classType.getEnclosingType(),
                                                                     List.of(target),
                                                                     syms.classType.tsym));
            }

            MethodSymbol ofType = rs.resolveInternalMethod(patt.pos(), env, syms.patternHandlesType,
                    names.fromString("ofType"), bsm_staticArgs, List.nil());

            if (tempType.isPrimitive()) {
                return makeCondyable(patt.pos(), ofType, new LoadableConstant[] {loadPrimitiveClass(patt.pos(), tempType)});
            } else {
                return makeCondyable(patt.pos(), ofType, new LoadableConstant[] {(ClassType) tempType, (ClassType) target});
            }
        } else if (patt.hasTag(Tag.LITERALPATTERN)) {
            JCLiteralPattern lpatt = (JCLiteralPattern) patt;
            boolean adapt = types.boxedTypeOrType(target) == target;
            List<Type> bsm_staticArgs = List.of(syms.objectType);

            if (adapt) {
                bsm_staticArgs = bsm_staticArgs.append(new ClassType(syms.classType.getEnclosingType(),
                                                                     List.of(target),
                                                                     syms.classType.tsym));
            }

            MethodSymbol ofConstant = rs.resolveInternalMethod(patt.pos(), env, syms.patternHandlesType,
                    names.fromString("ofConstant"), bsm_staticArgs, List.nil());

            LoadableConstant lc;
            Object constValue = lpatt.value.type.constValue();
            if (constValue instanceof Double) {
                lc = LoadableConstant.Double((Double) constValue);
            } else if (constValue instanceof Float) {
                lc = LoadableConstant.Float((Float) constValue);
            } else  if (constValue instanceof Integer) {
                lc = LoadableConstant.Int((Integer) constValue);
            } else if (constValue instanceof Long) {
                lc = LoadableConstant.Long((Long) constValue);
            } else if (constValue instanceof String) {
                lc = LoadableConstant.String((String) constValue);
            } else if (constValue == null) {
                lc = nullBootstrap();
            } else {
                Assert.error(constValue.getClass().getName());
                throw new InternalError();
            }

            if (adapt) {
                return makeCondyable(patt.pos(), ofConstant, new LoadableConstant[] {lc, (ClassType) target});
            } else {
                return makeCondyable(patt.pos(), ofConstant, new LoadableConstant[] {lc});
            }
        } else {
            throw new IllegalStateException();
        }
    }

    private Symbol.DynamicVarSymbol makeCondyable(DiagnosticPosition pos, MethodSymbol targetMethod, LoadableConstant[] parameters) {
        Assert.checkNonNull(currentClass);

        List<Type> bsm_staticArgs = List.of(syms.methodHandleLookupType,
                                            syms.stringType,
                                            new ClassType(syms.classType.getEnclosingType(),
                                                          List.of(syms.patternHandlesType),
                                                          syms.classType.tsym));
        bsm_staticArgs = bsm_staticArgs.appendList(targetMethod.type.getParameterTypes());

        MethodType indyType = new MethodType(bsm_staticArgs, targetMethod.type.getReturnType(), List.nil(), syms.methodClass);

        MethodSymbol condyable = new MethodSymbol(Flags.STATIC | Flags.SYNTHETIC, names.fromString("$condyable$" + pos.getPreferredPosition()), indyType, currentClass.sym);

        if ((targetMethod.flags() & Flags.VARARGS) != 0) {
            condyable.flags_field |= Flags.VARARGS;
        }

        currentClass.sym.members().enter(condyable);

        condyableMethods = condyableMethods.prepend(
                make.MethodDef(condyable,
                               condyable.externalType(types),
                               make.Block(0, List.of(make.Return(make.Apply(List.nil(), make.QualIdent(targetMethod), condyable.params().stream().skip(3).map(p -> make.Ident(p)).collect(List.collector())).setType(syms.patternHandleType))))));

        return new Symbol.DynamicVarSymbol(condyable.name,
                                           syms.noSymbol,
                                           new MethodHandleSymbol(condyable),
                                           targetMethod.type.getReturnType(),
                                           parameters);
    }

    private Symbol.DynamicVarSymbol loadPrimitiveClass(DiagnosticPosition pos, Type primitive) {
        Assert.checkNonNull(currentClass);
        final Type.ClassType primitiveClass = new ClassType(syms.classType.getEnclosingType(),
                List.of(primitive),
                syms.classType.tsym);

        List<Type> bsm_staticArgs = List.of(syms.methodHandleLookupType,
                                            syms.stringType, primitiveClass);
        VarSymbol TYPE = rs.resolveInternalField(pos, env, types.boxedTypeOrType(primitive),
                names.fromString("TYPE"));
        MethodType indyType = new MethodType(bsm_staticArgs, primitiveClass, List.nil(), syms.methodClass);

        MethodSymbol loadPrimitiveClass = new MethodSymbol(Flags.STATIC | Flags.SYNTHETIC, names.fromString("$primitiveClass$" + pos.getPreferredPosition()), indyType, currentClass.sym);

        currentClass.sym.members().enter(loadPrimitiveClass);

        condyableMethods = condyableMethods.prepend(
                make.MethodDef(loadPrimitiveClass,
                               loadPrimitiveClass.externalType(types),
                               make.Block(0, List.of(make.Return(make.QualIdent(TYPE))))));

        return new Symbol.DynamicVarSymbol(loadPrimitiveClass.name,
                                           syms.noSymbol,
                                           new MethodHandleSymbol(loadPrimitiveClass),
                                           primitiveClass,
                                           new LoadableConstant[] {});
    }

    private Symbol.DynamicVarSymbol nullBootstrap() {
        Assert.checkNonNull(currentClass);

        if (nullBootstrap == null) {
            List<Type> bsm_staticArgs = List.of(syms.methodHandleLookupType,
                                                syms.stringType,
                                                new ClassType(syms.classType.getEnclosingType(),
                                                              List.of(syms.objectType),
                                                              syms.classType.tsym));

            MethodType indyType = new MethodType(bsm_staticArgs, syms.objectType, List.nil(), syms.methodClass);

            nullBootstrap = new MethodSymbol(Flags.STATIC | Flags.SYNTHETIC, names.fromString("$null$bootstrap"), indyType, currentClass.sym);

            currentClass.sym.members().enter(nullBootstrap);

            nullBootstrapTree = make.MethodDef(nullBootstrap,
                                               nullBootstrap.externalType(types),
                                               make.Block(0, List.of(make.Return(make.Literal(BOT, null).setType(syms.botType)))));
        }

        return new Symbol.DynamicVarSymbol(nullBootstrap.name,
                                           syms.noSymbol,
                                           new MethodHandleSymbol(nullBootstrap),
                                           syms.objectType,
                                           new LoadableConstant[0]);
    }

    private JCExpression makeApply(JCExpression site, Symbol method, List<JCExpression> params) {
        JCFieldAccess acc = make.Select(site, method.name);
        acc.sym = method;
        acc.type = method.type;
        return make.Apply(List.nil(), acc, params).setType(acc.type.getReturnType());
    }

    //from Lower:
    /** Make an attributed tree representing a literal. This will be an
     *  Ident node in the case of boolean literals, a Literal node in all
     *  other cases.
     *  @param type       The literal's type.
     *  @param value      The literal's value.
     */
    JCExpression makeLit(Type type, Object value) {
        return make.Literal(type.getTag(), value).setType(type.constType(value));
    }

    /** Make an attributed tree representing null.
     */
    JCExpression makeNull() {
        return makeLit(syms.botType, null);
    }

    /** Make an attributed assignop expression.
     *  @param optag    The operators tree tag.
     *  @param lhs      The operator's left argument.
     *  @param rhs      The operator's right argument.
     */
    JCAssignOp makeAssignop(JCTree.Tag optag, JCTree lhs, JCTree rhs) {
        JCAssignOp tree = make.Assignop(optag, lhs, rhs);
        tree.operator = operators.resolveBinary(tree, tree.getTag().noAssignOp(), lhs.type, rhs.type);
        tree.type = lhs.type;
        return tree;
    }

//    JCNewArray makeArray(Type type, JCExpression... elements) {
//        JCNewArray newArray = make.NewArray(make.Type(types.erasure(type)),
//                                          List.nil(),
//                                          List.from(elements));
//        newArray.type = types.makeArrayType(newArray.elemtype.type);
//        return newArray;
//    }

    @Override
    public void visitBinary(JCBinary tree) {
        bindingContext = new BasicBindingContext();
        try {
            super.visitBinary(tree);
            result = bindingContext.decorateExpression(tree);
        } finally {
            bindingContext.pop();
        }
    }

    @Override
    public void visitBreak(JCBreak tree) {
        if (tree.target.hasTag(SWITCH) && ((JCSwitch) tree.target).kind == SwitchKind.MATCHING) {
            Assert.checkNonNull(pendingMatchLabel);
            tree.target = pendingMatchLabel;
            result = tree;
        } else {
            super.visitBreak(tree);
        }
    }

    @Override
    public void visitConditional(JCConditional tree) {
        bindingContext = new BasicBindingContext();
        try {
            super.visitConditional(tree);
            result = bindingContext.decorateExpression(tree);
        } finally {
            bindingContext.pop();
        }
    }

    @Override
    public void visitIf(JCIf tree) {
        bindingContext = new BasicBindingContext();
        try {
            super.visitIf(tree);
            result = bindingContext.decorateStatement(tree);
        } finally {
            bindingContext.pop();
        }
    }

    @Override
    public void visitForLoop(JCForLoop tree) {
        bindingContext = new BasicBindingContext();
        try {
            super.visitForLoop(tree);
            result = bindingContext.decorateStatement(tree);
        } finally {
            bindingContext.pop();
        }
    }

    @Override
    public void visitWhileLoop(JCWhileLoop tree) {
        bindingContext = new BasicBindingContext();
        try {
            super.visitWhileLoop(tree);
            result = bindingContext.decorateStatement(tree);
        } finally {
            bindingContext.pop();
        }
    }

    @Override
    public void visitDoLoop(JCDoWhileLoop tree) {
        bindingContext = new BasicBindingContext();
        try {
            super.visitDoLoop(tree);
            result = bindingContext.decorateStatement(tree);
        } finally {
            bindingContext.pop();
        }
    }

    @Override
    public void visitSwitch(JCSwitch tree) {
        if (tree.kind == SwitchKind.MATCHING) {
            JCLabeledStatement prevMatchLabel = pendingMatchLabel;
            try {
                pendingMatchLabel = make.Labelled(names.fromString("match$" + tree.pos), null);
                VarSymbol fallthroughSym = new VarSymbol(0, names.fromString("fallthrough$" + tree.pos), syms.booleanType, currentMethodSym);

                boolean hasFallThrough = false;
                boolean wasFallThrough = false;

                List<JCStatement> resultStatements = List.nil();

                for (JCCase clause : tree.cases) {
                    Assert.check(clause.pats.size() <= 1);
                    final JCExpression jcMatches = clause.pats.nonEmpty() ? make.TypeTest(tree.selector, clause.pats.head) : make.Literal(BOOLEAN, 1);
                    jcMatches.setType(syms.booleanType);
                    JCStatement body;
                    List<JCStatement> stats = clause.stats;
                    if (clause.completesNormally) {
                        stats = stats.append(make.at(tree.pos).Exec(make.Assign(make.Ident(fallthroughSym), make.Literal(BOOLEAN, 1).setType(syms.booleanType)).setType(syms.booleanType)));
                    }
                    body = make.Block(0, stats);
                    JCStatement translatedIf = translate(make.If(jcMatches, body, null));
                    JCIf testStatement = translatedIf.hasTag(Tag.IF) ? (JCIf)translatedIf : (JCIf) ((JCBlock)translatedIf).stats.tail.head;

                    if (wasFallThrough) {
                        testStatement.cond = makeBinary(Tag.OR,
                                make.Ident(fallthroughSym),
                                testStatement.cond);
                    }

                    hasFallThrough |= wasFallThrough = clause.completesNormally;
                    resultStatements = resultStatements.append(translatedIf);
                }
                if (hasFallThrough) {
                    resultStatements = resultStatements.prepend(make.at(tree.pos)
                                                                    .VarDef(fallthroughSym,
                                                                            make.Literal(BOOLEAN, 0).setType(syms.booleanType)));
                }
                pendingMatchLabel.body = make.Block(0, resultStatements);
                result = pendingMatchLabel;
            } finally {
                pendingMatchLabel = prevMatchLabel;
            }
        } else {
            super.visitSwitch(tree);
        }
    }

    @Override
    public void visitClassDef(JCTree.JCClassDecl tree) {
        JCClassDecl prevCurrentClass = currentClass;
        List<JCTree> prevCondyableMethods = condyableMethods;
        MethodSymbol prevNullBootstrap = nullBootstrap;
        JCMethodDecl prevNullBootstrapTree = nullBootstrapTree;
        try {
            currentClass = tree;
            condyableMethods = List.nil();
            nullBootstrap = null;
            nullBootstrapTree = null;
            super.visitClassDef(tree);
        } finally {
            currentClass.defs = currentClass.defs.prependList(condyableMethods);
            if (nullBootstrapTree != null) {
                currentClass.defs = currentClass.defs.prepend(nullBootstrapTree);
            }
            currentClass = prevCurrentClass;
            condyableMethods = prevCondyableMethods;
            nullBootstrap = prevNullBootstrap;
            nullBootstrapTree = prevNullBootstrapTree;
        }
    }

    @Override
    public void visitMethodDef(JCMethodDecl tree) {
        MethodSymbol prevMethodSym = currentMethodSym;
        try {
            currentMethodSym = tree.sym;
            super.visitMethodDef(tree);
        } finally {
            currentMethodSym = prevMethodSym;
        }
    }

    @Override
    public void visitIdent(JCIdent tree) {
        VarSymbol bindingVar = null;
        if ((tree.sym.flags() & Flags.MATCH_BINDING) != 0) {
            bindingVar = bindingContext.getBindingFor((BindingSymbol)tree.sym);
        }
        if (bindingVar == null) {
            super.visitIdent(tree);
        } else {
            result = make.at(tree.pos).Ident(bindingVar);
        }
    }

    @Override
    public void visitBlock(JCBlock tree) {
        ListBuffer<JCStatement> statements = new ListBuffer<>();
        bindingContext = new BindingDeclarationFenceBindingContext() {
            boolean tryPrepend(BindingSymbol binding, JCVariableDecl var) {
                //{
                //    if (E instanceof T N) {
                //        return ;
                //    }
                //    //use of N:
                //}
                //=>
                //{
                //    T N;
                //    if ((let T' N$temp = E; N$temp instanceof T && (N = (T) N$temp == (T) N$temp))) {
                //        return ;
                //    }
                //    //use of N:
                //}
                hoistedVarMap.put(binding, var.sym);
                statements.append(var);
                return true;
            }
        };
        Symbol prevOwnerSym = currentOwnerSym;
        try {
            if (currentOwnerSym == null) {
                currentOwnerSym = new MethodSymbol(tree.flags | Flags.BLOCK,
                                 names.empty, null,
                                 currentClass.sym);
            }
            for (List<JCStatement> l = tree.stats; l.nonEmpty(); l = l.tail) {
                statements.append(translate(l.head));
            }

            tree.stats = statements.toList();
            result = tree;
        } finally {
            bindingContext.pop();
            currentOwnerSym = prevOwnerSym;
        }
    }

    @Override
    public void visitLambda(JCLambda tree) {
        BindingContext prevContent = bindingContext;
        try {
            bindingContext = new BindingDeclarationFenceBindingContext();
            super.visitLambda(tree);
        } finally {
            bindingContext = prevContent;
        }
    }

    public JCTree translateTopLevelClass(Env<AttrContext> env, JCTree cdef, TreeMaker make) {
        try {
            this.make = make;
            this.env = env;
            translate(cdef);
        } finally {
            // note that recursive invocations of this method fail hard
            this.make = null;
            this.env = null;
        }

        return cdef;
    }

    /** Make an instanceof expression.
     *  @param lhs      The expression.
     *  @param type     The type to be tested.
     */

    JCInstanceOf makeTypeTest(JCExpression lhs, JCExpression type) {
        JCInstanceOf tree = make.TypeTest(lhs, type);
        tree.type = syms.booleanType;
        return tree;
    }

    /** Make an attributed binary expression (copied from Lower).
     *  @param optag    The operators tree tag.
     *  @param lhs      The operator's left argument.
     *  @param rhs      The operator's right argument.
     */
    JCBinary makeBinary(JCTree.Tag optag, JCExpression lhs, JCExpression rhs) {
        JCBinary tree = make.Binary(optag, lhs, rhs);
        tree.operator = operators.resolveBinary(tree, optag, lhs.type, rhs.type);
        tree.type = tree.operator.type.getReturnType();
        return tree;
    }

    JCExpression convert(JCExpression expr, Type target) {
        JCExpression result = make.at(expr.pos()).TypeCast(make.Type(target), expr);
        result.type = target;
        return result;
    }

    abstract class BindingContext {
        abstract VarSymbol bindingDeclared(BindingSymbol varSymbol);
        abstract VarSymbol getBindingFor(BindingSymbol varSymbol);
        abstract JCStatement decorateStatement(JCStatement stat);
        abstract JCExpression decorateExpression(JCExpression expr);
        abstract BindingContext pop();
        abstract boolean tryPrepend(BindingSymbol binding, JCVariableDecl var);
    }

    class BasicBindingContext extends BindingContext {
        Map<BindingSymbol, VarSymbol> hoistedVarMap;
        BindingContext parent;

        public BasicBindingContext() {
            this.parent = bindingContext;
            this.hoistedVarMap = new LinkedHashMap<>();
        }

        @Override
        VarSymbol bindingDeclared(BindingSymbol varSymbol) {
            VarSymbol res = parent.bindingDeclared(varSymbol);
            if (res == null) {
                res = new VarSymbol(varSymbol.flags(), varSymbol.name, varSymbol.type, varSymbol.owner);
                res.setTypeAttributes(varSymbol.getRawTypeAttributes());
                hoistedVarMap.put(varSymbol, res);
            }
            return res;
        }

        @Override
        VarSymbol getBindingFor(BindingSymbol varSymbol) {
            VarSymbol res = parent.getBindingFor(varSymbol);
            if (res != null) {
                return res;
            }
            return hoistedVarMap.entrySet().stream()
                    .filter(e -> e.getKey().isAliasFor(varSymbol))
                    .findFirst()
                    .map(e -> e.getValue()).orElse(null);
        }

        @Override
        JCStatement decorateStatement(JCStatement stat) {
            if (hoistedVarMap.isEmpty()) return stat;
            //if (E instanceof T N) {
            //     //use N
            //}
            //=>
            //{
            //    T N;
            //    if ((let T' N$temp = E; N$temp instanceof T && (N = (T) N$temp == (T) N$temp))) {
            //        //use N
            //    }
            //}
            ListBuffer<JCStatement> stats = new ListBuffer<>();
            for (Entry<BindingSymbol, VarSymbol> e : hoistedVarMap.entrySet()) {
                JCVariableDecl decl = makeHoistedVarDecl(stat.pos, e.getValue());
                if (!e.getKey().isPreserved() ||
                    !parent.tryPrepend(e.getKey(), decl)) {
                    stats.add(decl);
                }
            }
            if (stats.nonEmpty()) {
                stats.add(stat);
                stat = make.at(stat.pos).Block(0, stats.toList());
            }
            return stat;
        }

        @Override
        JCExpression decorateExpression(JCExpression expr) {
            //E instanceof T N && /*use of N*/
            //=>
            //(let T N; (let T' N$temp = E; N$temp instanceof T && (N = (T) N$temp == (T) N$temp)) && /*use of N*/)
            for (VarSymbol vsym : hoistedVarMap.values()) {
                expr = make.at(expr.pos).LetExpr(makeHoistedVarDecl(expr.pos, vsym), expr).setType(expr.type);
            }
            return expr;
        }

        @Override
        BindingContext pop() {
            return bindingContext = parent;
        }

        @Override
        boolean tryPrepend(BindingSymbol binding, JCVariableDecl var) {
            return false;
        }

        private JCVariableDecl makeHoistedVarDecl(int pos, VarSymbol varSymbol) {
            return make.at(pos).VarDef(varSymbol, null);
        }
    }

    private class BindingDeclarationFenceBindingContext extends BasicBindingContext {

        @Override
        VarSymbol bindingDeclared(BindingSymbol varSymbol) {
            return null;
        }

    }
}
