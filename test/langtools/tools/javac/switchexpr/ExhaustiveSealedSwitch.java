/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Verify that an switch expression over sealed type can be exhaustive without default.
 * @compile --enable-preview -source ${jdk.version} ExhaustiveSealedSwitch.java
 * @compile --enable-preview -source ${jdk.version} ExhaustiveSealedSwitchExtra.java
 * @run main/othervm --enable-preview ExhaustiveSealedSwitch
 */

public class ExhaustiveSealedSwitch {
    public static void main(String... args) throws Exception {
        new ExhaustiveSealedSwitch().run();
    }

    private void run() throws Exception {
        ExhaustiveSealedSwitchIntf i = (ExhaustiveSealedSwitchIntf) Class.forName("ExhaustiveSealedSwitchC").newInstance();

        try {
            print(i);
            throw new AssertionError("Expected exception did not occur.");
        } catch (IncompatibleClassChangeError err) {
            //ok
        }
    }

    private String print(ExhaustiveSealedSwitchIntf t) {
        return switch (t) {
            case ExhaustiveSealedSwitchA a -> "A";
            case ExhaustiveSealedSwitchB b -> "B";
        };
    }

}
sealed interface ExhaustiveSealedSwitchIntf permits ExhaustiveSealedSwitchA, ExhaustiveSealedSwitchB {
}
class ExhaustiveSealedSwitchA implements ExhaustiveSealedSwitchIntf {}
class ExhaustiveSealedSwitchB implements ExhaustiveSealedSwitchIntf {}
