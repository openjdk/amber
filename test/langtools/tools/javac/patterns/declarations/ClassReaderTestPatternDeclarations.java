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
 * @enablePreview
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.compiler/com.sun.tools.javac.code
 *          java.base/jdk.internal.classfile
 *          java.base/jdk.internal.classfile.attribute
 *          java.base/jdk.internal.classfile.constantpool
 *          java.base/jdk.internal.classfile.instruction
 *          java.base/jdk.internal.classfile.components
 *          java.base/jdk.internal.classfile.impl
 * @build toolbox.JavacTask toolbox.ToolBox
 * @run main ClassReaderTestPatternDeclarations
 */

import toolbox.TestRunner;
import toolbox.ToolBox;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.Task.OutputKind;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ClassReaderTestPatternDeclarations extends TestRunner {

    public static void main(String... args) throws Exception {
        ClassReaderTestPatternDeclarations t = new ClassReaderTestPatternDeclarations();
        t.runTests();
    }

    ToolBox tb;

    ClassReaderTestPatternDeclarations() {
        super(System.err);
        tb = new ToolBox();
    }

    protected void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    Path[] findJavaFiles(Path... paths) throws IOException {
        return tb.findJavaFiles(paths);
    }

    @Test
    public void testClassReadingDeconstructorInClass(Path base) throws Exception {
        Path src = base.resolve("src");
        Path test = base.resolve("test");
        Path out = base.resolve("out");
        Files.createDirectories(out);

        // record with pattern declaration
        tb.writeJavaFiles(src,
            "package test;\n" +
                    "public class Point {\n" +
                    "    public Integer x = 0, y = 0;\n" +
                    "    public pattern Point(Integer x, Integer y) {\n" +
                    "         match Point(this.x, this.y);" +
                    "    }\n" +
                    "}");

        new JavacTask(tb)
                .outdir(out)
                .options(List.of("--enable-preview", "-source", System.getProperty("java.specification.version")))
                .files(findJavaFiles(src))
                .run(Task.Expect.SUCCESS)
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        //XXX: note the type is erased!!!
        // test file
        tb.writeJavaFiles(test,
                "import test.Point;\n" +
                        "public class Test {\n" +
                        "    public static Integer testPointX(Object o) {\n" +
                        "        if (o instanceof Point(Integer x, Integer y)) {\n" +
                        "            return x;\n" +
                        "        }\n" +
                        "        return -1;\n" +
                        "    }" +
                        "}");

        new JavacTask(tb)
                .classpath(out)
                .options(List.of("--enable-preview", "-source", System.getProperty("java.specification.version")))
                .files(findJavaFiles(test))
                .run(Task.Expect.SUCCESS)
                .writeAll();
    }
}
