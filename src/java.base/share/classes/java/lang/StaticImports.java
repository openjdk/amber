/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.access.JavaLangAccess;
import jdk.internal.access.SharedSecrets;

import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * This class provides automatic access to commonly used static methods and
 * constant static fields.
 *
 * @since  20
 */
public final class StaticImports {
    /**
     * Private constructor.
     */
    private StaticImports() {
        throw new AssertionError("private constructor");
    }

    /**
     * Convenience field for {@link System#in}.
     */
    public static InputStream IN = System.in;

    /**
     * Convenience field for {@link System#out}.
     */
    public static PrintStream OUT = System.out;

    /**
     * Convenience field for {@link System#err}.
     */
    public static PrintStream ERR = System.err;

    /**
     * Convenience method for {@link System#currentTimeMillis}.
     * @return  the difference, measured in milliseconds, between
     *          the current time and midnight, January 1, 1970 UTC.
     */
    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * Convenience method for {@link System#nanoTime}.
     * @return the current value of the running Java Virtual Machine's
     *         high-resolution time source, in nanoseconds
     */
    public static long nanoTime() {
        return System.nanoTime();
    }

    /**
     * Convenience method for {@link System#arraycopy}.
     * @param      src      the source array.
     * @param      srcPos   starting position in the source array.
     * @param      dest     the destination array.
     * @param      destPos  starting position in the destination data.
     * @param      length   the number of array elements to be copied.
     * @throws     IndexOutOfBoundsException  if copying would cause
     *             access of data outside array bounds.
     * @throws     ArrayStoreException  if an element in the {@code src}
     *             array could not be stored into the {@code dest} array
     *             because of a type mismatch.
     * @throws     NullPointerException if either {@code src} or
     *             {@code dest} is {@code null}.
     */
    public static void arraycopy(Object src,  int  srcPos,
                                 Object dest, int destPos,
                                 int length) {
        Objects.requireNonNull(src, "src must not be null");
        Objects.requireNonNull(dest, "dest must not be null");
        System.arraycopy(src, srcPos, dest, destPos, length);
    }

    /**
     * Convenience method for {@link System#lineSeparator()}.
     * @return the system-dependent line separator string
     */
    public static String lineSeparator() {
        return System.lineSeparator();
    }

    /**
     * Convenience method for {@link System#exit(int)}.
     * @param      status   exit status.
     * @throws  SecurityException
     *          if a security manager exists and its {@code checkExit}
     *          method doesn't allow exit with the specified status.
     */
    public static void exit(int status) {
        System.exit(status);
    }

    /**
     * Gets the value of the specified environment variable. An
     * environment variable is a system-dependent externally named
     * value.
     *
     * @param  name the name of the environment variable
     *
     * @return the string value of the variable, or {@code null}
     *         if the variable is not defined in the system environment
     *
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public static String getenv(String name) {
        Objects.requireNonNull(name, "name must not be null");

        try {
            return System.getenv(name);
        } catch (SecurityException ex) {
            return null;
        }
    }

    /**
     * jline LineReader fetcher.
     */
    private class LineReader {
        /**
         * MethodHandle to LineReader::readLine.
         */
        private static final MethodHandle READ_LINE_MH;

        /**
         * Instance of LineReader.
         */
        private static final Object LINE_READER;

        static {
            MethodHandle readLineMH = null;
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
                Object builder = builderMH.invoke();
                lineReader = buildMH.invoke(builder);
            } catch (Throwable ex) {
                readLineMH = null;
                lineReader = null;
            }

            READ_LINE_MH = readLineMH;
            LINE_READER = lineReader;
        }

        /**
         * {@return true if LineReader is available.}
         */
        static boolean hasLineReader() {
            return LINE_READER != null;
        }

        /**
         * Invoke LineReader::readLine.
         *
         * @param prompt Read line prompt.
         *
         * @return Line read in.
         */
        static String readLine(String prompt) {
            try {
                return (String) READ_LINE_MH.invoke(LINE_READER, prompt);
            } catch (Throwable ex) {
                return null;
            }
        }
    }

    /**
     * Convenience method for readline the next line of input.
     * @return a string of characters read in from input.
     */
    public static String readln() {
        return readln("");
    }

