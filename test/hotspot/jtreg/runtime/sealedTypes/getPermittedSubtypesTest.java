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
 * @compile noLoadSubtypes.jcod
 * @run main getPermittedSubtypesTest
 */

import java.lang.constant.ClassDesc;
import java.util.ArrayList;

// Test Class.getPermittedSubtpes() and Class.isSealed() APIs.
public class getPermittedSubtypesTest {

    sealed class Sealed1 permits Sub1 {}

    class Sub1 extends Sealed1 {}

    sealed interface SealedI1 permits notSealed, Sub1, Extender {}

    interface Extender extends SealedI1 { }

    class notSealed implements SealedI1 {}

    sealed class noPermits {}

    final class Final4 {}

    public static void testSealedInfo(Class<?> c, String[] expected) {
        Object[] permitted = c.getPermittedSubtypes();

        if (permitted.length != expected.length) {
            throw new RuntimeException(
                "Unexpected number of permitted subtypes for: " + c.toString());
        }

        if (permitted.length > 0) {
            if (!c.isSealed()) {
                throw new RuntimeException("Expected sealed type: " + c.toString());
            }

            // Create ArrayList of permitted subtypes class names.
            ArrayList<String> permittedNames = new ArrayList<String>();
            for (int i = 0; i < permitted.length; i++) {
                permittedNames.add(((ClassDesc)permitted[i]).displayName());
            }

            if (permittedNames.size() != expected.length) {
                throw new RuntimeException(
                    "Unexpected number of permitted names for: " + c.toString());
            }

            // Check that expected class names are in the permitted subtypes list.
            for (int i = 0; i < expected.length; i++) {
                if (!permittedNames.contains(expected[i])) {
                    throw new RuntimeException(
                         "Expected class not found in permitted subtypes list, super type: " +
                         c.getName() + ", expected class: " + expected[i]);
                }
            }
        } else {
            // Must not be sealed if no permitted subtypes.
            if (c.isSealed()) {
                throw new RuntimeException("Unexpected sealed type: " + c.toString());
            }
        }
    }

    public static void main(String... args) {
        testSealedInfo(SealedI1.class, new String[] {"getPermittedSubtypesTest$notSealed",
                                                     "getPermittedSubtypesTest$Sub1",
                                                     "getPermittedSubtypesTest$Extender"});
        testSealedInfo(Sealed1.class, new String[] {"getPermittedSubtypesTest$Sub1"});
        testSealedInfo(noPermits.class, new String[] { });
        testSealedInfo(Final4.class, new String[] { });
        testSealedInfo(notSealed.class, new String[] { });
        // Test returning names of non-existing classes.
        try {
            testSealedInfo(noLoadSubtypes.class, new String[]{"iDontExist",
                    "I/Dont/Exist/Either"});
            throw new AssertionError("should fail");
        } catch (IllegalArgumentException iae) {
            // ok
        }
    }
}
