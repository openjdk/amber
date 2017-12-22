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
package java.lang.sym;

import java.lang.annotation.TrackableConstant;
import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * A descriptor for a dynamic constant.
 */
public class DynamicConstantRef<T> implements SymbolicRef<T> {
    private static final SymbolicRef<?>[] EMPTY_ARGS = new SymbolicRef<?>[0];

    private final MethodHandleRef bootstrapMethod;
    private final SymbolicRef<?>[] bootstrapArgs;
    private final String name;
    private final ClassRef type;

    @SuppressWarnings("rawtypes")
    private static final Map<MethodHandleRef, Function<DynamicConstantRef, SymbolicRef>> canonicalMap
            = Map.ofEntries(Map.entry(SymbolicRefs.BSM_PRIMITIVE_CLASS, d -> ClassRef.ofDescriptor(d.name)),
                            Map.entry(SymbolicRefs.BSM_ENUM_CONSTANT, d -> EnumRef.of(d.type, d.name)),
                            Map.entry(SymbolicRefs.BSM_NULL_CONSTANT, d -> SymbolicRefs.NULL));

    protected DynamicConstantRef(MethodHandleRef bootstrapMethod, String name, ClassRef type, SymbolicRef<?>[] bootstrapArgs) {
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("Illegal invocation name: " + name);
        this.bootstrapMethod = requireNonNull(bootstrapMethod);
        this.name = name;
        this.type = requireNonNull(type);
        this.bootstrapArgs = requireNonNull(bootstrapArgs).clone();
    }

    protected DynamicConstantRef(MethodHandleRef bootstrapMethod, String name, ClassRef type) {
        this(bootstrapMethod, name, type, EMPTY_ARGS);
    }

    /**
     * Return a descriptor for a dynamic constant whose bootstrap, invocation
     * name, and invocation type are the same as this one, but with the specified
     * bootstrap arguments
     *
     * @param bootstrapArgs the bootstrap arguments
     * @return the descriptor for the dynamic constant
     */
    @TrackableConstant
    public DynamicConstantRef<T> withArgs(SymbolicRef<?>... bootstrapArgs) {
        return new DynamicConstantRef<>(bootstrapMethod, name, type, bootstrapArgs);
    }

    /**
     * Return a descriptor for a dynamic constant.  If  the bootstrap corresponds
     * to a well-known bootstrap, for which a higher-level constant (e.g., ClassRef)
     * is available, then the higher-level constant will be returned
     * @param <T> the type of the dynamic constant
     * @param bootstrapMethod the bootstrap method
     * @param name the name for the dynamic constant
     * @param type the type of the dynamic constant
     * @param bootstrapArgs the bootstrap arguments
     * @return the descriptor for the symbolic reference
     */
    @TrackableConstant
    public static<T> SymbolicRef<T> ofCanonical(MethodHandleRef bootstrapMethod, String name, ClassRef type, SymbolicRef<?>[] bootstrapArgs) {
        DynamicConstantRef<T> dcr = new DynamicConstantRef<>(bootstrapMethod, name, type, bootstrapArgs);
        return dcr.canonicalize();
    }

    private SymbolicRef<T> canonicalize() {
        @SuppressWarnings("rawtypes")
        Function<DynamicConstantRef, SymbolicRef> f = canonicalMap.get(bootstrapMethod);
        if (f != null) {
            @SuppressWarnings("unchecked")
            SymbolicRef<T> converted = f.apply(this);
            return converted;
        }
        return this;
    }

    /**
     * Return a descriptor for a dynamic constant
     * @param <T> the type of the dynamic constant
     * @param bootstrapMethod the bootstrap method
     * @param name the name for the dynamic constant
     * @param type the type of the dynamic constant
     * @param bootstrapArgs the bootstrap arguments
     * @return the descriptor for the dynamic constant
     */
    @TrackableConstant
    public static<T> DynamicConstantRef<T> of(MethodHandleRef bootstrapMethod, String name, ClassRef type, SymbolicRef<?>[] bootstrapArgs) {
        return new DynamicConstantRef<>(bootstrapMethod, name, type, bootstrapArgs);
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
        return new DynamicConstantRef<>(bootstrapMethod, name, type);
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
        return of(bootstrapMethod, "_", type);
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
        return of(bootstrapMethod, name, bootstrapMethod.type().returnType());
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
        return of(bootstrapMethod, "_");
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
    public MethodHandleRef bootstrapMethod() { return bootstrapMethod; }

    /**
     * Returns the bootstrap arguments in the bootstrap specifier
     * @return the bootstrap arguments in the bootstrap specifier
     */
    public SymbolicRef<?>[] bootstrapArgs() { return bootstrapArgs.clone(); }

    private static Object[] resolveArgs(MethodHandles.Lookup lookup, SymbolicRef<?>[] args) {
        return Stream.of(args)
                     .map(arg -> {
                         try {
                             return arg.resolveRef(lookup);
                         }
                         catch (ReflectiveOperationException e) {
                             throw new RuntimeException(e);
                         }
                     })
                     .toArray();
    }

    /**
     * Resolve
     * @param lookup the lookup
     * @return the resolved object
     * @throws ReflectiveOperationException exception
     */
    @SuppressWarnings("unchecked")
    public T resolveRef(MethodHandles.Lookup lookup) throws ReflectiveOperationException {
        try {
            MethodHandle bsmMh = bootstrapMethod.resolveRef(lookup);
            return (T) ConstantBootstraps.makeConstant(bsmMh,
                                                       name,
                                                       type.resolveRef(lookup),
                                                       resolveArgs(lookup, bootstrapArgs),
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
