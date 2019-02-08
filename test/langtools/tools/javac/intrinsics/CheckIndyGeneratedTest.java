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
 * @run main CheckIndyGeneratedTest
 */

import com.sun.tools.javac.util.Assert;

import com.sun.tools.classfile.*;
import com.sun.tools.classfile.BootstrapMethods_attribute.*;
import com.sun.tools.classfile.ConstantPool.*;
import java.io.*;

public class CheckIndyGeneratedTest {
    static class CheckIndyGeneratedTestsub {
        void test() {
            String s = String.format("%s", "Bob");
            System.out.println(s);
        }
    }

    static final String SUBTEST_NAME = CheckIndyGeneratedTestsub.class.getName() + ".class";
    static final String TEST_METHOD_NAME = "test";

    public static void main(String... args) throws Exception {
        new CheckIndyGeneratedTest().run();
    }

    public void run() throws Exception {
        String workDir = System.getProperty("test.classes");
        File compiledTest = new File(workDir, SUBTEST_NAME);
        verifyIndyGenerated(compiledTest);
    }

    void verifyIndyGenerated(File f) {
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
            for (Instruction inst : ea.getInstructions()) {
                if (inst.getMnemonic().equals("invokedynamic")) {
                    int index1 = inst.getUnsignedByte(1);
                    int index2 = inst.getUnsignedByte(2);
                    int cpIndex = index1 << 8 | index2;
                    CONSTANT_InvokeDynamic_info indyCP = (CONSTANT_InvokeDynamic_info)cf.constant_pool.get(cpIndex);
                    System.out.println(indyCP.bootstrap_method_attr_index);
                    BootstrapMethods_attribute bsmAttr = (BootstrapMethods_attribute)cf.getAttribute(Attribute.BootstrapMethods);
                    BootstrapMethodSpecifier bsms = bsmAttr.bootstrap_method_specifiers[indyCP.bootstrap_method_attr_index];
                    CONSTANT_MethodHandle_info mhi = (CONSTANT_MethodHandle_info)cf.constant_pool.get(bsms.bootstrap_method_ref);
                    CONSTANT_Methodref_info mri = (CONSTANT_Methodref_info)cf.constant_pool.get(mhi.reference_index);
                    Assert.check(mri.getClassName().equals("java/lang/invoke/IntrinsicFactory"));
                    CONSTANT_NameAndType_info nti = mri.getNameAndTypeInfo();
                    Assert.check(nti.getName().equals("staticStringFormatBootstrap"));
                    Assert.check(nti.getType().equals("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;)Ljava/lang/invoke/CallSite;"));
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("error reading " + f +": " + e);
        }
    }
}
