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
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.PatternCaseLabelTree;
import com.sun.source.tree.SwitchTree;
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
        test.disambiguationTest("String s",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("String s when s.isEmpty()",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("String s",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("@Ann String s",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("String s",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("(String) s",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("((String) s)",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("((0x1))",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("a | b",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("a || b",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("a & b",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("a && b",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("a < b",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("a > b",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("a >> b",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("a >>> b",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("a < b | a > b",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("a << b | a >> b",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("a << b || a < b | a >>> b",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("(a > b)",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("(a >> b)",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("(a >>> b)",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("(a < b | a > b)",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("(a << b | a >> b)",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("(a << b || a < b | a >>> b)",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("a < c > b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a < c.d > b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a<? extends c.d> b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("@Ann a<? extends c.d> b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a<? extends @Ann c.d> b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a<? super c.d> b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a<? super @Ann c.d> b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a<b<c.d>> b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a<b<@Ann c.d>> b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a<b<c<d>>> b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a[] b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a[][] b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("int i",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("int[] i",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a[a]",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("a[b][c]",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("a & b",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("R r when (x > 0)",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("R(int x) when (x > 0)",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("R(int x) when foo.x() > 0",
                                 ExpressionType.PATTERN);

        test.disambiguationTest("test().R()",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("test().R(var v)",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("test().R(int v)",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("test().R(int _)",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("test().R(_)",
                                 ExpressionType.PATTERN);

        test.disambiguationTest("test().R(test().R(_))",
                                 ExpressionType.PATTERN);

        //TODO: error recovery, can be ignored if needed
        //expressions with method invocations can never be constant expessions,
        //and hence would fail later during attribution anyway:
//        test.disambiguationTest("test().R(test().R(1))",
//                                 ExpressionType.EXPRESSION);

        test.disambiguationTest("test().test().test().R(test().test().R(int i), test().test().R(int i), other.pack.Rec(var v))",
                                 ExpressionType.PATTERN);

        test.disambiguationTest("""
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
                                 ExpressionType.PATTERN);

        test.disambiguationTest("int _ when 0 == 0",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("Box<String>()",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("Box<String, Integer> b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("Box<String, Integer>()",
                                 ExpressionType.PATTERN);

        //TODO: in JDK 24 parsed as two expression:
//        test.disambiguationTest("a < b, b > a",
//                                 ExpressionType.EXPRESSION);
    }

    private final ParserFactory factory;

    public DisambiguatePatterns() {
        Context context = new Context();
        JavacFileManager jfm = new JavacFileManager(context, true, Charset.defaultCharset());
        Options.instance(context).put(Option.PREVIEW, "");
        factory = ParserFactory.instance(context);
    }

    void disambiguationTest(String snippet, ExpressionType expectedType) {
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
        ExpressionType actualType = switch (label) {
            case ConstantCaseLabelTree et -> ExpressionType.EXPRESSION;
            case PatternCaseLabelTree pt -> ExpressionType.PATTERN;
            default -> throw new AssertionError("Unexpected result: " + result);
        };
        if (expectedType != actualType) {
            throw new AssertionError("Expected: " + expectedType + ", actual: " + actualType +
                                      ", for: " + code + ", parsed: " + result);
        }
    }

    enum ExpressionType {
        PATTERN,
        EXPRESSION;
    }

}
