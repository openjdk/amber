/*
 * Copyright (c) 2017, 2025, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8172970 8362203
 * @summary Verify the functions that are allowed to operate in the start phase
 * with and without can_generate_early_vmstart capability.
 * @requires vm.jvmti
 * @run main/othervm/native -agentlib:AllowedFunctions AllowedFunctions
 * @run main/othervm/native -agentlib:AllowedFunctions=with_early_vmstart AllowedFunctions
 * @run main/othervm/native -agentlib:AllowedFunctions=with_early_vmstart -Xrunjdwp:transport=dt_socket,address=0,server=y,suspend=n AllowedFunctions
 */

public class AllowedFunctions {

    static {
        try {
            System.loadLibrary("AllowedFunctions");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load AllowedFunctions library");
            System.err.println("java.library.path: "
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static int check();

    public static void main(String args[]) {
        int status = check();
        if (status != 0) {
            throw new RuntimeException("Non-zero status returned from the agent: " + status);
        }
    }
}
