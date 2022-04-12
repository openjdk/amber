/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Objects;

import static java.lang.invoke.MethodType.methodType;

import jdk.internal.javac.PreviewFeature;
import jdk.internal.misc.Unsafe;

/**
 * A <em>carrier</em> is an opaque object that can be used to store component values
 * while avoiding primitive boxing associated with collection objects. Component values
 * can be primitive or Object.
 * <p>
 * Clients can create new carrier instances by describing a carrier <em>shape</em>, that
 * is, a {@linkplain MethodType method type} whose parameter types describe the types of
 * the carrier component values, or by providing the parameter types directly.
 *
 * {@snippet :
 * // Create a carrier for a string and an integer
 * CarrierElements elements = CarrierFactory.of(String.class, int.class);
 * // Fetch the carrier constructor MethodHandle
 * MethodHandle constructor = elements.constructor();
 * // Fetch the list of carrier component MethodHandles
 * List<MethodHandle> components = elements.components();
 *
 * // Create an instance of the carrier with a string and an integer
 * Object carrier = constructor.invokeExact("abc", 10);
 * // Extract the first component, type string
 * String string = (String)components.get(0).invokeExact(carrier);
 * // Extract the second component, type int
 * int i = (int)components.get(1).invokeExact(carrier);
 * }
 *
 * Alternatively, the client can use static methods when the carrier use is scattered.
 * This is possible since {@link Carriers} ensures that the same underlying carrier
 * class is used when the same component types are provided.
 *
 * {@snippet :
 * // Describe carrier using a MethodType
 * MethodType mt = MethodType.methodType(Object.class, String.class, int.class);
 * // Fetch the carrier constructor MethodHandle
 * MethodHandle constructor = Carriers.constructor(mt);
 * // Fetch the list of carrier component MethodHandles
 * List<MethodHandle> components = Carriers.components(mt);
 * }
 *
 * @implNote The strategy for storing components is deliberately left unspecified
 * so that future improvements will not be hampered by issues of backward compatibility.
 *
 * @since 19
 */
@PreviewFeature(feature=PreviewFeature.Feature.TEMPLATED_STRINGS)
public final class Carriers {

    /**
     * Number of integer slots used by a long.
     */
    private static final int LONG_SLOTS = Long.SIZE / Integer.SIZE;

    /*
     * Initialize {@link MethodHandle} constants.
     */
    static {
        try {
            Lookup lookup = MethodHandles.lookup();
            FLOAT_TO_INT = lookup.findStatic(Float.class, "floatToRawIntBits",
                    methodType(int.class, float.class));
            INT_TO_FLOAT = lookup.findStatic(Float.class, "intBitsToFloat",
                    methodType(float.class, int.class));
            DOUBLE_TO_LONG = lookup.findStatic(Double.class, "doubleToRawLongBits",
                    methodType(long.class, double.class));
            LONG_TO_DOUBLE = lookup.findStatic(Double.class, "longBitsToDouble",
                    methodType(double.class, long.class));
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("carrier static init fail", ex);
        }
    }

    /*
     * float/double conversions.
     */
    private static final MethodHandle FLOAT_TO_INT;
    private static final MethodHandle INT_TO_FLOAT;
    private static final MethodHandle DOUBLE_TO_LONG;
    private static final MethodHandle LONG_TO_DOUBLE;

