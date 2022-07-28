/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Check expected translation of various pattern related constructs
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.comp
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main TranslationTest
*/

import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.comp.TransPatterns;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCase;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Context.Factory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import toolbox.TestRunner;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

public class TranslationTest extends TestRunner {

    private static final String JAVA_VERSION = System.getProperty("java.specification.version");

    ToolBox tb;

    public static void main(String... args) throws Exception {
        new TranslationTest().runTests();
    }

    TranslationTest() {
        super(System.err);
        tb = new ToolBox();
    }

    public void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    @Test
    public void testMultiComponent(Path base) throws Exception {
        doTest(base,
               new String[]{"""
                            package lib;
                            public record Pair(Object o1, Object o2) {}
                            """,
                            """
                            package lib;
                            public record Triplet(Object o1, Object o2, Object o3) {}
                            """},
               """
               package test;
               import lib.*;
               public class Test {
                   private int test(Object obj) {
                       return switch (obj) {
                           case Pair(String c1, Pair(String o2, String s2)) -> 0;
                           case Pair(String c1, Pair(String o2, Integer s2)) -> 0;
                           case Pair(String c1, Pair(Integer o2, String s2)) -> 0;
                           case Pair(String c1, Pair(Integer o2, Integer s2)) -> 0;

                           case Pair(Integer c1, Pair(String o2, String s2)) -> 0;
                           case Pair(Integer c1, Pair(String o2, Integer s2)) -> 0;
                           case Pair(Integer c1, Pair(Integer o2, String s2)) -> 0;
                           case Pair(Integer c1, Pair(Integer o2, Integer s2)) -> 0;

                           default -> -1;
                       };
                   }
               }
               """,
               new ProcessedCasesValidator() {
                   int invocation;
                   @Override
                   public void validateProcessedCases(int depth, List<JCCase> cases) {
                       if (depth != 0) return ;
                       if (invocation++ > 0) return ;
                       if (cases.size() != 2) throw new AssertionError("Unexpected number of cases: " + cases);
                   }
               });
    }

    private void doTest(Path base, String[] libraryCode, String testCode, Validator validator) throws IOException {
        Path current = base.resolve(".");
        Path libClasses = current.resolve("libClasses");

        Files.createDirectories(libClasses);

        if (libraryCode.length != 0) {
            Path libSrc = current.resolve("lib-src");

            for (String code : libraryCode) {
                tb.writeJavaFiles(libSrc, code);
            }

            new JavacTask(tb)
                    .options("--enable-preview",
                             "-source", JAVA_VERSION)
                    .outdir(libClasses)
                    .files(tb.findJavaFiles(libSrc))
                    .run();
        }

        Path src = current.resolve("src");
        tb.writeJavaFiles(src, testCode);

        Path classes = current.resolve("libClasses");

        Files.createDirectories(libClasses);

        List<Throwable> failures = new ArrayList<>();

        new JavacTask(tb)
            .options("--enable-preview",
                     "-source", JAVA_VERSION,
                     "-Xlint:-preview",
                     "--class-path", libClasses.toString(),
                     "-XDshould-stop.at=FLOW")
            .outdir(classes)
            .files(tb.findJavaFiles(src))
            .callback(task -> {
                 Context ctx = ((JavacTaskImpl) task).getContext();

                 TestTransPatterns.preRegister(ctx, validator, failures);
             })
            .run()
            .writeAll()
            .getOutputLines(Task.OutputKind.DIRECT);

        if (!failures.isEmpty()) {
            var failure = new AssertionError("Testcase failed.");
            failures.forEach(t -> failure.addSuppressed(t));
            throw failure;
        }
    }

    public interface Validator {
        public default void validateProcessedCases(int depth, List<JCCase> cases) {}
    }

    public interface ProcessedCasesValidator extends Validator {
        public void validateProcessedCases(int depth, List<JCCase> cases);
    }

    private static final class TestTransPatterns extends TransPatterns {

        public static void preRegister(Context ctx, Validator validator, List<Throwable> failures) {
            ctx.put(transPatternsKey, (Factory<TransPatterns>) c -> new TestTransPatterns(c, validator, failures));
        }

        private final Validator validator;
        private final List<Throwable> failures;

        public TestTransPatterns(Context context, Validator validator, List<Throwable> failures) {
            super(context);
            this.validator = validator;
            this.failures = failures;
        }

        int processCasesDepth;

        @Override
        protected com.sun.tools.javac.util.List<JCTree.JCCase> processCases(
                JCTree currentSwitch, com.sun.tools.javac.util.List<JCTree.JCCase> inputCases) {
            processCasesDepth++;
            var result = super.processCases(currentSwitch, inputCases);
            try {
                validator.validateProcessedCases(--processCasesDepth, result);
            } catch (Throwable t) {
                failures.add(t);
            }
            return result;
        }

    }
}
