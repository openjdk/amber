/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.vm.annotation.Stable;

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
 * MethodHandle initializingConstructor = elements.initializingConstructor();
 * // Fetch the list of carrier component MethodHandles
 * List<MethodHandle> components = elements.components();
 *
 * // Create an instance of the carrier with a string and an integer
 * Object carrier = initializingConstructor.invokeExact("abc", 10);
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
 * @since 21
 *
 * Warning: This class is part of PreviewFeature.Feature.STRING_TEMPLATES.
 *          Do not rely on its availability.
 */
public final class Carriers {
    private static final MethodHandle CONSTRUCTOR;
    private static final MethodHandle COMPONENT_GETTER;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            CONSTRUCTOR = lookup.findStatic(Carriers.class, "constructor", MethodType.methodType(Object.class, Object[].class));
            COMPONENT_GETTER = lookup.findStatic(Carriers.class, "componentGetter", MethodType.methodType(Object.class, int.class, Carrier.class));
        }
        catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Carriers() {
    }

    /**
     * {@return the combination {@link MethodHandle} of the constructor and initializer
     * for the carrier representing {@code methodType}. The carrier constructor/initializer
     * will always take the component values and a return type of {@link Object} }
     *
     * @param lookup Represents a lookup context with the accessibility
     *               privileges of the caller.
     *               This is stacked automatically by the VM.
     * @param name unused
     * @param type unused
     * @param carrierShape {@link MethodType} whose parameter types supply the shape of the
     *                     carrier's components
     */
    static public MethodHandle initializingConstructor(MethodHandles.Lookup lookup, String name, Class<?> type, MethodType carrierShape) {
        return CONSTRUCTOR.asCollector(Object[].class, carrierShape.parameterCount())
                          .asType(carrierShape);
    }

    private static Object constructor(Object... args) {
        return new Carrier(args);
    }

    /**
     * {@return a component accessor {@link MethodHandle} for component {@code i} of the
     * carrier representing {@code methodType}. The receiver type of the accessor will always
     * be {@link Object} }
     *
     * @param lookup Represents a lookup context with the accessibility
     *               privileges of the caller.  When used with {@code invokedynamic},
     *               this is stacked automatically by the VM.
     * @param invocationName unused
     * @param invocationType The invocation type of the {@code CallSite} with one parameter,
     *                       a reference type, and type that matches the i-th parameter type
     *                       from {@code methodType} as a return type.
     * @param carrierShape {@link MethodType} whose parameter types supply the shape of the
     *                     carrier's components
     * @param i            component index
     *
     * @throws IllegalArgumentException if {@code i} is out of bounds
     */
    public static CallSite component(MethodHandles.Lookup lookup,
                                         String invocationName,
                                         MethodType invocationType,
                                         MethodType carrierShape,
                                         int i) {
        Class<?> componentType = carrierShape.parameterType(i);
        if (invocationType.parameterCount() != 1
            || (!invocationType.returnType().equals(componentType))
            || !invocationType.parameterType(0).equals(Object.class))
            throw new IllegalArgumentException("Illegal invocation type " + invocationType);
        return new ConstantCallSite(MethodHandles.insertArguments(COMPONENT_GETTER, 0, i).asType(MethodType.methodType(componentType, Object.class)));
    }

    private static Object componentGetter(int component, Carrier carrier) throws Throwable {
        return carrier.data()[component];
    }

    private record Carrier(@Stable Object[] data) {}
}