    /**
     * Given a constructor {@link MethodHandle} recast and reorder arguments to
     * match shape.
     *
     * @param carrierShape  carrier shape
     * @param constructor   carrier constructor to reshape
     *
     * @return constructor with arguments recasted and reordered
     */
    private static MethodHandle reshapeConstructor(CarrierShape carrierShape,
                                                   MethodHandle constructor) {
        int count = carrierShape.count();
        Class<?>[] ptypes = carrierShape.ptypes();
        int objectIndex = carrierShape.objectOffset();
        int intIndex = carrierShape.intOffset();
        int longIndex = carrierShape.longOffset();
        int[] reorder = new int[count];
        Class<?>[] permutePTypes = new Class<?>[count];
        MethodHandle[] filters = new MethodHandle[count];
        boolean hasFilters = false;
        int index = 0;

        for (Class<?> ptype : ptypes) {
            MethodHandle filter = null;
            int from;

            if (!ptype.isPrimitive()) {
                from = objectIndex++;
                ptype = Object.class;
            } else if (ptype == double.class) {
                from = longIndex++;
                filter = DOUBLE_TO_LONG;
            } else if (ptype == float.class) {
                from = intIndex++;
                filter = FLOAT_TO_INT;
            } else if (ptype == long.class) {
                from = longIndex++;
            } else {
                from = intIndex++;
                ptype = int.class;
            }

            permutePTypes[index] = ptype;
            reorder[from] = index++;

            if (filter != null) {
                filters[from] = filter;
                hasFilters = true;
            }
        }

        if (hasFilters) {
            constructor = MethodHandles.filterArguments(constructor, 0, filters);
        }

        MethodType permutedMethodType =
                methodType(constructor.type().returnType(), permutePTypes);
        constructor = MethodHandles.permuteArguments(constructor,
                permutedMethodType, reorder);
        constructor = MethodHandles.explicitCastArguments(constructor,
                methodType(Object.class, ptypes));

        return constructor;
    }

    /**
     * Given components array, recast and reorder components to match shape.
     *
     * @param carrierShape  carrier reshape
     * @param components    carrier components to reshape
     *
     * @return list of components reshaped
     */
    private static List<MethodHandle> reshapeComponents(CarrierShape carrierShape,
                                                        MethodHandle[] components) {
        int count = carrierShape.count();
        Class<?>[] ptypes = carrierShape.ptypes();
        MethodHandle[] reorder = new MethodHandle[count];
        int objectIndex = carrierShape.objectOffset();
        int intIndex = carrierShape.intOffset();
        int longIndex = carrierShape.longOffset();
        int index = 0;

        for (Class<?> ptype : ptypes) {
            MethodHandle component;

            if (!ptype.isPrimitive()) {
                component = components[objectIndex++];
            } else if (ptype == double.class) {
                component = MethodHandles.filterReturnValue(
                        components[longIndex++], LONG_TO_DOUBLE);
            } else if (ptype == float.class) {
                component = MethodHandles.filterReturnValue(
                        components[intIndex++], INT_TO_FLOAT);
            } else if (ptype == long.class) {
                component = components[longIndex++];
            } else {
                component = components[intIndex++];
            }

            MethodType methodType = methodType(ptype, Object.class);
            reorder[index++] =
                    MethodHandles.explicitCastArguments(component, methodType);
        }

        return List.of(reorder);
    }

    /**
     * Factory for carriers that are backed by int[] and Object[]. This strategy is
     * used when the number of components exceeds {@link Carriers#MAX_OBJECT_COMPONENTS}.
     */
    private static class CarrierArrayFactory {
        /**
         * Unsafe access.
         */
        private static final Unsafe UNSAFE;

        static {
            try {
                UNSAFE = Unsafe.getUnsafe();
                Lookup lookup = MethodHandles.lookup();
                CONSTRUCTOR = lookup.findConstructor(CarrierArray.class,
                        methodType(void.class, int.class, int.class));
                GET_LONG = lookup.findVirtual(CarrierArray.class, "getLong",
                        methodType(long.class, int.class));
                PUT_LONG = lookup.findVirtual(CarrierArray.class, "putLong",
                        methodType(CarrierArray.class, int.class, long.class));
                GET_INTEGER = lookup.findVirtual(CarrierArray.class, "getInteger",
                        methodType(int.class, int.class));
                PUT_INTEGER = lookup.findVirtual(CarrierArray.class, "putInteger",
                        methodType(CarrierArray.class, int.class, int.class));
                GET_OBJECT = lookup.findVirtual(CarrierArray.class, "getObject",
                        methodType(Object.class, int.class));
                PUT_OBJECT = lookup.findVirtual(CarrierArray.class, "putObject",
                        methodType(CarrierArray.class, int.class, Object.class));
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError("carrier static init fail", ex);
            }
        }

