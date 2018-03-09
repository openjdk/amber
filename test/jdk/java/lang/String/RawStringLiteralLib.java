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
 * @summary Unit tests for Raw String Literal library support
 * @run main RawStringLiteralLib
 */

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.lang.String.EscapeType;

public class RawStringLiteralLib {
    public static void main(String[] args) {
        test1();
        test2();
        test3();
    }

    /*
     * Test trim string functionality.
     */
    static void test1() {
        equal("   abc   ".trim(), "abc");
        equal("   abc   ".trimLeft(), "abc   ");
        equal("   abc   ".trimRight(), "   abc");
        equal("   abc\u2022   ".trim(), "abc\u2022");
        equal("   abc\u2022   ".trimLeft(), "abc\u2022   ");
        equal("   abc\u2022   ".trimRight(), "   abc\u2022");
        equal("".trim(), "");
        equal("".trimLeft(), "");
        equal("".trimRight(), "");

        // trimIndent
        for (String prefix : List.of("", "\n", "   \n"))
        for (String suffix : List.of("", "\n", "   \n"))
        for (String middle : List.of("",
                                     "xyz",
                                     "   xyz",
                                     "xyz   ",
                                     "   xyz   ",
                                     "xyz\u2022",
                                     "   xyz\u2022",
                                     "xyz\u2022   ",
                                     "   xyz\u2022   ",
                                     "   // comment"))
        {
            String input = prefix + "   abc   \n" + middle + "\n   def   \n" + suffix;
            String output = input.trimIndent();

            Iterator<String> inputIterator = input.lines().iterator();
            Iterator<String> outputIterator = output.lines().iterator();
            String first = input.lines().findFirst​().orElse("").trim();

            if (first.isEmpty() && inputIterator.hasNext()) {
                inputIterator.next();
            }

            int indent = -1;

            while (inputIterator.hasNext() && outputIterator.hasNext()) {
                String in = inputIterator.next();
                String out = outputIterator.next();
                int offset = in.indexOf(out);
                if (offset == -1) {
                    report("Loss of information", "Input:", in, "Output:", out);
                }
                if (!out.isEmpty()) {
                    if (indent == -1) {
                        indent = offset;
                    } else if (offset != indent ) {
                        report("Inconsistent indent", "Input:", in, "Output:", out);
                    }
                }
            }

            if (outputIterator.hasNext()) {
                String out = outputIterator.next();
                report("Too many lines", "Input:", "", "Output:", out);
            }

            if (inputIterator.hasNext()) {
                String in = inputIterator.next();

                if (!in.isEmpty()) {
                    report("Loss of information", "Input:", in, "Output:", "");
                }
            }
        }

        // trimMargins
        for (String prefix : List.of("", "\n", "   \n"))
        for (String suffix : List.of("", "\n", "   \n"))
        for (String middle : List.of("",
                                     "xyz",
                                     "   xyz",
                                     "xyz   ",
                                     "   xyz   ",
                                     "xyz\u2022",
                                     "   xyz\u2022",
                                     "xyz\u2022   ",
                                     "   xyz\u2022   ",
                                     "   // comment"))
        for (String leftMargin : List.of("", " ", "|"))
        for (String rightMargin : List.of("", " ", "|"))
        {
            String input = prefix +
                           leftMargin + "   abc   " + rightMargin + "\n" +
                           leftMargin + middle + rightMargin + "\n" +
                           leftMargin + "   def   " + rightMargin + "\n" +
                           suffix;
            String output = input.trimMargins(leftMargin, rightMargin);

            Iterator<String> inputIterator = input.lines().iterator();
            Iterator<String> outputIterator = output.lines().iterator();
            String first = input.lines().findFirst​().orElse("").trim();

            if (first.isEmpty() && inputIterator.hasNext()) {
                inputIterator.next();
            }

            while (inputIterator.hasNext() && outputIterator.hasNext()) {
                String in = inputIterator.next();
                String out = outputIterator.next();
                if (in.indexOf(out) == -1) {
                    report("Loss of information", "Input:", in, "Output:", out);
                }
                if (out.indexOf('|') != -1) {
                    report("Margin not removed", "Input:", in, "Output:", out);
                }

            }

            if (outputIterator.hasNext()) {
                String out = outputIterator.next();
                report("Too many lines", "Input:", "", "Output:", out);
            }

            if (inputIterator.hasNext()) {
                String in = inputIterator.next();

                if (!in.isEmpty()) {
                    report("Loss of information", "Input:", in, "Output:", "");
                }
            }
        }
    }

