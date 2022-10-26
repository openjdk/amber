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
 * @since  20
 */
public final class IO {
    /**
     * Private constructor.
     */
    private IO() {
        throw new AssertionError("private constructor");
    }

    /**
     * Convenience field for {@link System#in}.
     */
    public static final InputStream IN = System.in;

    /**
     * Convenience field for {@link System#out}.
     */
    public static final PrintStream OUT = System.out;

    /**
     * Convenience field for {@link System#err}.
     */
    public static final PrintStream ERR = System.err;

    /**
     * Environment variables for this session.
     */
    public static final Map<String, String> ENVS = new HashMap<>(System.getenv());

    /**
     * Command line arguments.
     */
    public static String[] ARGS = new String[0];

    /**
     * Working directory.
     */
    public static String CWD = System.getProperty("user.dir");

    /**
     * The stdout output from the last process run.
     */
    private static List<String> lastOutput = List.of();

    /**
     * The stderr output from the last process run.
     */
    private static List<String> lastErrorOutput = List.of();

    /**
     * The error code from the last process run.
     */
    private static int lastError = 0;

    /**
     * Set ARGS from the launcher.
     *
     * @param args arguments from the command line.
     */
    public static void setArgs(String[] args) {
        ARGS = args;
    }

    /**
     * {@return the output, as a list of strings, from the last command or exec}
     */
    public static List<String> lastOutput() {
        return lastOutput;
    }

    /**
     * {@return the error output, as a list of strings, from the last command or exec}
     */
    public static List<String> lastErrorOutput() {
        return lastErrorOutput;
    }

    /**
     * {@return the error code from the last command or exec}
     */
    public static int lastError() {
        return lastError;
    }

    /**
     * {@return the difference, measured in milliseconds, between
     * the current time and midnight, January 1, 1970 UTC}
     */
    public static long milliTime() {
        return System.currentTimeMillis();
    }

    /**
     * {@return the current value of the running Java Virtual Machine's
     * high-resolution time source, in nanoseconds}
     */
    public static long nanoTime() {
        return System.nanoTime();
    }

    /**
     * Convenience method for {@link System#arraycopy}.
     *
     * @param src     the source array.
     * @param srcPos  starting position in the source array.
     * @param dest    the destination array.
     * @param destPos starting position in the destination data.
     * @param length  the number of array elements to be copied.
     * @throws IndexOutOfBoundsException if copying would cause
     *                                   access of data outside array bounds.
     * @throws ArrayStoreException       if an element in the {@code src}
     *                                   array could not be stored into the {@code dest} array
     *                                   because of a type mismatch.
     * @throws NullPointerException      if either {@code src} or
     *                                   {@code dest} is {@code null}.
     */
    public static void arraycopy(Object src, int srcPos,
                                 Object dest, int destPos,
                                 int length) {
        Objects.requireNonNull(src, "src must not be null");
        Objects.requireNonNull(dest, "dest must not be null");
        System.arraycopy(src, srcPos, dest, destPos, length);
    }

    /**
     * {@return the system-dependent line separator string}
     */
    public static String lineSeparator() {
        return System.lineSeparator();
    }

    /**
     * Exit the application returning the specified error code.
     *
     * @param status exit status.
     * @throws SecurityException if a security manager exists and its {@code checkExit}
     *                           method doesn't allow exit with the specified status.
     */
    public static void exit(int status) {
        System.exit(status);
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
     * Return a string of characters read in from input after issuing a prompt but hide input.
     *
     * @param prompt string contain prompt for input
     * @return a string of characters read in from input or null if no input is available.
     */
    public static String readPassword(String prompt) {
        Objects.requireNonNull(prompt, "prompt must not be null");

        if (LineReader.hasLineReader()) {
            return LineReader.readLine(prompt, (char)0);
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

    /**
     * Wwrite a formatted string to output stream
     * using the specified format string and arguments.
     * Equivalent to invoking {@code System.out.printf(String, Object...)}.
     *
     * @param format A format string as described in <a
     *               href="../util/Formatter.html#syntax">Format string syntax</a>
     * @param args   Arguments referenced by the format specifiers in the format
     *               string.  If there are more arguments than format specifiers, the
     *               extra arguments are ignored.  The number of arguments is
     *               variable and may be zero.  The maximum number of arguments is
     *               limited by the maximum dimension of a Java array as defined by
     *               <cite>The Java Virtual Machine Specification</cite>.
     *               The behaviour on a
     *               {@code null} argument depends on the <a
     *               href="../util/Formatter.html#syntax">conversion</a>.
     * @throws java.util.IllegalFormatException If a format string contains an illegal syntax, a format
     *                                          specifier that is incompatible with the given arguments,
     *                                          insufficient arguments given the format string, or other
     *                                          illegal conditions.  For specification of all possible
     *                                          formatting errors, see the <a
     *                                          href="../util/Formatter.html#detail">Details</a> section of the
     *                                          formatter class specification.
     * @throws NullPointerException             If the {@code format} is {@code null}
     */
    public static void printf(String format, Object... args) {
        Objects.requireNonNull(format, "format must not be null");
        Objects.requireNonNull(args, "args must not be null");
        System.out.printf(format, args);
    }

    /**
     * Executes a system command as a separate process, returning the process
     * output as a list of strings. Command arguments are passed as a separate
     * list.
     *
     * @param command command to be executed
     * @param args    list of command arguments
     * @return output from the command as a list of strings
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
            pb.directory(Path.of(CWD).toFile());
            pb.environment().clear();
            pb.environment().putAll(ENVS);
            Process process = pb.start();
            Thread outputThread = new Thread(() -> {
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(process.getInputStream()));
                lastOutput = reader.lines().toList();
            });
            outputThread.start();
            Thread errorThread = new Thread(() -> {
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(process.getErrorStream()));
                lastErrorOutput = reader.lines().toList();
            });
            errorThread.start();
            lastError = process.waitFor();
            outputThread.join();
            errorThread.join();
            return lastOutput;
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException("Process failed", ex);
        }
    }

    /**
     * Executes a system command as a separate process, returning the process
     * output as a list of strings. Command arguments are passed as separate
     * arguments.
     *
     * @param command command to be executed
     * @param args    command arguments
     * @return output from the command as a list of strings
     * @throws RuntimeException if the process fails
     */
    public static List<String> exec(String command, String... args) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(args, "args must not be null");
        return exec(command, List.of(args));
    }

    /**
     * Executes a system command as a separate process, returning the process
     * output as a list of strings. Command arguments are space separated in the
     * command line.
     *
     * @param commandLine command and arguments as a single line
     * @return output from the command as a list of strings
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
