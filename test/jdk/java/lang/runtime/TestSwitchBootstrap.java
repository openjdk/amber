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

/**
 * @test
 * @run testng TestSwitchBootstrap
 */

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.SwitchBootstraps;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * TestSwitchBootstrap
 *
 * @author Brian Goetz
 */
@Test
public class TestSwitchBootstrap {
    private final static Set<Class<?>> ALL_TYPES = Set.of(int.class, short.class, byte.class, char.class,
                                                          Integer.class, Short.class, Byte.class, Character.class);
    private final static Set<Class<?>> NON_BYTE_TYPES = Set.of(int.class, Integer.class, short.class, Short.class);
    private final static Set<Class<?>> SIGNED_TYPES
            = Set.of(int.class, short.class, byte.class,
                     Integer.class, Short.class, Byte.class);

    public static final MethodHandle BSM_INT_SWITCH;
    public static final MethodHandle BSM_STRING_SWITCH;
    public static final MethodHandle BSM_ENUM_SWITCH;

    static {
        try {
            BSM_INT_SWITCH = MethodHandles.lookup().findStatic(SwitchBootstraps.class, "intSwitch",
                                                               MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, int[].class));
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
            }
        }

        assertEquals(-1, (int) mhs.get(Integer.class).invoke(null));
        assertEquals(-1, (int) mhs.get(Short.class).invoke(null));
        assertEquals(-1, (int) mhs.get(Byte.class).invoke(null));
        assertEquals(-1, (int) mhs.get(Character.class).invoke(null));
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
        testInt(ALL_TYPES, 3, 1, 2);
        testInt(ALL_TYPES, 1, 2, 3, 4);
        testInt(SIGNED_TYPES, -1);
        testInt(ALL_TYPES, new int[] { });

        Random r = new Random();
        int len = r.nextInt(1000);
        int[] arr = IntStream.generate(() -> r.nextInt(10000) - 5000)
                .distinct()
                .limit(len)
                .toArray();
        testInt(NON_BYTE_TYPES, arr);
    }

    public void testString() throws Throwable {
        testString("a", "b", "c");
        testString("c", "b", "a");
        testString("cow", "pig", "horse", "orangutan", "elephant", "dog", "frog", "ant");
        testString("a", "b", "c", "A", "B", "C");
        testString("C", "B", "A", "c", "b", "a");
        testString("a", null, "c");

        // Tests with hash collisions; Ba/CB, Ca/DB
        testString("Ba", "CB");
        testString("Ba", "CB", "Ca", "DB");
        testString("Ba", "pig", null, "CB", "cow", "Ca", "horse", "DB");
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
    }
}
