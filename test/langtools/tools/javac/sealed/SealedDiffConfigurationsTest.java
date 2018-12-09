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
 * @summary test different configurations of sealed classes, same compilation unit, diff pkg or mdl, etc
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main SealedDiffConfigurationsTest
 */

import java.util.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.IntStream;

import com.sun.tools.classfile.*;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.util.Assert;
import toolbox.TestRunner;
import toolbox.ToolBox;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.Task.OutputKind;

import static com.sun.tools.classfile.ConstantPool.*;

public class SealedDiffConfigurationsTest extends TestRunner {
    ToolBox tb;

    SealedDiffConfigurationsTest() {
        super(System.err);
        tb = new ToolBox();
    }

    protected void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    public static void main(String... args) throws Exception {
        SealedDiffConfigurationsTest t = new SealedDiffConfigurationsTest();
        t.runTests();
    }

    Path[] findJavaFiles(Path... paths) throws IOException {
        return tb.findJavaFiles(paths);
    }

    @Test
    public void testSameCompilationUnitPos(Path base) throws Exception {
        Path src = base.resolve("src");
        Path test = src.resolve("Test");

        tb.writeJavaFiles(test,
                          "class Test {\n" +
                           "    final class Sealed permits Sub1, Sub2 {}\n" +
                           "    class Sub1 extends Sealed {}\n" +
                           "    class Sub2 extends Sealed {}\n" +
                           "}");

        Path out = base.resolve("out");

        Files.createDirectories(out);

        new JavacTask(tb)
                .outdir(out)
                .files(findJavaFiles(test))
                .run()
                .writeAll();

        checkSealedClassFile(out, "Test$Sealed.class", List.of("Test$Sub1", "Test$Sub2"));
        checkSubtypeClassFile(out, "Test$Sub1.class", "Test$Sealed");
        checkSubtypeClassFile(out, "Test$Sub2.class", "Test$Sealed");
    }

    private void checkSealedClassFile(Path out, String cfName, List<String> expectedSubTypeNames) throws ConstantPoolException, Exception {
        ClassFile sealedCF = ClassFile.read(out.resolve(cfName));
        Assert.check((sealedCF.access_flags.flags & Flags.FINAL) != 0, String.format("class at file %s must be final", cfName));
        PermittedSubtypes_attribute permittedSubtypes = (PermittedSubtypes_attribute)sealedCF.attributes.get("PermittedSubtypes");
        Assert.check(permittedSubtypes.subtypes.length == expectedSubTypeNames.size());
        List<String> subtypeNames = new ArrayList<>();
        IntStream.of(permittedSubtypes.subtypes).forEach(i -> {
            try {
                subtypeNames.add(((CONSTANT_Class_info)sealedCF.constant_pool.get(i)).getName());
            } catch (ConstantPoolException ex) {
            }
        });
        subtypeNames.sort((s1, s2) -> s1.compareTo(s2));
        for (int i = 0; i < expectedSubTypeNames.size(); i++) {
            Assert.check(expectedSubTypeNames.get(0).equals(subtypeNames.get(0)));
        }
    }

    private void checkSubtypeClassFile(Path out, String cfName, String superClassName) throws Exception {
        ClassFile subCF1 = ClassFile.read(out.resolve(cfName));
        Assert.check((subCF1.access_flags.flags & Flags.FINAL) != 0, String.format("class at file %s must be final", cfName));
        Assert.checkNull((PermittedSubtypes_attribute)subCF1.attributes.get("PermittedSubtypes"));
        Assert.check(((CONSTANT_Class_info)subCF1.constant_pool.get(subCF1.super_class)).getName().equals(superClassName));
    }

