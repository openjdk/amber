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
 * @compile otherPkg/wrongPackage.java Pkg/Permitted.java Pkg/notPermitted.jcod Pkg/sealedInterface.java
 * @run main/othervm SealedUnnamedModuleIntfTest
 */

public class SealedUnnamedModuleIntfTest {

    public static void main(String args[]) throws Throwable {

        // Classes Permitted, notPermitted, and wrongPackage all try to implement
        // sealed interface sealedInterface.
        // Interface sealedInterface permits classes Permitted and wrongPackage.

        // Test permitted subtype and supertype in unnamed module and same package.
        // This should succeed.
        Class permitted = Class.forName("Pkg.Permitted");

        // Test unpermitted subtype and supertype in unnamed module and same package.
        // This should throw an exception.
        try {
            Class notPermitted = Class.forName("Pkg.notPermitted");
            throw new RuntimeException("Expected VerifyError exception not thrown");
        } catch (VerifyError e) {
            if (!e.getMessage().contains("cannot implement sealed interface")) {
                throw new RuntimeException("Wrong VerifyError exception thrown: " + e.getMessage());
            }
        }

        // Test both permitted subtype and supertype in unnamed module but in different
        // packages.  This should throw an exception.
        try {
            Class wrongPkg = Class.forName("otherPkg.wrongPackage");
        } catch (VerifyError e) {
            if (!e.getMessage().contains("cannot implement sealed interface")) {
                throw new RuntimeException("Wrong VerifyError exception thrown: " + e.getMessage());
            }
        }
    }
}