        /*
         * Constructor accessor MethodHandles.
         */
        private static final MethodHandle CONSTRUCTOR;
        private static final MethodHandle GET_LONG;
        private static final MethodHandle PUT_LONG;
        private static final MethodHandle GET_INTEGER;
        private static final MethodHandle PUT_INTEGER;
        private static final MethodHandle GET_OBJECT;
        private static final MethodHandle PUT_OBJECT;

        /**
         * Wrapper object for carrier arrays. Instances types are stored in the {@code objects}
         * array, while primitive types are recast to {@code int/long} and stored in the
         * {@code primitives} array. Primitive byte, short, char, boolean and int are stored as
         * integers. Longs and doubles are stored as longs.  Longs take up the first part of the
         * primitives array using normal indices. Integers follow using int[] indices offset beyond
         * the longs using unsafe getInt/putInt.
         */
        private static class CarrierArray {
            /**
             * Carrier for primitive values.
             */
            private final long[] primitives;

            /**
             * Carrier for objects;
             */
            private final Object[] objects;

            /**
             * Constructor.
             *
             * @param primitiveCount  slot count required for primitives
             * @param objectCount     slot count required for objects
             */
            CarrierArray(int primitiveCount, int objectCount) {
                this.primitives =
                        primitiveCount != 0 ? new long[(primitiveCount + 1) / LONG_SLOTS] : null;
                this.objects = objectCount != 0 ? new Object[objectCount] : null;
            }

            /**
             * Compute offset for unsafe access to long.
             *
             * @param i  index in primitive[]
             *
             * @return offset for unsafe access
             */
            private long offsetToLong(int i) {
                return Unsafe.ARRAY_LONG_BASE_OFFSET +
                        (long)i * Unsafe.ARRAY_LONG_INDEX_SCALE;
            }

            /**
             * Compute offset for unsafe access to int.
             *
             * @param i  index in primitive[]
             *
             * @return offset for unsafe access
             */
            private long offsetToInt(int i) {
                return Unsafe.ARRAY_LONG_BASE_OFFSET +
                        (long)i * Unsafe.ARRAY_INT_INDEX_SCALE;
            }

            /**
             * Compute offset for unsafe access to object.
             *
             * @param i  index in objects[]
             *
             * @return offset for unsafe access
             */
            private long offsetToObject(int i) {
                return Unsafe.ARRAY_OBJECT_BASE_OFFSET +
                        (long)i * Unsafe.ARRAY_OBJECT_INDEX_SCALE;
            }

            /**
             * {@return long value at index}
             *
             * @param i  array index
             */
            private long getLong(int i) {
                return UNSAFE.getInt(primitives, offsetToLong(i));
            }

            /**
             * Put a long value into the primitive[].
             *
             * @param i      array index
             * @param value  long value to store
             *
             * @return this object
             */
            private CarrierArray putLong(int i, long value) {
                UNSAFE.putLong(primitives, offsetToLong(i), value);

                return this;
            }

            /**
             * {@return int value at index}
             *
             * @param i  array index
             */
            private int getInteger(int i) {
                return UNSAFE.getInt(primitives, offsetToInt(i));
            }

            /**
             * Put a int value into the int[].
             *
             * @param i      array index
             * @param value  int value to store
             *
             * @return this object
             */
            private CarrierArray putInteger(int i, int value) {
                UNSAFE.putInt(primitives, offsetToInt(i), value);

                return this;
            }

            /**
             * {@return Object value at index}
             *
             * @param i  array index
             */
            private Object getObject(int i) {
                return UNSAFE.getReference(objects, offsetToObject(i));
            }

            /**
             * Put a object value into the objects[].
             *
             * @param i      array index
             * @param value  object value to store
             *
             * @return this object
             */
            private CarrierArray putObject(int i, Object value) {
                UNSAFE.putReference(objects, offsetToObject(i), value);

                return this;
            }
        }

