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
import com.sun.tools.javac.code.Kinds.Kind;
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
import com.sun.tools.javac.code.Symbol.RecordComponent;
import com.sun.tools.javac.code.Type;
import static com.sun.tools.javac.code.TypeTag.BOT;
import com.sun.tools.javac.jvm.Target;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCArrayPattern;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCDeconstructionPattern;
import com.sun.tools.javac.tree.JCTree.JCDoWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCPattern;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.LetExpr;
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
    private final Target target;
    private TreeMaker make;

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

    private ClassSymbol currentClass;
    private Symbol currentOwnerSym = null;

    protected TransPatterns(Context context) {
        context.put(transPatternsKey, this);
        syms = Symtab.instance(context);
        make = TreeMaker.instance(context);
        types = Types.instance(context);
        operators = Operators.instance(context);
        log = Log.instance(context);
        constFold = ConstFold.instance(context);
        names = Names.instance(context);
        target = Target.instance(context);
        debugTransPatterns = Options.instance(context).isSet("debug.patterns");
    }

    @Override
    public void visitTypeTest(JCInstanceOf tree) {
        if (tree.pattern.hasTag(Tag.BINDINGPATTERN) || tree.pattern.hasTag(Tag.DECONSTRUCTIONPATTERN) || tree.pattern.hasTag(Tag.ARRAYPATTERN)) {
            //E instanceof T N
            //E instanceof T(PATT1, PATT2, ...)
            //=>
            //(let T' N$temp = E; N$temp instanceof T && <pattern extractor>)
            Type tempType = tree.expr.type.hasTag(BOT) ?
                    syms.objectType
                    : tree.expr.type;
            VarSymbol temp = new VarSymbol(Flags.SYNTHETIC,
                    names.fromString(target.syntheticNameChar() + "e" + target.syntheticNameChar()),
                    tempType,
                    currentOwnerSym);
            JCExpression translatedExpr = translate(tree.expr);
            Type castTargetType;
            //TODO: use rule switch (when boot JDK is 14):
            switch (tree.pattern.getTag()) {
                case BINDINGPATTERN:
                    castTargetType = ((JCBindingPattern)tree.pattern).var.sym.type;
                    break;
                case DECONSTRUCTIONPATTERN:
                    castTargetType = ((JCDeconstructionPattern)tree.pattern).type;
                    break;
                case ARRAYPATTERN:
                    castTargetType = ((JCArrayPattern)tree.pattern).type;
                    break;
                default:
                    throw new AssertionError("Unexpected pattern type: " + tree.pattern.getTag());
            }

            castTargetType = types.boxedTypeOrType(types.erasure(castTargetType));

            result = makeTypeTest(make.at(tree.pos).Ident(temp), make.Type(castTargetType));
            result = makeBinary(Tag.AND, (JCExpression)result,
                                         preparePatternExtractor(tree, tree.getPattern(), temp, castTargetType));
            result = make.at(tree.pos).LetExpr(make.VarDef(temp, translatedExpr),
                                               (JCExpression)result).setType(syms.booleanType);
            ((LetExpr) result).needsCond = true;
        } else {
            super.visitTypeTest(tree);
        }
    }

    private JCExpression preparePatternExtractor(JCInstanceOf tree, JCPattern patt, VarSymbol temp, Type targetType) {
        if (targetType == syms.botType) {
            targetType = syms.objectType;
        }
        if (patt.hasTag(Tag.BINDINGPATTERN)) {
            //type test already done, finish handling of type test patterns ("T N")
            //=>
            //(let N = (T) N$temp; true)
            VarSymbol bindingVar = bindingContext.bindingDeclared((BindingSymbol) ((JCBindingPattern) patt).var.sym);
            if (bindingVar != null) { //TODO: cannot be null here?
                JCAssign fakeInit = (JCAssign)make.at(tree.pos).Assign(
                        make.Ident(bindingVar), convert(make.Ident(temp), targetType)).setType(bindingVar.erasure(types));
                LetExpr nestedLE = make.LetExpr(List.of(make.Exec(fakeInit)),
                                                make.Literal(true));
                nestedLE.needsCond = true;
                nestedLE.setType(syms.booleanType);
                return nestedLE;
            }
            return make.Literal(true);
        } else if (patt.hasTag(Tag.DECONSTRUCTIONPATTERN)) {
            //type test already done, finish handling of deconstruction patterns ("T(PATT1, PATT2, ...)")
            //=>
            //<PATT1-handling> && <PATT2-handling> && ...
            JCDeconstructionPattern dpatt = (JCDeconstructionPattern) patt;
            List<? extends RecordComponent> components = dpatt.record.getRecordComponents();
            List<? extends JCPattern> nestedPatterns = dpatt.nested;
            JCExpression test = null;
            while (components.nonEmpty() && nestedPatterns.nonEmpty()) {
                //PATTn for record component COMPn of type Tn;
                //PATTn is a type test pattern or a deconstruction pattern:
                //=>
                //(let Tn $c$COMPn = ((T) N$temp).COMPn(); <PATTn extractor>)
                //or
                //(let Tn $c$COMPn = ((T) N$temp).COMPn(); $c$COMPn != null && <PATTn extractor>)
                //or
                //(let Tn $c$COMPn = ((T) N$temp).COMPn(); $c$COMPn instanceof T' && <PATTn extractor>)
                RecordComponent component = components.head;
                JCPattern nested = nestedPatterns.head;
                VarSymbol nestedTemp = new VarSymbol(Flags.SYNTHETIC,
                    names.fromString(target.syntheticNameChar() + "c" + target.syntheticNameChar() + component.name),
                                     component.erasure(types),
                                     currentOwnerSym);
                Symbol accessor = dpatt.record
                                       .members()
                                       .findFirst(component.name, s -> s.kind == Kind.MTH &&
                                                                       ((MethodSymbol) s).params.isEmpty());
                JCVariableDecl nestedTempVar =
                        make.VarDef(nestedTemp,
                                    make.App(make.Select(convert(make.Ident(temp), dpatt.type),
                                                         accessor)));
                JCExpression extracted =
                        preparePatternExtractor(tree, nested, nestedTemp, nested.type);
                JCExpression extraTest = null;
                if (!types.isAssignable(nestedTemp.type, nested.type)) {
                    extraTest = makeTypeTest(make.Ident(nestedTemp),
                                             make.Type(nested.type));
                } else if (nested.type.isReference()) {
                    extraTest = makeBinary(Tag.NE, make.Ident(nestedTemp), makeNull());
                }
                if (extraTest != null) {
                    extracted = makeBinary(Tag.AND, extraTest, extracted);
                }
                LetExpr getAndRun = make.LetExpr(nestedTempVar, extracted);
                getAndRun.needsCond = true;
                getAndRun.setType(syms.booleanType);
                if (test == null) {
                    test = getAndRun;
                } else {
                    test = makeBinary(Tag.AND, test, getAndRun);
                }
                components = components.tail;
                nestedPatterns = nestedPatterns.tail;
            }
            Assert.check(components.isEmpty() == nestedPatterns.isEmpty());
            return test != null ? test : make.Literal(true);
        } else if (patt.hasTag(Tag.ARRAYPATTERN)) {
            //XXX: type test already done, finish handling of deconstruction patterns ("T(PATT1, PATT2, ...)")
            //=>
            //<PATT1-handling> && <PATT2-handling> && ...
            JCArrayPattern apatt = (JCArrayPattern) patt;
            Type elementType = types.elemtype(apatt.type);
            List<? extends JCPattern> nestedPatterns = apatt.nested;
            JCExpression test = makeBinary(apatt.orMore ? Tag.GE : Tag.EQ,
                                    make.Select(convert(make.Ident(temp), apatt.type), syms.lengthVar),
                                    make.Literal(nestedPatterns.size()));

            int i = 0;
            while (nestedPatterns.nonEmpty()) {
                //PATTn for record component COMPn of type Tn;
                //PATTn is a type test pattern or a deconstruction pattern:
                //=>
                //(let Tn $c$COMPn = ((T) N$temp).COMPn(); <PATTn extractor>)
                //or
                //(let Tn $c$COMPn = ((T) N$temp).COMPn(); $c$COMPn != null && <PATTn extractor>)
                //or
                //(let Tn $c$COMPn = ((T) N$temp).COMPn(); $c$COMPn instanceof T' && <PATTn extractor>)
                JCPattern nested = nestedPatterns.head;
                VarSymbol nestedTemp = new VarSymbol(Flags.SYNTHETIC,
                    names.fromString(target.syntheticNameChar() + "c" + target.syntheticNameChar() + i),
                                     types.erasure(elementType),
                                     currentOwnerSym);
                JCVariableDecl nestedTempVar =
                        make.VarDef(nestedTemp,
                                    make.Indexed(convert(make.Ident(temp), apatt.type), make.Literal(i)).setType(elementType));
                JCExpression extracted =
                        preparePatternExtractor(tree, nested, nestedTemp, nested.type);
                JCExpression extraTest = null;
                if (!types.isAssignable(nestedTemp.type, nested.type)) {
                    extraTest = makeTypeTest(make.Ident(nestedTemp),
                                             make.Type(nested.type));
                } else if (nested.type.isReference()) {
                    extraTest = makeBinary(Tag.NE, make.Ident(nestedTemp), makeNull());
                }
                if (extraTest != null) {
                    extracted = makeBinary(Tag.AND, extraTest, extracted);
                }
                LetExpr getAndRun = make.LetExpr(nestedTempVar, extracted);
                getAndRun.needsCond = true;
                getAndRun.setType(syms.booleanType);
                test = makeBinary(Tag.AND, test, getAndRun);
                i++;
                nestedPatterns = nestedPatterns.tail;
            }
            return test;
        } else {
            throw new IllegalStateException();
        }
    }

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
    public void visitMethodDef(JCMethodDecl tree) {
        Symbol prevOwnerSym = currentOwnerSym;
        try {
            currentOwnerSym = tree.sym;
            super.visitMethodDef(tree);
        } finally {
            currentOwnerSym = prevOwnerSym;
        }
    }

    @Override
    public void visitVarDef(JCVariableDecl tree) {
        Symbol prevOwnerSym = currentOwnerSym;
        try {
            if (tree.sym.owner.kind == Kind.TYP) {
                currentOwnerSym = tree.sym;
            }
            super.visitVarDef(tree);
        } finally {
            currentOwnerSym = prevOwnerSym;
        }
    }

    @Override
    public void visitClassDef(JCClassDecl tree) {
        ClassSymbol prevCurrentClass = currentClass;
        try {
            currentClass = tree.sym;
            super.visitClassDef(tree);
        } finally {
            currentClass = prevCurrentClass;
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
                                 currentClass);
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
            translate(cdef);
        } finally {
            // note that recursive invocations of this method fail hard
            this.make = null;
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
