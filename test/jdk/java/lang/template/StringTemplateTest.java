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
 * @enablePreview true
 */

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.*;
import java.util.function.Supplier;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import static javax.tools.StandardLocation.CLASS_OUTPUT;
import javax.tools.ToolProvider;

public class StringTemplateTest {
    enum Category{GENERAL, CHARACTER, INTEGRAL, BIG_INT, FLOATING, BIG_FLOAT, DATE};

    static final String[] BOOLS = {"true", "false"};
    static final String[] CHARS = {};
    static final String[] INTS = {};
    static final String[] BIGINTS = {};
    static final String[] FLOATS = {};
    static final String[] BIGFLOATS = {};
    static final String[] DATES = {"java.util.Calendar.getInstance().getTime()"};

    final Random r = new Random(1);

    String randomValue(Category category) {
        return switch (category) {
            case GENERAL -> randomChoice(
                    BOOLS,
                    () -> "(Object)null",
                    () -> randomValue(Category.CHARACTER),
                    () -> randomValue(Category.INTEGRAL),
                    () -> "\"" + randomString(r.nextInt(10)) + "\"");
            case CHARACTER -> randomChoice(
                    CHARS,
                    () -> "\'" + randomString(1) + "\'");
            case INTEGRAL -> randomChoice(
                    INTS,
                    () -> "(byte)" + String.valueOf(r.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE)),
                    () -> "(short)" + String.valueOf(r.nextInt(Short.MIN_VALUE, Short.MAX_VALUE)),
                    () -> String.valueOf(r.nextInt()),
                    () -> r.nextLong() + "l");
            case BIG_INT -> randomChoice(
                    BIGINTS,
                    () -> "new java.math.BigInteger(\"" + r.nextLong() + "\")");
            case FLOATING -> randomChoice(
                    FLOATS,
                    () -> String.valueOf(r.nextDouble()),
                    () -> r.nextFloat() + "f");
            case BIG_FLOAT -> randomChoice(
                    BIGFLOATS,
                    () -> "new java.math.BigDecimal(" + r.nextDouble() + ")");
            case DATE -> randomChoice(
                    DATES,
                    () -> "new java.util.Date(" + r.nextLong() + "l)",
                    () -> r.nextLong() + "l");
        };
    }

    String randomChoice(Supplier<String>... suppl) {
        return suppl[r.nextInt(suppl.length)].get();
    }

    String randomChoice(String... values) {
        return values[r.nextInt(values.length)];
    }

    String randomChoice(String[] values, Supplier<String>... suppl) {
        int i = r.nextInt(values.length + suppl.length);
        return i < values.length ? values[i] : suppl[i - values.length].get();
    }

    String randomString(int length) {
        var sb = new StringBuilder(length << 2);
        while (length-- > 0) {
            char ch = (char)r.nextInt(9, 128);
            var s = switch (ch) {
                case '\t' -> "\\t";
                case '\'' -> "\\\'";
                case '"' -> "\\\"";
                case '\r' -> "\\r";
                case '\\' -> "\\\\";
                case '\n' -> "\\n";
                case '\f' -> "\\f";
                case '\b' -> "\\b";
                default -> ch + "";
            };
            sb.append(s);
        }
        return sb.toString();
    }

    String randomFormat(Category category) {
        char c;
        return "%" + switch (category) {
            case GENERAL -> randomWidth("-") + randomPrecision(10, 10) + randomChar("bBhHsS");
            case CHARACTER -> randomWidth("-") + randomChar("cC");
            case INTEGRAL -> switch (c = randomChar("doxX")) {
                case 'd' -> randomFlags("+ ,(");
                default -> randomFlags("");
            } + randomWidth("-0") + c;
            case BIG_INT -> switch (c = randomChar("doxX")) {
                case 'd' -> randomFlags("+ ,(");
                default -> randomFlags("+ (");
            } + randomWidth("-0") + c;
            case FLOATING -> switch (c = randomChar("eEfaAgG")) {
                case 'a', 'A' -> randomFlags("+ ") + randomWidth("-0");
                case 'e', 'E' -> randomFlags("+ (") + randomWidth("-0") + randomPrecision(0, 10);
                default -> randomFlags("+ ,(") + randomWidth("-0") + randomPrecision(0, 10);
            } + c;
            case BIG_FLOAT -> switch (c = randomChar("eEfgG")) {
                case 'e', 'E' -> randomFlags("+ (") + randomWidth("-0") + randomPrecision(0, 10);
                default -> randomFlags("+ ,(") + randomWidth("-0") + randomPrecision(0, 10);
            } + c;
            case DATE ->  randomWidth("-") + randomChar("tT") + randomChar("BbhAaCYyjmdeRTrDFc");
        };
    }

    String randomFlags(String flags) {
        var sb = new StringBuilder(flags.length());
        for (var f : flags.toCharArray()) {
            if (r.nextBoolean() && (f != ' ' || sb.length() == 0 || sb.charAt(sb.length() - 1) != '+')) sb.append(f);
        }
        return sb.toString();
    }

    char randomChar(String chars) {
        return chars.charAt(r.nextInt(chars.length()));
    }

    String randomWidth(String flags) {
        var f = r.nextInt(flags.length() + 1);
        return f < flags.length() ? flags.charAt(f) + String.valueOf(r.nextInt(10) + 10) : "";
    }

    String randomPrecision(int shift, int range) {
        return r.nextBoolean() ? '.' + String.valueOf(shift + r.nextInt(range)) : "";
    }

    public Class<?> compile() throws Exception {
        var classes = new HashMap<String, byte[]>();
        var fileManager = new ForwardingJavaFileManager(ToolProvider.getSystemJavaCompiler().getStandardFileManager(null, null, null)) {
            @Override
            public ClassLoader getClassLoader(JavaFileManager.Location location) {
                return new ClassLoader() {
                    @Override
                    public Class<?> loadClass(String name) throws ClassNotFoundException {
                        try {
                            return super.loadClass(name);
                        } catch (ClassNotFoundException e) {
                            byte[] classData = classes.get(name);
                            return defineClass(name, classData, 0, classData.length);
                        }
                    }
                };
            }
            @Override
            public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String name, JavaFileObject.Kind kind, FileObject originatingSource) throws UnsupportedOperationException {
                return new SimpleJavaFileObject(URI.create(name + ".class"), JavaFileObject.Kind.CLASS) {
                    @Override
                    public OutputStream openOutputStream() {
                        return new FilterOutputStream(new ByteArrayOutputStream()) {
                            @Override
                            public void close() throws IOException {
                                classes.put(name, ((ByteArrayOutputStream)out).toByteArray());
                            }
                        };
                    }
                };
            }
        };
        var source = genSource();
