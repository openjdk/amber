/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 0000000
 * @summary Exercise runtime handing of templated strings.
 * @compile --enable-preview -source ${jdk.version} Basic.java
 * @run main/othervm --enable-preview Basic
 */

import java.util.*;
import static java.lang.TemplatePolicy.STR;

public class Basic {
    public static void main(String... arg) {
        test1();
        test2();
        test3();
    }

    /*
     * Concatenation tests.
     */
    static void test1() {
        int x = 10;
        int y = 20;

        ASSERT(STR."\{x} \{y}", x + " " + y);
        ASSERT(STR."\{x + y}", "" + (x + y));
        ASSERT(STR.apply("\{x} \{y}"), x + " " + y);
        ASSERT(STR.apply("\{x + y}"), "" + (x + y));
        ASSERT(("\{x} \{y}").apply(STR), x + " " + y);
        ASSERT(("\{x + y}").apply(STR), "" + (x + y));
    }

    /*
     * Component tests.
     */
    static void test2() {
        int x = 10;
        int y = 20;

        TemplatedString ts = "\{x} + \{y} = \{x + y}";
        ASSERT(ts.stencil(), "\uFFFC + \uFFFC = \uFFFC");
        ASSERT(ts.values(), List.of(x, y, x + y));
        ASSERT(ts.fragments(), List.of("", " + ", " = ", ""));
        ASSERT(ts.concat(), STR."\{x} + \{y} = \{x + y}");
    }

    /*
     * Limits tests.
     */
    static void test3() {
        int x = 9;

        TemplatedString ts250 = """
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}

             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}

             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             """;
        ASSERT(ts250.values().size(), 250);
        ASSERT(ts250.concat(), """
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999

               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999

               9999999999 9999999999
               9999999999 9999999999
               9999999999
               """);

        TemplatedString ts251 = """
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}

             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}

             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}
             """;
        ASSERT(ts251.values().size(), 251);
        ASSERT(ts251.concat(), """
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999

               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999

               9999999999 9999999999
               9999999999 9999999999
               9999999999 9
               """);

        TemplatedString ts252 = """
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}

             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}

             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}
             """;
        ASSERT(ts252.values().size(), 252);
        ASSERT(ts252.concat(), """
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999

               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999

               9999999999 9999999999
               9999999999 9999999999
               9999999999 99
               """);

        TemplatedString ts253 = """
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}

             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}

             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}
             """;
        ASSERT(ts253.values().size(), 253);
        ASSERT(ts253.concat(), """
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999

               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999

               9999999999 9999999999
               9999999999 9999999999
               9999999999 999
               """);

        TemplatedString ts254 = """
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}

             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}

             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}
             """;
        ASSERT(ts254.values().size(), 254);
        ASSERT(ts254.concat(), """
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999

               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999

               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999
               """);

        TemplatedString ts255 = """
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}

             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}

             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}
             """;
        ASSERT(ts255.values().size(), 255);
        ASSERT(ts255.concat(), """
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999

               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999

               9999999999 9999999999
               9999999999 9999999999
               9999999999 99999
               """);

        TemplatedString ts256 = """
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}

             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}

             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}
             \{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x}\{x} \{x}\{x}\{x}\{x}\{x}\{x}
             """;
        ASSERT(ts256.values().size(), 256);
        ASSERT(ts256.concat(), """
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999

               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999
               9999999999 9999999999

               9999999999 9999999999
               9999999999 9999999999
               9999999999 999999
               """);

    }

    static void ASSERT(String a, String b) {
        if (!Objects.equals(a, b)) {
            throw new RuntimeException("Test failed");
        }
    }

    static void ASSERT(Object a, Object b) {
        if (!Objects.deepEquals(a, b)) {
            throw new RuntimeException("Test failed");
        }
    }

    static void REJECT(String a, String b) {
        if (Objects.equals(a, b)) {
            throw new RuntimeException("Test failed");
        }
    }

    static void REJECT(Object a, Object b) {
        if (Objects.deepEquals(a, b)) {
            throw new RuntimeException("Test failed");
        }
    }


      /*

         */
}
