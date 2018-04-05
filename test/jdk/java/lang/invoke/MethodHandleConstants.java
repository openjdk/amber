/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8022066
 * @summary smoke test for method handle constants
 * @library /lib/testlibrary/bytecode /java/lang/invoke/common
 * @build jdk.experimental.bytecode.BasicClassBuilder test.java.lang.invoke.lib.InstructionHelper
 * @run main test.java.lang.invoke.MethodHandleConstants --check-output
 * @run main/othervm test.java.lang.invoke.MethodHandleConstants --security-manager
 */

package test.java.lang.invoke;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Iterator;

import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodType.methodType;
import static test.java.lang.invoke.lib.InstructionHelper.ldcMethodHandle;

public class MethodHandleConstants {
    private static final MethodHandle MH_String_replace_C2;
    private static final MethodHandle MH_MethodHandle_invokeExact_SC2;
    private static final MethodHandle MH_MethodHandle_invoke_SC2;
    private static final MethodHandle MH_Class_forName_S;
    private static final MethodHandle MH_Class_forName_SbCL;
    static {
        try {
            MethodHandles.Lookup l = lookup();

            MH_String_replace_C2 = ldcMethodHandle(l,
                    MethodHandleInfo.REF_invokeVirtual, String.class, "replace",
                    methodType(String.class, char.class, char.class));
            MH_MethodHandle_invokeExact_SC2 = ldcMethodHandle(l,
                    MethodHandleInfo.REF_invokeVirtual, MethodHandle.class, "invokeExact",
                    methodType(String.class, String.class, char.class, char.class));
            MH_MethodHandle_invoke_SC2 = ldcMethodHandle(l,
                    MethodHandleInfo.REF_invokeVirtual, MethodHandle.class, "invoke",
                    methodType(String.class, String.class, char.class, char.class));
            MH_Class_forName_S = ldcMethodHandle(l,
                    MethodHandleInfo.REF_invokeStatic, Class.class, "forName",
                    methodType(Class.class, String.class));
            MH_Class_forName_SbCL = ldcMethodHandle(l,
                    MethodHandleInfo.REF_invokeStatic, Class.class, "forName",
                    methodType(Class.class, String.class, boolean.class, ClassLoader.class));
        }
        catch (Exception ex) {
            ex.printStackTrace();
            throw new InternalError(ex);
        }
    }

    public static void main(String... av) throws Throwable {
        if (av.length > 0 && av[0].equals("--check-output"))  openBuf();
        if (av.length > 0 && av[0].equals("--security-manager"))  setSM();
        System.out.println("Obtaining method handle constants:");
        testCase((MethodHandle) MH_String_replace_C2.invokeExact(), String.class, "replace", String.class, String.class, char.class, char.class);
        testCase((MethodHandle) MH_MethodHandle_invokeExact_SC2.invokeExact(), MethodHandle.class, "invokeExact", String.class, MethodHandle.class, String.class, char.class, char.class);
        testCase((MethodHandle) MH_MethodHandle_invoke_SC2.invokeExact(), MethodHandle.class, "invoke", String.class, MethodHandle.class, String.class, char.class, char.class);
        testCase((MethodHandle) MH_Class_forName_S.invokeExact(), Class.class, "forName", Class.class, String.class);
        testCase((MethodHandle) MH_Class_forName_SbCL.invokeExact(), Class.class, "forName", Class.class, String.class, boolean.class, ClassLoader.class);
        System.out.println("Done.");
        closeBuf();
    }

    private static void testCase(MethodHandle mh, Class<?> defc, String name, Class<?> rtype, Class<?>... ptypes) throws Throwable {
        System.out.println(mh);
        // we include defc, because we assume it is a non-static MH:
        MethodType mt = methodType(rtype, ptypes);
        assertEquals(mh.type(), mt);
        // FIXME: Use revealDirect to find out more
    }
    private static void assertEquals(Object exp, Object act) {
        if (exp == act || (exp != null && exp.equals(act)))  return;
        throw new AssertionError("not equal: "+exp+", "+act);
    }

    private static void setSM() {
        Policy.setPolicy(new TestPolicy());
        System.setSecurityManager(new SecurityManager());
    }

    private static PrintStream oldOut;
    private static ByteArrayOutputStream buf;
    private static void openBuf() {
        oldOut = System.out;
        buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf));
    }
    private static void closeBuf() {
        if (buf == null)  return;
        System.out.flush();
        System.setOut(oldOut);
        String[] haveLines = new String(buf.toByteArray()).split("[\n\r]+");
        for (String line : haveLines)  System.out.println(line);
        Iterator<String> iter = Arrays.asList(haveLines).iterator();
        for (String want : EXPECT_OUTPUT) {
            String have = iter.hasNext() ? iter.next() : "[EOF]";
            if (want.equals(have))  continue;
            System.err.println("want line: "+want);
            System.err.println("have line: "+have);
            throw new AssertionError("unexpected output: "+have);
        }
        if (iter.hasNext())
            throw new AssertionError("unexpected output: "+iter.next());
    }
    private static final String[] EXPECT_OUTPUT = {
        "Obtaining method handle constants:",
        "MethodHandle(String,char,char)String",
        "MethodHandle(MethodHandle,String,char,char)String",
        "MethodHandle(MethodHandle,String,char,char)String",
        "MethodHandle(String)Class",
        "MethodHandle(String,boolean,ClassLoader)Class",
        "Done."
    };

    static class TestPolicy extends Policy {
        final PermissionCollection permissions = new Permissions();
        TestPolicy() {
            permissions.add(new java.io.FilePermission("<<ALL FILES>>", "read"));
        }
        public PermissionCollection getPermissions(ProtectionDomain domain) {
            return permissions;
        }

        public PermissionCollection getPermissions(CodeSource codesource) {
            return permissions;
        }

        public boolean implies(ProtectionDomain domain, Permission perm) {
            return permissions.implies(perm);
        }
    }
}
