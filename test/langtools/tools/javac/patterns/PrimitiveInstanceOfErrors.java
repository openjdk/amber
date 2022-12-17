/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Check behavior of instanceof for primitives
 * @compile/fail/ref=PrimitiveInstanceOfErrors.out --enable-preview -source ${jdk.version} -XDrawDiagnostics -XDshould-stop.at=FLOW PrimitiveInstanceOfErrors.java
 */
public class PrimitiveInstanceOfErrors {
    public static boolean unboxingAndNarrowingPrimitiveNotAllowedPerCastingConversion() {
        Long l_within_int_range = 42L;
        Long l_outside_int_range = 999999999999999999L;

        return l_within_int_range instanceof int && !(l_outside_int_range instanceof int);
    }

    public static void boxingConversionsBetweenIncompatibleTypes() {
        int i = 42;

        boolean ret1 = i instanceof Integer; // (Integer) i // OK and true
        boolean ret2 = i instanceof Double;  // error: incompatible types
        boolean ret3 = i instanceof Short;   // error: incompatible types
    }
}
