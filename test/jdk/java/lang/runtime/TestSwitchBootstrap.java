/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.SwitchBootstraps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import jdk.test.lib.RandomFactory;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * @test
 * @key randomness
 * @library /test/lib
 * @build jdk.test.lib.RandomFactory
 * @run testng TestSwitchBootstrap
 */
@Test
public class TestSwitchBootstrap {
    private final static Set<Class<?>> ALL_INT_TYPES = Set.of(int.class, short.class, byte.class, char.class,
                                                              Integer.class, Short.class, Byte.class, Character.class);
    private final static Set<Class<?>> SIGNED_NON_BYTE_TYPES = Set.of(int.class, Integer.class, short.class, Short.class);
    private final static Set<Class<?>> CHAR_TYPES = Set.of(char.class, Character.class);
    private final static Set<Class<?>> BYTE_TYPES = Set.of(byte.class, Byte.class);
    private final static Set<Class<?>> SIGNED_TYPES
            = Set.of(int.class, short.class, byte.class,
                     Integer.class, Short.class, Byte.class);

    public static final MethodHandle BSM_INT_SWITCH;
    public static final MethodHandle BSM_LONG_SWITCH;
    public static final MethodHandle BSM_FLOAT_SWITCH;
    public static final MethodHandle BSM_DOUBLE_SWITCH;
    public static final MethodHandle BSM_STRING_SWITCH;
    public static final MethodHandle BSM_ENUM_SWITCH;

    private final static Random random = RandomFactory.getRandom();

