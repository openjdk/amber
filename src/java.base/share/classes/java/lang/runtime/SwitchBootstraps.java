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

package java.lang.runtime;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Bootstrap methods for linking {@code invokedynamic} call sites that implement
 * the selection functionality of the {@code switch} statement.  The bootstraps
 * take additional static arguments corresponding to the {@code case} labels
 * of the {@code switch}, implicitly numbered sequentially from {@code [0..N)}.
 *
 * <p>The bootstrap call site accepts a single parameter of the type of the
 * operand of the {@code switch}, and return an {@code int} that is the index of
 * the matched {@code case} label, {@code -1} if the target is {@code null},
 * or {@code N} if the target is not null but matches no {@code case} label.
 */
public class SwitchBootstraps {

    private static final MethodHandle INIT_HOOK;

    private static final Set<Class<?>> INT_TYPES
            = Set.of(int.class, short.class, byte.class, char.class,
                     Integer.class, Short.class, Byte.class, Character.class);
    private static final Map<Class<?>, MethodHandle> switchMethods = new HashMap<>();

    private static final Comparator<String> STRING_BY_HASH
            = Comparator.comparingInt(Objects::hashCode);

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            INIT_HOOK = lookup.findStatic(SwitchBootstraps.class, "initHook",
                                          MethodType.methodType(MethodHandle.class, CallSite.class));
            for (Class<?> c : INT_TYPES)
                switchMethods.put(c, lookup.findVirtual(IntSwitchCallSite.class, "doSwitch",
                                                        MethodType.methodType(int.class, c)));
            switchMethods.put(String.class, lookup.findVirtual(StringSwitchCallSite.class, "doSwitch",
                                                               MethodType.methodType(int.class, String.class)));
            switchMethods.put(Enum.class, lookup.findVirtual(EnumSwitchCallSite.class, "doSwitch",
                                                             MethodType.methodType(int.class, Enum.class)));
        }
        catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static<T extends CallSite> MethodHandle initHook(T receiver) {
        return switchMethods.get(receiver.type().parameterType(0))
                            .bindTo(receiver);
    }

    /**
     * Bootstrap method for linking an {@code invokedynamic} call site that
     * implements a {@code switch} on an {@code int}, {@code short}, {@code byte},
     * {@code char}, or one of their box types.  The static arguments are a
     * varargs array of {@code int} labels.
     *
     * @param lookup Represents a lookup context with the accessibility
     *               privileges of the caller.  When used with {@code invokedynamic},
     *               this is stacked automatically by the VM.
     * @param invocationName The invocation name, which is ignored.  When used with
     *                       {@code invokedynamic}, this is provided by the
     *                       {@code NameAndType} of the {@code InvokeDynamic}
     *                       structure and is stacked automatically by the VM.
     * @param invocationType The invocation type of the {@code CallSite}.  This
     *                       method type should have a single parameter which is
     *                       one of the 32-bit or shorter primitive types, or
     *                       one of their box types, and return {@code int}.  When
     *                       used with {@code invokedynamic}, this is provided by
     *                       the {@code NameAndType} of the {@code InvokeDynamic}
     *                       structure and is stacked automatically by the VM.
     * @param intLabels integral values corresponding to the case labels of the
     *                  {@code switch} statement.
     * @return the index into {@code intLabels} of the target value, if the target
     *         matches any of the labels, {@literal -1} if the target value is
     *         {@code null}, or {@code intLabels.length} if the target value does
     *         not match any of the labels.
     * @throws Throwable if there is any error linking the call site
     */
    public static CallSite intSwitch(MethodHandles.Lookup lookup,
                                     String invocationName,
                                     MethodType invocationType,
                                     int... intLabels) throws Throwable {
        if (invocationType.parameterCount() != 1
            || (!invocationType.returnType().equals(int.class))
            || (!INT_TYPES.contains(invocationType.parameterType(0))))
            throw new IllegalArgumentException("Illegal invocation type " + invocationType);

        return new IntSwitchCallSite(invocationType, intLabels);
    }

    static class IntSwitchCallSite extends ConstantCallSite {
        private final int[] labels;
        private final int[] indexes;

        IntSwitchCallSite(MethodType targetType,
                          int[] intLabels) throws Throwable {
            super(targetType, INIT_HOOK);

            // expensive way to index an array
            indexes = IntStream.range(0, intLabels.length)
                               .boxed()
                               .sorted(Comparator.comparingInt(a -> intLabels[a]))
                               .mapToInt(Integer::intValue)
                               .toArray();
            labels = new int[indexes.length];
            for (int i=0; i<indexes.length; i++)
                labels[i] = intLabels[indexes[i]];
        }

        int doSwitch(int target) {
            int index = Arrays.binarySearch(labels, target);
            return (index >= 0) ? indexes[index] : indexes.length;
        }

        int doSwitch(short target) {
            return doSwitch((int) target);
        }

        int doSwitch(byte target) {
            return doSwitch((int) target);
        }

        int doSwitch(char target) {
            return doSwitch((int) target);
        }

        int doSwitch(Integer target) {
            return (target == null) ? -1 : doSwitch((int) target);
        }

        int doSwitch(Short target) {
            return (target == null) ? -1 : doSwitch((int) target);
        }

        int doSwitch(Character target) {
            return (target == null) ? -1 : doSwitch((int) target);
        }

        int doSwitch(Byte target) {
            return (target == null) ? -1 : doSwitch((int) target);
        }
    }

    /**
     * Bootstrap method for linking an {@code invokedynamic} call site that
     * implements a {@code switch} on a {@code String} target.  The static
     * arguments are a varargs array of {@code String} labels.
     *
     * @param lookup Represents a lookup context with the accessibility
     *               privileges of the caller.  When used with {@code invokedynamic},
     *               this is stacked automatically by the VM.
     * @param invocationName The invocation name, which is ignored.  When used with
     *                       {@code invokedynamic}, this is provided by the
     *                       {@code NameAndType} of the {@code InvokeDynamic}
     *                       structure and is stacked automatically by the VM.
     * @param invocationType The invocation type of the {@code CallSite}.  This
     *                       method type should have a single parameter of
     *                       {@code String}, and return {@code int}.  When
     *                       used with {@code invokedynamic}, this is provided by
     *                       the {@code NameAndType} of the {@code InvokeDynamic}
     *                       structure and is stacked automatically by the VM.
     * @param stringLabels non-null string values corresponding to the case
     *                     labels of the {@code switch} statement.
     * @return the index into {@code labels} of the target value, if the target
     *         matches any of the labels, {@literal -1} if the target value is
     *         {@code null}, or {@code stringLabels.length} if the target value
     *         does not match any of the labels.
     * @throws Throwable if there is any error linking the call site
     */
    public static CallSite stringSwitch(MethodHandles.Lookup lookup,
                                        String invocationName,
                                        MethodType invocationType,
                                        String... stringLabels) throws Throwable {
        if (invocationType.parameterCount() != 1
            || (!invocationType.returnType().equals(int.class))
            || (!invocationType.parameterType(0).equals(String.class)))
            throw new IllegalArgumentException("Illegal invocation type " + invocationType);
        if (Stream.of(stringLabels).anyMatch(Objects::isNull))
            throw new IllegalArgumentException("null label found");

        return new StringSwitchCallSite(invocationType, stringLabels);
    }

    static class StringSwitchCallSite extends ConstantCallSite {
        private final String[] sortedByHash;
        private final int[] indexes;
        private final boolean collisions;

        StringSwitchCallSite(MethodType targetType,
                             String[] stringLabels) throws Throwable {
            super(targetType, INIT_HOOK);

            // expensive way to index an array
            indexes = IntStream.range(0, stringLabels.length)
                               .boxed()
                               .sorted(Comparator.comparingInt(i -> stringLabels[i].hashCode()))
                               .mapToInt(Integer::intValue)
                               .toArray();
            sortedByHash = new String[indexes.length];
            for (int i=0; i<indexes.length; i++)
                sortedByHash[i] = stringLabels[indexes[i]];

            collisions = IntStream.range(0, sortedByHash.length-1)
                                  .anyMatch(i -> sortedByHash[i].hashCode() == sortedByHash[i + 1].hashCode());
        }

        int doSwitch(String target) {
            if (target == null)
                return -1;

            int index = Arrays.binarySearch(sortedByHash, target, STRING_BY_HASH);
            if (index < 0)
                return indexes.length;
            else if (target.equals(sortedByHash[index])) {
                return indexes[index];
            }
            else if (collisions) {
                int hash = target.hashCode();
                while (index > 0 && sortedByHash[index-1].hashCode() == hash)
                    --index;
                for (; index < sortedByHash.length && sortedByHash[index].hashCode() == hash; index++)
                    if (target.equals(sortedByHash[index]))
                        return indexes[index];
            }

            return indexes.length;
        }
    }

    /**
     * Bootstrap method for linking an {@code invokedynamic} call site that
     * implements a {@code switch} on an {@code Enum} target.  The static
     * arguments are the enum class, and a varargs arrays of {@code String}
     * that are the names of the enum constants corresponding to the
     * {@code case} labels.
     *
     * @param <E> the enum type
     * @param lookup Represents a lookup context with the accessibility
     *               privileges of the caller.  When used with {@code invokedynamic},
     *               this is stacked automatically by the VM.
     * @param invocationName The invocation name, which is ignored.  When used with
     *                       {@code invokedynamic}, this is provided by the
     *                       {@code NameAndType} of the {@code InvokeDynamic}
     *                       structure and is stacked automatically by the VM.
     * @param invocationType The invocation type of the {@code CallSite}.  This
     *                       method type should have a single parameter of
     *                       {@code String}, and return {@code int}.  When
     *                       used with {@code invokedynamic}, this is provided by
     *                       the {@code NameAndType} of the {@code InvokeDynamic}
     *                       structure and is stacked automatically by the VM.
     * @param enumClass the enum class
     * @param enumNames names of the enum constants against which the target
     *                  should be matched
     * @return the index into {@code labels} of the target value, if the target
     *         matches any of the labels, {@literal -1} if the target value is
     *         {@code null}, or {@code stringLabels.length} if the target value
     *         does not match any of the labels.
     * @throws IllegalArgumentException if the specified class is not an
     *                                  enum class
     * @throws Throwable if there is any error linking the call site
     */
    public static<E extends Enum<E>> CallSite enumSwitch(MethodHandles.Lookup lookup,
                                                         String invocationName,
                                                         MethodType invocationType,
                                                         Class<E> enumClass,
                                                         String... enumNames) throws Throwable {
        if (invocationType.parameterCount() != 1
            || (!invocationType.returnType().equals(int.class))
            || (!invocationType.parameterType(0).equals(Enum.class)))
            throw new IllegalArgumentException("Illegal invocation type " + invocationType);
        if (!enumClass.isEnum())
            throw new IllegalArgumentException("not an enum class");
        if (Stream.of(enumNames).anyMatch(Objects::isNull))
            throw new IllegalArgumentException("null label found");

        return new EnumSwitchCallSite<>(invocationType, enumClass, enumNames);
    }

    static class EnumSwitchCallSite<E extends Enum<E>> extends ConstantCallSite {
        private final int[] ordinalMap;

        EnumSwitchCallSite(MethodType targetType,
                           Class<E> enumClass,
                           String... enumNames) throws Throwable {
            super(targetType, INIT_HOOK);

            ordinalMap = new int[enumClass.getEnumConstants().length];
            Arrays.fill(ordinalMap, enumNames.length);

            for (int i=0; i<enumNames.length; i++) {
                try {
                    ordinalMap[E.valueOf(enumClass, enumNames[i]).ordinal()] = i;
                }
                catch (Exception e) {
                    // allow non-existent labels, but never match them
                    continue;
                }
            }
        }

        @SuppressWarnings("rawtypes")
        int doSwitch(Enum target) {
            return (target == null) ? -1 : ordinalMap[target.ordinal()];
        }
    }
}
