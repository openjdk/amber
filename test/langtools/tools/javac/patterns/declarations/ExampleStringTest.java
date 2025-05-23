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

/**
 * @test
 * @enablePreview
 * @compile ExampleStringTest.java
 * @run main ExampleStringTest
 */

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ExampleStringTest {

    interface StringOps {
        interface Split {
            pattern Split(String left, String right) {
                String[] parts = getString().split(getDelim());
                match Split(parts[0], parts[1]);
            }

            String getDelim();
            String getString();
        }

        interface Int {
            pattern Int(int datum) {
                int res = 0;
                try {
                    res = Integer.parseInt(getString());
                    match Int(res);
                } catch (NumberFormatException n) {
                    match Int(0);
                }
            }

            String getString();
        }

        default int examineString(Split s) {
            return switch (s) {
                case Split(String left, String right) -> {
                    Int int1 = new Int() {
                        @Override
                        public String getString() {
                            return left;
                        }
                    };

                    Int int2 = new Int() {
                        @Override
                        public String getString() {
                            return right;
                        }
                    };

                    yield switch (int1) {
                        case Int(int res1) -> {
                            int res = switch (int2) {
                                case Int(int res2) -> {
                                    yield res1 + res2;
                                }
                                default -> 0;
                            };
                            yield res;
                        }
                        default -> 0;
                    };
                }
                default -> 0;
            };
        }
    }

    static class StringOpsImpl implements StringOps {
        static class Split1 implements Split {
            @Override
            public String getDelim() {
                return ":";
            }

            @Override
            public String getString() {
                return "12:20";
            }
        }

        static class Split2 implements Split {
            @Override
            public String getDelim() {
                return "-";
            }

            @Override
            public String getString() {
                return "24-40";
            }
        }
    }

    public static void main(String[] args) {
        assertEquals(32, new StringOpsImpl().examineString(new StringOpsImpl.Split1()));
        assertEquals(64, new StringOpsImpl().examineString(new StringOpsImpl.Split2()));
    }

    private static <T> void assertEquals(T expected, T actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected: " + expected + ", but got: " + actual);
        }
    }
}
