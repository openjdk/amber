/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary test Origin of record members
 * @modules jdk.compiler/com.sun.tools.javac.util
 * @run main OriginTest
 */

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.ElementScanner9;
import javax.lang.model.util.Elements;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;

public class OriginTest {

    public static void main(String... args) throws Exception {
        new OriginTest().run();
    }

    private String ALL_MANDATED =
            "record AllMandated(int x, int y) { }";

    private String ALL_EXPLICIT =
            "record AllExplicit(int x, int y) {\n" +
            "    public AllExplicit { }\n" +
            "    public int x() { return x; }\n" +
            "    public int y() { return y; }\n" +
            "    public boolean equals(Object other) { return false; }\n" +
            "    public int hashCode() { return 0; }\n" +
            "    public String toString() { return null; }\n" +
            "}";

    void run() throws Exception {
        test(ALL_MANDATED, Elements.Origin.MANDATED);
        test(ALL_EXPLICIT, Elements.Origin.EXPLICIT);
    }

    private final static String JDK_VERSION = Integer.toString(Runtime.version().feature());

    void test(String src, Elements.Origin origin) throws Exception {
        Matcher m = Pattern.compile("record ([^(]+)").matcher(src);
        if (!m.find()) throw new IllegalArgumentException();
        Path file = Path.of(m.group(1) + ".java");
        Files.writeString(file, src);
        JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null);
        List<String> options = List.of("--enable-preview", "-source", JDK_VERSION);
        List<String> classes = List.of();
        Iterable<? extends JavaFileObject> files = fm.getJavaFileObjects(file);
        JavacTask task = (JavacTask) tool.getTask(null, null, null,
                options, classes, files);
        task.addTaskListener(new Checker(task.getElements(), origin));
        boolean ok = task.call();
        if (!ok) {
            error("Task failed");
        }

        if (errors > 0) {
            System.err.println(errors + " occurred");
            throw new Exception(errors + " occurred");
        }
    }

    class Checker implements TaskListener {
        final Elements elements;
        final Elements.Origin expectedOrigin;
        Checker(Elements elements, Elements.Origin expectedOrigin) {
            this.elements = elements;
            this.expectedOrigin = expectedOrigin;
        }

        @Override
        public void started(TaskEvent e) {
            System.err.println("Started " + e);
            if (e.getKind() == TaskEvent.Kind.ANALYZE) {
                ElementScanner9<Void, Void> scanner = new ElementScanner9<>() {
                    public Void visitExecutable(ExecutableElement ee, Void p) {
                        Elements.Origin o = elements.getOrigin(ee);
                        if (o != expectedOrigin) {
                            error("Unexpected origin " + o + " for " + ee);
                        }
                        return null;
                    }
                };
                scanner.scan(e.getTypeElement());
            }
        }
    }

    void error(String message) {
        System.err.println("Error: " + message);
        errors++;
    }

    int errors;
}
