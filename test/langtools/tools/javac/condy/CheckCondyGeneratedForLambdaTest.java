/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @summary check that an LDC + condy is generated for a non-capturing lambda
 * @library /tools/lib
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main CheckCondyGeneratedForLambdaTest
 */

import java.io.File;
import java.nio.file.Paths;

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.ConstantPool.*;
import com.sun.tools.classfile.Method;
import com.sun.tools.classfile.Opcode;
import com.sun.tools.javac.util.Assert;

import toolbox.JavacTask;
import toolbox.ToolBox;

public class CheckCondyGeneratedForLambdaTest {

    static final String testSource =
        "import java.util.stream.*;\n" +

        "public class CondyForLambdaSmokeTest {\n" +
        "    void lookForThisMethod() {\n" +
        "        IntStream.of(1,2,3).reduce((a,b) -> a+b);\n" +
        "    }\n" +
        "}";

    static final String methodToLookFor = "lookForThisMethod";
    static final int LDCByteCodePos = 18;

    public static void main(String[] args) throws Exception {
        new CheckCondyGeneratedForLambdaTest().run();
    }

    ToolBox tb = new ToolBox();

    void run() throws Exception {
        compileTestClass();
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"),
                "CondyForLambdaSmokeTest.class").toUri()), methodToLookFor);
    }

    void compileTestClass() throws Exception {
        new JavacTask(tb)
                .options("-XDforNonCapturingLambda=generateCondy")
                .sources(testSource)
                .run();
    }

    void checkClassFile(final File cfile, String methodToFind) throws Exception {
        ClassFile classFile = ClassFile.read(cfile);
        boolean methodFound = false;
        for (Method method : classFile.methods) {
            if (method.getName(classFile.constant_pool).equals(methodToFind)) {
                methodFound = true;
                Code_attribute code = (Code_attribute) method.attributes.get("Code");
                int cpIndex = code.getUnsignedByte(LDCByteCodePos + 1);
                Assert.check(code.getUnsignedByte(LDCByteCodePos) == Opcode.LDC.opcode, "ldc was expected");
                CPInfo cpInfo = classFile.constant_pool.get(cpIndex);
                Assert.check(cpInfo instanceof CONSTANT_Dynamic_info, "condy argument to ldc was expected");
            }
        }
        Assert.check(methodFound, "The seek method was not found");
    }

    void error(String msg) {
        throw new AssertionError(msg);
    }
}
