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

    abstract __datum abtractDatum(int AbstractFieldToSearchFor) {}

    __datum Datum(int AbstractFieldToSearchFor, int newField, __nonfinal int nonFinalField) {}

    public static void main(String args[]) throws Throwable {
        new CheckDatumMembersAccess().run();
    }

    void run() throws Throwable {
        checkAbstractDatum();
        checkNonAbstractDatum();
    }

    void checkAbstractDatum() throws Throwable {
        File testClasses = new File(System.getProperty("test.classes"));
        File file = new File(testClasses,
                CheckDatumMembersAccess.class.getName() + "$abtractDatum.class");
        ClassFile classFile = ClassFile.read(file);
        for (Field f : classFile.fields) {
            if (f.getName(classFile.constant_pool).equals("AbstractFieldToSearchFor")) {
                Assert.check((f.access_flags.flags & AccessFlags.ACC_PROTECTED) != 0, "fields of abstract datum should be protected");
            }
        }

        for (Method m : classFile.methods) {
            Assert.check((m.access_flags.flags & AccessFlags.ACC_PUBLIC) != 0, "methods of abstract datum should be public");
        }
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
            if (f.getName(classFile.constant_pool).equals("nonFinalField")) {
                Assert.check((f.access_flags.flags & AccessFlags.ACC_FINAL) == 0, "non-final fields of datum should be mutable");
            }
        }
    }
}
