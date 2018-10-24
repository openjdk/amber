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
package java.lang.compiler;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @test
 * @key randomness
 * @run testng CarrierTest
 * @summary unit tests for java.lang.compiler.ExtractorCarriers
 */
@Test
public class CarrierTest {
    static final int N_ITER = 1000;
    static final Class<?>[] TYPES = { byte.class, short.class, char.class, int.class, long.class, float.class, double.class, boolean.class, Object.class };

    static Object[] byteVals = { Byte.MIN_VALUE, Byte.MAX_VALUE, (byte) -1, (byte) 0, (byte) 1, (byte) 42 };
    static Object[] shortVals = { Short.MIN_VALUE, Short.MAX_VALUE, (short) -1, (short) 0, (short) 1, (short) 42 };
    static Object[] charVals = { Character.MIN_VALUE, Character.MAX_VALUE, (char) 0, 'a', 'Z' };
    static Object[] intVals = { Integer.MIN_VALUE, Integer.MAX_VALUE, -1, 0, 1, 42 };
    static Object[] longVals = { Long.MIN_VALUE, Long.MAX_VALUE, -1L, 0L, 1L, 42L };
    static Object[] floatVals = { Float.MIN_VALUE, Float.MAX_VALUE, -1.0f, 0.0f, 1.0f, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NaN };
    static Object[] doubleVals = { Double.MIN_VALUE, Double.MAX_VALUE, -1.0d, 0.0d, 1.0d, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN };
    static Object[] booleanVals = { true, false };
    static Object[] objectVals = {null, "", "Foo", "foo", List.of(), Collections.EMPTY_SET };

    // @@@ Should use RandomFactory, but can't get that to link
    private static final Random random = new Random(System.nanoTime());

    static Map<Class<?>, Object[]> primVals = Map.of(byte.class, byteVals,
                                                     short.class, shortVals,
                                                     char.class, charVals,
                                                     int.class, intVals,
                                                     long.class, longVals,
                                                     float.class, floatVals,
                                                     double.class, doubleVals,
                                                     boolean.class, booleanVals);

    void testCarrier(MethodType type, Object[] values) throws Throwable {
        for (ExtractorCarriers.CarrierFactory cf : ExtractorCarriers.CarrierFactories.values()) {
            assertEquals(type.parameterCount(), values.length);
            Object carrier = cf.constructor(type).invokeWithArguments(values);
            for (int i = 0; i < values.length; i++)
                assertEquals(values[i], cf.component(type, i).invoke(carrier));
        }
    }

    void testCarrier(MethodType type) throws Throwable {
        // generate data, in a loop
        for (int i=0; i<N_ITER; i++) {
            Object[] values = new Object[type.parameterCount()];
            for (int j=0; j<type.parameterCount(); j++) {
                Class<?> c = type.parameterType(j);
                Object[] vals = c.isPrimitive() ? primVals.get(c) : objectVals;
                values[j] = vals[random.nextInt(vals.length)];
            }
            testCarrier(type, values);
        }
    }

    public void testCarrier() throws Throwable {
        Class[] lotsOfInts = new Class[252];
        Arrays.fill(lotsOfInts, int.class);

        // known types
        for (MethodType mt : List.of(
                MethodType.methodType(Object.class),
                MethodType.methodType(Object.class, int.class),
                MethodType.methodType(Object.class, int.class, int.class),
                MethodType.methodType(Object.class, Object.class),
                MethodType.methodType(Object.class, Object.class, Object.class),
                MethodType.methodType(Object.class, byte.class, short.class, char.class, int.class, long.class, float.class, double.class, boolean.class),
                MethodType.methodType(Object.class, lotsOfInts))) {
            testCarrier(mt);
        }

        // random types
        for (int i=0; i<N_ITER; i++) {
            int nTypes = random.nextInt(10);
            Class[] paramTypes = new Class[nTypes];
            Arrays.setAll(paramTypes, ix -> TYPES[random.nextInt(TYPES.length)]);
            testCarrier(MethodType.methodType(Object.class, paramTypes));
        }
    }
}
