/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary check that anno processors wont see annotations before being trimmed from records
 * @modules jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.util
 * @library /tools/javac/lib
 * @build JavacTestingAbstractProcessor
 * @compile AnnotationProcessorOnSealedTypesTest.java
 * @compile -XDaccessInternalAPI -processor AnnotationProcessorOnSealedTypesTest -proc:only AnnotationProcessorOnSealedTypesTest.java
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;

import com.sun.tools.javac.util.Assert;

@SupportedAnnotationTypes("*")
public class AnnotationProcessorOnSealedTypesTest extends JavacTestingAbstractProcessor {

    @Retention(RetentionPolicy.RUNTIME)
    @interface Sealed {}

    @Retention(RetentionPolicy.RUNTIME)
    @interface Final {}

    @Sealed
    sealed class Super1 {}

    @Final
    final class Sub1 extends Super1 {}

    @Sealed
    sealed class Super2 {}

    @Sealed
    sealed class Sub2 extends Super3 {}

    @Final
    final class Sub3 extends Sub2 {}

    @Sealed
    sealed class Super3 {}

    public boolean process(Set<? extends TypeElement> tes, RoundEnvironment renv) {
        for (TypeElement te : tes) {
            for (Element e : renv.getElementsAnnotatedWith(te)) {
                Symbol s = (Symbol)e;
                if (s.isSealed()) {
                    Assert.check(te.toString().equals("AnnotationProcessorOnSealedTypesTest.Sealed"));
                    Assert.check(!s.isFinal());
                }
                if (s.isFinal()) {
                    Assert.check(te.toString().equals("AnnotationProcessorOnSealedTypesTest.Final"));
                    Assert.check(!s.isSealed());
                }
            }
        }
        return true;
    }
}
