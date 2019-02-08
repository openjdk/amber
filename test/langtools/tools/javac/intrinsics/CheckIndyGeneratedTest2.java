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
 * @summary intrinsics: check that javac is generating the expected bytecode
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.util
 * @run main CheckIndyGeneratedTest2
 */

import java.io.*;
import java.util.*;

import com.sun.tools.javac.util.Assert;
import com.sun.tools.classfile.*;
import com.sun.tools.classfile.BootstrapMethods_attribute.*;
import com.sun.tools.classfile.ConstantPool.*;

public class CheckIndyGeneratedTest2 {
    static class CheckIndyGeneratedTest2sub {
        int test() {
            return Objects.hash(1, 2);
        }
    }

    static final String SUBTEST_NAME = CheckIndyGeneratedTest2sub.class.getName() + ".class";
    static final String TEST_METHOD_NAME = "test";

    public static void main(String... args) throws Exception {
        new CheckIndyGeneratedTest2().run();
    }

    public void run() throws Exception {
        String workDir = System.getProperty("test.classes");
        File compiledTest = new File(workDir, SUBTEST_NAME);
        verifysipushGeneated(compiledTest);
    }

    void verifysipushGeneated(File f) {
        try {
            int count = 0;
            ClassFile cf = ClassFile.read(f);
            Method testMethod = null;
            for (Method m : cf.methods) {
                if (m.getName(cf.constant_pool).equals(TEST_METHOD_NAME)) {
                    testMethod = m;
                    break;
                }
            }
            if (testMethod == null) {
                throw new Error("Test method not found");
            }
            Code_attribute ea = (Code_attribute)testMethod.attributes.get(Attribute.Code);
            if (testMethod == null) {
                throw new Error("Code attribute for test() method not found");
            }
            boolean sipushFound = false;
            for (Instruction inst : ea.getInstructions()) {
                if (inst.getMnemonic().equals("invokestatic")) {
                    throw new AssertionError("unexpected invoke static instruction");
                }
                if (inst.getMnemonic().equals("sipush")) {
                    sipushFound = true;
                }
            }
            if (!sipushFound) {
                throw new AssertionError("sipush instruction not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error(e.getMessage());
        }
    }
}
