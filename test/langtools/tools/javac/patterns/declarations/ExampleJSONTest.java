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
 * @compile ExampleJSONTest.java
 * @run main ExampleJSONTest
 */

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class ExampleJSONTest {

    public static void main(String[] args) {
        JSONObject j = new JSONObject(
            Map.of("configuration", new JSONString("7"), "services", new JSONArray(List.of(
                new JSONObject(Map.of("name", new JSONString("a"), "id", new JSONNumber(3))),
                new JSONObject(Map.of("name", new JSONString("b"), "id", new JSONNumber(4)))))));

        assertEquals("a", serviceToNameView().apply(j));
    }

    record View<A, B>(Function<A, B> f) implements Function<A, B> {
        @Override
        public B apply(A a) {
            return f.apply(a);
        }
        public static <A, B> View<A, B> of(Function<A, B> f) {
            return new View<>(f);
        }
    }

    private static View<JSONObject, String> serviceToNameView() {
        return View.of((JSONObject j) -> switch (Service.Of(j)) {
            case Service(String name, int id) -> name;
            default -> throw new IllegalStateException("Unexpected value: " + j);
        });
    }

    sealed interface JSONValue {
    }

    record JSONString(String value) implements JSONValue {
    }

    record JSONNumber(double value) implements JSONValue {
    }

    record JSONObject(Map<String, JSONValue> members) implements JSONValue {
    }

    record JSONArray(List<JSONValue> values) implements JSONValue {
    }

    public static class Service {
        String name;
        int id;
        JSONObject blob;

        private Service(JSONObject blob) {
            this.blob = blob;
        }

        public Service(String name, int id) {
            this.name = name;
            this.id = id;
        }

        public static Service Of(JSONObject blob) {
            return new Service(blob);
        }

        public pattern Service(String name, int id) {
            if (blob instanceof JSONObject(var keys) &&
                    keys.get("services") instanceof JSONArray(var array) && array.size() == 2
                    && array.get(0) instanceof JSONObject(var keys2)
                    && keys2.get("name") instanceof JSONString(var name2)
                    && keys2.get("id") instanceof JSONNumber(int id2)) {
                match Service(name2, id2);
            }
        }
    }

    private static <T> void assertEquals(T expected, T actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected: " + expected + ", but got: " + actual);
        }
    }
}
