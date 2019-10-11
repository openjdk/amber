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
 * @compile --enable-preview -source 14 SealedCompilationTests.java
 * @run testng/othervm --enable-preview SealedCompilationTests
 */

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;
import tools.javac.combo.CompilationTestCase;

@Test
public class SealedCompilationTests extends CompilationTestCase {

    // @@@ When records become a permanent feature, we don't need these any more
    private static String[] PREVIEW_OPTIONS = {"--enable-preview", "-source",
                                               Integer.toString(Runtime.version().feature())};

    {
        setDefaultFilename("SealedTest.java");
        setCompileOptions(PREVIEW_OPTIONS);
    }

    private static final String NO_SHELL = """
                 #
                 """;
    private static final String NEST_SHELL = """
                 class SealedTest {
                     #
                 }
                 """;
    private static final String AUX_SHELL = """
                 class SealedTest {
                 }
                 #
                 """;
    private static final List<String> SHELLS = List.of(NO_SHELL, NEST_SHELL, AUX_SHELL);

    public void testSimpleExtension() {
        String CC1 =
            """
            sealed class Sup # { }
            # class Sub extends Sup { }
            """;
        String AC1 =
            """
            sealed abstract class Sup # { }
            # class Sub extends Sup { }
            """;
        String I1 =
            """
            sealed interface Sup # { }
            # class Sub implements Sup { }
            """;
        String I2 =
                """
            sealed interface Sup # { }
            # class Sub1 implements Sup { }
            # class Sub2 implements Sup { }
            """;

        // Assert that all combinations work:
        // { class, abs class, interface } x { implicit permits, explicit permits }
        //                                 x { final, non-sealed subtype }
        for (String shell : SHELLS)
            for (String b : List.of(CC1, AC1, I1))
                for (String p : List.of("", "permits Sub"))
                    for (String m : List.of("sealed", "final", "non-sealed"))
                        assertOK(shell, b, p, m);


        // Same for type with two subtypes
        for (String shell : SHELLS)
            for (String p : List.of("", "permits Sub1, Sub2"))
                for (String m : List.of("final", "non-sealed"))
                    assertOK(shell, expandMarkers(I2, p, m, m));

        // Expect failure if there is no explicit final / sealed / non-sealed
        // @@@ Currently failing
//        for (String shell : SHELLS)
//            for (String b : List.of(CC1, AC1, I1))
//                for (String p : List.of("", "permits Sub"))
//                    for (String m : List.of(""))
//                        assertFail("", shell, expandMarkers(b, p, m));
    }

    public void testSealedAndRecords() {
        String P =
            """
            sealed interface Sup # { }
            record A(int a) implements Sup { }
            record B(int b) implements Sup { }
            record C(int c) implements Sup { }
            """;

        for (String shell : SHELLS)
            for (String b : List.of(P))
                for (String p : List.of("", "permits A, B, C"))
                    assertOK(shell, b, p);
    }

    // Test that a type that explicitly permits one type, can't be extended by another
    public void testBadExtension() {
        String CC2 =
                """
                sealed class Sup permits Sub1 { }
                final class Sub1 extends Sup { }
                final class Sub2 extends Sup { }
                """;
        String AC2 =
                """
                sealed abstract class Sup permits Sub1 { }
                final class Sub1 extends Sup { }
                final class Sub2 extends Sup { }
                """;
        String I2c =
                """
                sealed interface Sup permits Sub1 { }
                final class Sub1 implements Sup { }
                final class Sub2 implements Sup { }
                """;
        String I2i =
                """
                sealed interface Sup permits Sub1 { }
                non-sealed interface Sub1 extends Sup { }
                non-sealed interface Sub2 extends Sup { }
                """;

        for (String shell : SHELLS)
            for (String b : List.of(CC2, AC2, I2c, I2i))
                assertFail("compiler.err.cant.inherit.from.sealed", shell, b);
    }

    public void testRestrictedKeyword() {
        for (String s : List.of(
                "class SealedTest { String sealed; }",
                "class SealedTest { int sealed = 0; int non = 0; int ns = non-sealed; }",
                "class SealedTest { void test(String sealed) { } }",
                "class SealedTest { void sealed(String sealed) { } }",
                "class SealedTest { void test() { String sealed = null; } }",
                "class sealed {}")) {
            assertOK(s);
        }
    }

    public void testRejectPermitsInNonSealedClass() {
        assertFail("compiler.err.permits.in.no.sealed.class",
                "class SealedTest {\n" +
                "    class NotSealed permits Sub {}\n" +
                "    class Sub extends NotSealed {}\n" +
                "}");
        assertFail("compiler.err.permits.in.no.sealed.class",
                "class SealedTest {\n" +
                "    interface NotSealed permits Sub {}\n" +
                "    class Sub implements NotSealed {}\n" +
                "}");
    }

    public void testBadModifiers() {
        assertFail("compiler.err.non.sealed.with.no.sealed.supertype",
                "class SealedTest { non-sealed class NoSealedSuper {} }");
        assertFail("compiler.err.mod.not.allowed.here",
                   "class SealedTest { sealed public void m() {} }");
        for (String s : List.of(
                "class SealedTest { sealed non-sealed class Super {} }",
                "class SealedTest { final non-sealed class Super {} }",
                "class SealedTest { final sealed class Super {} }",
                "class SealedTest { final sealed non-sealed class Super {} }",
                "class SealedTest {\n" +
                "    sealed class Super {}\n" +
                "    sealed non-sealed class Sub extends Super {}\n" +
                "}"))
            assertFail("compiler.err.illegal.combination.of.modifiers", s);
    }

    public void testAnonymousAndLambdaCantExtendSealed() {
        for (String s : List.of(
                "sealed interface I1 extends Runnable {\n" +
                "    public static I1 i = () -> {};\n" +
                "}",
                "sealed interface I2 extends Runnable {\n" +
                "    public static void foo() { new I2() { public void run() { } }; }\n" +
                "}"))
            assertFail("compiler.err.cant.inherit.from.sealed", s);
    }

    public void testNoLocalSealedClasses() {
        for (String s : List.of(
                """
                sealed class C {
                    void m() {
                        sealed class D { }
                    }
                }
                """,
                """
                sealed class C {
                    void m() {
                        non-sealed class D { }
                    }
                }
                """))
            assertFail("compiler.err.sealed.or.non.sealed.local.classes.not.allowed", s);
    }

    public void testLocalCantExtendSealed() {
        for (String s : List.of(
                """
                sealed class C {
                    void m() {
                        final class D extends C { }
                    }
                }
                """))
            assertFail("compiler.err.local.classes.cant.extend.sealed", s);
    }

    public void testSealedInterfaceAndAbstracClasses() {
        assertFail("compiler.err.sealed.interface.or.abstract.must.have.subtypes",
                "sealed interface I1 {}");
        assertFail("compiler.err.sealed.interface.or.abstract.must.have.subtypes",
                "sealed abstract class AC {}");
    }
}
