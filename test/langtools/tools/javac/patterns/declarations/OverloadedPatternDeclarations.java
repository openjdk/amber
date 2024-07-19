/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import toolbox.*;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * @test
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 * @enablePreview
 * @build toolbox.ToolBox toolbox.JavapTask
 * @run main OverloadedPatternDeclarations
 */
public class OverloadedPatternDeclarations extends TestRunner {
    private ToolBox tb;
    private static final String SOURCE_VERSION = System.getProperty("java.specification.version");

    OverloadedPatternDeclarations() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String... args) throws Exception {
        new OverloadedPatternDeclarations().runTests();
    }

    public void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    @Test
    public void testOne(Path base) throws Exception {
        runSingle(base, "A", "B", "B", 2);
        runSingle(base, "B", "C", "B", 1);
        runSingle(base, "B", "C", "A", 0);
        runSingle(base, "A", "B", "C", 0);
        runSingle(base, "A", "C", "B", 0);
        runSingle(base, "A", "D", "B", 0);
        runSingle(base, "E", "F", "I", 0);
    }

    void runSingle(Path base, String bt_1, String bt_2, String t1, Integer selected) throws IOException {
        String source =
                """
                package test;

                class Test {
                    static class A {}
                    static class B extends A {}
                    static class C extends B {}
                    static class D extends C {}
                    sealed interface I {}
                    final static class E implements I {}
                    final static class F implements I {}

                    static class Single {
                        public pattern Single($BINDING_1 b1) {
                            System.out.println(1);
                            match Single(new $BINDING_1());
                        }

                        public pattern Single($BINDING_2 b1) {
                            System.out.println(2);
                            match Single(new $BINDING_2());
                        }
                    }

                    public static void main(String[] args) {
                        Single t = new Single();
                        switch(t) {
                            case Single($TYPE1 t1) -> {}
                            default -> {}
                        };
                    }
                }
                """;

        source = source.replaceAll("\\$BINDING_1", bt_1)
                .replaceAll("\\$BINDING_2", bt_2)
                .replaceAll("\\$TYPE1", t1);

        compileAndRun(base, selected, source);
    }

    @Test
    public void testTwo(Path base) throws Exception {
        runTwo(base, "A", "B", "B", "B", "A", "A", 1);
        runTwo(base, "A", "B", "B", "A", "B", "B", 0);
    }

    void runTwo(Path base, String bt1_1, String bt2_1, String bt1_2, String bt2_2, String t1, String t2, Integer selected) throws IOException {
        String source =
            """
            package test;

            class Test {
                static class A {}
                static class B extends A {}
                static class C extends B {}
                static class D extends C {}
                sealed interface I {}
                final static class E implements I {}
                final static class F implements I {}

                static class Two {
                    public pattern Two($BINDING_TYPE1_1 b1, $BINDING_TYPE2_1 be) {
                        System.out.println(1);
                        match Two(new $BINDING_TYPE1_1(), new $BINDING_TYPE2_1());
                    }

                    public pattern Two($BINDING_TYPE1_2 b1, $BINDING_TYPE2_2 be) {
                        System.out.println(2);
                        match Two(new $BINDING_TYPE1_2(), new $BINDING_TYPE2_2());
                    }
                }

                public static void main(String[] args) {
                    Two t = new Two();
                    switch(t) {
                        case Two($TYPE1 t1, $TYPE2 t2) -> {}
                        default -> {}
                    };
                }
            }
            """;

        source = source.replaceAll("\\$BINDING_TYPE1_1", bt1_1)
                .replaceAll("\\$BINDING_TYPE2_1", bt2_1)
                .replaceAll("\\$BINDING_TYPE1_2", bt1_2)
                .replaceAll("\\$BINDING_TYPE2_2", bt2_2)
                .replaceAll("\\$TYPE1", t1)
                .replaceAll("\\$TYPE2", t2);

        compileAndRun(base, selected, source);
    }

    private void compileAndRun(Path base, Integer selected, String source) throws IOException {
        Path current = base.resolve(".");
        Path src = current.resolve("src");
        Path classes = current.resolve("classes");

        tb.writeJavaFiles(src, source);

        Files.createDirectories(classes);

        {
            String javacOut = new JavacTask(tb)
                    .options("-XDrawDiagnostics", "--enable-preview", "--release", SOURCE_VERSION)
                    .outdir(classes)
                    .files(tb.findJavaFiles(src))
                    .run(selected == 0 ? Task.Expect.FAIL : Task.Expect.SUCCESS)
                    .writeAll()
                    .getOutput(Task.OutputKind.DIRECT);

            if (selected > 0) {
                String javaOut = new JavaTask(tb)
                        .vmOptions("--enable-preview")
                        .classpath(classes.toString())
                        .className("test.Test")
                        .run()
                        .writeAll()
                        .getOutput(Task.OutputKind.STDOUT);

                if (!javaOut.contains(selected.toString()))
                    throw new AssertionError("Wrong overload resolution:\n" + javaOut);
            }
            else {
                if (!javacOut.contains("compiler.err.matcher.overloading.ambiguity"))
                    throw new AssertionError("Wrong overload resolution:\n" + javacOut);
            }
        }
    }
}
