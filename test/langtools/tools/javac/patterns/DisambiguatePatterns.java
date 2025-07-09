/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/**
 * @test
 * @modules jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.parser
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 */

import com.sun.source.tree.CaseLabelTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ConstantCaseLabelTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.PatternCaseLabelTree;
import com.sun.source.tree.PatternTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.util.Options;
import java.nio.charset.Charset;

public class DisambiguatePatterns {

    public static void main(String... args) throws Throwable {
        DisambiguatePatterns test = new DisambiguatePatterns();
        test.caseDisambiguationTest("String s",
                                    CaseExpressionType.PATTERN);
        test.caseDisambiguationTest("String s when s.isEmpty()",
                                    CaseExpressionType.PATTERN);
        test.caseDisambiguationTest("String s",
                                    CaseExpressionType.PATTERN);
        test.caseDisambiguationTest("@Ann String s",
                                    CaseExpressionType.PATTERN);
        test.caseDisambiguationTest("String s",
                                    CaseExpressionType.PATTERN);
        test.caseDisambiguationTest("(String) s",
                                    CaseExpressionType.EXPRESSION);
        test.caseDisambiguationTest("((String) s)",
                                    CaseExpressionType.EXPRESSION);
        test.caseDisambiguationTest("((0x1))",
                                    CaseExpressionType.EXPRESSION);
        test.caseDisambiguationTest("a | b",
                                    CaseExpressionType.EXPRESSION);
        test.caseDisambiguationTest("a || b",
                                    CaseExpressionType.EXPRESSION);
        test.caseDisambiguationTest("a & b",
                                    CaseExpressionType.EXPRESSION);
        test.caseDisambiguationTest("a && b",
                                    CaseExpressionType.EXPRESSION);
        test.caseDisambiguationTest("a < b",
                                    CaseExpressionType.EXPRESSION);
        test.caseDisambiguationTest("a > b",
                                    CaseExpressionType.EXPRESSION);
        test.caseDisambiguationTest("a >> b",
                                    CaseExpressionType.EXPRESSION);
        test.caseDisambiguationTest("a >>> b",
                                    CaseExpressionType.EXPRESSION);
        test.caseDisambiguationTest("a < b | a > b",
                                    CaseExpressionType.EXPRESSION);
        test.caseDisambiguationTest("a << b | a >> b",
                                    CaseExpressionType.EXPRESSION);
        test.caseDisambiguationTest("a << b || a < b | a >>> b",
                                    CaseExpressionType.EXPRESSION);
        test.caseDisambiguationTest("(a > b)",
                                    CaseExpressionType.EXPRESSION);
        test.caseDisambiguationTest("(a >> b)",
                                    CaseExpressionType.EXPRESSION);
        test.caseDisambiguationTest("(a >>> b)",
                                    CaseExpressionType.EXPRESSION);
        test.caseDisambiguationTest("(a < b | a > b)",
                                    CaseExpressionType.EXPRESSION);
        test.caseDisambiguationTest("(a << b | a >> b)",
                                    CaseExpressionType.EXPRESSION);
        test.caseDisambiguationTest("(a << b || a < b | a >>> b)",
                                    CaseExpressionType.EXPRESSION);
        test.caseDisambiguationTest("a < c > b",
                                    CaseExpressionType.PATTERN);
        test.caseDisambiguationTest("a < c.d > b",
                                    CaseExpressionType.PATTERN);
        test.caseDisambiguationTest("a<? extends c.d> b",
                                    CaseExpressionType.PATTERN);
        test.caseDisambiguationTest("@Ann a<? extends c.d> b",
                                    CaseExpressionType.PATTERN);
        test.caseDisambiguationTest("a<? extends @Ann c.d> b",
                                    CaseExpressionType.PATTERN);
        test.caseDisambiguationTest("a<? super c.d> b",
                                    CaseExpressionType.PATTERN);
        test.caseDisambiguationTest("a<? super @Ann c.d> b",
                                    CaseExpressionType.PATTERN);
        test.caseDisambiguationTest("a<b<c.d>> b",
                                    CaseExpressionType.PATTERN);
        test.caseDisambiguationTest("a<b<@Ann c.d>> b",
                                    CaseExpressionType.PATTERN);
        test.caseDisambiguationTest("a<b<c<d>>> b",
                                    CaseExpressionType.PATTERN);
        test.caseDisambiguationTest("a[] b",
                                    CaseExpressionType.PATTERN);
        test.caseDisambiguationTest("a[][] b",
                                    CaseExpressionType.PATTERN);
        test.caseDisambiguationTest("int i",
                                    CaseExpressionType.PATTERN);
        test.caseDisambiguationTest("int[] i",
                                    CaseExpressionType.PATTERN);
        test.caseDisambiguationTest("a[a]",
                                    CaseExpressionType.EXPRESSION);
        test.caseDisambiguationTest("a[b][c]",
                                    CaseExpressionType.EXPRESSION);
        test.caseDisambiguationTest("a & b",
                                    CaseExpressionType.EXPRESSION);
        test.caseDisambiguationTest("R r when (x > 0)",
                                    CaseExpressionType.PATTERN);
        test.caseDisambiguationTest("R(int x) when (x > 0)",
                                    CaseExpressionType.PATTERN);
        test.caseDisambiguationTest("R(int x) when foo.x() > 0",
                                    CaseExpressionType.PATTERN);

        test.caseDisambiguationTest("test().R()",
                                    CaseExpressionType.PATTERN);
        test.caseDisambiguationTest("test().R(var v)",
                                    CaseExpressionType.PATTERN);
        test.caseDisambiguationTest("test().R(int v)",
                                    CaseExpressionType.PATTERN);
        test.caseDisambiguationTest("test().R(int _)",
                                    CaseExpressionType.PATTERN);
        test.caseDisambiguationTest("test().R(_)",
                                    CaseExpressionType.PATTERN);

        test.caseDisambiguationTest("test().R(test().R(_))",
                                    CaseExpressionType.PATTERN);

        //TODO: error recovery, can be ignored if needed
        //expressions with method invocations can never be constant expessions,
        //and hence would fail later during attribution anyway:
//        test.disambiguationTest("test().R(test().R(1))",
//                                 ExpressionType.EXPRESSION);

        test.caseDisambiguationTest("test().test().test().R(test().test().R(int i), test().test().R(int i), other.pack.Rec(var v))",
                                    CaseExpressionType.PATTERN);

        test.caseDisambiguationTest("""
                                    test(t -> 0)
                                    .test((t) -> 0)
                                    .test((var t) -> {int j = 0;})
                                    .R( test(t -> 0)
                                       .test((t) -> 0)
                                       .R(int i),
                                        test((var t1) -> {
                                           final class Object {
                                               private void test() {
                                                   for (int i = 0; i < 10; i++) {}
                                               }
                                           }
                                       })
                                       .test((var t1, var t2) -> {})
                                       .R(int i),
                                        other.pack.Rec(var v)
                                      )
                                   """,
                                   CaseExpressionType.PATTERN);

        test.caseDisambiguationTest("int _ when 0 == 0",
                                    CaseExpressionType.PATTERN);
        test.caseDisambiguationTest("Box<String>()",
                                    CaseExpressionType.PATTERN);
        test.caseDisambiguationTest("Box<String, Integer> b",
                                    CaseExpressionType.PATTERN);
        test.caseDisambiguationTest("Box<String, Integer>()",
                                    CaseExpressionType.PATTERN);

        //TODO: in JDK 24 parsed as two expression:
//        test.disambiguationTest("a < b, b > a",
//                                 ExpressionType.EXPRESSION);

        test.ifDisambiguationTest("byte[]", InstanceOfType.TYPE);
        test.ifDisambiguationTest("String str", InstanceOfType.PATTERN);
        test.ifDisambiguationTest("String == i.m()", InstanceOfType.TYPE);
        test.ifDisambiguationTest("String != i.m()", InstanceOfType.TYPE);
        test.ifDisambiguationTest("String && i.m()", InstanceOfType.TYPE);
        test.ifDisambiguationTest("String || i.m()", InstanceOfType.TYPE);
        test.ifDisambiguationTest("String str == i.m()", InstanceOfType.PATTERN);
        test.ifDisambiguationTest("String str != i.m()", InstanceOfType.PATTERN);
        test.ifDisambiguationTest("String str && i.m()", InstanceOfType.PATTERN);
        test.ifDisambiguationTest("String str || i.m()", InstanceOfType.PATTERN);
    }