    static {
        try {
            BSM_INT_SWITCH = MethodHandles.lookup().findStatic(SwitchBootstraps.class, "intSwitch",
                                                               MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, int[].class));
            BSM_LONG_SWITCH = MethodHandles.lookup().findStatic(SwitchBootstraps.class, "longSwitch",
                                                                MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, long[].class));
            BSM_FLOAT_SWITCH = MethodHandles.lookup().findStatic(SwitchBootstraps.class, "floatSwitch",
                                                                 MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, float[].class));
            BSM_DOUBLE_SWITCH = MethodHandles.lookup().findStatic(SwitchBootstraps.class, "doubleSwitch",
                                                                  MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, double[].class));
            BSM_STRING_SWITCH = MethodHandles.lookup().findStatic(SwitchBootstraps.class, "stringSwitch",
                                                                  MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String[].class));
            BSM_ENUM_SWITCH = MethodHandles.lookup().findStatic(SwitchBootstraps.class, "enumSwitch",
                                                                MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, Class.class, String[].class));
        }
        catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private MethodType switchType(Class<?> target) {
        return MethodType.methodType(int.class, target);
    }

    private Object box(Class<?> clazz, int i) {
        if (clazz == Integer.class)
            return i;
        else if (clazz == Short.class)
            return (short) i;
        else if (clazz == Character.class)
            return (char) i;
        else if (clazz == Byte.class)
            return (byte) i;
        else
            throw new IllegalArgumentException(clazz.toString());
    }

    private void testInt(Set<Class<?>> targetTypes, int... labels) throws Throwable {
        Map<Class<?>, MethodHandle> mhs
                = Map.of(char.class, ((CallSite) BSM_INT_SWITCH.invoke(MethodHandles.lookup(), "", switchType(char.class), labels)).dynamicInvoker(),
                         byte.class, ((CallSite) BSM_INT_SWITCH.invoke(MethodHandles.lookup(), "", switchType(byte.class), labels)).dynamicInvoker(),
                         short.class, ((CallSite) BSM_INT_SWITCH.invoke(MethodHandles.lookup(), "", switchType(short.class), labels)).dynamicInvoker(),
                         int.class, ((CallSite) BSM_INT_SWITCH.invoke(MethodHandles.lookup(), "", switchType(int.class), labels)).dynamicInvoker(),
                         Character.class, ((CallSite) BSM_INT_SWITCH.invoke(MethodHandles.lookup(), "", switchType(Character.class), labels)).dynamicInvoker(),
                         Byte.class, ((CallSite) BSM_INT_SWITCH.invoke(MethodHandles.lookup(), "", switchType(Byte.class), labels)).dynamicInvoker(),
                         Short.class, ((CallSite) BSM_INT_SWITCH.invoke(MethodHandles.lookup(), "", switchType(Short.class), labels)).dynamicInvoker(),
                         Integer.class, ((CallSite) BSM_INT_SWITCH.invoke(MethodHandles.lookup(), "", switchType(Integer.class), labels)).dynamicInvoker());

        List<Integer> labelList = IntStream.of(labels)
                                           .boxed()
                                           .collect(Collectors.toList());

        for (int i=0; i<labels.length; i++) {
            // test with invokeExact
            if (targetTypes.contains(char.class))
                assertEquals(i, (int) mhs.get(char.class).invokeExact((char) labels[i]));
            if (targetTypes.contains(byte.class))
                assertEquals(i, (int) mhs.get(byte.class).invokeExact((byte) labels[i]));
            if (targetTypes.contains(short.class))
                assertEquals(i, (int) mhs.get(short.class).invokeExact((short) labels[i]));
            if (targetTypes.contains(int.class))
                assertEquals(i, (int) mhs.get(int.class).invokeExact(labels[i]));
            if (targetTypes.contains(Integer.class))
                assertEquals(i, (int) mhs.get(Integer.class).invokeExact((Integer) labels[i]));
            if (targetTypes.contains(Short.class))
                assertEquals(i, (int) mhs.get(Short.class).invokeExact((Short) (short) labels[i]));
            if (targetTypes.contains(Byte.class))
                assertEquals(i, (int) mhs.get(Byte.class).invokeExact((Byte) (byte) labels[i]));
            if (targetTypes.contains(Character.class))
                assertEquals(i, (int) mhs.get(Character.class).invokeExact((Character) (char) labels[i]));

            // and with invoke
            assertEquals(i, (int) mhs.get(int.class).invoke(labels[i]));
            assertEquals(i, (int) mhs.get(Integer.class).invoke(labels[i]));
        }

        for (int i=-1000; i<1000; i++) {
            if (!labelList.contains(i)) {
                assertEquals(labels.length, mhs.get(short.class).invoke((short) i));
                assertEquals(labels.length, mhs.get(Short.class).invoke((short) i));
                assertEquals(labels.length, mhs.get(int.class).invoke(i));
                assertEquals(labels.length, mhs.get(Integer.class).invoke(i));
                if (i >= 0) {
                    assertEquals(labels.length, mhs.get(char.class).invoke((char)i));
                    assertEquals(labels.length, mhs.get(Character.class).invoke((char)i));
                }
                if (i >= -128 && i <= 127) {
                    assertEquals(labels.length, mhs.get(byte.class).invoke((byte)i));
                    assertEquals(labels.length, mhs.get(Byte.class).invoke((byte)i));
                }
            }
        }

        assertEquals(-1, (int) mhs.get(Integer.class).invoke(null));
        assertEquals(-1, (int) mhs.get(Short.class).invoke(null));
        assertEquals(-1, (int) mhs.get(Byte.class).invoke(null));
        assertEquals(-1, (int) mhs.get(Character.class).invoke(null));
    }

    private void testFloat(float... labels) throws Throwable {
        Map<Class<?>, MethodHandle> mhs
                = Map.of(float.class, ((CallSite) BSM_FLOAT_SWITCH.invoke(MethodHandles.lookup(), "", switchType(float.class), labels)).dynamicInvoker(),
                         Float.class, ((CallSite) BSM_FLOAT_SWITCH.invoke(MethodHandles.lookup(), "", switchType(Float.class), labels)).dynamicInvoker());

        List<Float> labelList = new ArrayList<>();
        for (float label : labels)
            labelList.add(label);

        for (int i=0; i<labels.length; i++) {
            assertEquals(i, (int) mhs.get(float.class).invokeExact((float) labels[i]));
            assertEquals(i, (int) mhs.get(Float.class).invokeExact((Float) labels[i]));
        }

        float[] someFloats = { 1.0f, Float.MIN_VALUE, 3.14f };
        for (float f : someFloats) {
            if (!labelList.contains(f)) {
                assertEquals(labels.length, mhs.get(float.class).invoke((float) f));
                assertEquals(labels.length, mhs.get(Float.class).invoke((float) f));
            }
        }

        assertEquals(-1, (int) mhs.get(Float.class).invoke(null));
    }

    private void testDouble(double... labels) throws Throwable {
        Map<Class<?>, MethodHandle> mhs
                = Map.of(double.class, ((CallSite) BSM_DOUBLE_SWITCH.invoke(MethodHandles.lookup(), "", switchType(double.class), labels)).dynamicInvoker(),
                         Double.class, ((CallSite) BSM_DOUBLE_SWITCH.invoke(MethodHandles.lookup(), "", switchType(Double.class), labels)).dynamicInvoker());

        var labelList = new ArrayList<Double>();
        for (double label : labels)
            labelList.add(label);

        for (int i=0; i<labels.length; i++) {
            assertEquals(i, (int) mhs.get(double.class).invokeExact((double) labels[i]));
            assertEquals(i, (int) mhs.get(Double.class).invokeExact((Double) labels[i]));
        }

        double[] someDoubles = { 1.0, Double.MIN_VALUE, 3.14 };
        for (double f : someDoubles) {
            if (!labelList.contains(f)) {
                assertEquals(labels.length, mhs.get(double.class).invoke((double) f));
                assertEquals(labels.length, mhs.get(Double.class).invoke((double) f));
            }
        }

        assertEquals(-1, (int) mhs.get(Double.class).invoke(null));
    }

    private void testLong(long... labels) throws Throwable {
        Map<Class<?>, MethodHandle> mhs
                = Map.of(long.class, ((CallSite) BSM_LONG_SWITCH.invoke(MethodHandles.lookup(), "", switchType(long.class), labels)).dynamicInvoker(),
                         Long.class, ((CallSite) BSM_LONG_SWITCH.invoke(MethodHandles.lookup(), "", switchType(Long.class), labels)).dynamicInvoker());

        List<Long> labelList = new ArrayList<>();
        for (long label : labels)
            labelList.add(label);

        for (int i=0; i<labels.length; i++) {
            assertEquals(i, (int) mhs.get(long.class).invokeExact((long) labels[i]));
            assertEquals(i, (int) mhs.get(Long.class).invokeExact((Long) labels[i]));
        }

        long[] someLongs = { 1L, Long.MIN_VALUE, Long.MAX_VALUE };
        for (long l : someLongs) {
            if (!labelList.contains(l)) {
                assertEquals(labels.length, mhs.get(long.class).invoke((long) l));
                assertEquals(labels.length, mhs.get(Long.class).invoke((long) l));
            }
        }

        assertEquals(-1, (int) mhs.get(Long.class).invoke(null));
    }

    private void testString(String... targets) throws Throwable {
        MethodHandle indy = ((CallSite) BSM_STRING_SWITCH.invoke(MethodHandles.lookup(), "", switchType(String.class), targets)).dynamicInvoker();
        List<String> targetList = Stream.of(targets)
                                        .collect(Collectors.toList());

        for (int i=0; i<targets.length; i++) {
            String s = targets[i];
            int result = (int) indy.invoke(s);
            assertEquals((s == null) ? -1 : i, result);
        }

        for (String s : List.of("", "A", "AA", "AAA", "AAAA")) {
            if (!targetList.contains(s)) {
                assertEquals(targets.length, indy.invoke(s));
            }
        }
        assertEquals(-1, (int) indy.invoke(null));
    }

    private<E extends Enum<E>> void testEnum(Class<E> enumClass, String... targets) throws Throwable {
        MethodHandle indy = ((CallSite) BSM_ENUM_SWITCH.invoke(MethodHandles.lookup(), "", switchType(Enum.class), enumClass, targets)).dynamicInvoker();
        List<E> targetList = Stream.of(targets)
                                   .map(s -> Enum.valueOf(enumClass, s))
                                   .collect(Collectors.toList());

        for (int i=0; i<targets.length; i++) {
            String s = targets[i];
            E e = Enum.valueOf(enumClass, s);
            int result = (int) indy.invoke(e);
            assertEquals((s == null) ? -1 : i, result);
        }

        for (E e : enumClass.getEnumConstants()) {
            int index = (int) indy.invoke(e);
            if (targetList.contains(e))
                assertEquals(e.name(), targets[index]);
            else
                assertEquals(targets.length, index);
        }

        assertEquals(-1, (int) indy.invoke(null));
    }

    public void testInt() throws Throwable {
        testInt(ALL_INT_TYPES, 8, 6, 7, 5, 3, 0, 9);
        testInt(ALL_INT_TYPES, 1, 2, 4, 8, 16);
        testInt(ALL_INT_TYPES, 5, 4, 3, 2, 1, 0);
        testInt(SIGNED_TYPES, 5, 4, 3, 2, 1, 0, -1);
        testInt(SIGNED_TYPES, -1);
        testInt(ALL_INT_TYPES, new int[] { });

        for (int i=0; i<5; i++) {
            int len = 50 + random.nextInt(800);
            int[] arr = IntStream.generate(() -> random.nextInt(10000) - 5000)
                                 .distinct()
                                 .limit(len)
                                 .toArray();
            testInt(SIGNED_NON_BYTE_TYPES, arr);

            arr = IntStream.generate(() -> random.nextInt(10000))
                    .distinct()
                    .limit(len)
                    .toArray();
            testInt(CHAR_TYPES, arr);

            arr = IntStream.generate(() -> random.nextInt(127) - 64)
                           .distinct()
                           .limit(120)
                           .toArray();
            testInt(BYTE_TYPES, arr);
        }
    }

    public void testLong() throws Throwable {
        testLong(1L, Long.MIN_VALUE, Long.MAX_VALUE);
        testLong(8L, 2L, 5L, 4L, 3L, 9L, 1L);
        testLong(new long[] { });

        // @@@ Random tests
        // @@@ More tests for weird values
    }

    public void testFloat() throws Throwable {
        testFloat(0.0f, -0.0f, -1.0f, 1.0f, 3.14f, Float.MIN_VALUE, Float.MAX_VALUE, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY);
        testFloat(new float[] { });
        testFloat(0.0f, 1.0f, 3.14f, Float.NaN);

        // @@@ Random tests
        // @@@ More tests for weird values
    }

    public void testDouble() throws Throwable {
        testDouble(0.0, -0.0, -1.0, 1.0, 3.14, Double.MIN_VALUE, Double.MAX_VALUE,
                   Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
        testDouble(new double[] { });
        testDouble(0.0f, 1.0f, 3.14f, Double.NaN);

        // @@@ Random tests
        // @@@ More tests for weird values
    }

    public void testString() throws Throwable {
        testString("a", "b", "c");
        testString("c", "b", "a");
        testString("cow", "pig", "horse", "orangutan", "elephant", "dog", "frog", "ant");
        testString("a", "b", "c", "A", "B", "C");
        testString("C", "B", "A", "c", "b", "a");

        // Tests with hash collisions; Ba/CB, Ca/DB
        testString("Ba", "CB");
        testString("Ba", "CB", "Ca", "DB");

        // Test with null
        try {
            testString("a", null, "c");
            fail("expected failure");
        }
        catch (IllegalArgumentException t) {
            // success
        }
    }

    enum E1 { A, B }
    enum E2 { C, D, E, F, G, H }

    public void testEnum() throws Throwable {
        testEnum(E1.class);
        testEnum(E1.class, "A");
        testEnum(E1.class, "A", "B");
        testEnum(E1.class, "B", "A");
        testEnum(E2.class, "C");
        testEnum(E2.class, "C", "D", "E", "F", "H");
        testEnum(E2.class, "H", "C", "G", "D", "F", "E");

        // Bad enum class
        try {
            testEnum((Class) String.class, "A");
            fail("expected failure");
        }
        catch (IllegalArgumentException t) {
            // success
        }

        // Bad enum constants
        try {
            testEnum(E1.class, "B", "A", "FILE_NOT_FOUND");
            fail("expected failure");
        }
        catch (IllegalArgumentException t) {
            // success
        }

        // Null enum constant
        try {
            testEnum(E1.class, "A", null, "B");
            fail("expected failure");
        }
        catch (IllegalArgumentException t) {
            // success
        }
    }
}