//        System.out.println(source);
        if (ToolProvider.getSystemJavaCompiler().getTask(null, fileManager, null,
                List.of("--enable-preview", "-source", String.valueOf(Runtime.version().feature())), null,
                List.of(new SimpleJavaFileObject(URI.create("StringTemplateTest$.java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source;
            }
        })).call()) {
            return fileManager.getClassLoader(CLASS_OUTPUT).loadClass("StringTemplateTest$");
        } else {
            throw new AssertionError("compilation failed");
        }
    }

    String genSource() {
        var delimiter = "\n        ";
        var fragments = new LinkedList<String>();
        for (int i = 0; i < 1500; i++) {
            var c = Category.values()[r.nextInt(Category.values().length)];
            var format = randomFormat(c);
            var value = randomValue(c);
            var qValue = value.replace("\\", "\\\\").replace("\"", "\\\"");
            fragments.add(STR."test(STR.\"\{format}\\{\{value}}\", FMT.\"\{format}\\{\{value}}\", \"\{format}\", \"\{qValue}\", \{value}, log);");
        }
        return STR."""
            import static java.util.FormatProcessor.FMT;

            public class StringTemplateTest$ {
                public static void run(java.util.List<String> log) {
                    \{String.join(delimiter, fragments)}
                }
                static void test(String str, String fmt, String format, String expression, Object value, java.util.List<String> log) {
                    var concat = format + String.valueOf(value);
                    if (!str.equals(concat))
                        log.add("  concat expression: '%s' value: '%s' expected: '%s' found: '%s'".formatted(expression, value, concat, str));
                    var formatted = String.format(java.util.Locale.US, format, value);
                    if (!fmt.equals(formatted)) {
                        log.add("  format: '%s' expression: '%s' value: '%s' expected: '%s' found: '%s'".formatted(format, expression, value, formatted, fmt));
                    }
                }
            }
            """;
    }

    public static void main(String... args) throws Exception {
        var log = new LinkedList<String>();
        new StringTemplateTest().compile().getMethod("run", List.class).invoke(null, log);
        if (!log.isEmpty()) {
            log.addFirst(STR."failed \{log.size()} tests:");
            throw new AssertionError(String.join("\n", log));
        }
    }
}