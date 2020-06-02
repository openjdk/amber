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
 * @summary Basic test for fall through in switch
 * @compile SwitchFallThrough1.java
 * @run main SwitchFallThrough1
 */
public class SwitchFallThrough1 {

    public static void main(String[] args) {
        int check;
        Object i = 42;

        check = 0;

        switch (i) {
            case 41: check++; //fall-through
            case Integer j: check++; break;
        }

        if (check != 1) {
            throw new AssertionError("Incorrect check value: " + check);
        }

        check = 0;

        switch (i) {
            case 42: check++; //fall-through
            case Integer j: check++; break;
        }

        if (check != 2) {
            throw new AssertionError("Incorrect check value: " + check);
        }

        check = 0;

        switch (i) {
            case Integer j: check++; break;
            case String  j: check = 45; break; //legal?
        }

        if (check != 1) {
            throw new AssertionError("Incorrect check value: " + check);
        }

        check = 0;

        switch (i) {
            case 41: check++;
            case Integer j: check++;
            case "": check++; break;
        }

        if (check != 2) {
            throw new AssertionError("Incorrect check value: " + check);
        }
    }
}
