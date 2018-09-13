/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @summary check that members of abtract datum has protected access
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.util
 * @compile CheckDatumMembersAccess.java
 * @run main CheckDatumMembersAccess
 */

import com.sun.tools.classfile.ClassFile;

import java.io.File;

import com.sun.tools.classfile.AccessFlags;
import com.sun.tools.classfile.Field;
import com.sun.tools.classfile.Method;
import com.sun.tools.javac.util.Assert;

public class CheckDatumMembersAccess {

    record Datum(int AbstractFieldToSearchFor, int newField, int nonFinalField) {}

    public static void main(String args[]) throws Throwable {
        new CheckDatumMembersAccess().run();
    }

    void run() throws Throwable {
        checkNonAbstractDatum();
    }

    void checkNonAbstractDatum() throws Throwable {
        File testClasses = new File(System.getProperty("test.classes"));
        File file = new File(testClasses,
                CheckDatumMembersAccess.class.getName() + "$Datum.class");
        ClassFile classFile = ClassFile.read(file);
        for (Field f : classFile.fields) {
            if (f.getName(classFile.constant_pool).equals("AbstractFieldToSearchFor") ||
                f.getName(classFile.constant_pool).equals("newField")) {
                Assert.check((f.access_flags.flags & AccessFlags.ACC_FINAL) != 0, "fields of datum should be final");
            }
        }
    }
}
