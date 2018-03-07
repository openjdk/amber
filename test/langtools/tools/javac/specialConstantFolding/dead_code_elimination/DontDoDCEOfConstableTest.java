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
 * @summary dont do dead code elimination for subclasses of Constable
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.file
 *      jdk.compiler/com.sun.tools.javac.main
 *      jdk.jdeps/com.sun.tools.classfile
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 *      jdk.compiler/com.sun.tools.javac.util
 *      jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox toolbox.JavacTask toolbox.ModuleBuilder
 * @run main DontDoDCEOfConstableTest
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.tools.JavaFileManager.Location;
import javax.tools.StandardJavaFileManager;

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Dynamic_info;
import com.sun.tools.classfile.ConstantPool.CPInfo;
import com.sun.tools.classfile.Method;
import com.sun.tools.classfile.Opcode;
import com.sun.tools.javac.util.Assert;
import toolbox.TestRunner;
import toolbox.ToolBox;


public class DontDoDCEOfConstableTest extends TestRunner {

    ToolBox tb;

    public DontDoDCEOfConstableTest() {
        super(System.err);
        tb = new ToolBox();
    }

    protected void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    Path[] findJavaFiles(Path... paths) throws IOException {
        return tb.findJavaFiles(paths);
    }

    void checkOutputContains(String log, String... expect) throws Exception {
        for (String e : expect) {
            if (!log.contains(e)) {
                throw new Exception("expected output not found: " + e);
            }
        }
    }

    public static void main(String... args) throws Exception {
        DontDoDCEOfConstableTest t = new DontDoDCEOfConstableTest();
        t.runTests();
    }

    static List<String> locationPaths(StandardJavaFileManager fm, Location loc) {
        return StreamSupport.stream(fm.getLocationAsPaths(loc).spliterator(), false)
                            .map(p -> p.toString())
                            .collect(Collectors.toList());
    }

    private final static String stubSource =
            "package java.lang.sym;\n" +

            "import java.util.Optional;\n" +
            "import jdk.internal.vm.annotation.*;\n" +
            "import java.lang.invoke.*;\n" +
            "import static java.lang.sym.ConstantRefs.*;\n" +

            "/** --\n" +
            "*/\n" +
            "public class Stub {\n" +
            "   private static final ClassRef CR_BOX = ClassRef.of(\"java.lang.sym.Stub\").inner(\"Box\");\n" +
            "   private static final MethodHandleRef MH_BOX = MethodHandleRef.of(MethodHandleRef.Kind.CONSTRUCTOR, CR_BOX, \"_\", CR_void, CR_String);\n" +
            "   /**\n" +
            "    * x\n" +
            "    * @return x\n" +
            "    */\n" +
            "   @Foldable\n" +
            "   public static String string() { return \"foo\"; }    /**\n" +
            "    * x\n" +
            "    * @param s s\n" +
            "    * @return x\n" +
            "    */\n" +
            "   @Foldable\n" +
            "   public static String substring(String s) { return s.substring(1); }    /**\n" +
            "    * x\n" +
            "    * @param s s\n" +
            "    * @return x\n" +
            "    */\n" +
            "   @Foldable\n" +
            "   public static Box box(String s) { return new Box(s); }    /**\n" +
            "    * x\n" +
            "    * @param b x\n" +
            "    * @return x\n" +
            "    */\n" +
            "   @Foldable\n" +
            "   public static String unbox(Box b) { return b.s; }    /**\n" +
            "    * box\n" +
            "     */\n" +
            "   public static class Box implements Constable<Box> {\n" +
            "       /**\n" +
            "        * x\n" +
            "        */\n" +
            "       String s;        /**\n" +
            "        * x\n" +
            "        * @param s x\n" +
            "        */\n" +
            "       public Box(String s) {\n" +
            "           this.s = s;\n" +
            "       }        @Override\n" +
            "       public Optional<? extends ConstantRef<? super Box>> toConstantRef(MethodHandles.Lookup lookup) {\n" +
            "           return DynamicConstantRef.symbolizeHelper(lookup, MH_BOX, CR_BOX, s);\n" +
            "       }\n" +
            "   }\n" +
            "}";

    private static final String testSource =
            "import java.lang.sym.Stub;\n" +

            "class Test {\n" +
            "    public static void main(String... args) {\n" +
            "        new Test().foo();\n" +
            "    }\n" +

            "    public void foo() {\n" +
            "       Stub.Box b = Stub.box(\"foo\");\n" +
            "       System.out.println(b);\n" +
            "   }\n" +
            "}";

    @Test
    public void testPatchWithSource(Path base) throws Exception {
        Path patch = base.resolve("patch");
        tb.writeJavaFiles(patch, stubSource);
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, testSource);
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        new toolbox.JavacTask(tb)
            .options("-XDdoConstantFold", "--patch-module", "java.base=" + patch.toString())
            .outdir(classes)
            .files(findJavaFiles(src))
            .run()
            .writeAll();
        checkClassFile(new File(Paths.get(classes.toString(), "Test.class").toUri()), 0, 6);
    }

    void checkClassFile(final File cfile, int... ldcPositions) throws Exception {
        ClassFile classFile = ClassFile.read(cfile);
        boolean methodFound = false;
        for (Method method : classFile.methods) {
            if (method.getName(classFile.constant_pool).equals("foo")) {
                methodFound = true;
                Code_attribute code = (Code_attribute) method.attributes.get("Code");
                for (int ldcPos : ldcPositions) {
                    int cpIndex = code.getUnsignedByte(ldcPos + 1);
                    Assert.check(code.getUnsignedByte(ldcPos) == Opcode.LDC.opcode, "ldc was expected");
                    CPInfo cpInfo = classFile.constant_pool.get(cpIndex);
                    Assert.check(cpInfo instanceof CONSTANT_Dynamic_info, "condy argument to ldc was expected");
                }
            }
        }
        Assert.check(methodFound, "The seek method was not found");
    }
}
