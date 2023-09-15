/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @run main ClassReaderTestMatchers
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

public class ClassReaderTestMatchers extends TestRunner {

    public static void main(String... args) throws Exception {
        ClassReaderTestMatchers t = new ClassReaderTestMatchers();
        t.runTests();
    }

    ToolBox tb;

    ClassReaderTestMatchers() {
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

        // record with matcher
        tb.writeJavaFiles(src,
            "package test;\n" +
                    "public class Point {\n" +
                    "    public Integer x = 0, y = 0;\n" +
                    "    public __matcher Point(Integer x, Integer y) {\n" +
                    "         x = this.x;\n" +
                    "         y = this.y;\n" +
                    "    }\n" +
                    "}");

        new JavacTask(tb)
                .outdir(out)
                .files(findJavaFiles(src))
                .run(Task.Expect.SUCCESS)
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

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
                .files(findJavaFiles(test))
                .run(Task.Expect.SUCCESS)
                .writeAll();
    }
}
