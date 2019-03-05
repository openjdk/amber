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
 * @summary check subtypes of sealed classes
 * @library /tools/lib
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main CheckSubtypesOfSealedTest
 * @ignore
 */

import java.io.File;
import java.nio.file.Paths;

import com.sun.tools.classfile.*;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Utf8_info;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.util.Assert;

import toolbox.JavacTask;
import toolbox.ToolBox;

public class CheckSubtypesOfSealedTest {

    static final String testSource =
            "import java.lang.annotation.*;\n" +
            "public class SealedClasses {\n" +
            "    @Sealed abstract class SAC {}\n" +
            "    abstract class SAC2 extends SAC {}\n" +
            "    class SAC3 extends SAC {}\n" +
            "    class SAC4 extends SAC2 {}\n" +
            "    SAC sac = new SAC() {};\n" +
            "    @Sealed interface SI {}\n" +
            "    interface SSI extends SI {}\n" +
            "    class SAC5 implements SI {}\n" +
            "    @NotSealed abstract class SAC6 extends SAC {}\n" +
            "    @NotSealed class SAC7 extends SAC {}\n" +
            "}";

    public static void main(String[] args) throws Exception {
        new CheckSubtypesOfSealedTest().run();
    }

    ToolBox tb = new ToolBox();

    void run() throws Exception {
        compileTestClass();
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"), "SealedClasses$SAC2.class").toUri()), CheckFor.SEALED);
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"), "SealedClasses$SAC3.class").toUri()), CheckFor.FINAL);
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"), "SealedClasses$SAC4.class").toUri()), CheckFor.FINAL);
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"), "SealedClasses$1.class").toUri()), CheckFor.FINAL);
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"), "SealedClasses$SSI.class").toUri()), CheckFor.SEALED);
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"), "SealedClasses$SAC5.class").toUri()), CheckFor.FINAL);
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"), "SealedClasses$SAC6.class").toUri()), CheckFor.NOT_SEALED);
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"), "SealedClasses$SAC7.class").toUri()), CheckFor.NOT_SEALED, CheckFor.NON_FINAL);
    }

    void compileTestClass() throws Exception {
        new JavacTask(tb)
                .sources(testSource)
                .run();
    }

    enum CheckFor {
        SEALED,
        FINAL,
        NON_FINAL,
        NOT_SEALED
    }

    void checkClassFile(final File cfile, CheckFor... checkFor) throws Exception {
        ClassFile classFile = ClassFile.read(cfile);
        for (CheckFor whatToCheckFor : checkFor) {
            if (whatToCheckFor == CheckFor.SEALED) {
                for (Attribute attr: classFile.attributes) {
                    if (attr.getName(classFile.constant_pool).equals("RuntimeVisibleAnnotations")) {
                        RuntimeVisibleAnnotations_attribute rtva = (RuntimeVisibleAnnotations_attribute)attr;
                        Assert.check(rtva.annotations.length == 1, classFile.getName() + " should have only one runtime visible annotation");
                        CONSTANT_Utf8_info utfInfo = (CONSTANT_Utf8_info)classFile.constant_pool.get(rtva.annotations[0].type_index);
                        Assert.check(utfInfo.value.equals("Ljava/lang/annotation/Sealed;"), classFile.getName() + " should be sealed");
                        return;
                    }
                }
                throw new AssertionError(classFile.getName() + " should be sealed");
            } else if (whatToCheckFor == CheckFor.FINAL && (classFile.access_flags.flags & Flags.FINAL) == 0) {
                throw new AssertionError(classFile.getName() + " should be final");
            } else if (whatToCheckFor == CheckFor.NON_FINAL && (classFile.access_flags.flags & Flags.FINAL) != 0) {
                throw new AssertionError(classFile.getName() + " should not be final");
            } else if (whatToCheckFor == CheckFor.NOT_SEALED) {
                for (Attribute attr: classFile.attributes) {
                    if (attr.getName(classFile.constant_pool).equals("RuntimeVisibleAnnotations")) {
                        RuntimeVisibleAnnotations_attribute rtva = (RuntimeVisibleAnnotations_attribute)attr;
                        Assert.check(rtva.annotations.length == 1, classFile.getName() + " should have only one runtime visible annotation");
                        CONSTANT_Utf8_info utfInfo = (CONSTANT_Utf8_info)classFile.constant_pool.get(rtva.annotations[0].type_index);
                        Assert.check(utfInfo.value.equals("Ljava/lang/annotation/NotSealed;"), classFile.getName() + " should not be sealed");
                        return;
                    }
                }
                throw new AssertionError(classFile.getName() + " should not be sealed");
            }
        }
    }
}
