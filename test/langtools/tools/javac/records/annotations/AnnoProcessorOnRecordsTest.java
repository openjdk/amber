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
 * @compile AnnoProcessorOnRecordsTest.java
 * @compile -XDaccessInternalAPI -processor AnnoProcessorOnRecordsTest -proc:only AnnoProcessorOnRecordsTest.java
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
public class AnnoProcessorOnRecordsTest extends JavacTestingAbstractProcessor {

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.PARAMETER })
    @interface Parameter {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD })
    @interface Method {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD })
    @interface Field {}

    @Retention(RetentionPolicy.RUNTIME)
    @interface All {}

    record R1(@Parameter int i) {}

    record R2(@Method int i) {}

    record R3(@Field int i) {}

    record R4(@All int i) {}

    public boolean process(Set<? extends TypeElement> tes, RoundEnvironment renv) {
        for (TypeElement te : tes) {
            for (Element e : renv.getElementsAnnotatedWith(te)) {
                Symbol s = (Symbol)e;
                if (s.isRecord() || s.owner.isRecord()) {
                    // debug
                    // System.out.println(te.toString());
                    switch (te.toString()) {
                        case "AnnoProcessorOnRecordsTest.Parameter" :
                            // debug
                            // System.out.println(s.getKind());
                            Assert.check(s.getKind() == ElementKind.PARAMETER);
                            break;
                        case "AnnoProcessorOnRecordsTest.Method":
                            // debug
                            // System.out.println(s.getKind());
                            Assert.check(s.getKind() == ElementKind.METHOD);
                            break;
                        case "AnnoProcessorOnRecordsTest.Field":
                            // debug
                            // System.out.println(s.getKind());
                            Assert.check(s.getKind() == ElementKind.FIELD);
                            break;
                        case "AnnoProcessorOnRecordsTest.All":
                            // debug
                            // System.out.println(s.getKind());
                            Assert.check(s.getKind() == ElementKind.FIELD || s.getKind() == ElementKind.METHOD || s.getKind() == ElementKind.PARAMETER || s.getKind() == ElementKind.RECORD_COMPONENT);
                            break;
                        default:
                    }
                }
            }
        }
        return true;
    }
}
