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
package jdk.internal.misc;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * SwitchBootstraps
 *
 * @author Brian Goetz
 */
public class SwitchBootstrapsImpl {
    private final static Set<Class<?>> INTEGRAL_TYPES = Set.of(int.class, short.class, byte.class, char.class,
                                                               Integer.class, Short.class, Byte.class, Character.class);
    private static final Comparator<String> STRING_COMPARATOR = Comparator.comparingInt(Objects::hashCode);

    public static CallSite intSwitch(MethodHandles.Lookup lookup, String invocationName, MethodType invocationType,
                                     int... intLabels) throws Throwable {
        int[] labels = intLabels.clone();
        if (invocationType.parameterCount() != 1
            || invocationType.returnType() != int.class
            || (!INTEGRAL_TYPES.contains(invocationType.parameterType(0))))
            throw new IllegalArgumentException("Illegal invocation type " + invocationType);

        int[] indexes = IntStream.range(0, labels.length)
                                 .boxed()
                                 .sorted(Comparator.comparingInt(a -> labels[a]))
                                 .mapToInt(Integer::intValue)
                                 .toArray();
        Arrays.sort(labels);

        return new IntSwitchCallSite(invocationType, labels, indexes);
    }

    private static class IntSwitchCallSite extends ConstantCallSite {
        private static final MethodHandle HOOK;

        private final int[] labels;
        private final int[] indexes;

