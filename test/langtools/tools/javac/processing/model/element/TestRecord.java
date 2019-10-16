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
 * @bug  8888888
 * @summary Test basic modeling of a record type
 * @library /tools/javac/lib
 * @modules jdk.compiler
 * @build   JavacTestingAbstractProcessor TestRecord
 * @compile -processor TestRecord -proc:only TestRecord.java
 */

import java.io.*;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;
import java.time.*;
import java.util.*;

/**
 * Basic tests of modeling a record type.
 */
public class TestRecord extends JavacTestingAbstractProcessor {
    int recordCount = 0;

    public boolean process(Set<? extends TypeElement> annotations,
                          RoundEnvironment roundEnv) {
       if (!roundEnv.processingOver()) {
           ElementScanner scanner = new RecordScanner();
           for(Element rootElement : roundEnv.getRootElements()) {
               scanner.visit(rootElement);
           }

           if (recordCount != 1)
               throw new RuntimeException("Bad record count " +
                                          recordCount);
       }
       return true;
    }

    static record PersonalBest(Duration marathonTime) implements Serializable {
        private static final Duration MIN_QUAL_TIME = Duration.ofHours(3);
        public boolean bostonQualified() {
            return marathonTime.compareTo(MIN_QUAL_TIME) <= 0;
        }
    }

    /**
     * Verify that a record modeled as an element behaves as expected
     * under 6 and latest specific visitors.
     */
    private static void testRecord(Element element, Elements elements) {
        ElementVisitor visitor6 = new ElementKindVisitor6<Void, Void>() {};

        try {
            visitor6.visit(element);
            throw new RuntimeException("Expected UnknownElementException not thrown.");
        } catch (UnknownElementException uee) {
            ; // Expected.
        }

        ElementKindVisitor visitorLatest =
            new ElementKindVisitor<Object, Void>() {
            @Override
            public Object visitTypeAsRecord(TypeElement e,
                                            Void p) {
                System.out.println("printing record " + e);
                List<? extends Element> enclosedElements = e.getEnclosedElements();
                for (Element elem : enclosedElements) {
		    if (elem.getKind() == ElementKind.RECORD_COMPONENT)
			continue; // "marathonTime" as a record component is Origin.EXPLICIT
                    System.out.println("name " + elem.getSimpleName());
                    System.out.println("origin " + elements.getOrigin(elem));
		    String simpleName = elem.getSimpleName().toString();
                    switch (simpleName) {
                        case "marathonTime": case "toString":
                        case "<init>": case "hashCode":
                        case "equals": case "readResolve":
                            if (elements.getOrigin(elem) != Elements.Origin.MANDATED) {
                                throw new RuntimeException("MANDATED origin expected for " + simpleName);
                            }
                            break;
                        default:
                            break;
                    }
                }
                return e; // a non-null value
            }
        };

        if (visitorLatest.visit(element) == null) {
            throw new RuntimeException("Null result of record visitation.");
        }
    }

    class RecordScanner extends ElementScanner<Void, Void> {

       public RecordScanner() {
           super();
       }

       @Override
       public Void visitType(TypeElement element, Void p) {
           System.out.println("Name: " + element.getSimpleName() +
                              "\tKind: " + element.getKind());
           if (element.getKind() == ElementKind.RECORD) {
               testRecord(element, elements);
               recordCount++;
           }
           return super.visitType(element, p);
       }
    }
}
