/*
 * Copyright (c) 2017, 2024, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.constant.ConstantUtils;
import jdk.internal.misc.PreviewFeatures;

import java.lang.Enum.EnumDesc;
import java.lang.classfile.ClassFile;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.*;
import java.lang.reflect.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import static java.lang.invoke.MethodHandles.Lookup.ClassOption.NESTMATE;
import static java.lang.invoke.MethodHandles.Lookup.ClassOption.STRONG;
import static java.util.Objects.requireNonNull;

/**
 * Bootstrap methods for linking {@code invokedynamic} call sites that implement
 * the selection functionality of the {@code switch} statement.  The bootstraps
 * take additional static arguments corresponding to the {@code case} labels
 * of the {@code switch}, implicitly numbered sequentially from {@code [0..N)}.
 *
 * @since 21
 */
public class PatternBootstraps {

    private PatternBootstraps() {
    }

    private static final Object SENTINEL = new Object();
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final boolean previewEnabled = PreviewFeatures.isEnabled();

    private static class StaticHolders {
        private static final MethodHandle SYNTHETIC_PATTERN;

        static {
            try {
                SYNTHETIC_PATTERN = LOOKUP.findStatic(PatternBootstraps.class, "syntheticPattern",
                        MethodType.methodType(Object.class, Method[].class, MethodHandle.class, Object.class));
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

    /**
     * Bootstrap method for linking an {@code invokedynamic} call site that
     * implements a pattern invocation on a target of a reference type.
     * <p>
     * If the reference type is a record class without a deconstructor then a deconstructor is synthesized from the
     * accessors of the method, otherwise the discovered deconstructor is invoked.
     * <p>
     * The static arguments are the component types of the record or the binding types if its a deconstructor pattern.
     * <p>
     * The type of the returned {@code CallSite}'s method handle will have
     * a return type of {@code Object} (the Carrier Object).
     * It has one parameter: the sole argument will be an {@code Object} instance ({@code target}).
     * <p>
     * If the {@code target} is {@code null}, then the method of the call site
     * returns {@literal -1}.
     *
     * @param lookup         Represents a lookup context with the accessibility
     *                       privileges of the caller.  When used with {@code invokedynamic},
     *                       this is stacked automatically by the VM.
     * @param invocationName unused
     * @param invocationType The invocation type of the {@code CallSite} with one parameter,
     *                       a reference type, and an {@code Object} as a return type.
     * @param mangledName    The mangled name of the method declaration that will act as a pattern.
     * @return a {@code CallSite} returning the first matching element as described above
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if the invocation type is not a method type of first parameter of a reference type,
     *                                  and with {@code Object} as its return type,
     * @jvms 4.4.6 The CONSTANT_NameAndType_info Structure
     * @jvms 4.4.10 The CONSTANT_Dynamic_info and CONSTANT_InvokeDynamic_info Structures
     */
    public static CallSite invokePattern(MethodHandles.Lookup lookup,
                                         String invocationName,
                                         MethodType invocationType,
                                         String mangledName) {
        if (invocationType.parameterCount() == 2) {
            Class<?> receiverType = invocationType.parameterType(0);
            Class<?> matchCandidateType = invocationType.parameterType(1);
            if ((!invocationType.returnType().equals(Object.class)))
                throw new IllegalArgumentException("Illegal invocation type " + invocationType);

            MethodHandle target = null;
            try {
                // Attempt 1: discover the pattern declaration
                target = lookup.findStatic(receiverType, mangledName, MethodType.methodType(Object.class, receiverType, matchCandidateType));

                return new ConstantCallSite(target);
            } catch (Throwable t) {
                // Attempt 2: synthesize the pattern declaration from the record components
                if (!matchCandidateType.isRecord() || !receiverType.equals(matchCandidateType)) {
                    throw new IllegalArgumentException("Implicit pattern invocation with erroneous match-candidate type or received type");
                }

                String expectedMangledName = PatternBytecodeName.mangle(matchCandidateType,
                        Arrays.stream(matchCandidateType.getRecordComponents())
                                .map(RecordComponent::getType)
                                .toArray(Class<?>[]::new));

                if (!expectedMangledName.equals(mangledName)) {
                    throw new IllegalArgumentException("Unexpected pattern at use site: " + mangledName + "(was expecting: " + expectedMangledName + ")");
                }

                @SuppressWarnings("removal")
                final RecordComponent[] components = AccessController.doPrivileged(
                        (PrivilegedAction<RecordComponent[]>) matchCandidateType::getRecordComponents);

                Method[] accessors = Arrays.stream(components).map(c -> {
                    Method accessor = c.getAccessor();
                    accessor.setAccessible(true);
                    return accessor;
                }).toArray(Method[]::new);

                Class<?>[] ctypes = Arrays.stream(components).map(c -> c.getType()).toArray(Class<?>[]::new);

                Carriers.CarrierElements carrierElements = Carriers.CarrierFactory.of(ctypes);

                MethodHandle initializingConstructor = carrierElements.initializingConstructor();

                MethodHandle carrierCreator = initializingConstructor.asSpreader(Object[].class, ctypes.length);

                target =
                        MethodHandles.dropArguments(
                                MethodHandles.insertArguments(StaticHolders.SYNTHETIC_PATTERN,
                                        0,
                                        accessors,
                                        carrierCreator),
                                1,
                                Object.class).asType(invocationType);

                return new ConstantCallSite(target);
            }
        }

        // todo: static patterns will expect one parameter
        throw new IllegalStateException("Pattern Invocation Illegal State");
    }

    /**
     * Returns a carrier, initialized with the extracted data according to the record component list of
     * {@code matchCandidateType}.
     *
     * @param matchCandidateInstance the receiver of a pattern
     * @param matchCandidateType     the type of the match candidate
     * @return initialized carrier object
     * @throws Throwable throws if invocation of synthetic pattern fails
     */
    private static Object syntheticPattern(Method[] accessors, MethodHandle carrierCreator, Object matchCandidateInstance) throws Throwable {
        Object[] extracted = Arrays.stream(accessors).map(accessor -> {
            try {
                return accessor.invoke(matchCandidateInstance);
            } catch (IllegalAccessException e) {
                throw new MatchException(null, e.getCause());
            } catch (InvocationTargetException e) {
                throw new MatchException(null, e.getCause());
            }
        }).toArray();

        Object carrier = carrierCreator.invoke(extracted);

        return carrier;
    }
}