    private final ParserFactory factory;

    public DisambiguatePatterns() {
        Context context = new Context();
        JavacFileManager jfm = new JavacFileManager(context, true, Charset.defaultCharset());
        Options.instance(context).put(Option.PREVIEW, "");
        factory = ParserFactory.instance(context);
    }

    void caseDisambiguationTest(String snippet, CaseExpressionType expectedType) {
        String code = """
                      public class Test {
                          private void test() {
                              switch (null) {
                                  case SNIPPET -> {}
                              }
                          }
                      }
                      """.replace("SNIPPET", snippet);
        JavacParser parser = factory.newParser(code, false, false, false);
        CompilationUnitTree result = parser.parseCompilationUnit();
        ClassTree clazz = (ClassTree) result.getTypeDecls().get(0);
        MethodTree method = (MethodTree) clazz.getMembers().get(0);
        SwitchTree st = (SwitchTree) method.getBody().getStatements().get(0);
        CaseLabelTree label = st.getCases().get(0).getLabels().get(0);
        CaseExpressionType actualType = switch (label) {
            case ConstantCaseLabelTree et -> CaseExpressionType.EXPRESSION;
            case PatternCaseLabelTree pt -> CaseExpressionType.PATTERN;
            default -> throw new AssertionError("Unexpected result: " + result);
        };
        if (expectedType != actualType) {
            throw new AssertionError("Expected: " + expectedType + ", actual: " + actualType +
                                      ", for: " + code + ", parsed: " + result);
        }
    }