    /**
     * Convenience method for readline the next line of input.
     * @param prompt  string contain prompt for input
     * @return a string of characters read in from input.
     */
    public static String readln(String prompt) {
        Objects.requireNonNull(prompt, "prompt must not be null");

        if (LineReader.hasLineReader()) {
            return LineReader.readLine(prompt);
        } else {
            return System.console().readLine(prompt);
        }
    }

    /**
     * Convenience method for reading all the lines from a file.
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
     * Convenience method for printing a list of strings.
     * @param lines list of strings
     */
    public static void printlines(List<String> lines) {
        Objects.requireNonNull(lines, "lines must not be null");
        lines.forEach(System.out::println);
    }

    /**
     * Convenience method for writing a list of strings to a file.
     * @param filename file name or path string of file to be written
     * @param lines list of lines to be written to the file
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

    /** Convenience method for printing a boolean value.
     * Equivalent to invoking {@code System.out.print(boolean)}.
     * @param value value to be printed.
     */
    public static void print(boolean value) {
        System.out.print(value);
    }

    /** Convenience method for printing a char value.
     * Equivalent to invoking {@code System.out.print(char)}.
     * @param value value to be printed.
     */
    public static void print(char value) {
        System.out.print(value);
    }

    /** Convenience method for printing a char array value.
     * Equivalent to invoking {@code System.out.print(char[])}.
     * @param value value to be printed.
     */
    public static void print(char[] value) {
        System.out.print(value);
    }

    /** Convenience method for printing a integer value.
     * Equivalent to invoking {@code System.out.print(int)}.
     * @param value value to be printed.
     */
    public static void print(int value) {
        System.out.print(value);
    }

    /** Convenience method for printing a long value.
     * Equivalent to invoking {@code System.out.print(long)}.
     * @param value value to be printed.
     */
    public static void print(long value) {
        System.out.print(value);
    }

    /** Convenience method for printing a float value.
     * Equivalent to invoking {@code System.out.print(float)}.
     * @param value value to be printed.
     */
    public static void print(float value) {
        System.out.print(value);
    }

    /** Convenience method for printing a double value.
     * Equivalent to invoking {@code System.out.print(double)}.
     * @param value value to be printed.
     */
    public static void print(double value) {
        System.out.print(value);
    }

    /** Convenience method for printing a string value.
     * Equivalent to invoking {@code System.out.print(String)}.
     * @param value value to be printed.
     */
    public static void print(String value) {
        System.out.print(value);
    }

    /** Convenience method for printing an instance value.
     * Equivalent to invoking {@code System.out.print(Object)}.
     * @param value value to be printed.
     */
    public static void print(Object value) {
        System.out.print(value);
    }

    /** Convenience method for printlning a boolean value followed by the
     * platform lineSeparator.
     * Equivalent to invoking {@code System.out.println(boolean)}.
     * @param value value to be printlned.
     */
    public static void println(boolean value) {
        System.out.println(value);
    }

    /** Convenience method for printlning a charvalue followed by the
     * platform lineSeparator.
     * Equivalent to invoking {@code System.out.println(char)}.
     * @param value value to be printlned.
     */
    public static void println(char value) {
        System.out.println(value);
    }

    /** Convenience method for printlning a char arrayvalue followed by the
     * platform lineSeparator.
     * Equivalent to invoking {@code System.out.println(char[])}.
     * @param value value to be printlned.
     */
    public static void println(char[] value) {
        System.out.println(value);
    }

    /** Convenience method for printlning a integervalue followed by the
     * platform lineSeparator.
     * Equivalent to invoking {@code System.out.println(int)}.
     * @param value value to be printlned.
     */
    public static void println(int value) {
        System.out.println(value);
    }

    /** Convenience method for printlning a longvalue followed by the
     * platform lineSeparator.
     * Equivalent to invoking {@code System.out.println(long)}.
     * @param value value to be printlned.
     */
    public static void println(long value) {
        System.out.println(value);
    }

    /** Convenience method for printlning a floatvalue followed by the
     * platform lineSeparator.
     * Equivalent to invoking {@code System.out.println(float)}.
     * @param value value to be printlned.
     */
    public static void println(float value) {
        System.out.println(value);
    }

