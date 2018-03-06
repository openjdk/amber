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
 * @summary check that serializable lambdas work independently of the serialization approach
 * @library /tools/lib
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main LambdaSerializationTest
 */

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.sun.tools.javac.util.Assert;
import toolbox.JavacTask;
import toolbox.ToolBox;

public class LambdaSerializationTest {
    private static final String source =
            "import java.io.Serializable;\n" +
            "import java.util.function.*;\n" +
            "public class Test {\n" +
            "    public static String foo() {\n" +
            "        Function<String, String> f = (Function<String, String> & Serializable)(s) -> s;\n" +
            "        return f.apply(\"From serializable lambda\");\n" +
            "    }\n" +
            "}";

    static final String testOut = System.getProperty("user.dir");

    public static void main(String... args) throws Throwable {
        new LambdaSerializationTest().run();
    }

    ToolBox tb = new ToolBox();

    void run() throws Throwable {
        compileTestClass(false);
        String res1 = loadAndInvoke();
        compileTestClass(true);
        String res2 = loadAndInvoke();
        Assert.check(res1.equals(res2));
        Assert.check(res1.equals("From serializable lambda"));
    }

    void compileTestClass(boolean generateCondy) throws Throwable {
        String option = generateCondy ? "-XDforNonCapturingLambda=generateCondy" : "-XDforNonCapturingLambda=generateIndy";
        new JavacTask(tb)
                .options(option)
                .sources(source)
                .run();
    }

    String loadAndInvoke() throws Throwable {
        Path path = Paths.get(testOut);
        System.out.println(path);
        ClassLoader cl = new URLClassLoader(new URL[] { path.toUri().toURL() });
        Class<?> testClass = cl.loadClass("Test");
        Method theMethod = testClass.getDeclaredMethod("foo", new Class<?>[0]);
        return (String)theMethod.invoke(null, new Object[0]);
    }
}
