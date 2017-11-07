/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * @summary smoke test for datum classes
 * @run main Pos01
 */
public class Pos01 {

    static int assertCount;

    static void assertTrue(boolean cond) {
        assertCount++;
        if (!cond) {
            throw new AssertionError();
        }
    }

    static abstract __datum Sup(int x, int y) { }

    static __datum Foo(int x, int y, public int z) extends Sup(x, y);

    public static void main(String[] args) {
        Foo foo = new Foo(1, 2, 3);
        Foo foo2 = new Foo(1, 2, 3);
        Foo foo3 = new Foo(1, 2, 4);
        assertTrue(foo.toString().equals("Foo[x=1, y=2, z=3]"));
//        assertTrue(foo.hashCode() == java.util.Objects.hash(1, 2, 3));
        assertTrue(foo.equals(foo2));
        assertTrue(!foo.equals(foo3));
        assertTrue(foo.x() == 1);
        assertTrue(foo.y() == 2);
        assertTrue(foo.z() == 3);
    }
}
