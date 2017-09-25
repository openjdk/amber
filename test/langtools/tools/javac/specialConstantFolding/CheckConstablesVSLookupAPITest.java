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
 * @test 8168964
 * @summary check that javac resolves to the same method handle as using the MH API
 * @library /tools/lib /tools/javac/lib
 * @ignore
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.comp
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.compiler/com.sun.tools.javac.file
 * @build toolbox.ToolBox toolbox.Task toolbox.JavaTask CheckConstablesVSLookupAPITest
 * @run main CheckConstablesVSLookupAPITest
 */

import java.lang.invoke.*;

import java.io.IOException;

import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask.Result;
import combo.ComboTestHelper;

import toolbox.JavaTask;
import toolbox.ToolBox;
import toolbox.Task;

import static java.lang.invoke.MethodType.*;

public class CheckConstablesVSLookupAPITest extends ComboInstance<CheckConstablesVSLookupAPITest> {

    enum MethodParameters implements ComboParameter {
        MT1(methodType(void.class, Object.class, int.class)),
        MT2(methodType(void.class, Object.class, Integer.class)),
        MT3(methodType(void.class, Object.class, int.class, int.class)),
        MT4(methodType(void.class, Object.class, Integer.class, Integer.class)),
        MT5(methodType(void.class, Object.class, int.class, Integer.class)),
        MT6(methodType(void.class, Object.class, int[].class)),
        MT7(methodType(void.class, Object.class, Integer[].class)),
        MT8(methodType(void.class, String.class, Integer[].class));

        MethodType methodType;

        MethodParameters(MethodType methodType) {
            this.methodType = methodType;
        }

        @Override
        public String expand(String optParameter) {
            return (optParameter.equals("FORMAL")) ?
                    produceMethodParametersFromMT(methodType):
                    methodType.toMethodDescriptorString();
        }
    }

    enum Method implements ComboParameter {
        STATIC_MH("staticMethodHandle", "findStatic"),
        VIRTUAL_MH("virtualMethodHandle", "findVirtual"),
        CONSTRUCTOR_MH("constructorMethodHandle", "findConstructor"),
        SPECIAL_MH("specialMethodHandle", "findSpecial");

        String constablesName;
        String lookupName;

        Method(String constablesName, String lookupName) {
            this.constablesName = constablesName;
            this.lookupName = lookupName;
        }

        @Override
        public String expand(String optParameter) {
            switch (optParameter) {
                case "STATIC":
                    return this == STATIC_MH ? "static" : "";
                case "METHOD_NAME":
                    return this == CONSTRUCTOR_MH ? "MHTest" : "m";
                case "CONSTABLES":
                    return constablesName;
                case "LOOKUP":
                    return lookupName;
                case "RETURN_TYPE":
                    return this == CONSTRUCTOR_MH ? "" : "void";
                case "SPECIAL_CALLER_ARG":
                    return this == SPECIAL_MH ? ", MHTest.class" : "";
                case "NAME_ARG":
                    return this == CONSTRUCTOR_MH ? "" : "\"m\", ";
            }
            return "";
        }
    }

    public static void main(String... args) throws Exception {
        new ComboTestHelper<CheckConstablesVSLookupAPITest>()
                .withDimension("PARAMS1", null, MethodParameters.values())
                .withDimension("METHOD", null, Method.values())
                .withDimension("PARAMS2", null, MethodParameters.values())
                .run(CheckConstablesVSLookupAPITest::new);
    }

    ToolBox tb = new ToolBox();

    @Override
    public void doWork() throws IOException {
        Result<?> constablesCompilationResult = newCompilationTask()
                .withOption("-XDdoConstantFold")
                .withSourceFromTemplate(constablesTemplate)
                .analyze();

        boolean constablesCompiled = !constablesCompilationResult.hasErrors();

        System.err.println("constablesCompiled " + constablesCompiled);

        Result<?> lookupCompilationResult = newCompilationTask()
                .withSourceFromTemplate(lookupTemplate)
                .generate();

        boolean lookupCompiled = !lookupCompilationResult.hasErrors();

        System.err.println("lookupCompiled " + lookupCompiled);

        if (constablesCompiled && !lookupCompiled) {
            fail("found a behavior mismatch for sources:\n" +
                constablesCompilationResult.compilationInfo() +
                "\nand:\n " + lookupCompilationResult.compilationInfo());
        }

        if (!constablesCompiled && !lookupCompiled) {
            return;
        }

        boolean executionSuccessful;
        try {
            Task.Result result = new JavaTask(tb)
                    .includeStandardOptions(false)
                    .classpath(System.getProperty("user.dir"))
                    .className("MHTest")
                    .run();
            executionSuccessful = result.exitCode == 0;
        } catch (Throwable t) {
            System.err.println("execution error " + t.getMessage());
            executionSuccessful = false;
        }
        System.err.println("executionSuccessful " + executionSuccessful);
        if (constablesCompiled != executionSuccessful) {
            fail("found a behavior mismatch for sources:\n" +
                constablesCompilationResult.compilationInfo() +
                "\nand:\n " + lookupCompilationResult.compilationInfo());
        }
    }

    /** Without loss of generality, to simplify the generation of the method declaration we can
     *  assume that the return type is always void. If a the passed method type has a different
     *  return type, it will be ignored.
     */
    static String produceMethodParametersFromMT(MethodType mt) {
        String result = "";
        int paramIndex = 0;
        Class<?>[] params = mt.parameterArray();
        // for the first params but the last one
        for (int i = 0; i < params.length - 1; i++) {
            Class<?> param = params[i];
            result += param.getTypeName() + " p" + paramIndex + ", ";
            paramIndex++;
        }
        // now let's add the last param if any
        if (params.length > 0) {
            Class<?> param = params[params.length - 1];
            String paramType = param.getTypeName();
            if (paramType.endsWith("[]")) {
                paramType = paramType.substring(0, paramType.length() - 2);
                paramType += "...";
            }
            result += paramType + " p" + paramIndex;
        }
        return result;
    }

    String constablesTemplate =
        "import java.lang.invoke.*;\n" +
        "import java.util.*;\n" +
        "import static java.lang.invoke.Constables.*;\n" +
        "\n" +
        "class MHTest {\n" +
        "    #{METHOD.STATIC} #{METHOD.RETURN_TYPE} #{METHOD.METHOD_NAME} (#{PARAMS1.FORMAL}) {}\n" +
        "    void test() {\n" +
        "        MethodHandle mh = ldc(#{METHOD.CONSTABLES}(ClassRef.of(\"MHTest\"), #{METHOD.NAME_ARG} MethodTypeRef.of(\"#{PARAMS2.DESC}\")));\n" +
        "    }\n" +
        "}";

    String lookupTemplate =
        "import java.lang.invoke.*;\n" +
        "import java.util.*;\n" +
        "import static java.lang.invoke.MethodType.*;\n" +
        "\n" +
        "class MHTest {\n" +
        "    #{METHOD.STATIC} #{METHOD.RETURN_TYPE} #{METHOD.METHOD_NAME} (#{PARAMS1.FORMAL}) {}\n" +
        "    public static void main(String[] args) throws Exception {\n" +
        "        MethodHandles.lookup().#{METHOD.LOOKUP}(MHTest.class, #{METHOD.NAME_ARG} fromMethodDescriptorString(\"#{PARAMS2.DESC}\", null) #{METHOD.SPECIAL_CALLER_ARG});\n" +
        "    }\n" +
        "}";
}
