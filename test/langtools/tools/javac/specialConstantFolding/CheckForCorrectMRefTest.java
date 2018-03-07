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
 * @run main CheckForCorrectMRefTest
 */

import java.io.File;
import java.nio.file.Paths;

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPool.*;
import com.sun.tools.javac.util.Assert;

import toolbox.JavacTask;
import toolbox.ToolBox;

public class CheckForCorrectMRefTest {

    static final String source1 =
            "import java.lang.invoke.*;\n" +
            "import java.lang.invoke.constant.*;\n" +

            "public class Test1 {\n" +
            "    private static final ClassRef THIS = ClassRef.of(\"Test1\");\n" +
            "    public int m(int i) { return i; }\n" +

            "    public void test() {\n" +
            "        MethodHandleRef negIMethodRef = MethodHandleRef.of(\n" +
            "            MethodHandleRef.Kind.INTERFACE_VIRTUAL, THIS, \"m\", MethodTypeRef.ofDescriptor(\"(I)I\"));\n" +
            "        Intrinsics.ldc(negIMethodRef);\n" +
            "    }\n" +
            "}";

    static final String source2 =
            "import java.lang.invoke.*;\n" +
            "import java.lang.invoke.constant.*;\n" +

            "public class Test2 {\n" +
            "    interface I {}\n" +

            "    private static final ClassRef THE_INTERFACE = ClassRef.of(\"Test2$I\");\n" +

            "    public void test1() {\n" +
            "        MethodHandleRef negIMethodRef = MethodHandleRef.of(MethodHandleRef.Kind.CONSTRUCTOR, THE_INTERFACE, \"\", MethodTypeRef.ofDescriptor(\"()V\"));\n" +
            "        Intrinsics.ldc(negIMethodRef);\n" +
            "    }\n" +

            "    public void test2() {\n" +
            "        MethodHandleRef negIMethodRef = MethodHandleRef.ofField(MethodHandleRef.Kind.GETTER, THE_INTERFACE, \"strField\", ConstantRefs.CR_String);\n" +
            "        Intrinsics.ldc(negIMethodRef);\n" +
            "    }\n" +

            "    public void test3() {\n" +
            "        MethodHandleRef negIMethodRef = MethodHandleRef.ofField(MethodHandleRef.Kind.SETTER, THE_INTERFACE, \"strField\", ConstantRefs.CR_String);\n" +
            "        Intrinsics.ldc(negIMethodRef);\n" +
            "    }\n" +
            "}";

    public static void main(String[] args) throws Exception {
        new CheckForCorrectMRefTest().run();
    }

    void run() throws Exception {
        compileTestClass(source1);
        compileTestClass(source2);
        checkForInterfaceMRef(17);
        checkForMRef(22);
        checkForFieldRef(23, 24);
    }

    void compileTestClass(String source) throws Exception {
        ToolBox tb = new ToolBox();
        new JavacTask(tb)
                .options("-XDdoConstantFold")
                .sources(source)
                .run();
    }

    void checkForInterfaceMRef(int cpIndex) throws Exception {
        File cfile = new File(Paths.get(System.getProperty("user.dir"), "Test1.class").toUri());
        ClassFile classFile = ClassFile.read(cfile);
        CPInfo cpInfo = classFile.constant_pool.get(cpIndex);
        Assert.check(cpInfo instanceof CONSTANT_InterfaceMethodref_info, "unexpected CPInfo at pos " + cpIndex);
    }

    void checkForMRef(int... cpIndexes) throws Exception {
        File cfile = new File(Paths.get(System.getProperty("user.dir"), "Test2.class").toUri());
        ClassFile classFile = ClassFile.read(cfile);
        for (int index : cpIndexes) {
            CPInfo cpInfo = classFile.constant_pool.get(index);
            Assert.check(cpInfo instanceof CONSTANT_Methodref_info, "unexpected CPInfo at pos " + index);
        }
    }

    void checkForFieldRef(int... cpIndexes) throws Exception {
        File cfile = new File(Paths.get(System.getProperty("user.dir"), "Test2.class").toUri());
        ClassFile classFile = ClassFile.read(cfile);
        for (int index : cpIndexes) {
            CPInfo cpInfo = classFile.constant_pool.get(index);
            Assert.check(cpInfo instanceof CONSTANT_Fieldref_info, "unexpected CPInfo at pos " + index);
        }
    }
}