    enum CaseExpressionType {
        PATTERN,
        EXPRESSION;
    }

    void ifDisambiguationTest(String snippet, InstanceOfType expectedType) {
        String code = """
                      public class Test {
                          private void test(Object o) {
                              if (o instanceof SNIPPET) {
                              }
                          }
                      }
                      """.replace("SNIPPET", snippet);
        JavacParser parser = factory.newParser(code, false, false, false);
        CompilationUnitTree result = parser.parseCompilationUnit();
        ClassTree clazz = (ClassTree) result.getTypeDecls().get(0);
        MethodTree method = (MethodTree) clazz.getMembers().get(0);
        IfTree it = (IfTree) method.getBody().getStatements().get(0);
        InstanceOfType[] actualType = new InstanceOfType[1];
        new TreeScanner<Void, Void>() {
            @Override
            public Void visitInstanceOf(InstanceOfTree node, Void p) {
                if (actualType[0] != null) {
                    throw new AssertionError("More than one instanceof seen!");
                }
                if (node.getPattern() instanceof PatternTree) {
                    actualType[0] = InstanceOfType.PATTERN;
                } else {
                    actualType[0] = InstanceOfType.TYPE;
                }
                return super.visitInstanceOf(node, p);
            }
        }.scan((ParenthesizedTree) it.getCondition(), null);
        if (expectedType != actualType[0]) {
            throw new AssertionError("Expected: " + expectedType + ", actual: " + actualType[0] +
                                      ", for: " + code + ", parsed: " + result);
        }
    }

    enum InstanceOfType {
        PATTERN,
        TYPE
    }
}