        static {
            try {
                HOOK = MethodHandles.lookup().findVirtual(IntSwitchCallSite.class, "initHook", MethodType.methodType(MethodHandle.class));
            }
            catch (NoSuchMethodException | IllegalAccessException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        IntSwitchCallSite(MethodType targetType,
                                 int[] labels,
                                 int[] indexes) throws Throwable {
            super(targetType, HOOK);
            this.labels = labels;
            this.indexes = indexes;
        }

        private MethodHandle initHook() throws ReflectiveOperationException {
            return MethodHandles.lookup()
                                .findVirtual(IntSwitchCallSite.class, "intSwitch",
                                             MethodType.methodType(int.class, type().parameterType(0)))
                                .bindTo(this);
        }

        private int intSwitch(short target) {
            return intSwitch((int) target);
        }

        private int intSwitch(byte target) {
            return intSwitch((int) target);
        }

        private int intSwitch(char target) {
            return intSwitch((int) target);
        }

        private int intSwitch(int target) {
            int index = Arrays.binarySearch(labels, target);
            return (index >= 0) ? indexes[index] : indexes.length;
        }

        private int intSwitch(Integer target) {
            return (target == null)
                   ? -1
                   : intSwitch((int) target);
        }

        private int intSwitch(Short target) {
            return (target == null)
                   ? -1
                   : intSwitch((int) target);
        }

        private int intSwitch(Character target) {
            return (target == null)
                   ? -1
                   : intSwitch((int) target);
        }

        private int intSwitch(Byte target) {
            return (target == null)
                   ? -1
                   : intSwitch((int) target);
        }
    }

    public static CallSite stringSwitch(MethodHandles.Lookup lookup, String invocationName, MethodType invocationType,
                                        String... stringLabels) throws Throwable {
        if (invocationType.parameterCount() != 1
            || invocationType.returnType() != int.class
            || (!invocationType.parameterType(0).equals(String.class)))
            throw new IllegalArgumentException("Illegal invocation type " + invocationType);

        String[] sortedByHash = stringLabels.clone();
        Arrays.sort(sortedByHash, STRING_COMPARATOR);

        int[] indexes = new int[stringLabels.length];
        for (int i = 0; i < stringLabels.length; i++) {
            for (int j = 0; j < stringLabels.length; j++) {
                if (Objects.equals(sortedByHash[j], stringLabels[i])) {
                    indexes[j] = i;
                    break;
                }
            }
        }

        boolean collisions = IntStream.range(0, sortedByHash.length-1)
                .anyMatch(i -> Objects.hashCode(sortedByHash[i]) == Objects.hashCode(sortedByHash[i + 1]));

        return new StringSwitchCallSite(invocationType, sortedByHash, indexes, collisions);
    }

    private static class StringSwitchCallSite extends ConstantCallSite {
        private static final MethodHandle HOOK;

        private final String[] sortedByHash;
        private final int[] indexes;
        private final boolean collisions;

        static {
            try {
                HOOK = MethodHandles.lookup().findVirtual(StringSwitchCallSite.class, "initHook", MethodType.methodType(MethodHandle.class));
            }
            catch (NoSuchMethodException | IllegalAccessException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        StringSwitchCallSite(MethodType targetType,
                             String[] sortedByHash,
                             int[] indexes,
                             boolean collisions) throws Throwable {
            super(targetType, HOOK);
            this.sortedByHash = sortedByHash;
            this.indexes = indexes;
            this.collisions = collisions;
        }

        private MethodHandle initHook() throws ReflectiveOperationException {
            return MethodHandles.lookup()
                                .findVirtual(StringSwitchCallSite.class, "stringSwitch",
                                             MethodType.methodType(int.class, String.class))
                                .bindTo(this);
        }

        private int stringSwitch(String target) {
            if (target == null)
                return -1;

            int index = Arrays.binarySearch(sortedByHash, target, SwitchBootstrapsImpl.STRING_COMPARATOR);
            if (index < 0)
                return indexes.length;
            else if (target.equals(sortedByHash[index])) {
                return indexes[index];
            }
            else if (collisions) {
                int hash = target.hashCode();
                while (index > 0 && Objects.hashCode(sortedByHash[index-1]) == hash)
                    --index;
                for (; index < sortedByHash.length && Objects.hashCode(sortedByHash[index]) == hash; index++)
                    if (target.equals(sortedByHash[index]))
                        return indexes[index];
            }

            return indexes.length;
        }
    }

    public static<E extends Enum<E>> CallSite enumSwitch(MethodHandles.Lookup lookup, String invocationName, MethodType invocationType,
                                                         Class<E> enumClass, String... enumNames) throws Throwable {
        if (invocationType.parameterCount() != 1
            || invocationType.returnType() != int.class
            || (!invocationType.parameterType(0).equals(Enum.class)))
            throw new IllegalArgumentException("Illegal invocation type " + invocationType);

        MethodHandle valuesMH = lookup.findStatic(enumClass, "values", MethodType.methodType(Array.newInstance(enumClass, 0).getClass()));
        int[] ordinalMap = new int[((Object[]) valuesMH.invoke()).length];
        Arrays.fill(ordinalMap, enumNames.length);

        for (int i=0; i<enumNames.length; i++) {
            // @@@ Do we want to recover gracefully if labels are not present?
            if (enumNames[i] != null)
                ordinalMap[E.valueOf(enumClass, enumNames[i]).ordinal()] = i;
        }

        return new EnumSwitchCallSite(invocationType, ordinalMap);
    }

    private static class EnumSwitchCallSite extends ConstantCallSite {
        private static final MethodHandle HOOK;

        private final int[] ordinalMap;

        static {
            try {
                HOOK = MethodHandles.lookup().findVirtual(EnumSwitchCallSite.class, "initHook", MethodType.methodType(MethodHandle.class));
            }
            catch (NoSuchMethodException | IllegalAccessException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        EnumSwitchCallSite(MethodType targetType,
                           int[] ordinalMap) throws Throwable {
            super(targetType, HOOK);
            this.ordinalMap = ordinalMap;
        }

        private MethodHandle initHook() throws ReflectiveOperationException {
            return MethodHandles.lookup()
                                .findVirtual(EnumSwitchCallSite.class, "enumSwitch",
                                             MethodType.methodType(int.class, Enum.class))
                                .bindTo(this);
        }

        @SuppressWarnings("rawtypes")
        private int enumSwitch(Enum target) {
            return (target == null)
                   ? -1
                   : ordinalMap[target.ordinal()];
        }
    }
}