    /*
     * Test escaping functionality.
     */
    static void test2() {
        equal(`a\tb\u0063`, "a\\tb\\u0063");
        equal(`a\tb\u0063`.unescape(), "a\tbc");
        equal(`a\tb\u0063`.unescape(EscapeType.BACKSLASH), "a\tb\\u0063");
        equal(`a\tb\u0063`.unescape(EscapeType.UNICODE), "a\\tbc");
        equal(`a\tb\u2022`, "a\\tb\\u2022");
        equal(`a\tb\u2022`.unescape(), "a\tb\u2022");
        equal(`a\tb\u2022`.unescape(EscapeType.BACKSLASH), "a\tb\\u2022");
        equal(`a\tb\u2022`.unescape(EscapeType.UNICODE), "a\\tb\u2022");
        equal(`\0\12\012`.unescape(), "\0\12\012");
        equal(`•\0\12\012`.unescape(), "\u2022\0\12\012");

        equal("\\u0000\\u0001\\n\\u0010", "\u0000\u0001\n\u0010".escape());
        equal("\\u0000\\u0001\\n\\u0010", "\u0000\u0001\n\u0010".escape(EscapeType.BACKSLASH));
        equal("\u0000\u0001\n\u0010", "\u0000\u0001\n\u0010".escape(EscapeType.UNICODE));
        equal("\\u2022", "\u2022".escape());
        equal("\u2022", "\u2022".escape(EscapeType.BACKSLASH));
        equal("\\u2022", "\u2022".escape(EscapeType.UNICODE));

        equal(`\b`.unescape(), "\b");
        equal(`\f`.unescape(), "\f");
        equal(`\n`.unescape(), "\n");
        equal(`\r`.unescape(), "\r");
        equal(`\t`.unescape(), "\t");
        equal(`\0`.unescape(), "\0");
        equal(`\7`.unescape(), "\7");
        equal(`\12`.unescape(), "\12");
        equal(`\012`.unescape(), "\012");
        equal(`\u0000`.unescape(), "\u0000");
        equal(`\u2022`.unescape(), "\u2022");
        equal(`•\b`.unescape(), "•\b");
        equal(`•\f`.unescape(), "•\f");
        equal(`•\n`.unescape(), "•\n");
        equal(`•\r`.unescape(), "•\r");
        equal(`•\t`.unescape(), "•\t");
        equal(`•\0`.unescape(), "•\0");
        equal(`•\7`.unescape(), "•\7");
        equal(`•\12`.unescape(), "•\12");
        equal(`•\177`.unescape(), "•\177");
        equal(`•\u0000`.unescape(), "•\u0000");
        equal(`•\u2022`.unescape(), "•\u2022");

        equal(`\b`, "\b".escape());
        equal(`\f`, "\f".escape());
        equal(`\n`, "\n".escape());
        equal(`\r`, "\r".escape());
        equal(`\t`, "\t".escape());
        equal(`\u0000`, "\0".escape());
        equal(`\u0007`, "\7".escape());
        equal(`\u0011`, "\21".escape());
        equal(`\u0000`, "\u0000".escape());
        equal(`\u2022`, "\u2022".escape());
        equal(`\u2022\b`, "•\b".escape());
        equal(`\u2022\f`, "•\f".escape());
        equal(`\u2022\n`, "•\n".escape());
        equal(`\u2022\r`, "•\r".escape());
        equal(`\u2022\t`, "•\t".escape());
        equal(`\u2022\u0000`, "•\0".escape());
        equal(`\u2022\u0007`, "•\7".escape());
        equal(`\u2022\u0011`, "•\21".escape());
        equal(`\u2022\u007f`, "•\177".escape());
        equal(`\u2022\u0000`, "•\u0000".escape());
        equal(`\u2022\u2022`, "•\u2022".escape());
    }

