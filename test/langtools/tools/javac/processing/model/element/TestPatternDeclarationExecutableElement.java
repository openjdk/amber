/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test basic properties of the PatternDeclaration kind of javax.lang.element.ExecutableElement
 * @author  Angelos Bimpoudis
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @enablePreview
 * @build   JavacTestingAbstractProcessor
 * @compile TestPatternDeclarationExecutableElement.java
 * @compile --enable-preview --source ${jdk.version} -processor TestPatternDeclarationExecutableElement -proc:only TestPatternDeclarationExecutableElementData.java
 */

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import javax.lang.model.util.ElementFilter;
import java.util.Set;

import static javax.lang.model.util.ElementFilter.constructorsIn;
import static javax.lang.model.util.ElementFilter.deconstructorsIn;

/**
 * Test basic workings of javax.lang.element.ExecutableElement for pattern declarations
 */
public class TestPatternDeclarationExecutableElement extends JavacTestingAbstractProcessor {
    private final String name  = "Name";
    private boolean capitalize = false;

    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            int count = 0;
            for (Element element : roundEnv.getRootElements()) {
                for (ExecutableElement pattern : ElementFilter.deconstructorsIn(element.getEnclosedElements())) {
                    count++;
                    var simpleName = pattern.getSimpleName();
                    var returnType = pattern.getReturnType();
                    var parameters = pattern.getParameters();
                    var bindings   = pattern.getBindings();
                    var type       = pattern.asType();

                    if (!simpleName.contentEquals("TestPatternDeclarationExecutableElementData"))
                        throw new RuntimeException("Unexpected name for deconstructor " + simpleName);

                    if (!(returnType instanceof NoType))
                        throw new RuntimeException("Unexpected return type for deconstructor " + returnType);

                    if (!parameters.isEmpty())
                        throw new RuntimeException("Unexpected executable element parameters for the deconstructor " + parameters);

                    if (bindings.isEmpty())
                        throw new RuntimeException("Unexpected executable element bindings for the deconstructor " + bindings);

                    if (!(type instanceof ExecutableType))
                        throw new RuntimeException("Unexpected executable element type for the deconstructor " + type);
                }
            }
            if (count != 2)
                throw new RuntimeException("No valid number of deconstructors!");
        }
        return true;
    }
}
