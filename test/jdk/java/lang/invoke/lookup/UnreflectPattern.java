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

/* @test
 * @summary Verify unreflect works for deconstructors
 * @enablePreview
 * @compile UnreflectPattern.java
 * @run main UnreflectPattern
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Deconstructor;
import java.lang.runtime.Carriers;
import java.util.Arrays;

public class UnreflectPattern {

    public static void main(String... args) throws Throwable {
        Object[] expected = new Object[] {"correct", -1};
        UnreflectPattern instance = new UnreflectPattern();
        Deconstructor<?> deconstructor = UnreflectPattern.class.getDeclaredDeconstructor(String.class, int.class);
        Object[] result1 = deconstructor.invoke(instance);
        if (!Arrays.equals(expected, result1)) {
            throw new AssertionError("Unexpected result: " + Arrays.toString(result1));
        }
        MethodHandle unreflected =
                MethodHandles.lookup().unreflectDeconstructor(deconstructor);
        if (!MethodType.methodType(Object.class, UnreflectPattern.class, MethodHandle.class).equals(unreflected.type()))
            throw new AssertionError("Unexpected type: " + unreflected.type());
        MethodType bindingMT = MethodType.methodType(Object.class, String.class, Integer.TYPE);
        MethodHandle deconstructorHandle =
            MethodHandles.filterReturnValue(
                unreflected,
                Carriers.boxedComponentValueArray(bindingMT)
            );
        MethodHandle initializingConstructor = Carriers.initializingConstructor(bindingMT);

        Object[] result2 = (Object[]) deconstructorHandle.invokeExact(instance, initializingConstructor);
        if (!Arrays.equals(expected, result2)) {
            throw new AssertionError("Unexpected result: " + Arrays.toString(result2));
        }
    }

    public pattern UnreflectPattern(String s, int i) {
        match UnreflectPattern("correct", -1);
    }
}
