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

// key: compiler.err.no.compatible.matcher.found
// key: compiler.err.prob.found.req
// key: compiler.misc.inconvertible.types
// key: compiler.misc.feature.pattern.declarations
// key: compiler.warn.preview.feature.use.plural
// options: --enable-preview -source ${jdk.version} -Xlint:preview

public class NoCompatibleMatcherFound {
    private static int test(D o) {
        if (o instanceof D(String data, Integer out)) {
            return out;
        }
        return -1;
    }

    public static class D {
        public pattern D(Object v1, Float out) {
            out = 10.0f;
        }

        public pattern D(Float out, Integer v1) {
            out = 2;
        }
    }
}