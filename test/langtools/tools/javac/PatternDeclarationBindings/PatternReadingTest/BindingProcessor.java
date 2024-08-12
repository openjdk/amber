/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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
import static java.util.stream.Collectors.joining;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes("BindingProcessor.Bindings")
public class BindingProcessor extends JavacTestingAbstractProcessor {

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    @interface Bindings {
        String[] value() default {};
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Bindings.class)) {
            if (element instanceof ExecutableElement exec) {
                String message = String.format("%s.%s(%s)",
                        exec.getEnclosingElement(),
                        exec.getSimpleName(),
                        exec.getBindings().stream().map(this::printBinding).collect(joining(", ")));
                messager.printMessage(Diagnostic.Kind.OTHER, message);
            }
        }
        return false;
    }

    private String printBinding(VariableElement binding) {
        return binding.getAnnotationMirrors().stream().map(String::valueOf).collect(joining(" "))
                + (binding.getAnnotationMirrors().isEmpty() ? "" : " ")
                + binding.getSimpleName();
    }
}