        /**
         * Constructor
         *
         * @param carrierShape  carrier object shape
         *
         * @return {@link MethodHandle} to generic carrier constructor.
         */
        private static MethodHandle constructor(CarrierShape carrierShape) {
            int longCount = carrierShape.longCount();
            int intCount = carrierShape.intCount();
            int objectCount = carrierShape.objectCount();
            int primitiveCount = longCount * LONG_SLOTS + intCount;

            MethodHandle constructor = MethodHandles.insertArguments(CONSTRUCTOR,
                    0, primitiveCount, objectCount);

            // long array index
            int index = 0;
            for (int i = 0; i < longCount; i++) {
                MethodHandle put = MethodHandles.insertArguments(PUT_LONG, 1, index++);
                constructor = MethodHandles.collectArguments(put, 0, constructor);
            }

            // transition to int array index (double number of longs)
            index *= LONG_SLOTS;
            for (int i = 0; i < intCount; i++) {
                MethodHandle put = MethodHandles.insertArguments(PUT_INTEGER, 1, index++);
                constructor = MethodHandles.collectArguments(put, 0, constructor);
            }

            for (int i = 0; i < objectCount; i++) {
                MethodHandle put = MethodHandles.insertArguments(PUT_OBJECT, 1, i);
                constructor = MethodHandles.collectArguments(put, 0, constructor);
            }

            return constructor;
        }

        /**
         * Utility to construct the basic accessors from the components.
         *
         * @param carrierShape  carrier object shape
         *
         * @return array of carrier accessors
         */
        private static MethodHandle[] createComponents(CarrierShape carrierShape) {
            int longCount = carrierShape.longCount();
            int intCount = carrierShape.intCount();
            int objectCount = carrierShape.objectCount();
            MethodHandle[] components =
                    new MethodHandle[carrierShape.ptypes().length];

            // long array index
            int index = 0;
            // component index
            int comIndex = 0;
            for (int i = 0; i < longCount; i++) {
                components[comIndex++] = MethodHandles.insertArguments(GET_LONG, 1, index++);
            }

            // transition to int array index (double number of longs)
            index *= LONG_SLOTS;
            for (int i = 0; i < intCount; i++) {
                components[comIndex++] = MethodHandles.insertArguments(GET_INTEGER, 1, index++);
            }

            for (int i = 0; i < objectCount; i++) {
                components[comIndex++] = MethodHandles.insertArguments(GET_OBJECT, 1, i);
            }
            return components;
        }

        /**
         * Permute a raw constructor and component accessor {@link MethodHandle MethodHandles} to
         * match the order and types of the parameter types.
         *
         * @param carrierShape  carrier object shape
         *
         * @return {@link CarrierElements} instance
         */
        private static CarrierElements carrier(CarrierShape carrierShape) {
            MethodHandle constructor = constructor(carrierShape);
            MethodHandle[] components = createComponents(carrierShape);

            return new CarrierElements(CarrierArray.class,
                                       reshapeConstructor(carrierShape, constructor),
                                       reshapeComponents(carrierShape, components));
        }
    }

    /**
     * Constructor
     */
    private Carriers() {
        throw new AssertionError("private constructor");
    }

    private record CarrierCounts(int longCount, int intCount, int objectCount) {
        /**
         * Count the number of fields required in each of Object, int and long.
         *
         * @param ptypes  parameter types
         *
         * @return a {@link CarrierCounts} instance containing counts
         */
        static CarrierCounts tally(Class<?>[] ptypes) {
            return tally(ptypes, ptypes.length);
        }

        /**
         * Count the number of fields required in each of Object, int and long
         * limited to the first {@code n} parameters.
         *
         * @param ptypes  parameter types
         * @param n       number of parameters to check
         *
         * @return a {@link CarrierCounts} instance containing counts
         */
        private static CarrierCounts tally(Class<?>[] ptypes, int n) {
            int longCount = 0;
            int intCount = 0;
            int objectCount = 0;

            for (int i = 0; i < n; i++) {
                Class<?> ptype = ptypes[i];

                if (!ptype.isPrimitive()) {
                    objectCount++;
                } else if (ptype == long.class || ptype == double.class) {
                    longCount++;
                } else {
                    intCount++;
                }
            }

            return new CarrierCounts(longCount, intCount, objectCount);
        }

        /**
         * {@return total number of components}
         */
        private int count() {
            return longCount + intCount + objectCount;
        }

