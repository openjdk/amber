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

/**
 * @test
 * @summary Verify that annotation processing works with patterns.
 * @library /tools/lib
 *          /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.TestRunner toolbox.ToolBox InstancePatterns
 * @run main InstancePatterns
 */

import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.attribute.PatternAttribute;
import java.lang.reflect.AccessFlag;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import toolbox.JavaTask;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.TestRunner;
import toolbox.TestRunner.Test;
import toolbox.ToolBox;


//TODO: partiality/totality
//TODO: parsing of patterns with split receiver and match candidate: the receiver may be an arbitrary expression(?), this does not work currently!
public class InstancePatterns extends TestRunner {

    public static void main(String... args) throws Exception {
        new InstancePatterns().runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    private final ToolBox tb = new ToolBox();

    public InstancePatterns() {
        super(System.err);
    }

    @Test
    public void testQualifiedInstance(Path outerBase) throws Exception {
        Path src = outerBase.resolve("src");
        tb.writeJavaFiles(src,
                          """
                          public class Test {
                              private final boolean named;
                              public Test(boolean named) {
                                  this.named = named;
                              }
                              public pattern Test named(int i) {
                                  if (!named) {
                                      match-fail();
                                  }
                                  match named(1);
                              }
                              public pattern Test unnamed(int i) {
                                  if (named) {
                                      match-fail();
                                  }
                                  match unnamed(0);
                              }
                              public static void main(String... args) {
                                  for (Object o : new Object[] {new Test(true),
                                                                new Test(false)}) {
                                    switch (o) {
                                        case Test.named(var i) ->
                                            System.out.println("named: " + i);
                                        case Test.unnamed(var i) ->
                                            System.out.println("unnamed: " + i);
                                        default ->
                                            throw new AssertionError("Unexpected!");
                                    }
                                  }
                              }
                          }
                          """);

        Path classes = outerBase.resolve("classes");
        Files.createDirectories(classes);

        new JavacTask(tb)
            .options("--enable-preview", "--source", System.getProperty("java.specification.version"))
            .outdir(classes.toString())
            .files(tb.findJavaFiles(src))
            .run()
            .writeAll();

        List<String> actualOutput = new JavaTask(tb)
            .vmOptions("--enable-preview", "--class-path", classes.toString())
            .className("Test")
            .run()
            .getOutputLines(Task.OutputKind.STDOUT);
        List<String> expectedOutput = List.of(
            "named: 1",
            "unnamed: 0"
        );

        if (!expectedOutput.equals(actualOutput)) {
            throw new AssertionError("Expected: " + expectedOutput +
                                     ", but got: " + actualOutput);
        }
        ClassFile cf = ClassFile.of();
        ClassModel model = cf.parse(classes.resolve("Test.class"));
        MethodModel pattern = model.methods().stream().filter(m -> m.methodName().equalsString("named:I")).findAny().orElseThrow();
        Optional<PatternAttribute> patternAttribute = pattern.findAttribute(Attributes.pattern());
        assertTrue("expect not a deconstructor",
                   !patternAttribute.orElseThrow().patternFlags().contains(AccessFlag.DECONSTRUCTOR));
    }

    @Test
    public void testUnqualifiedInstance(Path outerBase) throws Exception {
        Path src = outerBase.resolve("src");
        tb.writeJavaFiles(src,
                          """
                          public class Test {
                              private final boolean named;
                              public Test(boolean named) {
                                  this.named = named;
                              }
                              public pattern Test named(int i) {
                                  if (!named) {
                                      match-fail();
                                  }
                                  match named(1);
                              }
                              public pattern Test unnamed(int i) {
                                  if (named) {
                                      match-fail();
                                  }
                                  match unnamed(0);
                              }
                              public static void main(String... args) {
                                  for (Test t : new Test[] {new Test(true),
                                                            new Test(false)}) {
                                    switch (t) {
                                        case named(var i) ->
                                            System.out.println("named: " + i);
                                        case unnamed(var i) ->
                                            System.out.println("unnamed: " + i);
                                        default ->
                                            throw new AssertionError("Unexpected!");
                                    }
                                  }
                              }
                          }
                          """);

        Path classes = outerBase.resolve("classes");
        Files.createDirectories(classes);

        new JavacTask(tb)
            .options("--enable-preview", "--source", System.getProperty("java.specification.version"))
            .outdir(classes.toString())
            .files(tb.findJavaFiles(src))
            .run()
            .writeAll();

        List<String> actualOutput = new JavaTask(tb)
            .vmOptions("--enable-preview", "--class-path", classes.toString())
            .className("Test")
            .run()
            .getOutputLines(Task.OutputKind.STDOUT);
        List<String> expectedOutput = List.of(
            "named: 1",
            "unnamed: 0"
        );

        if (!expectedOutput.equals(actualOutput)) {
            throw new AssertionError("Expected: " + expectedOutput +
                                     ", but got: " + actualOutput);
        }
        ClassFile cf = ClassFile.of();
        ClassModel model = cf.parse(classes.resolve("Test.class"));
        MethodModel pattern = model.methods().stream().filter(m -> m.methodName().equalsString("named:I")).findAny().orElseThrow();
        Optional<PatternAttribute> patternAttribute = pattern.findAttribute(Attributes.pattern());
        assertTrue("expect not a deconstructor",
                   !patternAttribute.orElseThrow().patternFlags().contains(AccessFlag.DECONSTRUCTOR));
    }

    @Test
    public void testSeparateReceivedMatchCandidate(Path outerBase) throws Exception {
        Path src = outerBase.resolve("src");
        //XXX: the receiver and match candidate types must be different so that the test is more powerful(!)
        tb.writeJavaFiles(src,
                          """
                          public class Test {
                              private final boolean named;
                              public Test(boolean named) {
                                  this.named = named;
                              }
                              public pattern MatchCandidate do_match(int i, int j) {
                                  match do_match(named ? 1 : 0, that.named ? 1 : 0);
                              }
                              public static void main(String... args) {
                                  for (MatchCandidate t1 : new MatchCandidate[] {new MatchCandidate(true),
                                                                                 new MatchCandidate(false)}) {
                                    for (Test t2 : new Test[] {new Test(true),
                                                               new Test(false)}) {
                                        switch (t1) {
                                            case t2.do_match(var i, var j) ->
                                                System.out.println("do_match: " + i + ", " + j);
                                            default ->
                                                throw new AssertionError("Unexpected!");
                                        }
                                    }
                                  }
                                  Object objectSelector = new MatchCandidate(true);
                                  Test receiver = new Test(false);
                                  switch (objectSelector) {
                                      case receiver.do_match(var i, var j) ->
                                        System.out.println("against Object: do_match: " + i + ", " + j);
                                      default -> throw new AssertionError("Unexpected!");
                                  }
                              }
                              public static class MatchCandidate {
                                  private final boolean named;
                                  public MatchCandidate(boolean named) {
                                      this.named = named;
                                  }
                              }
                          }
                          """);

        Path classes = outerBase.resolve("classes");
        Files.createDirectories(classes);

        new JavacTask(tb)
            .options("--enable-preview", "--source", System.getProperty("java.specification.version"))
            .outdir(classes.toString())
            .files(tb.findJavaFiles(src))
            .run()
            .writeAll();

        List<String> actualOutput = new JavaTask(tb)
            .vmOptions("--enable-preview", "--class-path", classes.toString())
            .className("Test")
            .run()
            .getOutputLines(Task.OutputKind.STDOUT);
        List<String> expectedOutput = List.of(
            "do_match: 1, 1",
            "do_match: 0, 1",
            "do_match: 1, 0",
            "do_match: 0, 0",
            "against Object: do_match: 0, 1"
        );

        if (!expectedOutput.equals(actualOutput)) {
            throw new AssertionError("Expected: " + expectedOutput +
                                     ", but got: " + actualOutput);
        }
        ClassFile cf = ClassFile.of();
        ClassModel model = cf.parse(classes.resolve("Test.class"));
        MethodModel pattern = model.methods().stream().filter(m -> m.methodName().equalsString("do_match:I:I")).findAny().orElseThrow();
        Optional<PatternAttribute> patternAttribute = pattern.findAttribute(Attributes.pattern());
        assertTrue("expect not a deconstructor",
                   !patternAttribute.orElseThrow().patternFlags().contains(AccessFlag.DECONSTRUCTOR));
    }

    private static void assertTrue(String message, boolean cond) {
        if (!cond) {
            throw new AssertionError("Unexpected false, expected true. Message: " + message);
        }
    }
}
