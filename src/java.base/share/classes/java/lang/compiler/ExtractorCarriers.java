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
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

/**
 * PatternCarriers
 */
public class ExtractorCarriers {

    private static final CarrierFactory factory = CarrierFactories.DUMB;

    interface CarrierFactory {
        MethodHandle constructor(MethodType methodType);
        MethodHandle component(MethodType methodType, int component);
    }

    static class DumbCarrier {
        private final Object[] args;

        DumbCarrier(Object... args) {
            this.args = args.clone();
        }

        Object get(int i) {
            return args[i];
        }
    }

    enum CarrierFactories implements CarrierFactory {
        DUMB {
            private final MethodHandle CARRIER_CTOR;
            private final MethodHandle CARRIER_GET;

            {
                try {
                    CARRIER_CTOR = MethodHandles.lookup().findConstructor(DumbCarrier.class, MethodType.methodType(void.class, Object[].class));
                    CARRIER_GET = MethodHandles.lookup().findVirtual(DumbCarrier.class, "get", MethodType.methodType(Object.class, int.class));
                }
                catch (ReflectiveOperationException e) {
                    throw new ExceptionInInitializerError(e);
                }
            }

            @Override
            public MethodHandle constructor(MethodType methodType) {
                return CARRIER_CTOR.asType(methodType.changeReturnType(Object.class));
            }

            @Override
            public MethodHandle component(MethodType methodType, int component) {
                return MethodHandles.insertArguments(CARRIER_GET, 1, component)
                                    .asType(MethodType.methodType(methodType.parameterType(component), Object.class));
            }
        },
        DUMB_SINGLE {
            // An optimization of DUMB, where we use the value itself as carrier when there is only one value

            @Override
            public MethodHandle constructor(MethodType methodType) {
                return methodType.parameterCount() == 1 ? MethodHandles.identity(methodType.parameterType(0)) : DUMB.constructor(methodType);
            }

            @Override
            public MethodHandle component(MethodType methodType, int component) {
                return methodType.parameterCount() == 1 ? MethodHandles.identity(methodType.parameterType(0)) : DUMB.component(methodType, component);
            }
        }
    }

    /**
     * Returns a method handle with the given method type that instantiates
     * a new carrier object.
     *
     * @param methodType the types of the carrier elements
     * @return the carrier factory
     */
    public static MethodHandle carrierFactory(MethodType methodType) {
        return factory.constructor(methodType);
    }

    /**
     * Returns a method handle that accepts a carrier and returns the i'th component
     *
     * @param methodType the type of the carrier elements
     * @param i the index of the component
     * @return the component method handle
     */
    public static MethodHandle carrierComponent(MethodType methodType, int i) {
        return factory.component(methodType, i);
    }

    /**
     * Return all the components method handles for a carrier
     * @param methodType the type of the carrier elements
     * @return the component method handles
     */
    public static MethodHandle[] carrierComponents(MethodType methodType) {
        MethodHandle[] components = new MethodHandle[methodType.parameterCount()];
        Arrays.setAll(components, i -> factory.component(methodType, i));
        return components;
    }
}
