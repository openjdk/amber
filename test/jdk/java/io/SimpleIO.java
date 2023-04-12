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

/* @test
 * @bug 8302326
 * @summary SimpleIO tests
 * @run main SimpleIO
 */

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import static java.io.SimpleIO.*;
import static java.nio.charset.StandardCharsets.UTF_8;

public class SimpleIO {
    static void provideInput(String string) {
        byte[] bytes = (string + "\n").getBytes(UTF_8);
        InputStream instream = new ByteArrayInputStream(bytes);
        System.setIn(instream);
    }

    static void testInput() {
        provideInput("Joan");
        String i1 = input("Name >> ");
        println(1, i1);

        provideInput("Pelham");
        print("Address >> ");
        String i2 = input("");
        println(2, i2);
    }

    static void testPrimitiveInput() {
        provideInput("123");
        int i = inputInt("");
        println(i);

        provideInput("12345678901234567");
        long l = inputLong("");
        println(l);

        provideInput("1.3333333333");
        float f = inputFloat("");
        println(f);

        provideInput("1.3333333333");
        double d = inputDouble("");
        println(d);

        try {
            provideInput("xyz");
            i = inputInt("");
            throw new RuntimeException("inputInt NumberFormatException not thrown");
        } catch (NumberFormatException ex) {
            // fine
        }

        try {
            provideInput("xyz");
            l = inputLong("");
            throw new RuntimeException("inputLong NumberFormatException not thrown");
        } catch (NumberFormatException ex) {
            // fine
        }

        try {
            provideInput("xyz");
            f = inputFloat("");
            throw new RuntimeException("inputFloat NumberFormatException not thrown");
        } catch (NumberFormatException ex) {
            // fine
        }

        try {
            provideInput("xyz");
            d = inputDouble("");
            throw new RuntimeException("inputDouble NumberFormatException not thrown");
        } catch (NumberFormatException ex) {
            // fine
        }
    }

    static void testPrint() {
        print();

        print("Hello ");
        print("Joan");
        println();

        print((Object[])null);
        print((byte)1, (short)2, (int)3, (long)4, (float)5.7, (double)8.9, 'a', true);
        print(List.of("a", "b", "c"));
        println();
    }

    static void testPrintln() {
        print();

        println("Hello ");
        println("Joan");
        println();

        println((Object[])null);
        println((byte)1, (short)2, (int)3, (long)4, (float)5.7, (double)8.9, 'a', true);
        println(List.of("a", "b", "c"));
        println();
    }

    static void testPrintLines() {
        printLines(List.of("a", "b", "c"));
    }

    static void testReadWriteFilename() {
        String content = """
           "Hope" is the thing with feathers
           That perches in the soul
           And sings the tune without the words
           And never stops - at all
           """;
        String filename = "data1.txt";
        write(filename, content);
        print(read(filename));
    }

    static void testReadWritePath() {
        String content = """
           "Hope" is the thing with feathers
           That perches in the soul
           And sings the tune without the words
           And never stops - at all
           """;
        Path path = Path.of("data2.txt");
        write(path, content);
        print(read(path));
    }

    static void testThrown(String test, Consumer<String> consumer) {
        try {
            consumer.accept(test);
            throw new RuntimeException(test + " exception not thrown");
        } catch (Throwable ex) {
            println(test + " " + ex.getClass().getSimpleName() + " thrown");
        }
    }

    static void testForExceptions() {
        testThrown("input", t -> input(null));

        testThrown("printLines null list", t -> printLines(null));

        testThrown("read null filename", t -> read((String)null));
        testThrown("read bad filename", t -> read(""));
        testThrown("read bad filename", t -> read("/\\"));
        testThrown("read null path", t -> read((Path)null));

        testThrown("write null filename", t -> write((String)null, ""));
        testThrown("write filename null content", t -> write("data3.txt", null));
        testThrown("write bad filename", t -> write("", ""));
        testThrown("write bad filename", t -> write("/\\", ""));
        testThrown("write null path", t -> write((Path)null, ""));
        testThrown("write path null content", t -> write(Path.of("data4.txt"), null));

        testThrown("writeLines null filename", t -> writeLines((String)null, List.of()));
        testThrown("writeLines filename null lines", t -> writeLines("data3.txt", null));
        testThrown("writeLines bad filename", t -> writeLines("", List.of()));
        testThrown("writeLines bad filename", t -> writeLines("/\\", List.of()));
        testThrown("writeLines null path", t -> writeLines((Path)null, List.of()));
        testThrown("writeLines path null lines", t -> writeLines(Path.of("data4.txt"), null));
    }

    static boolean isJLinePresent() {
        try {
            Class<?> lrClass = Class.forName("jdk.internal.org.jline.reader.LineReader",
                    false, ClassLoader.getSystemClassLoader());
            return lrClass != null;

        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    static final String INPUT_EXPECTED = """
Name >>
1 Joan
Address >>
2 Pelham
124
12345678901234567
1.33334
1.3333333333
""";

    static final String EXPECTED = """
Hello Joan
null1 2 3 4 5.7 8.9 a true[a, b, c]
Hello\s
Joan

null
1 2 3 4 5.7 8.9 a true
[a, b, c]

a
b
c
"Hope" is the thing with feathers
That perches in the soul
And sings the tune without the words
And never stops - at all
"Hope" is the thing with feathers
That perches in the soul
And sings the tune without the words
And never stops - at all
input NullPointerException thrown
printLines null list NullPointerException thrown
read null filename NullPointerException thrown
read bad filename UncheckedIOException thrown
read bad filename UncheckedIOException thrown
read null path NullPointerException thrown
write null filename NullPointerException thrown
write filename null content NullPointerException thrown
write bad filename UncheckedIOException thrown
write bad filename UncheckedIOException thrown
write null path NullPointerException thrown
write path null content NullPointerException thrown
writeLines null filename NullPointerException thrown
writeLines filename null lines NullPointerException thrown
writeLines bad filename UncheckedIOException thrown
writeLines bad filename UncheckedIOException thrown
writeLines null path NullPointerException thrown
writeLines path null lines NullPointerException thrown
""";

    public static void main(String[] args) throws IOException {
        String expected = EXPECTED;
        PrintStream out = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos, true, UTF_8);
        System.setOut(ps);

        if (!isJLinePresent()) {
            expected = INPUT_EXPECTED + EXPECTED;
            testInput();
            testPrimitiveInput();
        }

        testPrint();
        testPrintln();
        testPrintLines();
        testReadWriteFilename();
        testReadWritePath();
        testForExceptions();

        String output = baos.toString(UTF_8);

        if (!expected.strip().equals(output.strip())) {
            System.setOut(out);
            println();
            println("Expected:");
            println(expected);
            println();
            println("Output:");
            println(output);
            throw new RuntimeException("Unexpected output");
        }
    }
}
