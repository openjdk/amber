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
package com.sun.tools.javac.comp;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.jvm.Target;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.GenericSwitch;
import com.sun.tools.javac.tree.JCTree.GenericSwitch.SwitchKind;
import com.sun.tools.javac.tree.JCTree.JCBreak;
import com.sun.tools.javac.tree.JCTree.JCCase;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCPattern;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCSwitch;
import com.sun.tools.javac.tree.JCTree.JCSwitchExpression;
import com.sun.tools.javac.tree.JCTree.JCThrow;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;

/**
 * This pass translates JDK12 switch constructs, like cases with multiple patterns, rule switches
 * and switch expressions.
 */
public class TransSwitches extends TreeTranslator {

    protected static final Context.Key<TransSwitches> transSwitchesKey = new Context.Key<>();

    public static TransSwitches instance(Context context) {
        TransSwitches instance = context.get(transSwitchesKey);
        if (instance == null)
            instance = new TransSwitches(context);
        return instance;
    }

    private Symtab syms;
    private Resolve rs;
    private Names names;
    private TypeEnvs typeEnvs;
    private Target target;

    /** The current method symbol.
     */
    private MethodSymbol currentMethodSym;

    /** Environment for symbol lookup, set by translateTopLevelClass.
     */
    private Env<AttrContext> attrEnv;

    private TreeMaker make;

    public TransSwitches(Context context) {
        context.put(transSwitchesKey, this);
        syms = Symtab.instance(context);
        rs = Resolve.instance(context);
        names = Names.instance(context);
        typeEnvs = TypeEnvs.instance(context);
        target = Target.instance(context);
    }

    public void visitSwitch(JCSwitch tree) {
        handleSwitch(tree, tree.selector, tree.cases);
    }

    @Override
    public void visitSwitchExpression(JCSwitchExpression tree) {
        if (tree.kind == SwitchKind.MATCHING) {
            //XXX: this breaks matching switch used as condition!
            //translates switch expression to statement switch:
            //switch (selector) {
            //    case C: break value;
            //    ...
            //}
            //=>
            //(letexpr T exprswitch$;
            //         switch (selector) {
            //             case C: { exprswitch$ = value; break; }
            //         }
            //         exprswitch$
            //)
            VarSymbol dollar_switchexpr = new VarSymbol(Flags.FINAL|Flags.SYNTHETIC,
                               names.fromString("exprswitch" + tree.pos + target.syntheticNameChar()),
                               tree.type,
                               currentMethodSym);

            ListBuffer<JCStatement> stmtList = new ListBuffer<>();

            stmtList.append(make.at(tree.pos()).VarDef(dollar_switchexpr, null).setType(dollar_switchexpr.type));
            JCSwitch switchStatement = make.Switch(tree.selector, null);
            switchStatement.kind = tree.kind;
            switchStatement.cases =
                    tree.cases.stream()
                              .map(c -> convertCase(dollar_switchexpr, switchStatement, tree, c))
                              .collect(List.collector());
            if (tree.cases.stream().noneMatch(c -> c.pats.isEmpty())) {
                JCThrow thr = make.Throw(makeNewClass(syms.incompatibleClassChangeErrorType,
                                                      List.nil()));
                JCCase c = make.Case(JCCase.STATEMENT, List.nil(), List.of(thr), null);
                switchStatement.cases = switchStatement.cases.append(c);
            }

            stmtList.append(translate(switchStatement));

            result = make.LetExpr(stmtList.toList(), make.Ident(dollar_switchexpr))
                         .setType(dollar_switchexpr.type);
        } else {
            handleSwitch(tree, tree.selector, tree.cases);
        }
    }
        //where:
        private JCCase convertCase(VarSymbol dollar_switchexpr, JCSwitch switchStatement,
                                   JCSwitchExpression switchExpr, JCCase c) {
            make.at(c.pos());
            ListBuffer<JCStatement> statements = new ListBuffer<>();
            statements.addAll(new TreeTranslator() {
                @Override
                public void visitLambda(JCLambda tree) {}
                @Override
                public void visitClassDef(JCClassDecl tree) {}
                @Override
                public void visitMethodDef(JCMethodDecl tree) {}
                @Override
                public void visitBreak(JCBreak tree) {
                    if (tree.target == switchExpr) {
                        tree.target = switchStatement;
                        JCExpressionStatement assignment =
                                make.Exec(make.Assign(make.Ident(dollar_switchexpr),
                                                      translate(tree.value))
                                              .setType(dollar_switchexpr.type));
                        result = make.Block(0, List.of(assignment,
                                                       tree));
                        tree.value = null;
                    } else {
                        result = tree;
                    }
                }
            }.translate(c.stats));
            JCCase res = make.Case(JCCase.STATEMENT, c.pats, statements.toList(), null);
            res.completesNormally = c.completesNormally;
            return res;
        }