    @Test
    public void testSamePackagePos(Path base) throws Exception {
        Path src = base.resolve("src");
        Path pkg = src.resolve("pkg");
        Path sealed = pkg.resolve("Sealed");
        Path sub1 = pkg.resolve("Sub1");
        Path sub2 = pkg.resolve("Sub2");

        tb.writeJavaFiles(sealed,
                          "package pkg;\n" +
                          "\n" +
                          "final class Sealed permits Sub1, Sub2 {\n" +
                          "}");
        tb.writeJavaFiles(sub1,
                          "package pkg;\n" +
                          "\n" +
                          "class Sub1 extends Sealed {\n" +
                          "}");
        tb.writeJavaFiles(sub2,
                          "package pkg;\n" +
                          "\n" +
                          "class Sub2 extends Sealed {\n" +
                          "}");

        Path out = base.resolve("out");

        Files.createDirectories(out);

        new JavacTask(tb)
                .outdir(out)
                .files(findJavaFiles(pkg))
                .run()
                .writeAll();

        checkSealedClassFile(out.resolve("pkg"), "Sealed.class", List.of("pkg/Sub1", "pkg/Sub1"));
        checkSubtypeClassFile(out.resolve("pkg"), "Sub1.class", "pkg/Sealed");
        checkSubtypeClassFile(out.resolve("pkg"), "Sub2.class", "pkg/Sealed");
    }

    @Test
    public void testDiffPackagePos(Path base) throws Exception {
        Path src = base.resolve("src");
        Path pkg1 = src.resolve("pkg1");
        Path pkg2 = src.resolve("pkg2");
        Path sealed = pkg1.resolve("Sealed");
        Path sub1 = pkg2.resolve("Sub1");
        Path sub2 = pkg2.resolve("Sub2");

        tb.writeJavaFiles(sealed,
                          "package pkg1;\n" +
                          "import pkg2.*;\n" +
                          "public final class Sealed permits pkg2.Sub1, pkg2.Sub2 {\n" +
                          "}");
        tb.writeJavaFiles(sub1,
                          "package pkg2;\n" +
                          "import pkg1.*;\n" +
                          "public class Sub1 extends pkg1.Sealed {\n" +
                          "}");
        tb.writeJavaFiles(sub2,
                          "package pkg2;\n" +
                          "import pkg1.*;\n" +
                          "public class Sub2 extends pkg1.Sealed {\n" +
                          "}");

        Path out = base.resolve("out");

        Files.createDirectories(out);

        new JavacTask(tb)
                .outdir(out)
                .files(findJavaFiles(pkg1, pkg2))
                .run()
                .writeAll();

        checkSealedClassFile(out.resolve("pkg1"), "Sealed.class", List.of("pkg2/Sub1", "pkg2/Sub1"));
        checkSubtypeClassFile(out.resolve("pkg2"), "Sub1.class", "pkg1/Sealed");
        checkSubtypeClassFile(out.resolve("pkg2"), "Sub2.class", "pkg1/Sealed");
    }

    @Test
    public void testSameCompilationUnitNeg(Path base) throws Exception {
        Path src = base.resolve("src");
        Path test = src.resolve("Test");

        tb.writeJavaFiles(test,
                          "class Test {\n" +
                           "    final class Sealed permits Sub1 {}\n" +
                           "    class Sub1 extends Sealed {}\n" +
                           "    class Sub2 extends Sealed {}\n" +
                           "}");

        List<String> error = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .files(findJavaFiles(test))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        List<String> expected = List.of(
                "Test.java:4:24: compiler.err.cant.inherit.from.sealed: Test.Sealed",
                "1 error");
        if (!error.containsAll(expected)) {
            throw new AssertionError("Expected output not found. Expected: " + expected);
        }
    }

    @Test
    public void testSamePackageNeg(Path base) throws Exception {
        Path src = base.resolve("src");
        Path pkg = src.resolve("pkg");
        Path sealed = pkg.resolve("Sealed");
        Path sub1 = pkg.resolve("Sub1");
        Path sub2 = pkg.resolve("Sub2");

        tb.writeJavaFiles(sealed,
                          "package pkg;\n" +
                          "\n" +
                          "final class Sealed permits Sub1 {\n" +
                          "}");
        tb.writeJavaFiles(sub1,
                          "package pkg;\n" +
                          "\n" +
                          "class Sub1 extends Sealed {\n" +
                          "}");
        tb.writeJavaFiles(sub2,
                          "package pkg;\n" +
                          "\n" +
                          "class Sub2 extends Sealed {\n" +
                          "}");

        List<String> error = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .files(findJavaFiles(pkg))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        List<String> expected = List.of(
                "Sub2.java:3:20: compiler.err.cant.inherit.from.sealed: pkg.Sealed",
                "1 error");
        if (!error.containsAll(expected)) {
            throw new AssertionError("Expected output not found. Expected: " + expected);
        }
    }
}
