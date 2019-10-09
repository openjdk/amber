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
 * @summary checking for permitted subtypes attribute at runtime
 * @modules jdk.compiler/com.sun.tools.javac.util
 * @compile --enable-preview -source 14 CheckingAttributeAtRuntimeTest.java
 * @run testng/othervm --enable-preview CheckingAttributeAtRuntimeTest
 */

import java.lang.constant.ClassDesc;
import java.util.Set;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Test
public class CheckingAttributeAtRuntimeTest {

    private static final ClassDesc TEST_CLASS = ClassDesc.of("CheckingAttributeAtRuntimeTest");

    sealed class Sealed1 permits Sub1 {}
    final class Sub1 extends Sealed1 {}

    sealed interface SealedI1 permits Sub2 {}
    final class Sub2 implements SealedI1 {}

    sealed class Sealed2 {}
    final class Sub3 extends Sealed2 {}
    final class Sub4 extends Sealed2 {}

    sealed interface SealedI2 {}
    final class Sub5 implements SealedI2 {}
    non-sealed interface Int1 extends SealedI2 {}
    non-sealed class Sub6 implements SealedI2 {}

    //sealed interface SealedI3 { }

    private void assertSealed(Class<?> c,
                              Set<ClassDesc> expectedPermits) {
        assertTrue(c.isSealed());
        assertEquals(c.getPermittedSubtypes().length, expectedPermits.size());
        assertEquals(Set.of(c.getPermittedSubtypes()), expectedPermits);
    }

    public void testPermittedSubtypes() {
        assertSealed(Sealed1.class,
                Set.of(TEST_CLASS.nested("Sub1")));

        assertSealed(SealedI1.class,
                Set.of(TEST_CLASS.nested("Sub2")));

        assertSealed(Sealed2.class,
                Set.of(TEST_CLASS.nested("Sub3"),
                        TEST_CLASS.nested("Sub4")));

        assertSealed(SealedI2.class,
                Set.of(TEST_CLASS.nested("Sub5"),
                        TEST_CLASS.nested("Int1"),
                        TEST_CLASS.nested("Sub6")));

        //assertSealed(SealedI3.class, Set.of());
        // aux classes too
        assertSealed(Sealed3.class,
                Set.of(ClassDesc.of("Sub7")));
    }
}

sealed class Sealed3 permits Sub7 { }
final class Sub7 extends Sealed3 { }
