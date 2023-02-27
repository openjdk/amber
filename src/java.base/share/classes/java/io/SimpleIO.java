/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package java.io;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class provides simple to use I/O methods for simple applications. Typical use
 * would include {@code import static java.io.SinmpleIO.*; } in declarations.
 * There are two groups of methods;
 * <p>
 * - Console I/O methods include
 * {@linkplain SimpleIO#input()},
 * {@linkplain SimpleIO#input(String)},
 * {@linkplain SimpleIO#print(Object...)},
 * {@linkplain SimpleIO#println(Object...)} and
 * {@linkplain SimpleIO#printLines(List<String>)}
 * <p>
 * - File I/O methods include
 * {@linkplain SimpleIO#read(String)},
 * {@linkplain SimpleIO#readLines(String)},
 * {@linkplain SimpleIO#write(String, String)} and
 * {@linkplain SimpleIO#writeLines(String, List<String>)}.
 * @since  21
 */
public final class SimpleIO {
    /**
     * Private constructor.
     */
    private SimpleIO() {
        throw new AssertionError("private constructor");
    }

    /**
     * Fetch jline LineReader lazily.
     */
    private static class LineReader {
        /**
         * MethodHandle to LineReader::readLine.
         */
        private static final MethodHandle READ_LINE_MH;
        /**
         * MethodHandle to LineReader::readLine with mask.
         */
        private static final MethodHandle READ_LINE_MASK_MH;

        /**
         * Instance of LineReader.
         */
        private static final Object LINE_READER;

        static {
            MethodHandle readLineMH = null;
            MethodHandle readLineMaskMH = null;
            Object lineReader = null;

            try {
                Class<?> lrbClass = Class.forName("jdk.internal.org.jline.reader.LineReaderBuilder",
                        false, ClassLoader.getSystemClassLoader());
                Class<?> lrClass = Class.forName("jdk.internal.org.jline.reader.LineReader",
                        false, ClassLoader.getSystemClassLoader());
                Module lrbModule = lrbClass.getModule();
                Module baseModule = Object.class.getModule();
                baseModule.addReads(lrbModule);
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                MethodHandle builderMH = lookup.findStatic(lrbClass, "builder", MethodType.methodType(lrbClass));
                MethodHandle buildMH = lookup.findVirtual(lrbClass, "build", MethodType.methodType(lrClass));
                readLineMH = lookup.findVirtual(lrClass, "readLine",
                        MethodType.methodType(String.class, String.class));
                readLineMaskMH = lookup.findVirtual(lrClass, "readLine",
                        MethodType.methodType(String.class, String.class, Character.class));
                Object builder = builderMH.invoke();
                lineReader = buildMH.invoke(builder);
            } catch (Throwable ex) {
                readLineMH = null;
                readLineMaskMH = null;
                lineReader = null;
            }

            READ_LINE_MH = readLineMH;
            READ_LINE_MASK_MH = readLineMaskMH;
            LINE_READER = lineReader;
        }

        /**
         * {@return true if LineReader is available.}
         */
        private static boolean hasLineReader() {
            return LINE_READER != null;
        }

        /**
         * Invoke LineReader::readLine.
         *
         * @param prompt Read line prompt.
         * @return Line read in.
         */
        private static String readLine(String prompt) {
            try {
                return (String) READ_LINE_MH.invoke(LINE_READER, prompt);
            } catch (Throwable ex) {
                return null;
            }
        }

        /**
         * Invoke LineReader::readLine with mask.
         *
         * @param prompt Read line prompt.
         * @return Line read in.
         */
        private static String readLine(String prompt, Character mask) {
            try {
                return (String) READ_LINE_MASK_MH.invoke(LINE_READER, prompt, mask);
            } catch (Throwable ex) {
                return null;
            }
        }
    }

    /**
     * Return a string of characters from input. Unlike other input methods, this method
     * supports editing and navigation of the input, as well as scrolling back through
     * historic input.
     *
     * @return a string of characters read in from input
     */
    public static String input() {
        return input("");
    }

    /**
     * Return a string of characters from input after issuing a prompt. Unlike other
     * input methods, this method supports editing and navigation of the input, as well
     * as scrolling back through historic input.
     *
     * @param prompt string contain prompt for input
     * @return a string of characters read in from input
     */
    public static String input(String prompt) {
        Objects.requireNonNull(prompt, "prompt must not be null");

        if (LineReader.hasLineReader()) {
            String input = LineReader.readLine(prompt);
            return input != null ? input : "";
        } else {
            System.out.print(prompt);
            Scanner scanner = new Scanner(System.in);
            return scanner.hasNext() ? scanner.nextLine() : "";
        }
    }

    /**
     * Printing values with single spacing to output stream.
     *
     * @param values values to be printed.
     */
    public static void print(Object... values) {
        System.out.print(Stream.of(values)
                .map(String::valueOf)
                .collect(Collectors.joining(" ")));
    }

    /**
     * Print values with single spacing followed by a newline to output stream.
     *
     * @param values values to be printed.
     */
    public static void println(Object... values) {
        System.out.println(Stream.of(values)
                .map(String::valueOf)
                .collect(Collectors.joining(" ")));
    }

    /**
     * Print list of lines to output stream. Each line will be printed and
     * followed by a line terminator.
     *
     * @param lines list of strings
     */
    public static void printLines(List<String> lines) {
        Objects.requireNonNull(lines, "lines must not be null");
        lines.forEach(System.out::println);
    }

    /**
     * Reading the contents from the specified file as a string.
     *
     * @param filename file name or path string of file to be read
     * @return Content read from a file
     * @throws RuntimeException wrapping an IOException if an io error occurs.
     */
    public static String read(String filename) {
        Objects.requireNonNull(filename, "filename must not be null");
        return readLines(filename).stream()
                .collect(Collectors.joining("\n", "", "\n"));
    }

    /**
     * Reading all the lines from the specified file as a list of strings.
     *
     * @param filename file name or path string of file to be read
     * @return list of lines read from a file
     * @throws RuntimeException wrapping an IOException if an io error occurs.
     */
    public static List<String> readLines(String filename) {
        Objects.requireNonNull(filename, "filename must not be null");
        Path path = Paths.get(filename);

        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Write a string as content to a file.
     *
     * @param filename file name or path string of file to be written
     * @param content  string content of the file
     */
    public static void write(String filename, String content) {
        Objects.requireNonNull(filename, "filename must not be null");
        Objects.requireNonNull(content, "content must not be null");
        try (PrintWriter out = new PrintWriter(filename)) {
            content.lines().forEach(out::println);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Write a list of strings to a file. Each line will be followed by
     * a line terminator.
     *
     * @param filename file name or path string of file to be written
     * @param lines list of lines to be written to the file
     */
    public static void writeLines(String filename, List<String> lines) {
        Objects.requireNonNull(filename, "filename must not be null");
        Objects.requireNonNull(lines, "lines must not be null");
        try (PrintWriter out = new PrintWriter(filename)) {
            lines.forEach(out::println);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
