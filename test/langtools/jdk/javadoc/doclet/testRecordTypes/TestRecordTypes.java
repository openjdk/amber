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
                "public record R(int r1) { }");

        javadoc("-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                src.resolve("R.java").toString());
        checkExit(Exit.OK);

        checkOutput("R.html", true,
                "<h1 title=\"Record R\" class=\"title\">Record R</h1>",
                "public record <span class=\"typeNameLabel\">R</span>",
                "<code><span class=\"memberNameLink\"><a href=\"#%3Cinit%3E(int)\">R</a></span>&#8203;(int&nbsp;r1)</code>");
    }

    @Test
    public void testRecordKeywordNamedPackage(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "package p; public record R(int r1) { }");

        javadoc("-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                "p");
        checkExit(Exit.OK);

        checkOutput("p/R.html", true,
                "<h1 title=\"Record R\" class=\"title\">Record R</h1>",
                "public record <span class=\"typeNameLabel\">R</span>",
                "<code><span class=\"memberNameLink\"><a href=\"#%3Cinit%3E(int)\">R</a></span>&#8203;(int&nbsp;r1)</code>");
    }

    @Test
    public void testEmptyRecord(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "package p; public record R() { }");

        javadoc("-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                "p");
        checkExit(Exit.OK);

        checkOutput("p/R.html", true,
                "<h1 title=\"Record R\" class=\"title\">Record R</h1>",
                "public record <span class=\"typeNameLabel\">R</span>",
                "<code><span class=\"memberNameLink\"><a href=\"#%3Cinit%3E()\">R</a></span>()</code>");
    }

    @Test
    public void testAtParam(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "package p; /** This is record R. \n"
                + " * @param r1 This is a component.\n"
                + " */\n"
                + "public record R(int r1) { }");

        javadoc("-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                "p");
        checkExit(Exit.OK);

        checkOutput("p/R.html", true,
                "<h1 title=\"Record R\" class=\"title\">Record R</h1>",
                "public record <span class=\"typeNameLabel\">R</span>",
                "<dl>\n"
                + "<dt><span class=\"paramLabel\">State Components:</span></dt>\n"
                + "<dd><code><a id=\"param-r1\">r1</a></code> - This is a component.</dd>\n"
                + "</dl>",
                "<code><span class=\"memberNameLink\"><a href=\"#%3Cinit%3E(int)\">R</a></span>&#8203;(int&nbsp;r1)</code>");
    }

    @Test
    public void testAtParamTyParam(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "package p; /** This is record R. \n"
                + " * @param r1  This is a component.\n"
                + " * @param <T> This is a type parameter.\n"
                + " */\n"
                + "public record R<T>(int r1) { }");

        javadoc("-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                "p");
        checkExit(Exit.OK);

        checkOutput("p/R.html", true,
                "<h1 title=\"Record R\" class=\"title\">Record R&lt;T&gt;</h1>",
                "public record <span class=\"typeNameLabel\">R&lt;T&gt;</span>",
                "<dl>\n"
                + "<dt><span class=\"paramLabel\">Type Parameters:</span></dt>\n"
                + "<dd><code>T</code> - This is a type parameter.</dd>\n"
                + "<dt><span class=\"paramLabel\">State Components:</span></dt>\n"
                + "<dd><code><a id=\"param-r1\">r1</a></code> - This is a component.</dd>\n"
                + "</dl>",
                "<code><span class=\"memberNameLink\"><a href=\"#%3Cinit%3E(int)\">R</a></span>&#8203;(int&nbsp;r1)</code>");
    }

    @Test
    public void testGeneratedComments(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "package p; /** This is record R. \n"
                        + " * @param r1  This is a component.\n"
                        + " */\n"
                        + "public record R(int r1) { }");

        javadoc("-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                "p");
        checkExit(Exit.OK);

        // While we don't normally test values that just come from resource files,
        // in these cases, we want to verify that something non-empty was put into
        // the documentation for the generated members.
        checkOrder("p/R.html",
                "<section class=\"constructorSummary\">",
                "<a href=\"#%3Cinit%3E(int)\">R</a>",
                "Creates an instance of a <code>R</code> record.",
                "<section class=\"methodSummary\">",
                "<a href=\"#equals(java.lang.Object)\">equals</a>",
                "Indicates whether some other object is \"equal to\" this one.",
                "<a href=\"#hashCode()\">hashCode</a>",
                "Returns a hash code value for this object.",
                "<a href=\"#r1()\">r1</a>",
                "Returns the value of the <a href=\"#param-r1\"><code>r1</code></a> state component.",
                "<a href=\"#toString()\">toString</a>",
                "Returns a string representation of this object."
        );
    }

    @Test
    public void testUserComments(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "package p; /** This is record R. \n"
                + " * @param r1  This is a component.\n"
                + " */\n"
                + "public record R(int r1) {\n"
                + "/** User constructor. */ public R { }\n"
                + "/** User equals. */ public boolean equals(Object other) { return false; }\n"
                + "/** User hashCode. */ public int hashCode() { return 0; }\n"
                + "/** User toString. */ public String toString() { return \"\"; }\n"
                + "/** User accessor. */ public int r1() { return r1; }\n"
                + "}");

        javadoc("-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                "p");
        checkExit(Exit.OK);

        checkOrder("p/R.html",
                "<section class=\"constructorSummary\">",
                "<a href=\"#%3Cinit%3E(int)\">R</a>",
                "User constructor.",
                "<section class=\"methodSummary\">",
                "<a href=\"#equals(java.lang.Object)\">equals</a>",
                "User equals.",
                "<a href=\"#hashCode()\">hashCode</a>",
                "User hashCode.",
                "<a href=\"#r1()\">r1</a>",
                "User accessor.",
                "<a href=\"#toString()\">toString</a>",
                "User toString."
        );
    }

}
