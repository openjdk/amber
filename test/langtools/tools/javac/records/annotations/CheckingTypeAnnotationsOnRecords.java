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
 * @summary checking type annotations on records
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @modules jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.util
 * @build JavacTestingAbstractProcessor CheckingTypeAnnotationsOnRecords
 * @compile -XDaccessInternalAPI -processor CheckingTypeAnnotationsOnRecords -proc:only CheckingTypeAnnotationsOnRecords.java
 */

import java.lang.annotation.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.tools.Diagnostic.Kind;

import com.sun.tools.javac.util.Assert;

@SupportedAnnotationTypes("*")
public class CheckingTypeAnnotationsOnRecords extends JavacTestingAbstractProcessor {

    static private final Map<String, String> recordNameExpectedAnnotationMap = new HashMap<>();
    static private final Map<String, Integer> recordNameExpectedAnnotationNumberMap = new HashMap<>();
    static {
        recordNameExpectedAnnotationMap.put("CheckingTypeAnnotationsOnRecords.R1", "@CheckingTypeAnnotationsOnRecords.TypeUse");
        recordNameExpectedAnnotationMap.put("CheckingTypeAnnotationsOnRecords.R2", "@CheckingTypeAnnotationsOnRecords.TypeParameter");

        recordNameExpectedAnnotationNumberMap.put("CheckingTypeAnnotationsOnRecords.R1", 3);
        recordNameExpectedAnnotationNumberMap.put("CheckingTypeAnnotationsOnRecords.R2", 1);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE_USE })
    @interface TypeUse {}

    record R1(@TypeUse int annotated) {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE_PARAMETER })
    @interface TypeParameter {}

    record R2<@TypeParameter T>(T t) {}

    @Override
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            for (String key : recordNameExpectedAnnotationMap.keySet()) {
                Element element = processingEnv.getElementUtils().getTypeElement(key);
                numberOfAnnotations = 0;
                verifyReferredTypesAcceptable(element, recordNameExpectedAnnotationMap.get(key));
                Assert.check(numberOfAnnotations == recordNameExpectedAnnotationNumberMap.get(key), "expected = " +
                        recordNameExpectedAnnotationNumberMap.get(key) + " found = " + numberOfAnnotations);
            }
        }
        return true;
    }

    int numberOfAnnotations = 0;

    private void verifyReferredTypesAcceptable(Element rootElement, String expectedAnnotationName) {
        new ElementScanner<Void, Void>() {
            @Override public Void visitType(TypeElement e, Void p) {
                scan(e.getTypeParameters(), p);
                scan(e.getEnclosedElements(), p);
                verifyAnnotations(e.getAnnotationMirrors(), expectedAnnotationName);
                return null;
            }
            @Override public Void visitTypeParameter(TypeParameterElement e, Void p) {
                verifyTypesAcceptable(e.getBounds(), expectedAnnotationName);
                scan(e.getEnclosedElements(), p);
                verifyAnnotations(e.getAnnotationMirrors(), expectedAnnotationName);
                return null;
            }
            @Override public Void visitVariable(VariableElement e, Void p) {
                verifyTypeAcceptable(e.asType(), expectedAnnotationName);
                scan(e.getEnclosedElements(), p);
                verifyAnnotations(e.getAnnotationMirrors(), expectedAnnotationName);
                return null;
            }
            @Override
            public Void visitExecutable(ExecutableElement e, Void p) {
                scan(e.getTypeParameters(), p);
                verifyTypeAcceptable(e.getReturnType(), expectedAnnotationName);
                scan(e.getParameters(), p);
                verifyTypesAcceptable(e.getThrownTypes(), expectedAnnotationName);
                scan(e.getEnclosedElements(), p);
                verifyAnnotations(e.getAnnotationMirrors(), expectedAnnotationName);
                return null;
            }
        }.scan(rootElement, null);
    }

    private void verifyAnnotations(Iterable<? extends AnnotationMirror> annotations, String expectedName) {
        for (AnnotationMirror mirror : annotations) {
            Assert.check(mirror.toString().equals(expectedName));
            numberOfAnnotations++;
        }
    }

    private void verifyTypesAcceptable(Iterable<? extends TypeMirror> types, String expectedAnnotationName) {
        if (types == null) return ;

        for (TypeMirror type : types) {
            verifyTypeAcceptable(type, expectedAnnotationName);
        }
    }


    private void verifyTypeAcceptable(TypeMirror type, String expectedAnnotationName) {
        if (type == null) return ;

        verifyAnnotations(type.getAnnotationMirrors(), expectedAnnotationName);

        switch (type.getKind()) {
            case BOOLEAN: case BYTE: case CHAR: case DOUBLE: case FLOAT:
            case INT: case LONG: case SHORT: case VOID: case NONE: case NULL:
                return ;
            case DECLARED:
                DeclaredType dt = (DeclaredType) type;
                TypeElement outermostTypeElement = outermostTypeElement(dt.asElement());
                String outermostType = outermostTypeElement.getQualifiedName().toString();

                for (TypeMirror bound : dt.getTypeArguments()) {
                    verifyTypeAcceptable(bound, expectedAnnotationName);
                }
                break;
            case ARRAY:
                verifyTypeAcceptable(((ArrayType) type).getComponentType(), expectedAnnotationName);
                break;
            case INTERSECTION:
                for (TypeMirror element : ((IntersectionType) type).getBounds()) {
                    verifyTypeAcceptable(element, expectedAnnotationName);
                }
                break;
            case TYPEVAR:
                verifyTypeAcceptable(((TypeVariable) type).getLowerBound(), expectedAnnotationName);
                verifyTypeAcceptable(((TypeVariable) type).getUpperBound(), expectedAnnotationName);
                break;
            case WILDCARD:
                verifyTypeAcceptable(((WildcardType) type).getExtendsBound(), expectedAnnotationName);
                verifyTypeAcceptable(((WildcardType) type).getSuperBound(), expectedAnnotationName);
                break;
            default:
                error("Type not acceptable for this API: " + type.toString());
                break;

        }
    }


    private TypeElement outermostTypeElement(Element el) {
        while (el.getEnclosingElement().getKind() != ElementKind.PACKAGE) {
            el = el.getEnclosingElement();
        }

        return (TypeElement) el;
    }

    private void error(String text) {
        processingEnv.getMessager().printMessage(Kind.ERROR, text);
    }
}
