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

import com.sun.tools.javac.code.Attribute.Compound;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.DynamicMethodSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.intrinsics.IntrinsicContext;
import com.sun.tools.javac.intrinsics.IntrinsicProcessor;
import com.sun.tools.javac.intrinsics.IntrinsicProcessorFactory;
import com.sun.tools.javac.intrinsics.Intrinsics;
import com.sun.tools.javac.intrinsics.IntrinsicProcessor.Result;
import com.sun.tools.javac.jvm.ClassFile;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.resources.CompilerProperties.Warnings;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.JCDiagnostic.SimpleDiagnosticPosition;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Options;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DirectMethodHandleDesc.Kind;
import java.lang.constant.DynamicCallSiteDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;

import java.lang.invoke.MethodHandles;

import static com.sun.tools.javac.code.Flags.STATIC;
import static com.sun.tools.javac.code.TypeTag.BOT;

/**
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class IntrinsicsVisitor {
    protected static final Context.Key<IntrinsicsVisitor> intrinsicsVisitorKey = new Context.Key<>();

    private boolean disableIntrinsics;

    private final Names names;
    private final Symtab syms;
    private final Resolve rs;
    private final Types types;
    private TransTypes transTypes;
    private final Log log;
    private final TreeMaker make;
    private final MethodHandles.Lookup lookup;
    private ClassSymbol outerMostClass;
    private ClassSymbol thisClass;
    private MethodSymbol thisMethod;

    private Env<AttrContext> attrEnv;
    private DiagnosticPosition nopos;

    public static IntrinsicsVisitor instance(Context context) {
        IntrinsicsVisitor instance = context.get(intrinsicsVisitorKey);

        if (instance == null)
            instance = new IntrinsicsVisitor(context);
        return instance;
    }

    IntrinsicsVisitor(Context context) {
        context.put(intrinsicsVisitorKey, this);
        Options options = Options.instance(context);
        disableIntrinsics = options.isSet("disableIntrinsics");
        names = Names.instance(context);
        syms = Symtab.instance(context);
        rs = Resolve.instance(context);
        types = Types.instance(context);
        transTypes = TransTypes.instance(context);
        log = Log.instance(context);
        make = TreeMaker.instance(context);
        lookup = MethodHandles.lookup();
     }

    public JCTree analyzeTree(JCTree tree, Env<AttrContext> attrEnv) {
        if (disableIntrinsics ||
                !(tree instanceof JCTree.JCClassDecl) ||
                tree.type.tsym.packge().modle == syms.java_base) {
            return tree;
        }

        this.attrEnv = attrEnv;
        this.outerMostClass = ((JCClassDecl)tree).sym;

        return translator.translate(tree);
    }

    private static ConstantDesc<?> typeConstantDesc(Type type) {
        if (type.getTag() == BOT) {
            return ConstantDescs.NULL;
        }

        Object constant = type.constValue();

        if (constant == null) {
            return null;
        }

        return constant instanceof ConstantDesc ? (ConstantDesc<?>)constant : null;
    }

    private Object resolveConstantDesc(ConstantDesc<?> constantDesc) {
        try {
            return constantDesc.resolveConstantDesc(lookup);
        } catch (ReflectiveOperationException ex) {
            // Fall thru
        }
        return null;
    }

    private ClassDesc typeClassDesc(Type type) {
        return ClassDesc.ofDescriptor(signature(types.erasure(type)));
    }

    private MethodTypeDesc methodTypeDesc(Type type) {
        return MethodTypeDesc.ofDescriptor(signature(types.erasure(type)));
    }

    private Name fullName(ClassDesc cd) {
        if (cd.packageName().isEmpty()) {
            return names.fromString(cd.displayName());
        }

        return names.fromString(cd.packageName() + "." + cd.displayName());
    }

    class TransformIntrinsic {
        private final MethodSymbol msym;

        TransformIntrinsic(MethodSymbol msym) {
            this.msym = msym;
        }

        public JCTree translate(JCMethodInvocation tree) {
            if (!(tree.meth instanceof JCFieldAccess)) {
                return tree;
            }

            JCFieldAccess meth = (JCFieldAccess)tree.meth;
            ClassDesc owner = ClassDesc.of(meth.sym.owner.toString());
            String methodName = msym.name.toString();
            MethodTypeDesc methodTypeDesc = methodTypeDesc(msym.type);
            boolean isStatic = (msym.flags() & STATIC) != 0;
            List<JCExpression>args = tree.args;
            int argSize = args.size();
            int offset = isStatic ? 0 : 1;

            int allArgsSize = offset + argSize;
            JCExpression[] allArgs = new JCExpression[allArgsSize];
            ClassDesc[] argClassDescs = new ClassDesc[allArgsSize];
            ConstantDesc<?>[] constantArgs = new ConstantDesc<?>[allArgsSize];

            if (!isStatic) {
                JCExpression selected = meth.selected;
                allArgs[0] = selected;
                argClassDescs[0] = typeClassDesc(selected.type);
                constantArgs[0] = typeConstantDesc(selected.type);
            }

            for (int i = 0; i < argSize; i++) {
                JCExpression arg = args.get(i);

                if (arg == null) {
                    return tree;
                }

                int io = i + offset;
                allArgs[io] = arg;
                argClassDescs[io] = typeClassDesc(arg.type);
                constantArgs[io] = typeConstantDesc(arg.type);
            }

            IntrinsicProcessorFactory factory = Intrinsics.getProcessorFactory(
                    owner,
                    methodName,
                    methodTypeDesc
            );

            if (factory == null) {
                return tree;
            }

            IntrinsicsVisitorContext intrinsicContext = new IntrinsicsVisitorContext(tree, allArgs);
            IntrinsicProcessor processor = factory.processor();
            Result result = processor.tryIntrinsify(
                    intrinsicContext,
                    owner,
                    methodName,
                    methodTypeDesc,
                    isStatic,
                    argClassDescs,
                    constantArgs
            );

            if (result instanceof Result.None) {
                return tree;
            } else if (result instanceof Result.Ldc) {
                Result.Ldc ldc = (Result.Ldc)result;
                Object constant = ldc.constant();
                JCExpression value;

                if (constant == null) {
                    value = make.Literal(BOT, null).setType(syms.botType);
                } else if (constant instanceof ClassDesc) {
                    ClassDesc classDesc = (ClassDesc)constant;
                    Name fullName = fullName(classDesc);
                    ClassSymbol classSym = syms.enterClass(attrEnv.toplevel.modle, fullName);
                    value = make.ClassLiteral(classSym);
                } else {
                    value = make.Literal(constant);
                }

                return transTypes.coerce(attrEnv,  value, meth.type.getReturnType());
            } else if (result instanceof Result.Indy) {
                Result.Indy indy = (Result.Indy)result;
                DynamicCallSiteDesc callSite = indy.indy();
                String invocationName = callSite.invocationName();
                DirectMethodHandleDesc bootstrapMethod = (DirectMethodHandleDesc)callSite.bootstrapMethod();
                ClassDesc ownerClass = bootstrapMethod.owner();
                String bootstrapName = bootstrapMethod.methodName();
                List<Object> staticArgs = List.nil();

                for (ConstantDesc<?> constantDesc : callSite.bootstrapArgs()) {
                    staticArgs = staticArgs.append(resolveConstantDesc(constantDesc));
                }

                List<JCExpression> indyArgs = List.nil();
                for (int i : indy.args()) {
                    indyArgs = indyArgs.append(allArgs[i]);
                }

                List<Type> argTypes = List.nil();
                for (JCExpression arg : indyArgs) {
                    if (arg.type == syms.botType) {
                        argTypes = argTypes.append(syms.objectType);
                    } else {
                        argTypes = argTypes.append(arg.type);
                    }
                }

                Type returnType = msym.type.getReturnType();
                MethodType indyType = new MethodType(argTypes, returnType,  List.nil(), syms.methodClass);
                ClassSymbol classSymbol = syms.enterClass(msym.packge().modle, fullName(ownerClass));

                return makeDynamicCall(
                        new SimpleDiagnosticPosition(tree.pos),
                        classSymbol.type,
                        names.fromString(bootstrapName),
                        staticArgs,
                        indyType,
                        indyArgs,
                        names.fromString(invocationName)
                );
            } else {
                assert false : "tryIntrinsifyMethod result unknown";
            }

            return tree;
        }
    }

    Translator translator = new Translator();
    class Translator extends TreeTranslator {
        @Override
        public void visitApply(JCMethodInvocation tree) {
            super.visitApply(tree);
            Name methName = TreeInfo.name(tree.meth);

            if (methName == names._this || methName == names._super) {
                return;
            }

            MethodSymbol msym = (MethodSymbol)TreeInfo.symbol(tree.meth);
            Compound attr = msym.attribute(syms.intrinsicCandidateType.tsym);

            if (attr != null) {
                TransformIntrinsic transform = new TransformIntrinsic(msym);
                result = transform.translate(tree);
            }
        }

        @Override
        public void visitClassDef(JCClassDecl tree) {
            ClassSymbol saveClass = thisClass;
            thisClass = tree.sym;
            super.visitClassDef(tree);
            thisClass = saveClass;
        }

        @Override
        public void visitMethodDef(JCMethodDecl tree) {
            MethodSymbol saveMethod = thisMethod;
            thisMethod = tree.sym;
            super.visitMethodDef(tree);
            thisMethod = saveMethod;
        }
    }

    private JCExpression makeDynamicCall(DiagnosticPosition pos, Type site, Name bsmName,
                                         List<Object> staticArgs, MethodType indyType,
                                         List<JCExpression> indyArgs,
                                         Name methName) {
        int prevPos = make.pos;
        try {
            make.at(pos);
            List<Type> bsm_staticArgs = List.of(syms.methodHandleLookupType,
                    syms.stringType,
                    syms.methodTypeType).appendList(bsmStaticArgToTypes(staticArgs));
            Symbol bsm = rs.resolveQualifiedMethod(pos, attrEnv, site,
                    bsmName, bsm_staticArgs, List.nil());
            DynamicMethodSymbol dynSym =
                    new DynamicMethodSymbol(methName,
                            syms.noSymbol,
                            ClassFile.REF_invokeStatic,
                            (MethodSymbol)bsm,
                            indyType,
                            staticArgs.toArray());

            JCFieldAccess qualifier = make.Select(make.QualIdent(site.tsym), bsmName);
            qualifier.sym = dynSym;
            qualifier.type = indyType;
            JCMethodInvocation proxyCall = make.Apply(List.nil(), qualifier, indyArgs);
            proxyCall.type = indyType.getReturnType();

            return proxyCall;
        } finally {
            make.at(prevPos);
        }
    }

    private List<Type> bsmStaticArgToTypes(List<Object> args) {
        ListBuffer<Type> argtypes = new ListBuffer<>();

        for (Object arg : args) {
            argtypes.append(bsmStaticArgToType(arg));
        }

        return argtypes.toList();
    }

    private Type bsmStaticArgToType(Object arg) {
        if (arg == null) {
            return syms.objectType;
        } else if (arg instanceof ClassSymbol) {
            return syms.classType;
        } else if (arg instanceof Integer) {
            return syms.intType;
        } else if (arg instanceof Long) {
            return syms.longType;
        } else if (arg instanceof Float) {
            return syms.floatType;
        } else if (arg instanceof Double) {
            return syms.doubleType;
        } else if (arg instanceof String) {
            return syms.stringType;
        } else {
            Assert.error("bad static arg " + arg);
            return null;
        }
    }

    private String signature(Type type) {
        SignatureGenerator generator = new SignatureGenerator();
        generator.assembleSig(type.getTag() == BOT ? syms.objectType : type);

        return generator.toString();
    }

    private class SignatureGenerator extends Types.SignatureGenerator {
        StringBuilder sb = new StringBuilder();

        SignatureGenerator() {
            super(types);
        }

        @Override
        protected void append(char ch) {
            sb.append(ch);
        }

        @Override
        protected void append(byte[] ba) {
            sb.append(new String(ba));
        }

        @Override
        protected void append(Name name) {
            sb.append(name.toString());
        }

        @Override
        public String toString() {
            return sb.toString();
        }
    }

    private class IntrinsicsVisitorContext implements IntrinsicContext {
        private JCMethodInvocation tree;
        private JCExpression[] allArgs;

        IntrinsicsVisitorContext(JCMethodInvocation tree, JCExpression[] allArgs) {
            this.tree = tree;
            this.allArgs = allArgs;
        }

        @Override
        public void warning(String message) {
            log.warning(new SimpleDiagnosticPosition(tree.pos),
                    Warnings.IntrinsicWarning(message));
        }

        @Override
        public void warning(String message, int arg) {
            assert 0 < arg && arg < allArgs.length : "arg index out of range";
            log.warning(new SimpleDiagnosticPosition(allArgs[arg].pos),
                    Warnings.IntrinsicWarning(message));
        }

        @Override
        public void warning(String message, int arg, int offset) {
            assert 0 < arg && arg < allArgs.length : "arg index out of range";
            log.warning(new SimpleDiagnosticPosition(allArgs[arg].pos + offset),
                    Warnings.IntrinsicWarning(message));
        }

        @Override
        public void error(String message) {
            log.error(new SimpleDiagnosticPosition(tree.pos),
                    Errors.IntrinsicError(message));
        }

        @Override
        public void error(String message, int arg) {
            assert 0 < arg && arg < allArgs.length : "arg index out of range";
            log.error(new SimpleDiagnosticPosition(allArgs[arg].pos),
                    Errors.IntrinsicError(message));
        }

        @Override
        public void error(String message, int arg, int offset) {
            assert 0 < arg && arg < allArgs.length : "arg index out of range";
            log.error(new SimpleDiagnosticPosition(allArgs[arg].pos + offset),
                    Errors.IntrinsicError(message));
        }

        @Override
        public ClassDesc getThisClass() {
            return typeClassDesc(thisClass.type);
        }

        @Override
        public ClassDesc getOuterMostClass() {
            return typeClassDesc(outerMostClass.type);
        }

        @Override
        public DirectMethodHandleDesc getMethod() {
            if (thisMethod == null) {
                return null;
            }

            Name name = thisMethod.name;

            Kind kind = name == names.init                 ? Kind.CONSTRUCTOR :
                        (thisMethod.flags() & STATIC) != 0 ? Kind.STATIC :
                                                             Kind.VIRTUAL;

            ClassDesc returnClassDesc =  typeClassDesc(thisMethod.getReturnType());
            List<VarSymbol> parameters = thisMethod.getParameters();
            int length = parameters.length();
            ClassDesc[] argTypes = new ClassDesc[length];
            for (int i = 0; i < length; i++) {
                argTypes[i] = typeClassDesc(parameters.get(i).type);
            }

            DirectMethodHandleDesc result = MethodHandleDesc.of(
                    kind,
                    getThisClass(),
                    name.toString(),
                    returnClassDesc,
                    argTypes
            );
            return result;
        }

        @Override
        public String getSourceName() {
            return attrEnv.toplevel.sourcefile.getName();
        }

        @Override
        public int getLineNumber() {
            return attrEnv.toplevel.lineMap.getLineNumber(tree.pos);
        }
    }
}
