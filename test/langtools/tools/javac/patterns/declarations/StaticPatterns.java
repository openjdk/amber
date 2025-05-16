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
 * @summary Add static patterns
 * @library /tools/lib
 *          /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.TestRunner toolbox.ToolBox StaticPatterns
 * @run main StaticPatterns
 */

import toolbox.*;

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


//TODO: partiality/totality
//TODO: parsing of patterns with split receiver and match candidate: the receiver may be an arbitrary expression(?), this does not work currently!
public class StaticPatterns extends TestRunner {

    public static void main(String... args) throws Exception {
        new StaticPatterns().runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    private final ToolBox tb = new ToolBox();

    public StaticPatterns() {
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

                              public static pattern Test named(int i) {
                                  if (!that.named) {
                                      match-fail();
                                  }
                                  match named(1);
                              }
                              public static pattern Test unnamed(int i) {
                                  if (that.named) {
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

    private static void assertTrue(String message, boolean cond) {
        if (!cond) {
            throw new AssertionError("Unexpected false, expected true. Message: " + message);
        }
    }
}
