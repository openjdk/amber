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
 */

import com.sun.tools.javac.util.Assert;

public class CheckingAttributeAtRuntimeTest {

    sealed class Sealed1 permits Sub1 {}

    class Sub1 extends Sealed1 {}

    sealed interface SealedI1 permits Sub2 {}

    class Sub2 implements SealedI1 {}

    sealed class Sealed2 {}

    class Sub3 extends Sealed2 {}

    public static void main(String... args) {
        Class<?> sealedClass1 = Sealed1.class;
        Assert.check(sealedClass1.isSealed());
        Assert.check(sealedClass1.getPermittedSubtypes().length == 1);
        Assert.check(sealedClass1.getPermittedSubtypes()[0] == Sub1.class);

        Class<?> sealedI = SealedI1.class;
        Assert.check(sealedI.isSealed());
        Assert.check(sealedI.getPermittedSubtypes().length == 1);
        Assert.check(sealedI.getPermittedSubtypes()[0] == Sub2.class);

        Class<?> sealedClass2 = Sealed2.class;
        Assert.check(sealedClass2.isSealed());
        Assert.check(sealedClass2.getPermittedSubtypes().length == 1);
        Assert.check(sealedClass2.getPermittedSubtypes()[0] == Sub3.class);

        Class<?> sealedClass3 = Sealed3.class;
        Assert.check(sealedClass3.isSealed());
        Assert.check(sealedClass3.getPermittedSubtypes().length == 1);
        Assert.check(sealedClass3.getPermittedSubtypes()[0] == Sub4.class);

        Class<?> sealedClass4 = Sealed4.class;
        Assert.check(sealedClass4.isSealed());
        Assert.check(sealedClass4.getPermittedSubtypes().length == 1);
        Assert.check(sealedClass4.getPermittedSubtypes()[0] == Sub5.class);
    }
}

sealed class Sealed3 {}

class Sub4 extends Sealed3 {}

sealed class Sealed4 permits Sub5 {}

class Sub5 extends Sealed4 {}
