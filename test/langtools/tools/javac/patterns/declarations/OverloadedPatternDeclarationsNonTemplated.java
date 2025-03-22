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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Gatherers;
import java.util.stream.Stream;

/**
 * @test
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 * @enablePreview
 * @build toolbox.ToolBox toolbox.JavapTask
 * @run main OverloadedPatternDeclarationsNonTemplated
 */
public class OverloadedPatternDeclarationsNonTemplated extends TestRunner {
    private ToolBox tb;
    private static final String SOURCE_VERSION = System.getProperty("java.specification.version");

    OverloadedPatternDeclarationsNonTemplated() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String... args) throws Exception {
        new OverloadedPatternDeclarationsNonTemplated().runTests();
    }

    public void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    @Test
    public void test1(Path base) throws Exception {
        String source =
                """
                package test;

                class Test {
                    public static void main(String... args) {
                         Top t = new Top();
                         switch (t) {
                             case Top(NestedB(A a)) -> {}
                             default -> {}
                         }
                     }

                     static class A {}
                     static class B extends A {}
                     static class C extends B {}

                     static class NestedA {
                         public pattern NestedA(A a) { System.out.println("NestedA1"); match NestedA(new A());}
                         public pattern NestedA(B b) { System.out.println("NestedA2"); match NestedA(new B());}
                         // public pattern NestedA(C c) { System.out.println("NestedA3"); match NestedA(new C());}
                     }
                     static class NestedB extends NestedA {
                         // public pattern NestedB(A a) { System.out.println("NestedB1"); match NestedB(new A());}
                         public pattern NestedB(B b) { System.out.println("NestedB2"); match NestedB(new B());}
                          public pattern NestedB(C c) { System.out.println("NestedB3"); match NestedB(new C());}
                     }
                     static class NestedC extends NestedB {
                         // public pattern NestedC(A a) { System.out.println("NestedC1"); match NestedC(new A());}
                         public pattern NestedC(B b) { System.out.println("NestedC2"); match NestedC(new B());}
                         public pattern NestedC(C c) { System.out.println("NestedC3"); match NestedC(new C());}
                     }

                     static class Top {
                         // public pattern Top(NestedA na) { System.out.println("Top1"); match Top(new NestedA());}
                         // public pattern Top(NestedB nb) { System.out.println("Top2"); match Top(new NestedB());}
                         public pattern Top(NestedC nc) { System.out.println("Top3"); match Top(new NestedC());}
                     }
                }
                """;

        compileAndRun(base, source, """
                Top3
                NestedB2
                """, Task.Expect.SUCCESS);
    }

    @Test
    public void test2(Path base) throws Exception {
        String source =
                """
                package test;

                class Test {
                    public static void main(String... args) {
                          R r = new R();

                          switch (r) {
                              case R(A1 a1) -> {}          // R:A1
                              default -> {}
                          }

                          switch (r) {
                              case R(A1(String s)) -> {}   // R:A1, A1
                              default -> {}
                          }

                      }

                      static class A1 {
                          pattern A1(String s) {
                              System.out.println("A1");
                              match A1("A1");
                          }
                      }

                      static class A2 extends A1 {
                          pattern A2(String s) {
                              System.out.println("A2");
                              match A2("A2");
                          }
                      }

                      static class R {
                          pattern R(A1 a1) {
                              System.out.println("R:A1");
                              match R(new A1());
                          }

                          pattern R(A2 a2) {
                              System.out.println("R:A2");
                              match R(new A2());
                          }
                      }
                }
                """;

        compileAndRun(base, source, """
                R:A1
                R:A1
                A1
                """,
                Task.Expect.SUCCESS);
    }

    @Test
    public void test3(Path base) throws Exception {
        String source =
                """
                package test;

                class Test {
                   public static void main(String... args) {
                       R r = new R();

                       switch (r) {
                           case R(I i, String s) -> {}
                           default -> {}
                       }
                   }

                   sealed interface I {}
                   sealed static interface E extends I {}
                   sealed static interface F extends I {}
                   static final class EF implements E, F {}

                   static class R {
                       pattern R(E e, String s) {
                           System.out.println("R:E");
                           match R(new EF(), "");
                       }

                       pattern R(F f, String s) {
                           System.out.println("R:F");
                           match R(new EF(), "");
                       }

                       pattern R(EF ef, Object o) {
                           System.out.println("R:EF");
                           match R(new EF(), "");
                       }
                   }
                }
                """;

        compileAndRun(base, source,
                """
                        Test.java:8:17: compiler.err.matcher.overloading.ambiguity: test.Test.R
                        - compiler.note.preview.filename: Test.java, DEFAULT
                        - compiler.note.preview.recompile
                        1 error
                        """, Task.Expect.FAIL);
    }

    @Test
    public void test4(Path base) throws Exception {
        String source =
                """
                package test;

                class Test {
                   public static void main(String... args) {
                       JsonValue r = new JsonValue();

                       switch (r) {
                           case JsonValue(short s) -> {}
                           default -> {}
                       }
                   }

                   static class JsonValue {
                        pattern JsonValue(int i)    { System.out.println(1); match JsonValue(1); }
                        pattern JsonValue(long l)   { System.out.println(2); match JsonValue(2); }
                        pattern JsonValue(double d) { System.out.println(3); match JsonValue(3); }
                    }
                }
                """;

        compileAndRun(base, source,
                """
                        1
                        """, Task.Expect.SUCCESS);
    }

    private void compileAndRun(Path base, String source, String expected, Task.Expect compilationExpectation) throws IOException {
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
                    .run(compilationExpectation)
                    .writeAll()
                    .getOutput(Task.OutputKind.DIRECT);

            if (compilationExpectation == Task.Expect.SUCCESS) {
                String javaOut = new JavaTask(tb)
                        .vmOptions("--enable-preview")
                        .classpath(classes.toString())
                        .className("test.Test")
                        .run()
                        .writeAll()
                        .getOutput(Task.OutputKind.STDOUT);

                shouldContainOrdered(javaOut, expected, "Wrong overload pattern execution:\n");
            }
            else {
                shouldContainOrdered(javacOut, expected, "Wrong overload pattern compilation:\n");
            }
        }
    }

    private static void shouldContainOrdered(String expected, String actual, String message) {
        List<String> expectedLines = expected.lines().map(s -> s.strip()).toList();
        Stream<String> actualLines = actual.lines().map(s -> s.strip());

        if (!actualLines.gather(Gatherers.windowSliding(expectedLines.size())).anyMatch(window -> window.equals(expectedLines)))
            throw new AssertionError(message + actual);
    }
}
