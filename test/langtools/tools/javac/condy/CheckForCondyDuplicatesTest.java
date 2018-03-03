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
 * @summary check that javac is not generating duplicate condys
 * @library /tools/lib
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main CheckForCondyDuplicatesTest
 */

import java.io.File;
import java.nio.file.Paths;
import java.lang.sym.*;

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPool.*;
import com.sun.tools.javac.util.Assert;

import toolbox.JavacTask;
import toolbox.ToolBox;

public class CheckForCondyDuplicatesTest {

    static final String testSource1 =
        "import java.lang.sym.*;\n" +
        "import java.lang.invoke.*;\n" +

        "public class Test1 {\n" +
        "    void m() {\n" +
        "        Object o1 = Intrinsics.ldc(ConstantRefs.NULL);\n" +
        "        Object o2 = Intrinsics.ldc(ConstantRefs.NULL);\n" +
        "    }\n" +
        "}";

    static final String testSource2 =
        "class Test2 {\n" +
        "   Runnable r = Test2::foo;\n" +
        "   Runnable k = Test2::foo;\n" +
        "   static void foo() {}\n" +
        "}";

    public static void main(String[] args) throws Exception {
        new CheckForCondyDuplicatesTest().run();
    }

    ToolBox tb = new ToolBox();

    void run() throws Exception {
        compileTestClass(testSource1);
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"), "Test1.class").toUri()));

        compileTestClass(testSource2);
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"), "Test2.class").toUri()));
    }

    void compileTestClass(String source) throws Exception {
        new JavacTask(tb)
                .options("-XDdoConstantFold")
                .sources(source)
                .run();
    }

    void checkClassFile(final File cfile) throws Exception {
        ClassFile classFile = ClassFile.read(cfile);
        int numberOfCondys = 0;
        for (CPInfo cpInfo : classFile.constant_pool.entries()) {
            if (cpInfo instanceof CONSTANT_Dynamic_info) {
                numberOfCondys++;
            }
        }
        Assert.check(numberOfCondys > 0, "there should be at least one condy in the class file");
        Assert.check(numberOfCondys == 1, "the CP has duplicate condys");
    }
}