    /*
     * Test for MalformedEscapeException.
     */
    static void test3() {
        wellFormed(`\b`);
        wellFormed(`\f`);
        wellFormed(`\n`);
        wellFormed(`\r`);
        wellFormed(`\t`);
        wellFormed(`\0`);
        wellFormed(`\7`);
        wellFormed(`\12`);
        wellFormed(`\012`);
        wellFormed(`\u0000`);
        wellFormed(`\u2022`);
        wellFormed(`•\b`);
        wellFormed(`•\f`);
        wellFormed(`•\n`);
        wellFormed(`•\r`);
        wellFormed(`•\t`);
        wellFormed(`•\0`);
        wellFormed(`•\7`);
        wellFormed(`•\12`);
        wellFormed(`•\012`);
        wellFormed(`•\u0000`);
        wellFormed(`•\u2022`);

        malformed(`\x`);
        malformed(`\+`);
        malformed(`\u`);
        malformed(`\uuuuu`);
        malformed(`\u2`);
        malformed(`\u20`);
        malformed(`\u202`);
        malformed(`\u2   `);
        malformed(`\u20  `);
        malformed(`\u202 `);
        malformed(`\uuuuu2`);
        malformed(`\uuuuu20`);
        malformed(`\uuuuu202`);
        malformed(`\uuuuu2   `);
        malformed(`\uuuuu20  `);
        malformed(`\uuuuu202 `);
        malformed(`\uG`);
        malformed(`\u2G`);
        malformed(`\u20G`);
        malformed(`\uG   `);
        malformed(`\u2G  `);
        malformed(`\u20G `);
        malformed(`\uuuuuG`);
        malformed(`\uuuuu2G`);
        malformed(`\uuuuu20G`);
        malformed(`\uuuuuG   `);
        malformed(`\uuuuu2G  `);
        malformed(`\uuuuu20G `);

        malformed(`•\x`);
        malformed(`•\+`);
        malformed(`•\u`);
        malformed(`•\uuuuu`);
        malformed(`•\u2`);
        malformed(`•\u20`);
        malformed(`•\u202`);
        malformed(`•\u2   `);
        malformed(`•\u20  `);
        malformed(`•\u202 `);
        malformed(`•\uuuuu2`);
        malformed(`•\uuuuu20`);
        malformed(`•\uuuuu202`);
        malformed(`•\uuuuu2   `);
        malformed(`•\uuuuu20  `);
        malformed(`•\uuuuu202 `);
        malformed(`•\uG`);
        malformed(`•\u2G`);
        malformed(`•\u20G`);
        malformed(`•\uG   `);
        malformed(`•\u2G  `);
        malformed(`•\u20G `);
        malformed(`•\uuuuuG`);
        malformed(`•\uuuuu2G`);
        malformed(`•\uuuuu20G`);
        malformed(`•\uuuuuG   `);
        malformed(`•\uuuuu2G  `);
        malformed(`•\uuuuu20G `);
    }

    /*
     * Report difference in result.
     */
    static void report(String message, String inputTag, String input,
                                       String outputTag, String output) {
        System.err.println(message);
        System.err.println();
        System.err.println(inputTag);
        System.err.println(input.replaceAll(" ", "."));
        System.err.println();
        System.err.println(outputTag);
        System.err.println(output.replaceAll(" ", "."));
        throw new RuntimeException();
    }

    /*
     * Raise an exception if the two inputs are not equivalent.
     */
    static void equal(String input, String expected) {
        if (input == null || expected == null || !expected.equals(input)) {
            report("Failed equal", "Input:", input, "Expected:", expected);
        }
    }

    /*
     * Raise an exception if the string contains a malformed escape.
     */
    static void wellFormed(String rawString) {
        try {
            rawString.unescape();
        } catch (MalformedEscapeException ex) {
            System.err.println("Failed wellFormed");
            System.err.println(rawString);
            throw new RuntimeException();
        }
    }

    /*
     * Raise an exception if the string does not contain a malformed escape.
     */
    static void malformed(String rawString) {
        try {
            rawString.unescape();
            System.err.println("Failed malformed");
            System.err.println(rawString);
            throw new RuntimeException();
        } catch (MalformedEscapeException ex) {
            // incorrectly formed escapes
        }
    }
}
