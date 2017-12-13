/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @summary check that records are always static
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.source.util
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @build DPrinter
 * @run main RecordsMustBeStaticTest
 */

import java.io.*;
import java.net.URI;
import java.util.Arrays;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Context;

public class RecordsMustBeStaticTest {
    public static void main(String... args) throws Exception {
        new RecordsMustBeStaticTest().run();
    }

    void run() throws Exception {
        Context context = new Context();
        JavacFileManager.preRegister(context);
        Trees trees = JavacTrees.instance(context);
        strOut = new StringWriter();
        PrintWriter pw = new PrintWriter(strOut);
        dprinter = new DPrinter(pw, trees);
        tool = ToolProvider.getSystemJavaCompiler();
        test("Foo.java", source1);
        test("Foo2.java", source11);
        test("Foo3.java", source111);
        test("Bar.java", source2);
        test("Bar2.java", source3);
        test("Baz.java", source4);
    }

    StringWriter strOut;
    DPrinter dprinter;
    JavaCompiler tool;

    void test(String fileName, String source) throws Exception {
        JavacTask ct = (JavacTask)tool.getTask(null, null, null, null, null, Arrays.asList(new JavaSource(fileName, source)));
        Iterable<? extends CompilationUnitTree> elements = ct.parse();
        Assert.check(elements.iterator().hasNext());
        dprinter.treeTypes(true).printTree("", (JCTree)elements.iterator().next());
        String output = strOut.toString();
        Assert.check(output.contains("flags: [static, record]"), "nested records should be static");
    }

    static final String source1 =
            "class Foo {\n" +
            "    record R (int x);\n" +
            "}";

    static final String source11 =
            "class Foo2 {\n" +
            "    class Inner {\n" +
            "        record R (int x);\n" +
            "    }\n" +
            "}";
    static final String source111 =
            "class Foo3 {\n" +
            "    Runnable r = new Runnable() {\n" +
            "        record R(int i);\n" +
            "        public void run() {}\n" +
            "    };" +
            "}";
    static final String source2 =
            "class Bar {\n" +
            "    void m() {\n" +
            "        record R (int x);\n" +
            "    }\n" +
            "}";

    static final String source3 =
            "class Bar2 {\n" +
            "    void m() {\n" +
            "        static record R (int x);\n" +
            "    }\n" +
            "}";

    static final String source4 =
            "class Baz {\n" +
            "    void m() {\n" +
            "        Runnable r = () -> {" +
            "            record R (int x);\n" +
            "        };\n" +
            "    }\n" +
            "}";

    static class JavaSource extends SimpleJavaFileObject {

        String source;

        public JavaSource(String fileName, String source) {
            super(URI.create("myfo:/" + fileName), JavaFileObject.Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }
}
