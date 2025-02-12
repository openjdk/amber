/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @test
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 * @enablePreview
 * @build toolbox.ToolBox toolbox.JavapTask
 * @run main SeparateCompilation
 */
public class SeparateCompilation extends TestRunner {
    private ToolBox tb;
    private static final String SOURCE_VERSION = System.getProperty("java.specification.version");

    SeparateCompilation() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String... args) throws Exception {
        new SeparateCompilation().runTests();
    }

    public void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    @Test
    public void testOne(Path base) throws Exception {
        Path current = base.resolve(".");
        Path lib = current.resolve("lib");
        Path libSrc = lib.resolve("src");
        Path libClasses = lib.resolve("classes");

        tb.writeJavaFiles(libSrc,
                          """
                          package lib;
                          import java.util.List;
                          public class Lib {
                              public pattern Lib(List<? extends String> values) {
                                  match Lib(null);
                              }
                          }
                          """);

        Files.createDirectories(libClasses);

        new JavacTask(tb)
            .options("-XDrawDiagnostics", "--enable-preview", "--release", SOURCE_VERSION)
            .outdir(libClasses)
            .files(tb.findJavaFiles(libSrc))
            .run()
            .writeAll()
            .getOutput(Task.OutputKind.DIRECT);

        Path test = current.resolve("test");
        Path testSrc = test.resolve("src");
        Path testClasses = test.resolve("classes");

        tb.writeJavaFiles(testSrc,
                          """
                          package test;
                          import java.util.List;
                          import lib.Lib;
                          public class Test {
                              public List<? extends String> convert(Object o) {
                                  return o instanceof Lib(List<? extends String> data) ? data : null;
                              }
                          }
                          """);

        Files.createDirectories(testClasses);

        new JavacTask(tb)
            .options("--enable-preview", "--release", SOURCE_VERSION,
                     "--class-path", libClasses.toString())
            .outdir(testClasses)
            .files(tb.findJavaFiles(testSrc))
            .run()
            .writeAll()
            .getOutput(Task.OutputKind.DIRECT);
    }

    @Test
    public void testIncompatibleChange(Path base) throws Exception {
        Path current = base.resolve(".");
        Path lib = current.resolve("lib");
        Path libSrc = lib.resolve("src");
        Path libClasses = lib.resolve("classes");

        tb.writeJavaFiles(libSrc,
                """
                package lib;
                import java.util.List;
                public class Lib {
                    public pattern Lib(int a, int b) {
                        match Lib(1, 2);
                    }
                }
                """);

        Files.createDirectories(libClasses);

        new JavacTask(tb)
                .options("-XDrawDiagnostics", "--enable-preview", "--release", SOURCE_VERSION)
                .outdir(libClasses)
                .files(tb.findJavaFiles(libSrc))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        Path test = current.resolve("test");
        Path testSrc = test.resolve("src");
        Path testClasses = test.resolve("classes");

        tb.writeJavaFiles(testSrc,
                """
                package test;
                import java.util.List;
                import lib.Lib;

                public class Test {
                    public static void main(String... args) {
                           Lib l = new Lib();

                           switch (l) {
                               case Lib(int x, int y) -> { System.out.println(x + y); }
                               default -> {}
                           }
                    }
                }
                """);

        Files.createDirectories(testClasses);

        new JavacTask(tb)
                .options("-XDrawDiagnostics", "--enable-preview", "--release", SOURCE_VERSION)
                .classpath(libClasses)
                .outdir(testClasses)
                .files(tb.findJavaFiles(testSrc))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        String javaOut = new JavaTask(tb)
                .vmOptions("--enable-preview")
                .classpath(testClasses.toString() + System.getProperty("path.separator") + libClasses.toString())
                .className("test.Test")
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.STDOUT);

        // edit Lib
        tb.writeJavaFiles(libSrc,
                """
                package lib;
                import java.util.List;
                public class Lib {
                    public pattern Lib(int a) {
                        match Lib(1);
                    }
                }
                """);

        new JavacTask(tb)
                .options("-XDrawDiagnostics", "--enable-preview", "--release", SOURCE_VERSION)
                .outdir(libClasses)
                .files(tb.findJavaFiles(libSrc))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        String javaOut2 = new JavaTask(tb)
                .vmOptions("--enable-preview")
                .classpath(testClasses.toString() + System.getProperty("path.separator") + libClasses.toString())
                .className("test.Test")
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.STDOUT);
    }

    @Test
    public void testGenericClass(Path base) throws Exception {
        Path current = base.resolve(".");
        Path lib = current.resolve("lib");
        Path libSrc = lib.resolve("src");
        Path libClasses = lib.resolve("classes");

        tb.writeJavaFiles(libSrc,
                """
                package lib;
                public class Box<T extends Integer> {
                     public pattern Box(GBox<T> o) {
                         match Box(new GBox<T>());
                     }

                     public static class GBox<T> {}
                 }
                """);

        Files.createDirectories(libClasses);

        new JavacTask(tb)
                .options("-XDrawDiagnostics", "--enable-preview", "--release", SOURCE_VERSION)
                .outdir(libClasses)
                .files(tb.findJavaFiles(libSrc))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        Path test = current.resolve("test");
        Path testSrc = test.resolve("src");
        Path testClasses = test.resolve("classes");

        tb.writeJavaFiles(testSrc,
                """
                package test;
                import java.util.List;
                import lib.Box;
                public class Test {
                    public static void main(String... args) {
                        Box<Integer> l = new Box<>();

                        switch (l) {
                            case Box(Box.GBox<Integer> ll) -> {  }
                            default -> {}
                        }
                    }
                }
                """);

        Files.createDirectories(testClasses);

        new JavacTask(tb)
                .options("-XDrawDiagnostics", "--enable-preview", "--release", SOURCE_VERSION)
                .classpath(libClasses)
                .outdir(testClasses)
                .files(tb.findJavaFiles(testSrc))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        String javaOut = new JavaTask(tb)
                .vmOptions("--enable-preview")
                .classpath(testClasses.toString() + System.getProperty("path.separator") + libClasses.toString())
                .className("test.Test")
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.STDOUT);
    }

    @Test
    public void testGenericRecord(Path base) throws Exception {
        Path current = base.resolve(".");
        Path lib = current.resolve("lib");
        Path libSrc = lib.resolve("src");
        Path libClasses = lib.resolve("classes");

        tb.writeJavaFiles(libSrc,
                """
                package lib;
                public record Box<T> (T data){ }
                """);

        Files.createDirectories(libClasses);

        new JavacTask(tb)
                .options("-XDrawDiagnostics", "--enable-preview", "--release", SOURCE_VERSION)
                .outdir(libClasses)
                .files(tb.findJavaFiles(libSrc))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        Path test = current.resolve("test");
        Path testSrc = test.resolve("src");
        Path testClasses = test.resolve("classes");

        tb.writeJavaFiles(testSrc,
                """
                package test;
                import java.util.List;
                import lib.Box;

                public class Test {
                    public static void main(String... args) {
                           Box<Integer> l = new Box<>(42);

                           switch (l) {
                               case Box<Integer>(Integer i) -> { System.out.println(i); }
                               default -> {}
                           }
                    }
                }
                """);

        Files.createDirectories(testClasses);

        new JavacTask(tb)
                .options("-XDrawDiagnostics", "--enable-preview", "--release", SOURCE_VERSION)
                .classpath(libClasses)
                .outdir(testClasses)
                .files(tb.findJavaFiles(testSrc))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        String javaOut = new JavaTask(tb)
                .vmOptions("--enable-preview")
                .classpath(testClasses.toString() + System.getProperty("path.separator") + libClasses.toString())
                .className("test.Test")
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.STDOUT);
    }
}