    public <T extends JCTree&GenericSwitch> void handleSwitch(T tree, JCExpression selector, List<JCCase> cases) {
        //expand multiple label cases:
        ListBuffer<JCCase> newCases = new ListBuffer<>();

        for (JCCase c : cases) {
            switch (c.pats.size()) {
                case 0: //default
                case 1: //single label
                    newCases.append(c);
                    break;
                default: //multiple labels, expand:
                    //case C1, C2, C3: ...
                    //=>
                    //case C1:
                    //case C2:
                    //case C3: ...
                    List<JCPattern> patterns = c.pats;
                    while (patterns.tail.nonEmpty()) {
                        JCCase cse = make_at(c.pos()).Case(JCCase.STATEMENT,
                                                           List.of(patterns.head),
                                                           List.nil(),
                                                           null);
                        cse.completesNormally = true;
                        newCases.append(cse);
                        patterns = patterns.tail;
                    }
                    c.pats = patterns;
                    newCases.append(c);
                    break;
            }
        }

        for (JCCase c : newCases) {
            if (c.caseKind == JCCase.RULE && c.completesNormally) {
                JCBreak b = make_at(c.pos()).Break(null);
                b.target = tree;
                c.stats = c.stats.append(b);
            }
        }

        tree.setSelector(translate(selector));
        tree.setCases(translateCases(newCases.toList()));

        result = tree;
    }

    public void visitClassDef(JCClassDecl tree) {
        MethodSymbol currentMethodSymPrev = currentMethodSym;
        Env<AttrContext> prevEnv = attrEnv;

        try {
            currentMethodSym = null;
            attrEnv = typeEnvs.get(tree.sym);
            if (attrEnv == null)
                attrEnv = prevEnv;
            super.visitClassDef(tree);
        } finally {
            attrEnv = prevEnv;
            currentMethodSym = currentMethodSymPrev;
        }
    }

    public void visitMethodDef(JCMethodDecl tree) {
        MethodSymbol prevMethodSym = currentMethodSym;
        try {
            currentMethodSym = tree.sym;
            super.visitMethodDef(tree);
        } finally {
            currentMethodSym = prevMethodSym;
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

    //from Lower (probably generalize):
    private DiagnosticPosition make_pos;

    /** Equivalent to make.at(pos.getStartPosition()) with side effect of caching
     *  pos as make_pos, for use in diagnostics.
     **/
    TreeMaker make_at(DiagnosticPosition pos) {
        make_pos = pos;
        return make.at(pos);
    }

    /** Make an attributed class instance creation expression.
     *  @param ctype    The class type.
     *  @param args     The constructor arguments.
     */
    JCNewClass makeNewClass(Type ctype, List<JCExpression> args) {
        JCNewClass tree = make.NewClass(null,
            null, make.QualIdent(ctype.tsym), args, null);
        tree.constructor = rs.resolveConstructor(
            make_pos, attrEnv, ctype, TreeInfo.types(args), List.nil());
        tree.type = ctype;
        return tree;
    }
}