        /**
         * {@return total number of slots}
         */
        private int slotCount() {
            return longCount * LONG_SLOTS + intCount + objectCount;
        }

    }

    /**
     * Shape of carrier based on counts of each of the three fundamental data
     * types.
     */
    private static class CarrierShape {
        /**
         * {@link MethodType} providing types for the carrier's components.
         */
        private final MethodType methodType;

        /**
         * Counts of different parameter types.
         */
        private final CarrierCounts counts;

        /**
         * Constructor.
         *
         * @param methodType  {@link MethodType} providing types for the
         *                    carrier's components
         */
        public CarrierShape(MethodType methodType) {
            this.methodType = methodType;
            this.counts = CarrierCounts.tally(methodType.parameterArray());
        }

        /**
         * {@return number of long fields needed}
         */
        private int longCount() {
            return counts.longCount();
        }

        /**
         * {@return number of int fields needed}
         */
        private int intCount() {
            return counts.intCount();
        }

        /**
         * {@return number of object fields needed}
         */
        private int objectCount() {
            return counts.objectCount();
        }

        /**
         * {@return array of parameter types}
         */
        private Class<?>[] ptypes() {
            return methodType.parameterArray();
        }

        /**
         * {@return number of components}
         */
        private int count() {
            return counts.count();
        }

        /**
         * {@return number of slots used}
         */
        private int slotCount() {
            return counts.slotCount();
        }

        /**
         * {@return index of first long component}
         */
        private int longOffset() {
            return 0;
        }

        /**
         * {@return index of first int component}
         */
        private int intOffset() {
            return longCount();
        }

        /**
         * {@return index of first object component}
         */
        private int objectOffset() {
            return longCount() + intCount();
        }
    }

    /**
     * This factory class generates {@link CarrierElements} instances containing the
     * {@link MethodHandle MethodHandles} to the constructor and accessors of a carrier
     * object.
     * <p>
     * Clients can create instances by describing a carrier <em>shape</em>, that
     * is, a {@linkplain MethodType method type} whose parameter types describe the types of
     * the carrier component values, or by providing the parameter types directly.
     */
    public static class CarrierFactory {
        /**
         * Constructor
         */
        private CarrierFactory() {
            throw new AssertionError("private constructor");
        }

        /**
         * Cache mapping {@link MethodType} to previously defined {@link CarrierElements}.
         */
        private static ConcurrentWeakMap<MethodType, CarrierElements>
                methodTypeCache = new ConcurrentWeakMap<>();

        /**
         * Factory method to return a {@link CarrierElements} instance that matches the shape of
         * the supplied {@link MethodType}. The return type of the {@link MethodType} is ignored.
         *
         * @param methodType  {@link MethodType} whose parameter types supply the
         *                    the shape of the carrier's components
         *
         * @return {@link CarrierElements} instance
         *
         * @throws NullPointerException is methodType is null
         * @throws IllegalArgumentException if number of component slots exceeds maximum
         */
        public static CarrierElements of(MethodType methodType) {
            Objects.requireNonNull(methodType, "methodType mustnot be null");
            MethodType constructorMT = methodType.changeReturnType(Object.class);
            CarrierShape carrierShape = new CarrierShape(constructorMT);
            int slotCount = carrierShape.slotCount();

            if (MAX_COMPONENTS < slotCount) {
                throw new IllegalArgumentException("Exceeds maximum number of component slots");
            }

            return methodTypeCache.computeIfAbsent(constructorMT,
                    (mt) -> CarrierArrayFactory.carrier(carrierShape));
        }

        /**
         * Factory method to return  a {@link CarrierElements} instance that matches the shape of
         * the supplied parameter types.
         *
         * @param ptypes   parameter types that supply the shape of the carrier's components
         *
         * @return {@link CarrierElements} instance
         *
         * @throws NullPointerException is ptypes is null
         * @throws IllegalArgumentException if number of component slots exceeds maximum
         */
        public static CarrierElements of(Class < ? >...ptypes) {
            Objects.requireNonNull(ptypes, "ptypes must not be null");
            return of(methodType(Object.class, ptypes));
        }
    }

