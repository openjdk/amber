/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * SealedCompilationTests
 *
 * @test
 * @summary Negative compilation tests, and positive compilation (smoke) tests for sealed types
 * @library /lib/combo
 * @modules jdk.compiler/com.sun.tools.javac.util
 * @run testng SealedCompilationTests
 */

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import tools.javac.combo.JavacTemplateTestBase;

import static java.lang.annotation.ElementType.*;
import static java.util.stream.Collectors.toList;
import static org.testng.Assert.assertEquals;

@Test
public class SealedCompilationTests extends JavacTemplateTestBase {

    // @@@ When sealed types become a permanent feature, we don't need these any more
    private static String[] PREVIEW_OPTIONS = {"--enable-preview", "-source", "14"};

    // -- test framework code --

    @AfterMethod
    public void dumpTemplateIfError(ITestResult result) {
        // Make sure offending template ends up in log file on failure
        if (!result.isSuccess()) {
            System.err.printf("Diagnostics: %s%nTemplate: %s%n", diags.errorKeys(),
                    sourceFiles.stream().map(p -> p.snd).collect(toList()));
        }
    }

    private String expand(String... constructs) {
        String s = "#";
        for (String c : constructs)
            s = s.replace("#", c);
        return s;
    }

    private void assertCompile(String program, Runnable postTest) {
        reset();
        addCompileOptions(PREVIEW_OPTIONS);
        addSourceFile("SealedTest.java", new StringTemplate(program));
        try {
            compile();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        postTest.run();
    }

    private void assertOK(String... constructs) {
        assertCompile(expand(constructs), this::assertCompileSucceeded);
    }

    private void assertOKWithWarning(String warning, String... constructs) {
        assertCompile(expand(constructs), () -> assertCompileSucceededWithWarning(warning));
    }

    private void assertFail(String expectedDiag, String... constructs) {
        assertCompile(expand(constructs), () -> assertCompileFailed(expectedDiag));
    }

    // -- Actual test cases start here --

    public void testSuccessExpected() {
        // with permits
        assertOK("class SealedTest {\n" +
                "    sealed class SC permits C_SC { }\n" +
                "    class C_SC extends SC { }\n" +
                "}");
        assertOK("class SealedTest {\n" +
                "    sealed abstract class SAC permits C_SAC { }\n" +
                "    class C_SAC extends SAC { }\n" +
                "}");
        assertOK("class SealedTest {\n" +
                "    sealed interface SI permits C_SI, I_SI { }\n" +
                "    class C_SI implements SI { }\n" +
                "    non-sealed interface I_SI extends SI { }\n" +
                "}");

        // wo permits
        assertOK("class SealedTest {\n" +
                "    sealed class SC { }\n" +
                "    class C_SC extends SC { }\n" +
                "}");
        assertOK("class SealedTest {\n" +
                "    sealed abstract class SAC { }\n" +
                "    class C_SAC extends SAC { }\n" +
                "}");
        assertOK("class SealedTest {\n" +
                "    sealed interface SI { }\n" +
                "    class C_SI implements SI { }\n" +
                "    non-sealed interface I_SI extends SI { }\n" +
                "}");
    }

    public void testErrorExpected() {
        assertFail("compiler.err.cant.inherit.from.sealed","class SealedTest {\n" +
                "    sealed class SC permits C_SC { }\n" +
                "    class C_SC extends SC { }\n" +
                "    class C_SC2 extends SC { }\n" +
                "}");
        assertFail("compiler.err.cant.inherit.from.sealed","class SealedTest {\n" +
                "    sealed abstract class SAC permits C_SAC {}\n" +
                "    class C_SAC extends SAC {}\n" +
                "    class C_SAC2 extends SAC {}\n" +
                "}");
        assertFail("compiler.err.cant.inherit.from.sealed","class SealedTest {\n" +
                "    sealed interface SI permits C_SI, I_SI {}\n" +
                "    class C_SI implements SI {}\n" +
                "    interface I_SI extends SI {}\n" +
                "    class C_SI2 implements SI {}\n" +
                "    interface I_SI2 extends SI {}\n" +
                "}");
    }

    public void testValidUsesOfSealed() {
        for (String s : List.of(
                "class SealedTest {\n" +
                "    String sealed;\n" +
                "}",
                "class SealedTest {\n" +
                "    void test(String sealed) { }\n" +
                "}",
                "class SealedTest {\n" +
                "    void test() {\n" +
                "        String sealed = null;\n" +
                "    }\n" +
                "}",
                "class sealed {}")) {
            assertOK(s);
        }
    }

    public void testPermitsInNoSealedClass() {
        assertFail("compiler.err.permits.in.no.sealed.class",
                "class SealedTest {\n" +
                "    class NotSealed permits Sub {}\n" +
                "    class Sub extends NotSealed {}\n" +
                "}");
    }

    public void testWrongUseOfModifiers() {
        assertFail("compiler.err.non.sealed.with.no.sealed.supertype",
                "class SealedTest {\n" +
                        "    non-sealed class NoSealedSuper {}\n" +
                        "}");
        assertFail("compiler.err.illegal.combination.of.modifiers",
                "class SealedTest {\n" +
                        "    final non-sealed class Super {}\n" +
                        "}");
        assertFail("compiler.err.illegal.combination.of.modifiers",
                "class SealedTest {\n" +
                        "    final sealed class Super {}\n" +
                        "}");
        assertFail("compiler.err.illegal.combination.of.modifiers",
                "class SealedTest {\n" +
                        "    final sealed non-sealed class Super {}\n" +
                        "}");
        assertFail("compiler.err.illegal.combination.of.modifiers",
                "class SealedTest {\n" +
                        "    sealed class Super {}\n" +
                        "    sealed non-sealed class Sub extends Super {}\n" +
                        "}");
        assertFail("compiler.err.mod.not.allowed.here",
                "class SealedTest {\n" +
                        "    sealed public void m() {}\n" +
                        "}");
    }

    public void testAnonymousAndLambdaCantExtendSealed() {
        assertFail("compiler.err.cant.inherit.from.sealed",
                "sealed interface I1 extends Runnable {\n" +
                        "    public static I1 i = () -> {};\n" +
                        "}");
        assertFail("compiler.err.cant.inherit.from.sealed",
                "sealed interface I2 extends Runnable {\n" +
                        "    public static void foo() { new I2() { public void run() { } }; }\n" +
                        "}");
    }
}
