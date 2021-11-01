/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tools.javac.code.Preview;
import com.sun.tools.javac.code.Scope.WriteableScope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.Completer;
import com.sun.tools.javac.code.Symbol.DynamicMethodSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.jvm.PoolConstant.LoadableConstant;
import com.sun.tools.javac.jvm.Target;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.JCDiagnostic.SimpleDiagnosticPosition;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Scope.LookupKind.NON_RECURSIVE;
import static com.sun.tools.javac.tree.JCTree.Tag.*;

/** This pass translates constructed literals (templated strings, ...) to conventional Java.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class TransLiterals extends TreeTranslator {
    /**
     * The context key for the TransTypes phase.
     */
    protected static final Context.Key<TransLiterals> transLiteralsKey = new Context.Key<>();

    /**
     * Use direct field access instead of expressions.
     * Used to indicate use of invokeDynamic for policy.apply (TBD - for release)
     */
    private static final boolean USE_INVOKE_DYNAMIC = true;

    /**
     * Used to indicate which methodology is used for expression access (TBD - for release)
     */
    private enum ExpressionAccess {
        USE_FIELDS,             // direct access from instance field
        USE_EXPRESSIONS,        // wrapper methods around field access
        USE_LAZY_EXPRESSIONS    // lazy evaluation of expressions
    }
    private static final ExpressionAccess ACCESS =
            System.getenv("JDK_TS_LAMBDA") != null ?
                    ExpressionAccess.USE_LAZY_EXPRESSIONS : ExpressionAccess.USE_FIELDS;

    /**
     * Character used as separator for synthetic names.
     */
    private final char SYNTHETIC_NAME_CHAR;

    /**
     * Get the instance for this context.
     */
    public static TransLiterals instance(Context context) {
        TransLiterals instance = context.get(transLiteralsKey);
        if (instance == null)
            instance = new TransLiterals(context);
        return instance;
    }

    private final Symtab syms;
    private final Attr attr;
    private final Resolve rs;
    private final Types types;
    private final Check chk;
    private final Operators operators;
    private final Names names;
    private final Target target;
    private final Preview preview;
    private TreeMaker make = null;
    private Env<AttrContext> env = null;
    private ClassSymbol currentClass = null;
    private MethodSymbol currentMethodSym = null;

    protected TransLiterals(Context context) {
        context.put(transLiteralsKey, this);
        syms = Symtab.instance(context);
        attr = Attr.instance(context);
        rs = Resolve.instance(context);
        make = TreeMaker.instance(context);
        types = Types.instance(context);
        chk = Check.instance(context);
        operators = Operators.instance(context);
        names = Names.instance(context);
        target = Target.instance(context);
        preview = Preview.instance(context);

        SYNTHETIC_NAME_CHAR = target.syntheticNameChar();
    }

    JCExpression makeLit(Type type, Object value) {
        return make.Literal(type.getTag(), value).setType(type.constType(value));
    }

    JCExpression makeNull() {
        return makeLit(syms.botType, null);
    }

    JCExpression makeString(String string) {
        return makeLit(syms.stringType, string);
    }

    Type makeListType(Type elemType) {
        return new ClassType(syms.listType, List.of(elemType), syms.listType.tsym);
    }

    JCBinary makeBinary(JCTree.Tag optag, JCExpression lhs, JCExpression rhs) {
        JCBinary tree = make.Binary(optag, lhs, rhs);
        tree.operator = operators.resolveBinary(tree, optag, lhs.type, rhs.type);
        tree.type = tree.operator.type.getReturnType();
        return tree;
    }

    JCVariableDecl makeField(JCClassDecl cls, long flags, Name name, Type type, JCExpression init) {
        VarSymbol sym = new VarSymbol(flags | FINAL | SYNTHETIC, name, type, cls.sym);
        JCVariableDecl var = make.VarDef(sym, init);
        cls.defs = cls.defs.append(var);
        cls.sym.members().enter(var.sym);

        return var;
    }
    MethodType makeMethodType(Type returnType, List<Type> argTypes) {
        return new MethodType(argTypes, returnType, List.nil(), syms.methodClass);
    }

    JCFieldAccess makeThisFieldSelect(Type owner, JCVariableDecl field) {
        JCFieldAccess select = make.Select(make.This(owner), field.name);
        select.type = field.type;
        select.sym = field.sym;
        return select;
    }

    JCIdent makeParamIdent(List<JCVariableDecl> params, Name name) {
        VarSymbol param = params.stream()
                .filter(p -> p.name == name)
                .findFirst()
                .get().sym;
        JCIdent ident = make.Ident(name);
        ident.type = param.type;
        ident.sym = param;
        return ident;
    }

    JCFieldAccess makeSelect(Symbol sym, Name name) {
        return make.Select(make.QualIdent(sym), name);
    }

    JCMethodInvocation makeApply(JCFieldAccess method, List<JCExpression> args) {
        return make.Apply(List.nil(), method, args);
    }

    Symbol findMember(ClassSymbol classSym, Name name) {
        return classSym.members().getSymbolsByName(name, NON_RECURSIVE).iterator().next();
    }

    JCFieldAccess makeFieldAccess(JCClassDecl owner, Name name) {
        Symbol sym = findMember(owner.sym, name);
        JCFieldAccess access = makeSelect(owner.sym, name);
        access.type = sym.type;
        access.sym = sym;
        return access;
    }

    MethodSymbol lookupMethod(DiagnosticPosition pos, Name name, Type qual, List<Type> args) {
        return rs.resolveInternalMethod(pos, env, qual, name, args, List.nil());
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
    public void visitMethodDef(JCMethodDecl tree) {
        MethodSymbol prevMethodSym = currentMethodSym;
        try {
            currentMethodSym = tree.sym;
            super.visitMethodDef(tree);
        } finally {
            currentMethodSym = prevMethodSym;
        }
    }

    record MethodInfo(MethodType type, MethodSymbol sym, JCMethodDecl decl) {
        void addStatement(JCStatement statement) {
            JCBlock body = decl.body;
            body.stats = body.stats.append(statement);
        }
    }

    class TransTemplatedString {
        JCTemplatedString tree;
        JCExpression policy;
        String string;
        List<String> strings;
        List<JCExpression> expressions;
        List<Type> expressionTypes;
        JCClassDecl templatedStringClass;
        List<JCVariableDecl> fields;
        List<JCMethodDecl> exprMethods;
        MethodInfo concatMethod;

        TransTemplatedString(JCTemplatedString tree) {
            this.tree = tree;
            this.policy = tree.policy;
            this.string = (String)((JCLiteral)tree.string).value;
            this.strings = split(this.string);
            this.expressions = translate(tree.expressions);
            this.expressionTypes = expressions.stream()
                    .map(arg -> arg.type == syms.botType ? syms.objectType : arg.type)
                    .collect(List.collector());
            this.templatedStringClass = null;
            this.fields = List.nil();
            this.exprMethods = List.nil();
            this.concatMethod = null;
        }

        private final static char OBJECT_REPLACEMENT_CHARACTER = '\uFFFC';

        List<String> split(String string) {
            List<String> strings = List.nil();
            StringBuilder sb = new StringBuilder();
            for (char ch : string.toCharArray()) {
                if (ch == OBJECT_REPLACEMENT_CHARACTER) {
                    strings = strings.append(sb.toString());
                    sb.setLength(0);
                } else {
                    sb.append(ch);
                }
            }
            strings = strings.append(sb.toString());
            return strings;
        }

        JCExpression concatExpression(List<String> strings, List<JCExpression> expressions) {
            JCExpression expr = null;
            Iterator<JCExpression> iterator = expressions.iterator();
            for (String segment : strings) {
                expr = expr == null ? makeString(segment)
                        : makeBinary(PLUS, expr, makeString(segment));
                if (iterator.hasNext()) {
                    JCExpression expression = iterator.next();
                    Type expressionType = expression.type;
                    expr = makeBinary(PLUS, expr, expression.setType(expressionType));
                }
            }
            return expr;
        }

        JCMethodInvocation createApply(Type owner, Name name, List<JCExpression> args, JCFieldAccess method) {
            List<Type> argTypes = TreeInfo.types(args);
            MethodSymbol methodSym = lookupMethod(tree.pos(), name, owner, argTypes);
            method.sym = methodSym;
            method.type = types.erasure(methodSym.type);
            JCMethodInvocation apply = makeApply(method, args);
            apply.type = methodSym.getReturnType();
            return apply;
        }

        JCMethodInvocation createApply(Type owner, Name name, List<JCExpression> args) {
            JCFieldAccess method = makeSelect(owner.tsym, name);
            return createApply(owner, name, args, method);
        }

        JCMethodInvocation createApply(Symbol receiver, Name name, List<JCExpression> args) {
            Type owner = receiver.type;
            JCFieldAccess method = makeSelect(receiver, name);
            return createApply(owner, name, args, method);
        }

        JCMethodInvocation createApplyAsList(JCExpression array) {
            ArrayType arrayType = (ArrayType)array.type;
            Type elemType = arrayType.elemtype;
            JCMethodInvocation asListApplied = createApply(syms.arraysType, names.asList, List.of(array));
            asListApplied.type = makeListType(elemType);
            return asListApplied;
        }

        JCMethodInvocation createApplyExprMethod(JCMethodDecl exprMethod) {
            return createApply(templatedStringClass.sym, exprMethod.name, List.nil());
        }

        JCExpression createClassArg(Symbol sym) {
            return makeSelect(sym, names._class).setType(syms.classType);
        }

        MethodInfo createMethod(long flags, Name name, Type returnType, List<Type> argTypes, JCClassDecl owner) {
            MethodType type = makeMethodType(returnType, argTypes);
            MethodSymbol sym = new MethodSymbol(flags, name, type, owner.sym);
            JCMethodDecl decl = make.MethodDef(sym, type, make.Block(0, List.nil()));
            owner.defs = owner.defs.append(decl);
            owner.sym.members().enter(sym);
            return new MethodInfo(type, sym, decl);
        }

        void createFields() {
            boolean useFields = ACCESS.equals(ExpressionAccess.USE_FIELDS);

            int i = 0;
            for (JCExpression expression : expressions) {
                Type type = expression.type == syms.botType ? syms.objectType : expression.type;
                JCVariableDecl fieldVar = makeField(templatedStringClass, PRIVATE, make.paramName(i++),
                        type, useFields ? null : expression);
                fields = fields.append(fieldVar);
            }
        }

        Name exprMethodName(int i) {
            return names.fromString("expr" + SYNTHETIC_NAME_CHAR + i);
        }

        void createExprMethods() {
            int i = 0;
            for (JCVariableDecl field : fields) {
                Name name = exprMethodName(i++);
                MethodInfo method = createMethod(PUBLIC, name, field.type, List.nil(), templatedStringClass);
                method.addStatement(make.Return(make.Ident(field.sym)));
                exprMethods = exprMethods.append(method.decl);
            }
        }

        void createLazyExprMethods() {
            int i = 0;
            for (JCExpression expression : expressions) {
                Name name = exprMethodName(i++);
                MethodInfo method = createMethod(PUBLIC, name, expression.type, List.nil(), templatedStringClass);
                method.addStatement(make.Return(expression));
                exprMethods = exprMethods.append(method.decl);
            }
        }

        List<JCExpression> createAccessors() {
            if (ACCESS.equals(ExpressionAccess.USE_FIELDS)) {
                return fields.stream()
                        .map(f -> make.QualIdent(f.sym))
                        .collect(List.collector());
            } else {
                return exprMethods.stream()
                        .map(m -> createApplyExprMethod(m))
                        .collect(List.collector());
            }
        }

        void createInitMethod() {
            boolean useFields = ACCESS.equals(ExpressionAccess.USE_FIELDS);

            List<Type> argTypes = useFields ? expressionTypes : List.nil();
            MethodInfo method = createMethod(PUBLIC, names.init,
                    syms.voidType, argTypes, templatedStringClass);
            JCIdent superIdent = make.Ident(names._super);
            superIdent.sym = lookupMethod(tree.pos(), names.init, syms.objectType, List.nil());
            superIdent.type = superIdent.sym.type;
            JCMethodInvocation superApply = make.Apply(List.nil(), translate(superIdent), List.nil());
            superApply.type = syms.voidType;
            method.addStatement(make.Exec(superApply));

            if (useFields) {
                List<JCVariableDecl> params = method.decl.params;
                for (JCVariableDecl field : fields) {
                    JCFieldAccess select = makeThisFieldSelect(templatedStringClass.type, field);
                    JCIdent ident = makeParamIdent(params, field.name);
                    JCAssign assign = make.Assign(select, ident);
                    assign.type = ident.type;
                    method.addStatement(make.Exec(assign));
                }
            }
        }

        void createTemplateMethod(JCVariableDecl templateVar) {
            MethodInfo method = createMethod(SYNTHETIC | PUBLIC, names.template,
                    syms.stringType, List.nil(), templatedStringClass);
            JCExpression string = make.QualIdent(templateVar.sym);
            string.type = syms.stringType;
            method.addStatement(make.Return(string));
        }

        void createSegmentsMethod(JCVariableDecl segmentsVar) {
            Type stringListType = makeListType(syms.stringType);
            MethodInfo method = createMethod(SYNTHETIC | PUBLIC, names.segments,
                    stringListType, List.nil(), templatedStringClass);
            JCFieldAccess segments = makeFieldAccess(templatedStringClass, names.segmentsUpper);
            method.addStatement(make.Return(segments));
        }

        void createValuesMethod() {
            Type objectListType = makeListType(syms.objectType);
            MethodInfo method = createMethod(SYNTHETIC | PUBLIC, names.values,
                    objectListType, List.nil(), templatedStringClass);
            JCNewArray array = make.NewArray(make.Type(syms.objectType), List.nil(), createAccessors());
            array.type = new ArrayType(syms.objectType, syms.arrayClass);
            JCMethodInvocation asListApplied = createApplyAsList(array);
            method.addStatement(make.Return(asListApplied));
        }

        void createVarsMethod() {
            boolean useFields = ACCESS.equals(ExpressionAccess.USE_FIELDS);

            Type mhListType = makeListType(syms.methodHandleType);
            MethodInfo method = createMethod(SYNTHETIC | PUBLIC, names.vars,
                    mhListType, List.nil(), templatedStringClass);
            method.decl.thrown = method.decl.thrown.append(make.Type(syms.reflectiveOperationExceptionType));
            JCMethodInvocation lookupApply = createApply(syms.methodHandlesType, names.lookup, List.nil());
            VarSymbol lookupSym = new VarSymbol(0, names.lookup, syms.methodHandleLookupType, method.sym);
            method.addStatement(make.VarDef(lookupSym, lookupApply));

            List<JCExpression> accessors = List.nil();

            if (useFields) {
                int i = 0;
                for (JCVariableDecl field : fields) {
                    Symbol sym = field.sym;
                    Name findName = names.fromString("find" + SYNTHETIC_NAME_CHAR + i++);
                    VarSymbol findSym = new VarSymbol(0, findName, syms.methodHandleType, method.sym);
                    List<JCExpression> args = List.of(
                            createClassArg(templatedStringClass.sym),
                            makeString(field.name.toString()).setType(syms.stringType),
                            createClassArg(sym)
                    );
                    JCMethodInvocation find = createApply(lookupSym, names.findGetter, args);
                    method.addStatement(make.VarDef(findSym, find));
                    accessors = accessors.append(make.QualIdent(findSym));
                }
            } else {
                Map<Symbol, VarSymbol> mtMap = new HashMap<>();

                int i = 0;
                for (JCExpression expression : expressions) {
                    Symbol sym = expression.type.tsym;

                    if (!mtMap.containsKey(sym)) {
                        Name name = names.fromString("mt" + SYNTHETIC_NAME_CHAR + i++);
                        List<JCExpression> args = List.of(createClassArg(sym));
                        JCExpression mt = createApply(syms.methodTypeType, names.methodType, args);
                        VarSymbol mtSym = new VarSymbol(0, name, syms.methodTypeType, method.sym);
                        method.addStatement(make.VarDef(mtSym, mt));
                        mtMap.put(sym, mtSym);
                    }
                }

                i = 0;
                for (JCMethodDecl exprMethod : exprMethods) {
                    Symbol sym = exprMethod.restype.type.tsym;
                    VarSymbol mtSym = mtMap.get(sym);
                    Name findName = names.fromString("find" + SYNTHETIC_NAME_CHAR + i++);
                    VarSymbol findSym = new VarSymbol(0, findName, syms.methodHandleType, method.sym);
                    List<JCExpression> args = List.of(
                            createClassArg(templatedStringClass.sym),
                            makeString(exprMethod.name.toString()).setType(syms.stringType),
                            make.QualIdent(mtSym)
                    );
                    JCMethodInvocation find = createApply(lookupSym, names.findVirtual, args);
                    method.addStatement(make.VarDef(findSym, find));
                    accessors = accessors.append(make.QualIdent(findSym));
                }
            }

            JCNewArray array = make.NewArray(make.Type(syms.methodHandleType), List.nil(), accessors);
            array.type = new ArrayType(syms.methodHandleType, syms.arrayClass);
            JCMethodInvocation asListApplied = createApplyAsList(array);
            method.addStatement(make.Return(asListApplied));
        }

        void createConcatMethod() {
            concatMethod = createMethod(SYNTHETIC | PUBLIC, names.concat,
                    syms.stringType, List.nil(), templatedStringClass);
            concatMethod.addStatement(make.Return(concatExpression(strings, createAccessors())));
        }

        void createToStringMethod() {
            MethodInfo toStringMethod = createMethod(PUBLIC, names.toString,
                    syms.stringType, List.nil(), templatedStringClass);
            JCExpression applytoString = this.createApply(syms.templatedStringType, names.toString,
                    List.of(make.This(templatedStringClass.type)));
            toStringMethod.addStatement(make.Return(applytoString));
        }

        private JCClassDecl newTemplatedStringClass() {
            long flags = PUBLIC | FINAL | SYNTHETIC;

            if (currentMethodSym.isStatic()) {
                flags |= NOOUTERTHIS;
            }

            String nameBase = syms.templatedStringType.tsym.getSimpleName().toString();
            Name name = chk.localClassName(syms.defineClass(names.fromString(nameBase), currentClass));
            JCClassDecl cDecl =  make.ClassDef(make.Modifiers(flags), name, List.nil(), null, List.nil(), List.nil());
            ClassSymbol cSym = syms.defineClass(name, currentMethodSym);
            cSym.sourcefile = currentClass.sourcefile;
            cSym.completer = Completer.NULL_COMPLETER;
            cSym.members_field = WriteableScope.create(cSym);
            cSym.flags_field = flags;
            ClassType cType = (ClassType)cSym.type;
            cType.supertype_field = syms.objectType;
            cType.interfaces_field = List.of(syms.templatedStringType);
            cType.all_interfaces_field = List.of(syms.templatedStringType);
            cType.setEnclosingType(currentClass.type);
            cDecl.sym = cSym;
            cDecl.type = cType;
            cSym.complete();

            return cDecl;
        }

        void createTemplatedStringClass() {
            ClassSymbol saveCurrentClass = currentClass;

            try {
                Type templatedStringType = syms.templatedStringType;
                templatedStringClass = newTemplatedStringClass();
                currentClass = templatedStringClass.sym;
                JCVariableDecl templateVar =
                        makeField(templatedStringClass, PRIVATE | STATIC, names.templateUpper,
                                syms.stringType, makeString(string));
                Symbol templateSym = findMember(templatedStringClass.sym, names.templateUpper);
                JCExpression templateIdent = make.QualIdent(templateSym);
                templateIdent.type = syms.stringType;
                Type stringList = makeListType(syms.stringType);
                JCMethodInvocation splitApplied = createApply(templatedStringType, names.split, List.of(templateIdent));
                splitApplied.type = stringList;
                JCVariableDecl segmentsVar = makeField(templatedStringClass, PRIVATE | STATIC,
                        names.segmentsUpper, stringList, splitApplied);

                switch (ACCESS) {
                    case USE_FIELDS:
                        createFields();
                        break;
                    case USE_EXPRESSIONS:
                        createFields();
                        createExprMethods();
                        break;
                    case USE_LAZY_EXPRESSIONS:
                        createLazyExprMethods();
                        break;
                }

                createInitMethod();
                createTemplateMethod(templateVar);
                createSegmentsMethod(segmentsVar);
                createValuesMethod();
                createVarsMethod();
                createConcatMethod();
                createToStringMethod();

                saveCurrentClass.members().enter(templatedStringClass.sym);
            } finally {
                currentClass = saveCurrentClass;
            }
        }

        JCExpression newTemplatedString() {
            boolean useFields = ACCESS.equals(ExpressionAccess.USE_FIELDS);

            createTemplatedStringClass();
            List<JCExpression> args = useFields ? expressions : List.nil();
            List<Type> argTypes = useFields ? expressionTypes : List.nil();
            JCExpression encl = currentMethodSym.isStatic() ? null :
                    make.This(currentMethodSym.owner.type);
            JCNewClass newClass = make.NewClass(encl,
                    null, make.QualIdent(templatedStringClass.type.tsym), args, templatedStringClass);
            newClass.constructor = rs.resolveConstructor(
                    new SimpleDiagnosticPosition(make.pos), env, templatedStringClass.type, argTypes, List.nil());
            newClass.type = templatedStringClass.type;

            return newClass;
        }

        JCExpression createPolicyAcceptMethodCall(JCExpression templatedString) {
            JCExpression apply;

            if (USE_INVOKE_DYNAMIC) {
                Name name = names.templatedStringBSM;
                List<JCExpression> args = List.of(policy, templatedString);
                List<Type> argTypes = List.of(syms.templatePolicyType, syms.templatedStringType);
                MethodType methodType = makeMethodType(tree.type, argTypes);
                JCFieldAccess qualifier = make.Select(make.Type(syms.templatedStringType), name);

                List<Type> staticArgsTypes =
                        List.of(syms.methodHandleLookupType, syms.stringType, syms.methodTypeType);
                LoadableConstant[] constants = new LoadableConstant[0];
                Symbol bsm = rs.resolveQualifiedMethod(policy.pos(), env,
                        syms.templatedStringType, name, staticArgsTypes, List.nil());
                DynamicMethodSymbol dynSym = new DynamicMethodSymbol(
                        name,
                        syms.noSymbol,
                        ((MethodSymbol)bsm).asHandle(),
                        methodType,
                        constants
                );
                qualifier.sym = dynSym;
                qualifier.type = methodType;
                apply = make.Apply(List.nil(), qualifier, args);
            } else {
                Name name = names.apply;
                List<JCExpression> args = List.of(templatedString);
                List<Type> argTypes = List.of(syms.templatedStringType);
                MethodType methodType = makeMethodType(syms.objectType, argTypes);
                JCFieldAccess qualifier = make.Select(policy, name);
                Symbol sym = rs.resolveQualifiedMethod(policy.pos(), env,
                        syms.templatePolicyType, name, argTypes, List.nil());
                qualifier.type = methodType;
                qualifier.sym = sym;
                apply = make.Apply(List.nil(), qualifier, args);
                apply.type = methodType.getReturnType();
                apply = make.TypeCast(tree.type, apply);
            }

            apply.type = tree.type;

            return apply;
        }

        JCExpression visit() {
            make.at(tree.string.pos);
            JCExpression result = newTemplatedString();

            if (policy != null) {
                make.at(tree.pos);
                result = createPolicyAcceptMethodCall(result);
            }

            return result;
        }
    }

    public void visitTemplatedString(JCTemplatedString tree) {
        int prevPos = make.pos;
        try {
            tree.policy = translate(tree.policy);
            tree.string = translate(tree.string);
            tree.expressions = translate(tree.expressions);

            TransTemplatedString transTemplatedString = new TransTemplatedString(tree);

            result = transTemplatedString.visit();
        } catch (Throwable ex) {
            ex.printStackTrace();
            throw ex;
        } finally {
            make.at(prevPos);
        }
    }

    public JCTree translateTopLevelClass(Env<AttrContext> env, JCTree cdef, TreeMaker make) {
        try {
            this.make = make;
            this.env = env;
            translate(cdef);
        } finally {
            this.make = null;
            this.env = null;
        }

        return cdef;
    }

}
