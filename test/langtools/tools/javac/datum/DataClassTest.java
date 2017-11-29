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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.tools.JavaFileObject;

import com.sun.tools.javac.file.PathFileObject;
import combo.ComboTask;

/**
 * DataClassTest
 *
 * @test
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper

 * @run main DataClassTest
 */
public class DataClassTest extends combo.ComboInstance<DataClassTest> {

    enum FieldTypeKind implements combo.ComboParameter {
        BYTE("byte", byte.class),
        SHORT("short", short.class),
        CHAR("char", char.class),
        INT("int", int.class),
        LONG("long", long.class),
        FLOAT("float", float.class),
        DOUBLE("double", double.class),
        BOOLEAN("boolean", boolean.class),
        OBJECT("Object", Object.class),
        STRING("String", String.class);

        String retTypeStr;
        Class<?> clazz;

        FieldTypeKind(String retTypeStr, Class<?> clazz) {
            this.retTypeStr = retTypeStr;
            this.clazz = clazz;
        }

        public String expand(String optParameter) {
            return retTypeStr;
        }
    }

    static final Map<FieldTypeKind, List<Object>> dataValues
            = Map.ofEntries(
            Map.entry(FieldTypeKind.BYTE, List.of(Byte.MIN_VALUE, (byte) -4, (byte) -1, (byte) 0, (byte) 1, (byte) 4, Byte.MAX_VALUE)),
            Map.entry(FieldTypeKind.SHORT, List.of(Short.MIN_VALUE, (short) -4, (short) -1, (short) 0, (short) 1, (short) 4, Short.MAX_VALUE)),
            Map.entry(FieldTypeKind.CHAR, List.of(Character.MIN_VALUE, 'a', 'A', 'z', (char) 0, Character.MAX_VALUE)),
            Map.entry(FieldTypeKind.INT, List.of(Integer.MIN_VALUE, (int) -4, (int) -1, (int) 0, (int) 1, (int) 4, Integer.MAX_VALUE)),
            Map.entry(FieldTypeKind.LONG, List.of(Long.MIN_VALUE, (long) -4, (long) -1, (long) 0, (long) 1, (long) 4, Long.MAX_VALUE)),
            Map.entry(FieldTypeKind.FLOAT, List.of(Float.MIN_VALUE, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 0.0f, 1.0f, -1.0f, 2.0f, -2.0f, Float.MAX_VALUE)),
            Map.entry(FieldTypeKind.DOUBLE, List.of(Double.MIN_VALUE, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0d, 1.0d, -1.0d, 2.0d, -2.0d, Double.MAX_VALUE)),
            Map.entry(FieldTypeKind.BOOLEAN, List.of(true, false)),
            Map.entry(FieldTypeKind.OBJECT, Arrays.asList(null, 3, "foo", new String[] {"a"})),
            Map.entry(FieldTypeKind.STRING, Arrays.asList(null, "", "foo", "bar"))
    );

    static final String sourceTemplate =
            "record Data(#{FT[0]} f0, #{FT[1]} f1) { }";

    public static void main(String... args) throws Exception {
        new combo.ComboTestHelper<DataClassTest>()
                .withArrayDimension("FT", (x, t, index) -> {
                    x.fieldType[index] = t;
                }, 2, FieldTypeKind.values())
                .run(DataClassTest::new);
    }

    FieldTypeKind[] fieldType = new FieldTypeKind[2];

    @Override
    public void doWork() throws Throwable {
        newCompilationTask()
                .withSourceFromTemplate(sourceTemplate)
                .generate(this::check);
    }

    void check(ComboTask.Result<Iterable<? extends JavaFileObject>> result) {
        List<Object> f0s = dataValues.get(fieldType[0]);
        List<Object> f1s = dataValues.get(fieldType[1]);

        if (result.hasErrors() || result.hasWarnings())
            fail("Compilation errors not expected: " + result.compilationInfo());

        Iterable<? extends PathFileObject> pfoIt = (Iterable<? extends PathFileObject>) result.get();
        PathFileObject pfo = pfoIt.iterator().next();
        Class<?> clazz;
        Constructor<?> ctor;
        Method getterF0, getterF1, hashCodeMethod, equalsMethod, toStringMethod;
        Field fieldF0, fieldF1;

        try {
            URL[] urls = new URL[] {pfo.getPath().getParent().toUri().toURL()};
            ClassLoader cl = new URLClassLoader(urls);
            clazz = cl.loadClass("Data");

            ctor = clazz.getConstructor(fieldType[0].clazz, fieldType[1].clazz);
            getterF0 = clazz.getMethod("f0");
            getterF1 = clazz.getMethod("f1");
            fieldF0 = clazz.getDeclaredField("f0");
            fieldF1 = clazz.getDeclaredField("f1");
            equalsMethod = clazz.getMethod("equals", Object.class);
            hashCodeMethod = clazz.getMethod("hashCode");
            toStringMethod = clazz.getMethod("toString");

            if (getterF0.getReturnType() != fieldType[0].clazz
                || getterF1.getReturnType() != fieldType[1].clazz
                || fieldF0.getType() != fieldType[0].clazz
                || fieldF1.getType() != fieldType[1].clazz)
                fail("Unexpected field or getter type: " + result.compilationInfo());

            for (AccessibleObject o : List.of(ctor, getterF0, getterF1, equalsMethod, hashCodeMethod, toStringMethod)) {
                // @@@ Why do we need this?
                o.setAccessible(true);
            }

            for (Object f0 : f0s) {
                for (Object f1 : f1s) {
                    // Create object
                    Object datum = ctor.newInstance(f0, f1);

                    // Test getters
                    Object actualF0 = getterF0.invoke(datum);
                    Object actualF1 = getterF1.invoke(datum);
                    if (!Objects.equals(f0, actualF0) || !Objects.equals(f1, actualF1))
                        fail(String.format("Getters don't report back right values for %s %s/%s, %s %s/%s",
                                           fieldType[0].clazz, f0, actualF0,
                                           fieldType[1].clazz, f1, actualF1));

                    int hashCode = (int) hashCodeMethod.invoke(datum);
                    int expectedHash = Objects.hash(f0, f1);
                    // @@@ fail
                    if (hashCode != expectedHash) {
                        System.err.println(String.format("Hashcode not as expected: expected=%d, actual=%d",
                                           expectedHash, hashCode));
                    }

                    String toString = (String) toStringMethod.invoke(datum);
                    String expectedToString = String.format("Data[f0=%s, f1=%s]", f0, f1);
                    if (!toString.equals(expectedToString)) {
                        fail(String.format("ToString not as expected: expected=%s, actual=%s",
                                           expectedToString, toString));
                    }

                    // Test equals
                    for (Object f2 : f0s) {
                        for (Object f3 : f1s) {
                            Object other = ctor.newInstance(f2, f3);
                            boolean isEqual = (boolean) equalsMethod.invoke(datum, other);
                            boolean isEqualReverse = (boolean) equalsMethod.invoke(other, datum);
                            boolean shouldEqual = Objects.equals(f0, f2) && Objects.equals(f1, f3);
                            // @@@ fail
                            if (shouldEqual != isEqual)
                                System.err.println(String.format("Equals not as expected: %s %s/%s, %s %s/%s",
                                                   fieldType[0].clazz, f0, f2,
                                                   fieldType[1].clazz, f1, f3));
                            if (isEqualReverse != isEqual)
                                fail(String.format("Equals not symmetric: %s %s/%s, %s %s/%s",
                                                   fieldType[0].clazz, f0, f2,
                                                   fieldType[1].clazz, f1, f3));

                        }
                    }
                }
            }
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }
}
