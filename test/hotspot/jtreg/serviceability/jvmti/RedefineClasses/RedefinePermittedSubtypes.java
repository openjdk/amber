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
 * @summary Test that PermittedSubtypes attribute cannot be redefined.
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @modules java.compiler
 *          java.instrument
 *          jdk.jartool/sun.tools.jar
 * @run main RedefineClassHelper
 * @run main/othervm -javaagent:redefineagent.jar RedefinePermittedSubtypes
 */

// package access top-level class to avoid problem with RedefineClassHelper
// and nested types.
sealed class RedefinePermittedSubtypes_B permits java.lang.String, java.lang.Class { }

public class RedefinePermittedSubtypes {

    public static String newBGood =
        "sealed class RedefinePermittedSubtypes_B permits java.lang.Class, java.lang.String { }";

    public static String newBBad =
        "sealed class RedefinePermittedSubtypes_B permits java.lang.String { }";

    public static void main(String[] args) throws Exception {

        // Test redefinition is okay if permitted types are the same.
        RedefineClassHelper.redefineClass(RedefinePermittedSubtypes_B.class, newBGood);

        // Test redefinition fails if permitted types are different.
        try {
            RedefineClassHelper.redefineClass(RedefinePermittedSubtypes_B.class, newBBad);
           throw new RuntimeException("Expected UnsupportedOperationException not thrown");
        } catch (java.lang.UnsupportedOperationException e) {
            if (!e.getMessage().contains("class redefinition failed: attempted to change the class NestHost, NestMembers, or PermittedSubtypes attribute")) {
                throw new RuntimeException(
                    "Wrong UnsupportedOperationException thrown:" + e.getMessage());
            }
        }
    }
}
