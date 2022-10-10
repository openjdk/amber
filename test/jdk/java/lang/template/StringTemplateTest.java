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
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import static javax.tools.StandardLocation.CLASS_OUTPUT;
import javax.tools.ToolProvider;

public class StringTemplateTest {

    enum Category{GENERAL, CHARACTER, INTEGRAL, BIG, FLOATING, DATE, PERCENT, LINE};

    static String randomFormat(Category category, Random r) {
        char c;
        return "%" + switch (category) {
            case GENERAL -> randomFlags("-", r) + randomWidth(r) + randomMax(r) + randomConversion("bBhHsS", r);
            case CHARACTER -> randomFlags("-", r) + randomWidth(r) + randomConversion("cC", r);
            case INTEGRAL -> switch (c = randomConversion("doxX", r)) {
                case 'd'-> randomFlags("-+ 0,(", r);
                default -> randomFlags("-#0", r);
            } + randomWidth(r) + c;
            case BIG -> switch (c = randomConversion("doxX", r)) {
                case 'd' -> randomFlags("-+ 0,(", r);
                default -> randomFlags("-#+ 0(", r);
            } + randomWidth(r) + c;
            case FLOATING -> switch (c = randomConversion("eEfgGaA", r)) {
                case 'a', 'A' -> randomFlags("-#+ 0", r);
                case 'g', 'G' -> randomFlags("-#+ 0,(", r) + randomMax(r);
                default -> randomFlags("-#+ 0,(", r) + randomPrecision(r);
            } + randomWidth(r) + c;
            case DATE ->  randomFlags("-", r) + randomWidth(r) + randomConversion("tT", r);
            case PERCENT ->  randomWidth(r) + '%';
            case LINE -> 'n';
        };
    }

    static String randomFlags(String flags, Random r) {
        var sb = new StringBuilder(flags.length());
        for (var f : flags.toCharArray()) {
            if (r.nextBoolean()) sb.append(f);
        }
        return sb.toString();
    }

    static char randomConversion(String conversions, Random r) {
        return conversions.charAt(r.nextInt(conversions.length()));
    }

    static String randomWidth(Random r) {
        return r.nextBoolean() ? String.valueOf(r.nextInt(10)) : "";
    }

    static String randomPrecision(Random r) {
        return r.nextBoolean() ? ',' + String.valueOf(r.nextInt(10)) : "";
    }

    static String randomMax(Random r) {
        return r.nextBoolean() ? ',' + String.valueOf(10 + r.nextInt(10)) : "";
    }

    public static void main(String... args) throws Exception {
        var r = new Random(1);
        for (int i = 0; i < 20; i++) {
            System.out.println(randomFormat(Category.values()[r.nextInt(Category.values().length)], r));
        }
        var gen = compile("""
            public static void run() {
                var what = "ahoj";
                var i = 15;
                System.out.println(FMT."%s\\{what} %4d\\{i}");
            }
        """).getMethod("run").invoke(null);
    }

    public static Class<?> compile(String sourceFragment) throws Exception {
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
        if (ToolProvider.getSystemJavaCompiler().getTask(null, fileManager, null,
                List.of("--enable-preview", "-source", String.valueOf(Runtime.version().feature())), null,
                List.of(new SimpleJavaFileObject(URI.create("StringTemplateTest$.java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return STR."""
                    public class StringTemplateTest$ {
                        \{sourceFragment}
                    }
                    """;
            }
        })).call()) {
            return fileManager.getClassLoader(CLASS_OUTPUT).loadClass("StringTemplateTest$");
        } else {
            throw new AssertionError("compilation failed");
        }
    }
}