    /** Convenience method for printlning a doublevalue followed by the
     * platform lineSeparator.
     * Equivalent to invoking {@code System.out.println(double)}.
     * @param value value to be printlned.
     */
    public static void println(double value) {
        System.out.println(value);
    }

    /** Convenience method for printlning a stringvalue followed by the
     * platform lineSeparator.
     * Equivalent to invoking {@code System.out.println(String)}.
     * @param value value to be printlned.
     */
    public static void println(String value) {
        System.out.println(value);
    }

    /** Convenience method for printlning an instancevalue followed by the
     * platform lineSeparator.
     * Equivalent to invoking {@code System.out.println(Object)}.
     * @param value value to be printlned.
     */
    public static void println(Object value) {
        System.out.println(value);
    }

    /**
     * Convenience method to write a formatted string to this output stream
     * using the specified format string and arguments.
     * Equivalent to invoking {@code System.out.printf(String, Object...)}.
     * @param  format
     *         A format string as described in <a
     *         href="../util/Formatter.html#syntax">Format string syntax</a>
     * @param  args
     *         Arguments referenced by the format specifiers in the format
     *         string.  If there are more arguments than format specifiers, the
     *         extra arguments are ignored.  The number of arguments is
     *         variable and may be zero.  The maximum number of arguments is
     *         limited by the maximum dimension of a Java array as defined by
     *         <cite>The Java Virtual Machine Specification</cite>.
     *         The behaviour on a
     *         {@code null} argument depends on the <a
     *         href="../util/Formatter.html#syntax">conversion</a>.
     * @throws  java.util.IllegalFormatException
     *          If a format string contains an illegal syntax, a format
     *          specifier that is incompatible with the given arguments,
     *          insufficient arguments given the format string, or other
     *          illegal conditions.  For specification of all possible
     *          formatting errors, see the <a
     *          href="../util/Formatter.html#detail">Details</a> section of the
     *          formatter class specification.
     * @throws  NullPointerException
     *          If the {@code format} is {@code null}
     */
    public static void printf(String format, Object ... args) {
        Objects.requireNonNull(format, "format must not be null");
        Objects.requireNonNull(args, "args must not be null");
        System.out.printf(format, args);
    }

    /**
     * Executes a system command as a separate process, returning the process
     * output as a list of strings. Command arguments are passed as a separate
     * list.
     *
     * @param command  command to be executed
     * @param args     list of command arguments
     *
     * @return output from the command as a list of strings
     *
     * @throws RuntimeException if the process fails
     */
    public static List<String> exec(String command, List<String> args) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(args, "args must not be null");
        try {
            List<String> all = new ArrayList<>(args.size() + 1);
            all.add(command);
            all.addAll(args);
            ProcessBuilder pb = new ProcessBuilder(all);
            Process process = pb.start();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));
            List<String> result = reader.lines().toList();
            int code = process.waitFor();
            if (code != 0) {
                throw new RuntimeException("Error code = " + code);
            }
            return result;
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException("Process failed", ex);
        }
    }

    /**
     * Executes a system command as a separate process, returning the process
     * output as a list of strings. Command arguments are passed as separate
     * arguments.
     *
     * @param command  command to be executed
     * @param args     command arguments
     *
     * @return output from the command as a list of strings
     *
     * @throws RuntimeException if the process fails
     */
    public static List<String> exec(String command, String... args) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(args, "args must not be null");
        return exec(command, List.of(args));
    }

    /**
     * Executes a system command as a separate process, returning the process
     * output as a list of strings. Command arguments are space separated on the
     * command line.
     *
     * @param commandLine  command and arguments as a single line
     *
     * @return output from the command as a list of strings
     *
     * @throws RuntimeException if the process fails
     */
    public static List<String> command(String commandLine) {
        Objects.requireNonNull(commandLine, "command must not be null");
        StringTokenizer st = new StringTokenizer(commandLine);
        List<String> args = new ArrayList<>(st.countTokens() - 1);
        String command = st.hasMoreTokens() ? st.nextToken() : "";

        for (int i = 0; st.hasMoreTokens(); i++) {
            args.add(st.nextToken());
        }

        return exec(command, args);
    }
}
