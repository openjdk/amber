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

/**
 * @test
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @enablePreview
 * @build toolbox.ToolBox toolbox.JavacTask toolbox.JavaTask
 * @run main MatchFail
 */

import toolbox.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import toolbox.Task.Expect;

public class MatchFail extends TestRunner {
    private ToolBox tb;
    private static final String SOURCE_VERSION = System.getProperty("java.specification.version");

    MatchFail() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String... args) throws Exception {
        new MatchFail().runTests();
    }

    public void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    @Test
    public void testPartialDeconstructorWorks(Path base) throws Exception {
        Path current = base.resolve(".");
        Path src = current.resolve("src");
        Path classes = current.resolve("classes");

        tb.writeJavaFiles(src,
                          """
                          package test;
                          public class Test {

                              private final String content;
                              private final boolean fail;

                              public Test(String content, boolean fail) {
                                  this.content = content;
                                  this.fail = fail;
                              }

                              public static void main(String... args) {
                                  Object o;

                                  o = new Test("hello", false);

                                  if (!(o instanceof Test(var content)) || !"hello".equals(content)) {
                                      throw new IllegalStateException();
                                  }

                                  switch (o) {
                                      case Test(var c) -> {
                                          if (!"hello".equals(c)) {
                                              throw new IllegalStateException();
                                          }
                                      }
                                      default -> throw new IllegalStateException();
                                  }

                                  o = new Test("hello", true);

                                  if (o instanceof Test(_)) {
                                      throw new IllegalStateException();
                                  }

                                  switch (o) {
                                      case Test(_) ->
                                              throw new IllegalStateException();
                                      default -> {}
                                  }
                                  System.out.println("OK");
                              }

                              public case pattern Test(String content) {
                                  if (fail) {
                                      match-fail();
                                  } else {
                                      match Test(content);
                                  }
                              }
                          }
                          """);

        Files.createDirectories(classes);

        new JavacTask(tb)
            .options("--enable-preview", "--release", SOURCE_VERSION)
            .outdir(classes)
            .files(tb.findJavaFiles(src))
            .run()
            .writeAll();

        String out = new JavaTask(tb)
            .vmOptions("--enable-preview")
            .classpath(classes.toString())
            .className("test.Test")
            .run()
            .writeAll()
            .getOutput(Task.OutputKind.STDOUT);

        String expectedOut = "OK" + System.getProperty("line.separator");

        if (!Objects.equals(out, expectedOut)) {
            throw new AssertionError("Unexpected output, expected: " + expectedOut +
                                     ", got: " + out);
        }
    }

    @Test
    public void testErrors(Path base) throws Exception {
        Path current = base.resolve(".");
        Path src = current.resolve("src");
        Path classes = current.resolve("classes");

        tb.writeJavaFiles(src,
                          """
                          package test;
                          public class Test {
                              public pattern Test(String s) {
                              }

                              public pattern Test(Integer i) {
                                  match-fail();
                              }
                          }
                          """);

        Files.createDirectories(classes);

        List<String> actual = new JavacTask(tb)
            .options("--enable-preview", "--release", SOURCE_VERSION,
                     "-XDrawDiagnostics", "-XDshould-stop.at=FLOW")
            .outdir(classes)
            .files(tb.findJavaFiles(src))
            .run(Expect.FAIL)
            .writeAll()
            .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expected = List.of(
            "Test.java:7:9: compiler.err.unmarked.partial.deconstructor",
            "Test.java:4:5: compiler.err.missing.match.stmt",
            "- compiler.note.preview.filename: Test.java, DEFAULT",
            "- compiler.note.preview.recompile",
            "2 errors"
        );

        if (!Objects.equals(actual, expected)) {
            throw new AssertionError("Unexpected errors, expected: " + expected +
                                     ", actual: " + actual);
        }
    }

    @Test
    public void testTotalPartialExhaustiveness(Path base) throws Exception {
        Path current = base.resolve(".");
        Path lib = current.resolve("lib");

        Path libSrc = lib.resolve("src");
        Path libClasses = lib.resolve("classes");

        tb.writeJavaFiles(libSrc,
                          """
                          package lib;
                          public class Lib {
                              private final Object content;

                              public Lib(Object content) {
                                  this.content = content;
                              }

                              public case pattern Lib(String s) {
                                  if (content instanceof String s) {
                                      match Lib(s);
                                  } else {
                                      match-fail();
                                  }
                              }

                              public pattern Lib(Object content) {
                                  match Lib(content);
                              }
                          }
                          """);

        Files.createDirectories(libClasses);

        new JavacTask(tb)
            .options("--enable-preview", "--release", SOURCE_VERSION)
            .outdir(libClasses)
            .files(tb.findJavaFiles(libSrc))
            .run()
            .writeAll()
            .getOutput(Task.OutputKind.DIRECT);

        Path test = current.resolve("test");
        Path src = test.resolve("src");
        Path classes = test.resolve("classes");

        tb.writeJavaFiles(src,
                          """
                          package test;
                          import lib.Lib;
                          public class Test {

                              public static void main(String... args) {
                                  Lib l = new Lib("");
                                  switch (l) {//exhaustive
                                      case Lib(Object o) -> {}
                                  }
                                  switch (l) {//not exhaustive
                                      case Lib(String s) -> {}
                                  }
                              }
                          }
                          """);

        Files.createDirectories(classes);

        List<String> actual = new JavacTask(tb)
            .options("--enable-preview", "--release", SOURCE_VERSION,
                     "-XDrawDiagnostics")
            .classpath(libClasses)
            .outdir(classes)
            .files(tb.findJavaFiles(src))
            .run(Expect.FAIL)
            .writeAll()
            .getOutputLines(Task.OutputKind.DIRECT);


        List<String> expected = List.of(
            "Test.java:10:9: compiler.err.not.exhaustive.statement",
            "1 error"
        );

        if (!Objects.equals(actual, expected)) {
            throw new AssertionError("Unexpected errors, expected: " + expected +
                                     ", actual: " + actual);
        }
    }
}
