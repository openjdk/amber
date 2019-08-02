/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      8225055
 * @summary  Record types
 * @library  /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    toolbox.ToolBox javadoc.tester.*
 * @run main TestRecordTypes
 */


import java.io.IOException;
import java.nio.file.Path;

import javadoc.tester.JavadocTester;
import toolbox.ToolBox;

public class TestRecordTypes extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestRecordTypes tester = new TestRecordTypes();
        tester.runTests(m -> new Object[] { Path.of(m.getName()) });
    }

    private final ToolBox tb = new ToolBox();

    @Test
    public void testRecordKeywordUnnamedPackage(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "public record A(int a) { }");

        javadoc("-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                src.resolve("A.java").toString());
        checkExit(Exit.OK);

        checkOutput("A.html", true,
                "<h1 title=\"Record A\" class=\"title\">Record A</h1>",
                "public record <span class=\"typeNameLabel\">A</span>",
                "<code><span class=\"memberNameLink\"><a href=\"#%3Cinit%3E(int)\">A</a></span>&#8203;(int&nbsp;a)</code>");
    }

    @Test
    public void testRecordKeywordNamedPackage(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "package p; public record A(int a) { }");

        javadoc("-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                "p");
        checkExit(Exit.OK);

        checkOutput("p/A.html", true,
                "<h1 title=\"Record A\" class=\"title\">Record A</h1>",
                "public record <span class=\"typeNameLabel\">A</span>",
                "<code><span class=\"memberNameLink\"><a href=\"#%3Cinit%3E(int)\">A</a></span>&#8203;(int&nbsp;a)</code>");
    }

}
