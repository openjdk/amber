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
 * @compile --enable-preview --source 14 recordAccessorsTest.java
 * @run main/othervm --enable-preview recordAccessorsTest
 */


import java.lang.reflect.Method;

public class recordAccessorsTest {

    record recordNames(int intX, String stringY) {
        public Method[] getMethods() throws Throwable {
            return recordNames.class.getRecordAccessors();
        }
    }

    class notRecord {
        public Method[] getMethods() throws Throwable {
            return notRecord.class.getRecordAccessors();
        }
    }

    record recordGeneric<T>(T xx, String yy) {
        public Method[] getMethods() throws Throwable {
            return recordGeneric.class.getRecordAccessors();
        }
    }

    record recordEmpty() {
        public Method[] getMethods() throws Throwable {
            return recordEmpty.class.getRecordAccessors();
        }
    }

    notRecord nr = new notRecord();

    public static boolean hasMethod(Method[] methods, String method) {
        for (int x = 0; x < methods.length; x++) {
            if (methods[x].toString().contains(method)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String... args) throws Throwable {
        recordNames rn = new recordNames(3, "abc");
        Method[] methods = rn.getMethods();
        if (methods.length != 2 ||
            !hasMethod(methods, "int recordAccessorsTest$recordNames.intX()") ||
            !hasMethod(methods, "java.lang.String recordAccessorsTest$recordNames.stringY()")) {
            throw new RuntimeException("Bad component accessors returned for recordNames");
        }

        recordAccessorsTest rat = new recordAccessorsTest();
        methods = rat.nr.getMethods();
        if (methods.length != 0) {
            throw new RuntimeException("Non-empty component accessors returned for notRecord");
        }

        recordGeneric rg = new recordGeneric(35, "abcd");
        methods = rg.getMethods();
        if (methods.length != 2 ||
            !hasMethod(methods, "java.lang.Object recordAccessorsTest$recordGeneric.xx()") ||
            !hasMethod(methods, "java.lang.String recordAccessorsTest$recordGeneric.yy()")) {
            throw new RuntimeException("Bad component accessors returned for recordGeneric");
        }

        recordEmpty re = new recordEmpty();
        methods = re.getMethods();
        if (methods.length != 0) {
            throw new RuntimeException("Non-empty component accessors returned for recordEmpty");
        }
    }
}
