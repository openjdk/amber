/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Unit tests for Raw String Literal language changes
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main RawStringLiteralLang
 */

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

import java.util.Arrays;
import static java.nio.charset.StandardCharsets.US_ASCII;

public class RawStringLiteralLang {
    private static ToolBox TOOLBOX = new ToolBox();

    public static void main(String... args) {
        test1();
        test2();
        test3();
        test4();
        test5();
    }

    /*
     * Check that correct syntax is properly detected
     */
    static void test1() {
        for (int i = 1; i < 10; i++) {
            for (int j = 0; j < i; j++) {
                compPass("public class NoEndRawString {\n" +
                        "    public static void main(String... args) {\n" +
                        "        String xxx = " +
                        repeat('`', i) + "abc" + repeat('`', j) + "def" +  repeat('`', i) +
                        ";\n" +
                        "    }\n" +
                        "}\n");
            }
        }
    }

    /*
     * Check that syntax errors are properly detected
     */
    static void test2() {
        for (int i = 1; i < 10; i++) {
            for (int j = 0; j < i; j++) {
                compFail("public class NoEndRawString {\n" +
                        "    public static void main(String... args) {\n" +
                        "        String xxx = " +
                        repeat('`', i) + "abc" + repeat('`', j) + "def" +  repeat('`', i - 1) +
                        ";\n" +
                        "    }\n" +
                        "}\n", "compiler.err.unclosed.str.lit");
            }
        }
    }

    /*
     * Check that misuse of \u0060 is properly detected
     */
    static void test3() {
        compFail("public class BadDelimiter {\n" +
                "    public static void main(String... args) {\n" +
                "        String xxx = \\u0060`abc`;\n" +
                "    }\n" +
                "}\n", "compiler.err.illegal.char");
    }

    /*
     * Test raw string functionality.
     */
    static void test4() {
        EQ(`abc`, "abc");
        EQ(`can't`, "can\'t");
        EQ(``can`t``, "can`t");
        EQ(`can\\'t`, "can\\\\'t");
        EQ(``can\\`t``, "can\\\\`t");

        LENGTH("abc``def", 8);
        EQ("abc`\u0020`def", "abc` `def");
    }

    /*
     * Test multi-line string functionality.
     */
    static void test5() {
        EQ(`abc
def
ghi`, "abc\ndef\nghi");
        EQ(`abc
def
ghi
`, "abc\ndef\nghi\n");
        EQ(`
abc
def
ghi`, "\nabc\ndef\nghi");
        EQ(`
abc
def
ghi
`, "\nabc\ndef\nghi\n");
    }

    /*
     * Create a string with the specified character repeated.
     */
    public static String repeat(char ch, int count) {
        byte[] value = new byte[count];
        Arrays.fill(value, (byte)ch);
        return new String(value, US_ASCII);
    }

    /*
     * Test source for successful compile.
     */
    static void compPass(String source) {
        new JavacTask(TOOLBOX)
                .sources(source)
                .run();
    }

    /*
     * Test source for unsuccessful compile and specific error.
     */
    static void compFail(String source, String error)  {
        String errors = new JavacTask(TOOLBOX)
                .sources(source)
                .classpath(".")
                .options("-XDrawDiagnostics")
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!errors.contains(error)) {
            throw new RuntimeException("Wrong error");
        }
    }

    /*
     * Raise an exception if the string is not the expected length.
     */
    static void LENGTH(String rawString, int length) {
        if (rawString == null || rawString.length() != length) {
            System.err.println("Failed LENGTH");
            System.err.println(rawString + " " + length);
            throw new RuntimeException("Failed LENGTH");
        }
    }

    /*
     * Raise an exception if the two input strings are not equal.
     */
    static void EQ(String input, String expected) {
        if (input == null || expected == null || !expected.equals(input)) {
            System.err.println("Failed EQ");
            System.err.println();
            System.err.println("Input:");
            System.err.println(input.replaceAll(" ", "."));
            System.err.println();
            System.err.println("Expected:");
            System.err.println(expected.replaceAll(" ", "."));
            throw new RuntimeException();
        }
    }
}
