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

import java.util.List;

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
        EQ("   abc   ".trim(), "abc");
        EQ("   abc   ".trimLeft(), "abc   ");
        EQ("   abc   ".trimRight(), "   abc");
        EQ("   abc\u2022   ".trim(), "abc\u2022");
        EQ("   abc\u2022   ".trimLeft(), "abc\u2022   ");
        EQ("   abc\u2022   ".trimRight(), "   abc\u2022");
        EQ("".trim(), "");
        EQ("".trimLeft(), "");
        EQ("".trimRight(), "");

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
            String[] inputLines = input.lines();
            int inputLength = inputLines.length;
            String output = input.trimIndent();
            String[] outputLines = output.lines();
            int outputLength = outputLines.length;

            int start = inputLines[0].trim().isEmpty() ? 1 : 0;
            int end = inputLines[inputLength - 1].trim().isEmpty() ? inputLength - 1 : inputLength;

            if (outputLength != (end - start)) {
                report("Wrong number of lines", "Input:", input, "Output:", output);
            }
            int indent = 0;
            for (int i = 0; i < outputLength; i++) {
                String line = outputLines[i];
                int offset = inputLines[start + i].indexOf(line);
                if (offset == -1) {
                    report("Loss of lines", "Input:", input, "Output:", output);
                }
                if (i == 0) {
                    indent = offset;
                } else if (offset != indent ) {
                    report("Inconsistent indent", "Input:", input, "Output:", output);
                }
            }
        }

        // trimMargin
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
        for (String leftMargin : List.of("", " ", "|", "   |"))
        for (String rightMargin : List.of("", " ", "|", "|   "))
        {
            String input = prefix +
                           leftMargin + "   abc   " + rightMargin + "\n" +
                           leftMargin + middle + rightMargin + "\n" +
                           leftMargin + "   def   " + rightMargin + "\n" +
                           suffix;
            String[] inputLines = input.lines();
            int inputLength = inputLines.length;
            String output;

            if (leftMargin.isEmpty() && rightMargin.isEmpty()) {
                output = input.trimMargins();
            } else {
                String beginMarker = leftMargin.contains("|") ? "|" : leftMargin;
                String endMarker = rightMargin.contains("|") ? "|" : rightMargin;

                if (" ".equals(endMarker)) {
                    output = input.trimMargins(beginMarker);
                } else {
                    output = input.trimMargins(beginMarker, endMarker);
                }
            }

            String[] outputLines = output.lines();
            int outputLength = outputLines.length;

            int start = inputLines[0].trim().isEmpty() ? 1 : 0;
            int end = inputLines[inputLength - 1].trim().isEmpty() ? inputLength - 1 : inputLength;

            if (outputLength != (end - start)) {
                report("Wrong number of lines", "Input:", input, "Output:", output);
            }
            int indent = 0;
            for (int i = 0; i < outputLength; i++) {
                String line = outputLines[i];
                if (!inputLines[start + i].contains(line)) {
                    report("Loss of lines", "Input:", input, "Output:", output);
                }
                if (line.indexOf('|') != -1) {
                    report("Margin not removed", "Input:", input, "Output:", output);
                }
            }
        }
    }

    /*
     * Test escaping functionality.
     */
    static void test2() {
        EQ(`a\tb\u0063`, "a\\tb\\u0063");
        EQ(`a\tb\u0063`.unescape(), "a\tbc");
        EQ(`a\tb\u0063`.unescapeBackslash(), "a\tb\\u0063");
        EQ(`a\tb\u0063`.unescapeUnicode(), "a\\tbc");
        EQ(`a\tb\u2022`, "a\\tb\\u2022");
        EQ(`a\tb\u2022`.unescape(), "a\tb\u2022");
        EQ(`a\tb\u2022`.unescapeBackslash(), "a\tb\\u2022");
        EQ(`a\tb\u2022`.unescapeUnicode(), "a\\tb\u2022");
        EQ(`\0\12\012`.unescape(), "\0\12\012");
        EQ(`•\0\12\012`.unescape(), "\u2022\0\12\012");

        EQ("\\u0000\\u0001\\n\\u0010", "\u0000\u0001\n\u0010".escape());
        EQ("\\u0000\\u0001\\n\\u0010", "\u0000\u0001\n\u0010".escapeBackslash());
        EQ("\u0000\u0001\n\u0010", "\u0000\u0001\n\u0010".escapeUnicode());
        EQ("\\u2022", "\u2022".escape());
        EQ("\u2022", "\u2022".escapeBackslash());
        EQ("\\u2022", "\u2022".escapeUnicode());

        EQ(`\b`.unescape(), "\b");
        EQ(`\f`.unescape(), "\f");
        EQ(`\n`.unescape(), "\n");
        EQ(`\r`.unescape(), "\r");
        EQ(`\t`.unescape(), "\t");
        EQ(`\0`.unescape(), "\0");
        EQ(`\7`.unescape(), "\7");
        EQ(`\12`.unescape(), "\12");
        EQ(`\012`.unescape(), "\012");
        EQ(`\u0000`.unescape(), "\u0000");
        EQ(`\u2022`.unescape(), "\u2022");
        EQ(`•\b`.unescape(), "•\b");
        EQ(`•\f`.unescape(), "•\f");
        EQ(`•\n`.unescape(), "•\n");
        EQ(`•\r`.unescape(), "•\r");
        EQ(`•\t`.unescape(), "•\t");
        EQ(`•\0`.unescape(), "•\0");
        EQ(`•\7`.unescape(), "•\7");
        EQ(`•\12`.unescape(), "•\12");
        EQ(`•\177`.unescape(), "•\177");
        EQ(`•\u0000`.unescape(), "•\u0000");
        EQ(`•\u2022`.unescape(), "•\u2022");

        EQ(`\b`, "\b".escape());
        EQ(`\f`, "\f".escape());
        EQ(`\n`, "\n".escape());
        EQ(`\r`, "\r".escape());
        EQ(`\t`, "\t".escape());
        EQ(`\u0000`, "\0".escape());
        EQ(`\u0007`, "\7".escape());
        EQ(`\u0011`, "\21".escape());
        EQ(`\u0000`, "\u0000".escape());
        EQ(`\u2022`, "\u2022".escape());
        EQ(`\u2022\b`, "•\b".escape());
        EQ(`\u2022\f`, "•\f".escape());
        EQ(`\u2022\n`, "•\n".escape());
        EQ(`\u2022\r`, "•\r".escape());
        EQ(`\u2022\t`, "•\t".escape());
        EQ(`\u2022\u0000`, "•\0".escape());
        EQ(`\u2022\u0007`, "•\7".escape());
        EQ(`\u2022\u0011`, "•\21".escape());
        EQ(`\u2022\u007f`, "•\177".escape());
        EQ(`\u2022\u0000`, "•\u0000".escape());
        EQ(`\u2022\u2022`, "•\u2022".escape());
    }

    /*
     * Test for MalformedEscapeException.
     */
    static void test3() {
        WELLFORMED(`\b`);
        WELLFORMED(`\f`);
        WELLFORMED(`\n`);
        WELLFORMED(`\r`);
        WELLFORMED(`\t`);
        WELLFORMED(`\0`);
        WELLFORMED(`\7`);
        WELLFORMED(`\12`);
        WELLFORMED(`\012`);
        WELLFORMED(`\u0000`);
        WELLFORMED(`\u2022`);
        WELLFORMED(`•\b`);
        WELLFORMED(`•\f`);
        WELLFORMED(`•\n`);
        WELLFORMED(`•\r`);
        WELLFORMED(`•\t`);
        WELLFORMED(`•\0`);
        WELLFORMED(`•\7`);
        WELLFORMED(`•\12`);
        WELLFORMED(`•\012`);
        WELLFORMED(`•\u0000`);
        WELLFORMED(`•\u2022`);

        MALFORMED(`\x`);
        MALFORMED(`\+`);
        MALFORMED(`\u`);
        MALFORMED(`\uuuuu`);
        MALFORMED(`\u2`);
        MALFORMED(`\u20`);
        MALFORMED(`\u202`);
        MALFORMED(`\u2   `);
        MALFORMED(`\u20  `);
        MALFORMED(`\u202 `);
        MALFORMED(`\uuuuu2`);
        MALFORMED(`\uuuuu20`);
        MALFORMED(`\uuuuu202`);
        MALFORMED(`\uuuuu2   `);
        MALFORMED(`\uuuuu20  `);
        MALFORMED(`\uuuuu202 `);
        MALFORMED(`\uG`);
        MALFORMED(`\u2G`);
        MALFORMED(`\u20G`);
        MALFORMED(`\uG   `);
        MALFORMED(`\u2G  `);
        MALFORMED(`\u20G `);
        MALFORMED(`\uuuuuG`);
        MALFORMED(`\uuuuu2G`);
        MALFORMED(`\uuuuu20G`);
        MALFORMED(`\uuuuuG   `);
        MALFORMED(`\uuuuu2G  `);
        MALFORMED(`\uuuuu20G `);

        MALFORMED(`•\x`);
        MALFORMED(`•\+`);
        MALFORMED(`•\u`);
        MALFORMED(`•\uuuuu`);
        MALFORMED(`•\u2`);
        MALFORMED(`•\u20`);
        MALFORMED(`•\u202`);
        MALFORMED(`•\u2   `);
        MALFORMED(`•\u20  `);
        MALFORMED(`•\u202 `);
        MALFORMED(`•\uuuuu2`);
        MALFORMED(`•\uuuuu20`);
        MALFORMED(`•\uuuuu202`);
        MALFORMED(`•\uuuuu2   `);
        MALFORMED(`•\uuuuu20  `);
        MALFORMED(`•\uuuuu202 `);
        MALFORMED(`•\uG`);
        MALFORMED(`•\u2G`);
        MALFORMED(`•\u20G`);
        MALFORMED(`•\uG   `);
        MALFORMED(`•\u2G  `);
        MALFORMED(`•\u20G `);
        MALFORMED(`•\uuuuuG`);
        MALFORMED(`•\uuuuu2G`);
        MALFORMED(`•\uuuuu20G`);
        MALFORMED(`•\uuuuuG   `);
        MALFORMED(`•\uuuuu2G  `);
        MALFORMED(`•\uuuuu20G `);
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
    static void EQ(String input, String expected) {
        if (input == null || expected == null || !expected.equals(input)) {
            report("Failed EQ", "Input:", input, "Expected:", expected);
        }
    }

    /*
     * Raise an exception if the string contains a malformed escape.
     */
    static void WELLFORMED(String rawString) {
        try {
            rawString.unescape();
        } catch (MalformedEscapeException ex) {
            System.err.println("Failed WELLFORMED");
            System.err.println(rawString);
            throw new RuntimeException();
        }
    }

    /*
     * Raise an exception if the string does not contain a malformed escape.
     */
    static void MALFORMED(String rawString) {
        try {
            rawString.unescape();
            System.err.println("Failed MALFORMED");
            System.err.println(rawString);
            throw new RuntimeException();
        } catch (MalformedEscapeException ex) {
            // incorrectly formed escapes
        }
    }
}
