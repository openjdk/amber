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

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPool.*;
import com.sun.tools.javac.util.Assert;

import toolbox.JavacTask;
import toolbox.ToolBox;

public class CheckForCondyDuplicatesTest {

    static final String testSource =
        "import java.lang.sym.*;\n" +
        "import java.lang.invoke.*;\n" +

        "public class MultipleEntriesCP {\n" +
        "    void m() {\n" +
        "        Object o1 = Intrinsics.ldc(SymbolicRefs.NULL);\n" +
        "        Object o2 = Intrinsics.ldc(SymbolicRefs.NULL);\n" +
        "    }\n" +
        "}";

    public static void main(String[] args) throws Exception {
        new CheckForCondyDuplicatesTest().run();
    }

    ToolBox tb = new ToolBox();

    void run() throws Exception {
        compileTestClass();
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"), "MultipleEntriesCP.class").toUri()));
    }

    void compileTestClass() throws Exception {
        new JavacTask(tb)
                .options("-XDdoConstantFold")
                .sources(testSource)
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
        Assert.check(numberOfCondys == 1, "the CP has duplicate condys");
    }
}
