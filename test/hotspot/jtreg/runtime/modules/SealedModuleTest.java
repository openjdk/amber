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
 * @modules java.base/jdk.internal.misc
 * @library /test/lib ..
 * @compile sealedP1/superClass.java sealedP1/c1.java sealedP2/c2.java sealedP3/c3.java
 * @build sun.hotspot.WhiteBox
 * @compile/module=java.base java/lang/ModuleHelper.java
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox
 *                              sun.hotspot.WhiteBox$WhiteBoxPermission
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI SealedModuleTest
 */

public class SealedModuleTest {

    // Test sub-classing a sealed super class in a named module. In this test,
    // sealed class sealedP1/superClass permits sealedP1.c1, sealedP2.c2, and
    // sealedP3.c3.  All three of those classes extend sealedP1/superClass.

    public static void main(String args[]) throws Throwable {
        Object m1x, m2x;

        // Get the class loader for SealedModuleTest and assume it's also used
        // to load the other classes.
        ClassLoader this_cldr = AccessCheckRead.class.getClassLoader();

        // Define a module for packages sealedP1 and sealedP2.
        m1x = ModuleHelper.ModuleObject("module_one", this_cldr,
                                        new String[] { "sealedP1", "sealedP2" });
        ModuleHelper.DefineModule(m1x, false, "9.0", "m1x/here",
                                  new String[] { "sealedP1", "sealedP2" });

        // Define a module for package sealedP3 with the same class loader.
        m2x = ModuleHelper.ModuleObject("module_two", this_cldr, new String[] { "sealedP3" });
        ModuleHelper.DefineModule(m2x, false, "9.0", "m2x/there", new String[] { "sealedP3" });

        // Make package sealedP1 in m1x visible to everyone because it contains
        // the super class for c1, c2, and c3.
        ModuleHelper.AddModuleExportsToAll(m1x, "sealedP1");
        ModuleHelper.AddReadsModule(m2x, m1x);

        // Test subtype in the same named package and named module as its super
        // class.  This should succeed.
        // Class sealedP1.c1 extends class sealedP1.superClass.
        Class p1_c1_class = Class.forName("sealedP1.c1");

        // Test subtype in different named package but same named module as its
        // super class. This should succeed.
        // Class sealedP2.c2 extends class sealedP1.superClass.
        Class p2_c2_class = Class.forName("sealedP2.c2");

        // Test subtype in a different module than its super type.  This should
        // fail even though they have the same class loader.
        // Class sealedP3.c3 extends class sealedP1.superClass.
        try {
            Class p3_c3_class = Class.forName("sealedP3.c3");
            throw new RuntimeException("Expected VerifyError exception not thrown");
        } catch (VerifyError e) {
            if (!e.getMessage().contains("cannot inherit from final class")) {
                throw new RuntimeException("Wrong VerifyError exception thrown: " + e.getMessage());
            }
        }

    }
}
