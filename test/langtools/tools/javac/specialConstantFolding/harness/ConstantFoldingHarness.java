/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @summary provide a test harness for special constant folding
 * @library /tools/lib /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.util
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox toolbox.JavaTask JavacTestingAbstractProcessor HarnessAnnotations ConstantFoldingHarness
 * @run main ConstantFoldingHarness
 */

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.*;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.*;

import com.sun.source.util.JavacTask;
import com.sun.tools.classfile.*;
import com.sun.tools.classfile.ConstantPool.*;
import com.sun.tools.javac.util.Assert;

import static javax.tools.StandardLocation.*;
import static javax.tools.JavaFileObject.Kind.SOURCE;

import toolbox.JavaTask;
import toolbox.ToolBox;

/** This test harness does two main things:
 *  1 - checks that the code generated for each test case is correct according to annotations present
 *      in the test case, and
 *  2 - executes the test cases for them to reinforce additional checks.
 */
public class ConstantFoldingHarness {

    static int nerrors = 0;

    static final JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
    static final StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null);

    public static void main(String[] args) throws Throwable {
        try {
            String testDir = System.getProperty("test.src");
            fm.setLocation(SOURCE_PATH, Arrays.asList(new File(testDir, "tests")));

            // Make sure classes are written to scratch dir.
            fm.setLocation(CLASS_OUTPUT, Arrays.asList(new File(".")));

            for (JavaFileObject jfo : fm.list(SOURCE_PATH, "", Collections.singleton(SOURCE), true)) {
                new ConstantFoldingHarness(jfo).checkAndExecute();
            }
            if (nerrors > 0) {
                throw new AssertionError("Errors were found");
            }
        } finally {
            fm.close();
        }
    }

    JavaFileObject jfo;
    Map<ElementKey, Annotation> lcdInfoMap = new HashMap<>();
    Set<String> declaredKeys = new HashSet<>();
    List<ElementKey> seenMultipleLDCInfo = new ArrayList<>();

    protected ConstantFoldingHarness(JavaFileObject jfo) {
        this.jfo = jfo;
    }

    ToolBox tb = new ToolBox();

    String generateClassPath() {
        String testDir = System.getProperty("test.src");
        String pathSeparator = System.getProperty("path.separator");
        return testDir + pathSeparator + System.getProperty("test.classes");
    }

    void checkAndExecute() throws Throwable {
        String classPathValue = generateClassPath();
        JavacTask ct = (JavacTask) comp.getTask(
                null, fm, null, Arrays.asList("-cp", classPathValue, "-XDdoConstantFold"),
                null, Arrays.asList(jfo));
        System.err.println("------------------------------------------------------------------");
        System.err.println("compiling code " + jfo);
        ct.setProcessors(Collections.singleton(new LCDInfoFinder()));
        lcdInfoMap.clear();
        if (!ct.call()) {
            throw new AssertionError("Error during compilation");
        }

        if (ignoreTest) {
            return;
        }

        File javaFile = new File(jfo.getName());
        File classFile = new File(javaFile.getName().replace(".java", ".class"));
        checkClassFile(classFile);

        //check all candidates have been used up
        for (Map.Entry<ElementKey, Annotation> entry : lcdInfoMap.entrySet()) {
            if (!seenMultipleLDCInfo.contains(entry.getKey())) {
                error("Redundant @LCDData annotation on method " +
                        entry.getKey().elem + " with key " + entry.getKey());
            }
        }

        if (executeTestCase) {
            System.err.println("executing the test case");
            new JavaTask(tb)
                    .includeStandardOptions(false)
                    .classpath(System.getProperty("user.dir"))
                    .vmOptions("--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED")
                    .className(classFile.getName().replace(".class", ""))
                    .run();
            System.err.println("test case successfully executed");
        }
    }

    void checkClassFile(File file) throws Throwable {
        ClassFile classFile = ClassFile.read(file);
        ConstantPool constantPool = classFile.constant_pool;

        //lets get all the methods in the class file.
        for (Method method : classFile.methods) {
            System.err.println("processing method " + method.getName(constantPool));
            for (ElementKey elementKey: lcdInfoMap.keySet()) {
                System.err.println("processing elementKey.elem " + elementKey.elem);
                String methodDesc = method.getName(constantPool) +
                        method.descriptor.getParameterTypes(constantPool).replace(" ", "").replace('$', '.');
                String elemStr = elementKey.elem.toString();
                if (elementKey.elem.getKind() == ElementKind.CLASS) {
                    elemStr = "<clinit>()";
                }
                System.err.println("method descriptor " + methodDesc);
                if (methodDesc.equals(elemStr)) {
                    checkMethod(constantPool, method, lcdInfoMap.get(elementKey));
                    seenMultipleLDCInfo.add(elementKey);
                }
            }
        }
    }

    void checkMethod(ConstantPool constantPool, Method method, Annotation annotation) throws Throwable {
        System.err.println("checking method " + method.getName(constantPool));
        Code_attribute code = (Code_attribute) method.attributes.get(Attribute.Code);
        InstructionInfo[] ldcInfoArr;
        if (annotation instanceof InstructionsInfo) {
            InstructionsInfo lcInfos = (InstructionsInfo)annotation;
            ldcInfoArr = lcInfos.value();
        } else {
            ldcInfoArr = new InstructionInfo[1];
            ldcInfoArr[0] = (InstructionInfo)annotation;
        }
        for (Annotation anno : ldcInfoArr) {
            InstructionInfo instructionInfo = (InstructionInfo)anno;
            int bytecodePosition = instructionInfo.bytecodePosition();
            System.err.println("checking instruction at " + bytecodePosition);
            Assert.check(code.getUnsignedByte(bytecodePosition) == instructionInfo.instructionCode(),
                    "instruction code found " + code.getUnsignedByte(bytecodePosition) +
                    " expected " + instructionInfo.instructionCode());
            if (instructionInfo.instructionCode() == Opcode.LDC.opcode) {
                processLDC(constantPool, code, instructionInfo);
            }
        }
    }

    void processLDC(ConstantPool constantPool, Code_attribute code, InstructionInfo ldcInfo) throws Throwable {
        int cpIndex = code.getUnsignedByte(ldcInfo.bytecodePosition() + 1);
        String cpInfoTypeName = ldcInfo.values()[0];
        String cpInfoValue = ldcInfo.values()[1];
        CPInfo cpInfo = constantPool.get(cpIndex);
        Assert.check(cpInfo.getClass().getSimpleName().equals(cpInfoTypeName),
                cpInfo.getClass().getSimpleName() + " is not equal to " + cpInfoTypeName);
        if (cpInfo instanceof CONSTANT_Class_info) {
            CONSTANT_Class_info classInfo = (CONSTANT_Class_info)cpInfo;
            Assert.check(classInfo.getName().equals(cpInfoValue), "found " + cpInfoValue + " expected " + classInfo.getName());
        } else if (cpInfo instanceof CONSTANT_MethodHandle_info) {
            CONSTANT_MethodHandle_info mHandleInfo = (CONSTANT_MethodHandle_info)cpInfo;
            Assert.check(mHandleInfo.reference_kind.toString().equals(cpInfoValue));
        } else if (cpInfo instanceof CONSTANT_MethodType_info) {
            CONSTANT_MethodType_info mTypeInfo = (CONSTANT_MethodType_info)cpInfo;
            Assert.check(mTypeInfo.getType().equals(cpInfoValue));
        } else {
            throw new AssertionError("unexpected constant pool info " + cpInfo);
        }
    }

    protected void error(String msg) {
        nerrors++;
        System.err.printf("Error occurred while checking file: %s\nreason: %s\n",
                jfo.getName(), msg);
    }

    boolean executeTestCase = true;
    boolean ignoreTest;

    class LCDInfoFinder extends JavacTestingAbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            if (roundEnv.processingOver())
                return true;

            ignoreTest = !roundEnv.getElementsAnnotatedWith(elements.getTypeElement("IgnoreTest")).isEmpty();
            if (!ignoreTest) {
                executeTestCase = roundEnv.getElementsAnnotatedWith(elements.getTypeElement("SkipExecution")).isEmpty();
                TypeElement ldcInfoRepeatedAnno = elements.getTypeElement("InstructionsInfo");
                TypeElement ldcInfoSingleAnno = elements.getTypeElement("InstructionInfo");
//                if (!annotations.contains(ldcInfoRepeatedAnno) && !annotations.contains(ldcInfoSingleAnno)) {
//                    error("no @InstructionInfo annotation found in test class");
//                }

                for (Element elem: roundEnv.getElementsAnnotatedWith(ldcInfoRepeatedAnno)) {
                    Annotation annotation = elem.getAnnotation(InstructionsInfo.class);
                    System.err.println("putting into key " + elem + " with annotation " + annotation);
                    lcdInfoMap.put(new ElementKey(elem), annotation);
                }

                for (Element elem: roundEnv.getElementsAnnotatedWith(ldcInfoSingleAnno)) {
                    Annotation annotation = elem.getAnnotation(InstructionInfo.class);
                    System.err.println("putting into key " + elem + " with annotation " + annotation);
                    lcdInfoMap.put(new ElementKey(elem), annotation);
                }
            }
            return true;
        }
    }

    class ElementKey {
        String key;
        Element elem;

        public ElementKey(Element elem) {
            this.elem = elem;
            this.key = computeKey(elem);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ElementKey) {
                ElementKey other = (ElementKey)obj;
                return other.key.equals(key);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        String computeKey(Element e) {
            StringBuilder buf = new StringBuilder();
            while (e != null) {
                buf.append(e.toString());
                e = e.getEnclosingElement();
            }
            buf.append(jfo.getName());
            return buf.toString();
        }

        @Override
        public String toString() {
            return "Key{" + key + "}";
        }
    }
}
