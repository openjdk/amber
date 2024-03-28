/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
 * @summary SimplePatternDeclarationsTest
 * @enablePreview
 * @run testng/othervm SimplePatternDeclarationsTest
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class SimplePatternDeclarationsTest {
    @Test
    public void test1() {

        Class<?> class1 = Person1.class;

        Method[] methods = class1.getDeclaredPatternDeclarations();

        assertEquals(methods.length, 2);
    }
}

class Person1 {
    private final String name;
    private final String username;
    private boolean capitalize;

    public Person1(String name) {
        this(name, "default", false);
    }

    public Person1(String name, String username, boolean capitalize) {
        this.name = name;
        this.username = username;
        this.capitalize = capitalize;
    }

    public pattern Person1(String name, String username) {
        match Person1(this.name, this.username);
    }

    public pattern Person1(String name) {
        if (capitalize) {
            match Person1(this.name.toUpperCase());
        } else {
            match Person1(this.name);
        }
    }
}