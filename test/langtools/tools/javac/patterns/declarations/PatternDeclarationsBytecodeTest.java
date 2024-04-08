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

/*
 * @test
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox toolbox.JavapTask
 * @run main PatternDeclarationsBytecodeTest
 */

import toolbox.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class PatternDeclarationsBytecodeTest extends TestRunner  {
    private ToolBox tb;
    private static final String SOURCE_VERSION = System.getProperty("java.specification.version");

    public static void main(String... args) throws Exception {
        new PatternDeclarationsBytecodeTest().runTests();
    }

    PatternDeclarationsBytecodeTest() {
        super(System.err);
        tb = new ToolBox();
    }

    public void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    @Test
    public void testMethodSignature(Path base) throws Exception {
        Path current = base.resolve(".");
        Path src = current.resolve("src");
        Path classes = current.resolve("classes");
        tb.writeJavaFiles(src,
                """
                package test;
                public class Test {
                     private final String name = "";
                     private final String username = "";

                     public pattern Test(String name, String username) {
                          match Test(this.name, this.username);
                     }
                 }
                 """);

        Files.createDirectories(classes);

        {
            new JavacTask(tb)
                    .options("--enable-preview", "--release", SOURCE_VERSION)
                    .outdir(classes)
                    .files(tb.findJavaFiles(src))
                    .run(Task.Expect.SUCCESS)
                    .writeAll();

            String javapOut = new JavapTask(tb)
                    .options("-v")
                    .classpath(classes.toString())
                    .classes("test.Test")
                    .run()
                    .getOutput(Task.OutputKind.DIRECT);

            if (!javapOut.contains("public static java.lang.Object Test\\%Ljava\\|lang\\|String\\?\\%Ljava\\|lang\\|String\\?(test.Test);"))
                throw new AssertionError("Wrongly generated signature of pattern declaration");
        }

    }

    @Test
    public void testPatternAttribute(Path base) throws Exception {
        Path current = base.resolve(".");
        Path src = current.resolve("src");
        Path classes = current.resolve("classes");
        tb.writeJavaFiles(src,
                """
                package test;
                public class Test {
                     private final String name = "";
                     private final String username = "";

                     public pattern Test(String name, String username) {
                          match Test(this.name, this.username);
                     }
                 }
                 """);

        Files.createDirectories(classes);

        {
            new JavacTask(tb)
                    .options("--enable-preview", "--release", SOURCE_VERSION)
                    .outdir(classes)
                    .files(tb.findJavaFiles(src))
                    .run(Task.Expect.SUCCESS)
                    .writeAll();

            String javapOut = new JavapTask(tb)
                    .options("-v")
                    .classpath(classes.toString())
                    .classes("test.Test")
                    .run()
                    .getOutput(Task.OutputKind.DIRECT);

            String[] outputs = {
                    "pattern_name: Test",
                    "pattern_flags: deconstructor",
                    "pattern_type: (Ljava/lang/String;Ljava/lang/String;)V"
            };
            if (!Arrays.stream(outputs).allMatch(o -> javapOut.contains(o)))
                throw new AssertionError("Wrongly generated basic structure of Pattern attribute");
        }
    }

    @Test
    public void testBindingAnnotationsInPatternAttribute(Path base) throws Exception {
        Path current = base.resolve(".");
        Path src = current.resolve("src");
        Path classes = current.resolve("classes");
        tb.writeJavaFiles(src,
                """
                package test;
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Retention;
                import java.lang.annotation.RetentionPolicy;
                import java.lang.annotation.Target;
                import java.util.Collection;
                import java.util.List;
                import java.util.Objects;
                                
                public class Test {
                  private Collection<Integer> xs = null;
                  private Collection<Integer> ys = null;
                  
                  @MatcherAnnotation(annotField = 42)
                  public pattern Test(@BindingAnnotation Collection<Integer> xs, @BindingAnnotation Collection<Integer> ys) {
                      match Test(this.xs, this.ys);
                  }
              
                  @Target(ElementType.METHOD)
                  @Retention(RetentionPolicy.RUNTIME)
                  public @interface MatcherAnnotation{
                      int annotField();
                  }
              
                  @Target(ElementType.PARAMETER)
                  @Retention(RetentionPolicy.RUNTIME)
                  public @interface BindingAnnotation { }
                 }
                 """);

        Files.createDirectories(classes);

        {
            new JavacTask(tb)
                    .options("-parameters", "--enable-preview", "--release", SOURCE_VERSION)
                    .outdir(classes)
                    .files(tb.findJavaFiles(src))
                    .run(Task.Expect.SUCCESS)
                    .writeAll();

            String javapOut = new JavapTask(tb)
                    .options("-v")
                    .classpath(classes.toString())
                    .classes("test.Test")
                    .run()
                    .getOutput(Task.OutputKind.DIRECT);

            String[] outputs = {
                "Signature: #32                          // (Ljava/util/Collection<Ljava/lang/Integer;>;Ljava/util/Collection<Ljava/lang/Integer;>;)V",
                "RuntimeVisibleParameterAnnotations:",
                    "parameter 0:",
                        "0: #42()",
                            "test.Test$BindingAnnotation",
                    "parameter 1:",
                        "0: #42()",
                            "test.Test$BindingAnnotation"
            };

            if (!Arrays.stream(outputs).allMatch(o -> javapOut.contains(o)))
                throw new AssertionError("Wrongly generated Pattern attribute with binding annotations and generic arguments");
        }

    }
}