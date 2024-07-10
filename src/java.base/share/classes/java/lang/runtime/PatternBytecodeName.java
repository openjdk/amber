/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package java.lang.runtime;

import sun.invoke.util.BytecodeName;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Encapsulates the name mangling that happens between the source level
 * representation of a deconstructor and its method manged name at the
 * classfile level.
 *
 * @since 24
 */
public class PatternBytecodeName {

    private PatternBytecodeName() { }

    /**
     * Mangles the binary name of a deconstructor.
     *
     * @param enclosingClass the enclosing class that contains the deconstructor
     * @param bindingTypes the types of bindings
     *
     * @return the symbolic name
     */
    public static String mangle(Class<?> enclosingClass, Class<?>...bindingTypes) {
        String postFix = Arrays.stream(bindingTypes)
            .map(param ->{
                String mangled = BytecodeName.toBytecodeName(param.descriptorString());
                mangled = mangled.toString().replaceFirst("\\\\=", "");
                return mangled;
            }).collect(Collectors.joining(":"));

        return enclosingClass.getSimpleName() + ":" + postFix;
    }
}
