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
 * @summary check that members of abtract datum has protected access
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.util
 * @compile MethodParametersForCanonicalConstructorTest.java
 * @run main MethodParametersForCanonicalConstructorTest
 */

import java.io.*;

import com.sun.tools.classfile.*;

public class MethodParametersForCanonicalConstructorTest {

    record R1(int i, int j);

    record R2(int i, int j) {
        public R2 {}
    }

    record R3(int i, int j) {
        public R3(int i, int j) {}
    }

    public static void main(String args[]) throws Throwable {
        new MethodParametersForCanonicalConstructorTest().run();
    }

    void run() throws Throwable {
        checkCanonical("R1", "i", "j");
        checkCanonical("R2", "i", "j");
        checkCanonical("R3", "i", "j");
    }

    void checkCanonical(String className, String... expectedParamNames) throws Throwable {
        if (expectedParamNames == null) {
            return;
        }
        File testClasses = new File(System.getProperty("test.classes"));
        File file = new File(testClasses,
                MethodParametersForCanonicalConstructorTest.class.getName() + "$" + className +".class");
        ClassFile classFile = ClassFile.read(file);
        boolean found = false;
        for (Method m : classFile.methods) {
            if (m.getName(classFile.constant_pool).equals("<init>")) {
                for (Attribute attribute : m.attributes) {
                    if (attribute instanceof MethodParameters_attribute) {
                        found = true;
                        MethodParameters_attribute mpa = (MethodParameters_attribute)attribute;
                        if (mpa.method_parameter_table_length != expectedParamNames.length) {
                            throw new AssertionError("unexpected method parameter table lenght");
                        }
                        int i = 0;
                        for (MethodParameters_attribute.Entry entry : mpa.method_parameter_table) {
                            if (!classFile.constant_pool.getUTF8Value(entry.name_index).equals(expectedParamNames[i])) {
                                throw new AssertionError("unexpected name at position " + i);
                            }
                            i++;
                        }
                    }
                }
            }
        }
        if (!found) {
            throw new AssertionError("attribute MethodParameters not found");
        }
    }
}
