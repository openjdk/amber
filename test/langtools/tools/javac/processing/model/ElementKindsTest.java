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
 * @summary Verify Elements have the appropriate ElementKinds
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.TestRunner toolbox.ToolBox ElementKindsTest
 * @run main ElementKindsTest
 */

import com.sun.source.tree.VariableTree;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import toolbox.JavacTask;
import toolbox.TestRunner;
import toolbox.TestRunner.Test;
import toolbox.ToolBox;

public class ElementKindsTest extends TestRunner {

    public static void main(String... args) throws Exception {
        new ElementKindsTest().runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    private final ToolBox tb = new ToolBox();

    public ElementKindsTest() {
        super(System.err);
    }

    @Test
    public void testVariables(Path outerBase) throws Exception {
        Path src = outerBase.resolve("src");
        tb.writeJavaFiles(src,
                          """
                          import javax.lang.model.element.ElementKind;
                          public class Test {
                              @ExpectedElementKind(ElementKind.FIELD)
                              int f;
                              public void test(@ExpectedElementKind(ElementKind.PARAMETER) int p) {
                                  try (@ExpectedElementKind(ElementKind.RESOURCE_VARIABLE) AutoCloseable a = null) {
                                      @ExpectedElementKind(ElementKind.LOCAL_VARIABLE)
                                      boolean b = p instanceof @ExpectedElementKind(ElementKind.BINDING_VARIABLE) int bv;
                                  } catch (@ExpectedElementKind(ElementKind.EXCEPTION_PARAMETER) Throwable t) {}
                              }
                              public pattern Test(@ExpectedElementKind(ElementKind.PATTERN_BINDING) int b) {
                                  try (@ExpectedElementKind(ElementKind.RESOURCE_VARIABLE) AutoCloseable a = null) {
                                      @ExpectedElementKind(ElementKind.LOCAL_VARIABLE)
                                      boolean x = 0 instanceof @ExpectedElementKind(ElementKind.BINDING_VARIABLE) int bv;
                                  } catch (@ExpectedElementKind(ElementKind.EXCEPTION_PARAMETER) Throwable t) {}
                                  match Test(0);
                              }
                              enum E {
                                  @ExpectedElementKind(ElementKind.ENUM_CONSTANT) A;
                              }
                          }
                          @interface ExpectedElementKind {
                              public ElementKind value();
                          }
                          """);
        Path classes = outerBase.resolve("classes");
        Files.createDirectories(classes);
        new JavacTask(tb)
                .outdir(classes)
                .options("--enable-preview",
                         "--source", System.getProperty("java.specification.version"))
                .files(tb.findJavaFiles(src))
                .callback(task -> {
                    task.addTaskListener(new TaskListener() {
                        @Override
                        public void finished(TaskEvent e) {
                            if (e.getKind() != TaskEvent.Kind.ANALYZE) {
                                return ;
                            }
                            Trees trees = Trees.instance(task);
                            new TreePathScanner<Void, Void>() {
                                @Override
                                public Void visitVariable(VariableTree node, Void p) {
                                    Element el = trees.getElement(getCurrentPath());

                                    ExpectedElementKind eek = el.getAnnotation(ExpectedElementKind.class);

                                    if (eek.value() != el.getKind()) {
                                        throw new AssertionError("Expected: " + eek.value() + ", but was: " + el.getKind());
                                    }

                                    return super.visitVariable(node, p);
                                }
                            }.scan(e.getCompilationUnit(), null);
                        }
                    });
                })
                .run()
                .writeAll();
    }

}

@interface ExpectedElementKind {
    public ElementKind value();
}
