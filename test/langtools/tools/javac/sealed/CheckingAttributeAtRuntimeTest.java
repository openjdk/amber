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

import java.lang.constant.*;

import org.testng.annotations.*;
import static org.testng.Assert.*;

@Test
public class CheckingAttributeAtRuntimeTest {

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

    public void testPermittedSubtypes() {
        Class<?> sealedClass1 = Sealed1.class;
        assertTrue(sealedClass1.isSealed());
        assertTrue(sealedClass1.getPermittedSubtypes().length == 1);
        assertTrue(sealedClass1.getPermittedSubtypes()[0].equals(ClassDesc.of("CheckingAttributeAtRuntimeTest").nested("Sub1")));

        Class<?> sealedI = SealedI1.class;
        assertTrue(sealedI.isSealed());
        assertTrue(sealedI.getPermittedSubtypes().length == 1);
        assertTrue(sealedI.getPermittedSubtypes()[0].equals(ClassDesc.of("CheckingAttributeAtRuntimeTest").nested("Sub2")));

        Class<?> sealedClass2 = Sealed2.class;
        assertTrue(sealedClass2.isSealed());
        assertTrue(sealedClass2.getPermittedSubtypes().length == 2);
        assertTrue(sealedClass2.getPermittedSubtypes()[0].equals(ClassDesc.of("CheckingAttributeAtRuntimeTest").nested("Sub3")));
        assertTrue(sealedClass2.getPermittedSubtypes()[1].equals(ClassDesc.of("CheckingAttributeAtRuntimeTest").nested("Sub4")));

        Class<?> sealedI2 = SealedI2.class;
        assertTrue(sealedI2.isSealed());
        assertTrue(sealedI2.getPermittedSubtypes().length == 3);
        assertTrue(sealedI2.getPermittedSubtypes()[0].equals(ClassDesc.of("CheckingAttributeAtRuntimeTest").nested("Sub5")));
        assertTrue(sealedI2.getPermittedSubtypes()[1].equals(ClassDesc.of("CheckingAttributeAtRuntimeTest").nested("Int1")));
        assertTrue(sealedI2.getPermittedSubtypes()[2].equals(ClassDesc.of("CheckingAttributeAtRuntimeTest").nested("Sub6")));
    }
}
