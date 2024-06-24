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
 * @compile InterfacePatternDeclarations.java
 * @run main InterfacePatternDeclarations
 */
import java.util.*;
import java.util.function.BiConsumer;

public class InterfacePatternDeclarations {

    interface Map<K, V> {
        interface Entry<K,V> {
            pattern Entry(K k, V v) {
                match Entry(this.getKey(), this.getValue());
            }

            K getKey();
            V getValue();
        }

        Set<Entry<K, V>> entrySet();

        default void forEach(BiConsumer<? super K, ? super V> action) {
            Objects.requireNonNull(action);
            for (Entry<K, V> entry : entrySet()) {
                if (entry instanceof Entry(K k, V v)){
                    action.accept(k, v);
                }
            }
        }
    }

    static class MyMap implements Map<String, String> {
        class MyEntry implements Map.Entry<String, String> {
            @Override
            public String getKey() {
                return "KEY";
            }

            @Override
            public String getValue() {
                return "VALUE";
            }
        }

        @Override
        public Set<Map.Entry<String, String>> entrySet() {
            return Set.of(new MyEntry());
        }
    }

    public static void main(String[] args) {
        MyMap map = new MyMap();
        StringBuilder res = new StringBuilder();
        map.forEach((k, v) -> res.append(k + " -> " + v));
        assertEquals("KEY -> VALUE", res.toString());
    }

    private static <T> void assertEquals(T expected, T actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected: " + expected + ", but got: " + actual);
        }
    }
}
