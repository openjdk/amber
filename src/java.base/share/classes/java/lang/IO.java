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

package java.lang;

import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class provides quick access to commonly used methods, fields and tasks.
 *
 * @since  21
 */
public final class IO {
    /**
     * Private constructor.
     */
    private IO() {
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
     * {@return a string of characters read in from input}
     */
    public static String readln() {
        return readln("");
    }

    /**
     * Return a string of characters read in from input after issuing a prompt.
     *
     * @param prompt string contain prompt for input
     * @return a string of characters read in from input or null if no input is available.
     */
    public static String readln(String prompt) {
        Objects.requireNonNull(prompt, "prompt must not be null");

        if (LineReader.hasLineReader()) {
            return LineReader.readLine(prompt);
        } else {
            System.out.print(prompt);
            Scanner input = new Scanner(System.in);
            return input.hasNext() ? input.nextLine() : "";
        }
    }

    /**
     * Reading all the lines from the specified file.
     *
     * @param filename file name or path string of file to be read
     * @return list of lines read from a file
     * @throws RuntimeException wrapping an IOException if an io error occurs.
     */
    public static List<String> readlines(String filename) {
        Objects.requireNonNull(filename, "filename must not be null");
        Path path = Paths.get(filename);

        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Print list of lines to output stream.
     *
     * @param lines list of strings
     */
    public static void printlines(List<String> lines) {
        Objects.requireNonNull(lines, "lines must not be null");
        lines.forEach(System.out::println);
    }

    /**
     * Write a list of strings to a file.
     *
     * @param filename file name or path string of file to be written
     * @param lines    list of lines to be written to the file
     */
    public static void writelines(String filename, List<String> lines) {
        Objects.requireNonNull(lines, "filename must not be null");
        Objects.requireNonNull(lines, "lines must not be null");
        try (PrintWriter out = new PrintWriter(filename)) {
            lines.forEach(out::println);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Print values with single spacing followed by a newline to output stream.
     *
     * @param values values to be printed.
     */
    public static void println(Object... values) {
        String line = Stream.of(values)
                .map(String::valueOf)
                .collect(Collectors.joining(" "));
        System.out.println(line);
    }

    /**
     * Printing values with single spacing to output stream.
     *
     * @param values values to be printed.
     */
    public static void print(Object... values) {
        String line = Stream.of(values)
                .map(String::valueOf)
                .collect(Collectors.joining(" "));
        System.out.print(line);
    }
}
