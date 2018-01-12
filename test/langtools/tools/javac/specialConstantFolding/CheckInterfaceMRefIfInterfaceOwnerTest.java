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
 * @run main CheckInterfaceMRefIfInterfaceOwnerTest
 */

import java.io.File;
import java.nio.file.Paths;

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPool.*;
import com.sun.tools.javac.util.Assert;

import toolbox.JavacTask;
import toolbox.ToolBox;

public class CheckInterfaceMRefIfInterfaceOwnerTest {

    static final String testSource =
            "import java.lang.invoke.*;\n" +
            "import java.lang.sym.*;\n" +

            "public class Test {\n" +
            "    private static final ClassRef THIS = ClassRef.of(\"Test\");\n" +
            "    public int m(int i) { return i; }\n" +

            "    public void test() {\n" +
            "        MethodHandleRef negIMethodRef = MethodHandleRef.of(\n" +
            "            MethodHandleRef.Kind.INTERFACE_VIRTUAL, THIS, \"m\", MethodTypeRef.ofDescriptor(\"(I)I\"));\n" +
            "        Intrinsics.ldc(negIMethodRef);\n" +
            "    }\n" +
            "}";

    public static void main(String[] args) throws Exception {
        new CheckInterfaceMRefIfInterfaceOwnerTest().run();
    }

    void run() throws Exception {
        compileTestClass();
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"), "Test.class").toUri()), 33);
    }

    void compileTestClass() throws Exception {
        ToolBox tb = new ToolBox();
        new JavacTask(tb)
                .options("-XDdoConstantFold")
                .sources(testSource)
                .run();
    }

    void checkClassFile(final File cfile, int cpIndex) throws Exception {
        ClassFile classFile = ClassFile.read(cfile);
        CPInfo cpInfo = classFile.constant_pool.get(cpIndex);
        Assert.check(cpInfo instanceof CONSTANT_InterfaceMethodref_info, "unexpected CPInfo at pos " + cpIndex);
    }
}
