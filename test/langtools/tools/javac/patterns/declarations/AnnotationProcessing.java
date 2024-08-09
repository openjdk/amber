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
 * @build toolbox.TestRunner toolbox.ToolBox AnnotationProcessing
 * @run main AnnotationProcessing
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.TestRunner;
import toolbox.TestRunner.Test;
import toolbox.ToolBox;

import static java.util.stream.Collectors.joining;

public class AnnotationProcessing extends TestRunner {

    public static void main(String... args) throws Exception {
        new AnnotationProcessing().runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    private final ToolBox tb = new ToolBox();

    public AnnotationProcessing() {
        super(System.err);
    }

    @Test
    public void testErrorsAfter(Path outerBase) throws Exception {
        Path src = outerBase.resolve("src");
        tb.writeJavaFiles(src,
                          """
                          public class T {
                              public pattern T(int i) {
                                  match T(0);
                              }
                          }
                          """);
        Path classes = outerBase.resolve("classes");
        Files.createDirectories(classes);
        new JavacTask(tb)
            .options("-processor", "AnnotationProcessing$P",
                     "-processorpath", System.getProperty("test.classes"),
                     "--enable-preview", "--source", System.getProperty("java.specification.version"))
            .outdir(classes.toString())
            .files(tb.findJavaFiles(src))
            .run()
            .writeAll();
    }

    @Test
    public void testAnnotationProcessing(Path outerBase) throws Exception {
        Path src = outerBase.resolve("src");
        tb.writeJavaFiles(src,
                """
                import static java.lang.annotation.RetentionPolicy.CLASS;
                import static java.lang.annotation.RetentionPolicy.RUNTIME;
                import java.lang.annotation.Retention;
                public class T {
                    @Retention(RUNTIME)
                    @interface RuntimeAnnotation {
                        int value() default 0;
                    }
                
                    @Retention(CLASS)
                    @interface ClassAnnotation {
                        int value() default 0;
                    }
                
                    public static class Person1 {
                        private final String name;
                        private final String username;
                        private boolean capitalize;
                
                        public Person1(String name, String username, boolean capitalize) {
                            this.name = name;
                            this.username = username;
                            this.capitalize = capitalize;
                        }
                
                        @AnnotationProcessing.BindingProcessor.Bindings
                        public pattern Person1(@ClassAnnotation(21) String name, @RuntimeAnnotation(42) String username) {
                            if (capitalize) {
                                match Person1(this.name.toUpperCase(), this.username.toUpperCase());
                            } else {
                                match Person1(this.name, this.username);
                            }
                        }
                    }
                }
                """);
        Path classes = outerBase.resolve("classes");
        Files.createDirectories(classes);
        List<String> output = new JavacTask(tb)
                .options("-XDrawDiagnostics", "-proc:only", "-processor", "AnnotationProcessing$BindingProcessor",
                        "-processorpath", System.getProperty("test.classes"), "-parameters",
                        "--enable-preview", "--source", System.getProperty("java.specification.version"))
                .outdir(classes.toString())
                .files(tb.findJavaFiles(src))
                .run()
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        if (!output.contains("- compiler.note.proc.messager: T.Person1.Person1(@T.ClassAnnotation(21) name, @T.RuntimeAnnotation(42) username)")) {
            throw new AssertionError("Error in annotation processing when processing matchers\n" + output);
        }
    }

    @SupportedAnnotationTypes("*")
    public static class P extends AbstractProcessor {
        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            return false;
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }
    }

    @SupportedAnnotationTypes("*")
    public static class BindingProcessor extends JavacTestingAbstractProcessor {

        @Retention(RetentionPolicy.RUNTIME)
        @Target({ElementType.METHOD})
        @interface Bindings {
            String[] value() default {};
        }

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            for (Element element : roundEnv.getElementsAnnotatedWith(Bindings.class)) {
                if (element instanceof ExecutableElement exec) {
                    String message = String.format("%s.%s(%s)",
                            exec.getEnclosingElement(),
                            exec.getSimpleName(),
                            exec.getBindings().stream().map(this::printBinding).collect(joining(", ")));
                    messager.printMessage(Diagnostic.Kind.OTHER, message);
                }
            }
            return false;
        }

        private String printBinding(VariableElement binding) {
            return binding.getAnnotationMirrors().stream().map(String::valueOf).collect(joining(" "))
                    + (binding.getAnnotationMirrors().isEmpty() ? "" : " ")
                    + binding.getSimpleName();
        }
    }
}
