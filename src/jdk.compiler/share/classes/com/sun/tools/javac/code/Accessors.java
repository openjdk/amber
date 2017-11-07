/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.code;

import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import java.util.function.Function;

public class Accessors {

    public enum Kind {
        GET(names -> names.get) {
            @Override
            public Type accessorType(Symtab syms, Type type) {
                return new MethodType(List.nil(), type, List.nil(), syms.methodClass);
            }
        },
        SET(names -> names.set) {
            @Override
            public Type accessorType(Symtab syms, Type type) {
                return new MethodType(List.of(type), syms.voidType, List.nil(), syms.methodClass);
            }
        };

        Function<Names, Name> nameFunc;

        Kind(Function<Names, Name> nameFunc) {
            this.nameFunc = nameFunc;
        }

        public Name name(Names names) {
            return nameFunc.apply(names);
        }

        public abstract Type accessorType(Symtab syms, Type type);
    }

    public static Kind fromName(Name name) {
        for (Kind k : Kind.values()) {
            if (k.name(name.table.names).equals(name)) {
                return k;
            }
        }
        return null;
    }
}
