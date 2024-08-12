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
}
