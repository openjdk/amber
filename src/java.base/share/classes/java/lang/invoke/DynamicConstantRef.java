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
package java.lang.invoke;

import java.lang.annotation.TrackableConstant;

/**
 * A descriptor for a dynamic constant.
 */
public final class DynamicConstantRef<T> implements ConstantRef<T> {
    private final String name;
    private final BootstrapSpecifier bootstrapSpecifier;
    private final ClassRef type;

    private DynamicConstantRef(BootstrapSpecifier bootstrapSpecifier, String name, ClassRef type) {
        this.name = name;
        this.bootstrapSpecifier = bootstrapSpecifier;
        this.type = type;
    }

    /**
     * Return a descriptor for a dynamic constant.
     * @param <T> the type of the dynamic constant
     * @param bootstrapSpecifier the bootstrap specifier for the dynamic constant
     * @param name the name for the dynamic constant
     * @param type the type of the dynamic constant
     * @return the descriptor for the dynamic constant
     */
    @TrackableConstant
    public static<T> DynamicConstantRef<T> of(BootstrapSpecifier bootstrapSpecifier, String name, ClassRef type) {
        return new DynamicConstantRef<>(bootstrapSpecifier, name, type);
    }

    /**
     * Return a descriptor for a dynamic constant, whose name is not used by the bootstrap
     * @param <T> the type of the dynamic constant
     * @param bootstrapSpecifier the bootstrap specifier for the dynamic constant
     * @param type the type of the dynamic constant
     * @return the descriptor for the dynamic constant
     */
    @TrackableConstant
    public static<T> DynamicConstantRef<T> of(BootstrapSpecifier bootstrapSpecifier, ClassRef type) {
        return of(bootstrapSpecifier, "_", type);
    }

    /**
     * Return a descriptor for a dynamic constant, whose type is the same as the bootstrap return
     * @param <T> the type of the dynamic constant
     * @param bootstrapSpecifier the bootstrap specifier for the dynamic constant
     * @param name the name for the dynamic constant
     * @return the descriptor for the dynamic constant
     */
    @TrackableConstant
    public static<T> DynamicConstantRef<T> of(BootstrapSpecifier bootstrapSpecifier, String name) {
        return of(bootstrapSpecifier, name, bootstrapSpecifier.method().type().returnType());
    }

    /**
     * Return a descriptor for a dynamic constant, whose type is the same as the bootstrap return,
     * and whose name is not used by the bootstrap
     * @param <T> the type of the dynamic constant
     * @param bootstrapSpecifier the bootstrap specifier for the dynamic constant
     * @return the descriptor for the dynamic constant
     */
    @TrackableConstant
    public static<T> DynamicConstantRef<T> of(BootstrapSpecifier bootstrapSpecifier) {
        return of(bootstrapSpecifier, "_");
    }

    /**
     * Return a descriptor for a dynamic constant whose bootstrap has no static arguments.
     * @param <T> the type of the dynamic constant
     * @param bootstrapMethod the bootstrap method
     * @param name the name for the dynamic constant
     * @param type the type of the dynamic constant
     * @return the descriptor for the dynamic constant
     */
    @TrackableConstant
    public static<T> DynamicConstantRef<T> of(MethodHandleRef bootstrapMethod, String name, ClassRef type) {
        return of(BootstrapSpecifier.of(bootstrapMethod), name, type);
    }

    /**
     * Return a descriptor for a dynamic constant whose bootstrap has no static arguments,
     * and whose name is ignored by the bootstrap
     * @param <T> the type of the dynamic constant
     * @param bootstrapMethod the bootstrap method
     * @param type the type of the dynamic constant
     * @return the descriptor for the dynamic constant
     */
    @TrackableConstant
    public static<T> DynamicConstantRef<T> of(MethodHandleRef bootstrapMethod, ClassRef type) {
        return of(BootstrapSpecifier.of(bootstrapMethod), type);
    }

    /**
     * Return a descriptor for a dynamic constant whose bootstrap has no static arguments,
     * anmd whose type is the same as the bootstrap return
     * @param <T> the type of the dynamic constant
     * @param bootstrapMethod the bootstrap method
     * @param name the name for the dynamic constant
     * @return the descriptor for the dynamic constant
     */
    @TrackableConstant
    public static<T> DynamicConstantRef<T> of(MethodHandleRef bootstrapMethod, String name) {
        return of(BootstrapSpecifier.of(bootstrapMethod), name);
    }

    /**
     * Return a descriptor for a dynamic constant whose bootstrap has no static arguments,
     * whose name is ignored by the bootstrap, and whose type is the same as the bootstrap return
     * @param <T> the type of the dynamic constant
     * @param bootstrapMethod the bootstrap method
     * @return the descriptor for the dynamic constant
     */
    @TrackableConstant
    public static<T> DynamicConstantRef<T> of(MethodHandleRef bootstrapMethod) {
        return of(BootstrapSpecifier.of(bootstrapMethod));
    }

    /**
     * returns the bootstrap specifier
     * @return the bootstrap specifier
     */
    @TrackableConstant
    public BootstrapSpecifier bootstrap() {
        return bootstrapSpecifier;
    }

    /**
     * returns the name
     * @return the name
     */
    @TrackableConstant
    public String name() {
        return name;
    }

    /**
     * returns the type
     * @return the type
     */
    @TrackableConstant
    public ClassRef type() {
        return type;
    }

    /**
     * Returns the bootstrap method in the bootstrap specifier
     * @return the bootstrap method in the bootstrap specifier
     */
    @TrackableConstant
    public MethodHandleRef bootstrapMethod() { return bootstrapSpecifier.method(); }

    /**
     * Returns the bootstrap arguments in the bootstrap specifier
     * @return the bootstrap arguments in the bootstrap specifier
     */
    public ConstantRef<?>[] bootstrapArgs() { return bootstrapSpecifier.arguments(); }

    /**
     * Resolve
     * @param lookup the lookup
     * @return the resolved object
     * @throws ReflectiveOperationException exception
     */
    @SuppressWarnings("unchecked")
    public T resolve(MethodHandles.Lookup lookup) {
        try {
            MethodHandle bsmMh = bootstrapSpecifier.method().resolve(lookup);
            return (T) ConstantBootstraps.makeConstant(bsmMh,
                                                       name,
                                                       type.resolve(lookup),
                                                       Constables.resolveArgs(lookup, bootstrapSpecifier.arguments()),
                                                       // TODO pass lookup
                                                       lookup.lookupClass());
        }
        catch (RuntimeException|Error e) {
            throw e;
        }
        catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
