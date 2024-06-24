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

/*
 * @test
 * @summary Pattern reading of bindings
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @enablePreview
 * @compile -parameters BindingProcessor.java
 * @compile/process/ref=PatternReadingTest.out -XDrawDiagnostics -proc:only -processor BindingProcessor PatternReadingTest.java
 */
import static java.lang.annotation.RetentionPolicy.CLASS;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Retention;

public class PatternReadingTest {

    @Retention(RUNTIME)
    @interface RuntimeAnnotation {
        int value() default 0;
    }

    @Retention(CLASS)
    @interface ClassAnnotation {
        int value() default 0;
    }

    public static class Person1 {
        private final String name;
        private final String username;
        private boolean capitalize;

        public Person1(String name, String username, boolean capitalize) {
            this.name = name;
            this.username = username;
            this.capitalize = capitalize;
        }

        @BindingProcessor.Bindings
        public pattern Person1(@ClassAnnotation(21) String name, @RuntimeAnnotation(42) String username) {
            if (capitalize) {
                match Person1(this.name.toUpperCase(), this.username.toUpperCase());
            } else {
                match Person1(this.name, this.username);
            }
        }
    }
}
