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
 * @summary
 * @library /tools/lib
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main NoLocalsMustBeReservedForDCEedVarsTest
 */

import java.io.File;
import java.nio.file.Paths;

import com.sun.tools.classfile.*;
import com.sun.tools.javac.util.Assert;

import toolbox.JavacTask;
import toolbox.ToolBox;

public class NoLocalsMustBeReservedForDCEedVarsTest {

    static final String source =
            "import java.lang.constant.ClassDesc;\n" +
            "import java.lang.constant.DirectMethodHandleDesc.Kind;\n" +
            "import java.lang.constant.DynamicConstantDesc;\n" +
            "import java.lang.constant.MethodHandleDesc;\n" +
            "import java.lang.constant.MethodTypeDesc;\n" +
            "import java.lang.invoke.Intrinsics;\n" +
            "import java.lang.invoke.MethodHandles.Lookup;\n" +

            "public class ConstantDynamicExample {\n" +
            "    public static long primitiveExample() {\n" +
            "        MethodTypeDesc methodDescriptor = MethodTypeDesc.ofDescriptor(\"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)J\");\n" +
            "        DynamicConstantDesc<Long> dynamicConstant = DynamicConstantDesc.of(\n" +
            "            MethodHandleDesc.of(Kind.STATIC, ClassDesc.of(\"jdk12.ConstantDynamicExample\"), \"bsm\", methodDescriptor));\n" +
            "        return Intrinsics.ldc(dynamicConstant);\n" +
            "    }\n" +

            "    private static long bsm(Lookup lookup, String name, Class<?> type) {\n" +
            "        return 3L;\n" +
            "    }\n" +
            "}";

    public static void main(String[] args) throws Exception {
        new NoLocalsMustBeReservedForDCEedVarsTest().run();
    }

    void run() throws Exception {
        ToolBox tb = new ToolBox();
        new JavacTask(tb)
                .sources(source)
                .run();

        File cfile = new File(Paths.get(System.getProperty("user.dir"), "ConstantDynamicExample.class").toUri());
        ClassFile classFile = ClassFile.read(cfile);
        for (Method method: classFile.methods) {
            if (method.getName(classFile.constant_pool).equals("primitiveExample")) {
                Code_attribute codeAttr = (Code_attribute)method.attributes.get("Code");
                Assert.check(codeAttr.max_locals == 0);
            }
        }
    }
}