    /**
     * Instances of this class provide the {@link MethodHandle MethodHandles} to the
     * constructor and accessors of a carrier object. The original component types can be
     * gleaned from the parameter types of the constructor {@link MethodHandle} or by the
     * return types of the components' {@link MethodHandle MethodHandles}.
     */
    public static class CarrierElements {
        /**
         * Underlying carrier class.
         */
        private final Class<?> carrierClass;

        /**
         * Constructor {@link MethodHandle}.
         */
        private final MethodHandle constructor;

        /**
         * List of component {@link MethodHandle MethodHandles}
         */
        private final List<MethodHandle> components;

        /**
         * Constructor
         */
        private CarrierElements() {
            throw new AssertionError("private constructor");
        }

        /**
         * Constructor
         */
        private CarrierElements(Class<?> carrierClass,
                                MethodHandle constructor,
                                List<MethodHandle> components) {
            this.carrierClass = carrierClass;
            this.constructor = constructor;
            this.components = components;
        }

        /**
         * {@return the underlying carrier class}
         */
        public Class<?> carrierClass() {
            return carrierClass;
        }

        /**
         * {@return the constructor {@link MethodHandle} for the carrier. The
         * carrier constructor will always have a return type of {@link Object} }
         */
        public MethodHandle constructor() {
            return constructor;
        }

        /**
         * {@return immutable list of component accessor {@link MethodHandle MethodHandles}
         * for all the carrier's components. The receiver type of the accessors
         * will always be {@link Object} }
         */
        public List<MethodHandle> components() {
            return components;
        }

        /**
         * {@return a component accessor {@link MethodHandle} for component {@code i}.
         * The receiver type of the accessor will be {@link Object} }
         *
         * @param i  component index
         *
         * @throws IllegalArgumentException if {@code i} is out of bounds
         */
        public MethodHandle component(int i) {
            if (i < 0 || components.size() <= i) {
                throw new IllegalArgumentException("i is out of bounds " + i +
                        " of " + components.size());
            }

            return components.get(i);
        }

        @Override
        public String toString() {
            return "Carrier" + constructor.type().parameterList();
        }
    }

    /**
     * {@return the underlying carrier class of the carrier representing {@code methodType} }
     *
     * @param methodType  {@link MethodType} whose parameter types supply the the shape of the
     *                    carrier's components
     *
     * @implNote Used internally by Condy APIs.
     */
    public static Class<?> carrierClass(MethodType methodType) {
        return CarrierFactory.of(methodType).carrierClass();
    }

    /**
     * {@return the constructor {@link MethodHandle} for the carrier representing {@code
     * methodType}. The carrier constructor will always have a return type of {@link Object} }
     *
     * @param methodType  {@link MethodType} whose parameter types supply the the shape of the
     *                    carrier's components
     *
     * @implNote Used internally by Condy APIs.
     */
    public static MethodHandle constructor(MethodType methodType) {
        return CarrierFactory.of(methodType).constructor();
    }

    /**
     * {@return immutable list of component accessor {@link MethodHandle MethodHandles} for
     * all the components of the carrier representing {@code methodType}. The receiver type of
     * the accessors will always be {@link Object} }
     *
     * @param methodType  {@link MethodType} whose parameter types supply the the shape of the
     *                    carrier's components
     *
     * @implNote Used internally by Condy APIs.
     */
    public static List<MethodHandle> components(MethodType methodType) {
        return CarrierFactory.of(methodType).components();
    }

    /**
     * {@return a component accessor {@link MethodHandle} for component {@code i} of the
     * carrier representing {@code methodType}. The receiver type of the accessor will always
     * be {@link Object} }
     *
     * @param methodType  {@link MethodType} whose parameter types supply the the shape of the
     *                    carrier's components
     * @param i           component index
     *
     * @implNote Used internally by Condy APIs.
     *
     * @throws IllegalArgumentException if {@code i} is out of bounds
     */
    public static MethodHandle component(MethodType methodType, int i) {
        return CarrierFactory.of(methodType).component(i);
    }
}
