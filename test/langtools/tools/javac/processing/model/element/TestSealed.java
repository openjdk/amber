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
 * @summary Test basic modeling of a sealed type
 * @library /tools/javac/lib
 * @modules jdk.compiler
 * @build   JavacTestingAbstractProcessor TestSealed
 * @compile -processor TestSealed -proc:only TestSealed.java
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
 * Basic tests of modeling a sealed types.
 */
public class TestSealed extends JavacTestingAbstractProcessor {

    public boolean process(Set<? extends TypeElement> annotations,
                          RoundEnvironment roundEnv) {
       if (!roundEnv.processingOver()) {
           ElementScanner scanner = new SealedScanner();
           for(Element rootElement : roundEnv.getRootElements()) {
               scanner.visit(rootElement);
           }
       }
       return true;
    }

    sealed interface SealedI permits Sub1, Sub2 {}

    class Sub1 implements SealedI {}
    class Sub2 implements SealedI {}

    class SealedScanner extends ElementScanner<Void, Void> {

       public SealedScanner() {
           super();
       }

       @Override
       public Void visitType(TypeElement element, Void p) {
           System.out.println("Name: " + element.getSimpleName() +
                              "\tKind: " + element.getKind());
           if (element.getKind() == ElementKind.INTERFACE) {
               if (!element.getModifiers().contains(Modifier.SEALED)) {
                   throw new RuntimeException("sealed modifier expected");
               }
               List<? extends TypeMirror> permittedSubtypes = element.getPermittedSubtypes();
               if (permittedSubtypes.size() != 2) {
                   throw new RuntimeException("unexpected number of permitted subtypes");
               }
           }
           return super.visitType(element, p);
       }
    }
}
