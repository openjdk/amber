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

import java.lang.annotation.Foldable;
import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * A symbolic reference for a dynamic constant (one described in the constant
 * pool with {@code Constant_Dynamic_info}.)
 *
 * @param <T> the type of the dynamic constant
 */
public class DynamicConstantRef<T> implements ConstantRef<T>, Constable<ConstantRef<T>> {
    private static final ConstantRef<?>[] EMPTY_ARGS = new ConstantRef<?>[0];

    private final MethodHandleRef bootstrapMethod;
    private final ConstantRef<?>[] bootstrapArgs;
    private final String name;
    private final ClassRef type;

    @SuppressWarnings("rawtypes")
    private static final Map<MethodHandleRef, Function<DynamicConstantRef, ConstantRef<?>>> canonicalMap
            = Map.ofEntries(Map.entry(SymbolicRefs.BSM_PRIMITIVE_CLASS, d -> ClassRef.ofDescriptor(d.name)),
                            Map.entry(SymbolicRefs.BSM_ENUM_CONSTANT, d -> EnumRef.of(d.type, d.name)),
                            Map.entry(SymbolicRefs.BSM_NULL_CONSTANT, d -> SymbolicRefs.NULL),
                            Map.entry(SymbolicRefs.BSM_VARHANDLE_STATIC_FIELD,
                                      d -> VarHandleRef.ofStaticField((ClassRef) d.bootstrapArgs[0],
                                                                      (String) d.bootstrapArgs[1],
                                                                      (ClassRef) d.bootstrapArgs[2])),
                            Map.entry(SymbolicRefs.BSM_VARHANDLE_FIELD,
                                      d -> VarHandleRef.ofField((ClassRef) d.bootstrapArgs[0],
                                                                (String) d.bootstrapArgs[1],
                                                                (ClassRef) d.bootstrapArgs[2])),
                            Map.entry(SymbolicRefs.BSM_VARHANDLE_ARRAY,
                                      d -> VarHandleRef.ofArray((ClassRef) d.bootstrapArgs[0]))
    );

    /**
     * Construct a symbolic reference for a dynamic constant
     *
     * @param bootstrapMethod The bootstrap method for the constant
     * @param name The name that would appear in the {@code NameAndType} operand
     *             of the {@code LDC} for this constant
     * @param type The type that would appear in the {@code NameAndType} operand
     *             of the {@code LDC} for this constant
     * @param bootstrapArgs The static arguments to the bootstrap, that would
     *                      appear in the {@code BootstrapMethods} attribute
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if {@code name.length()} is zero
     */
    protected DynamicConstantRef(MethodHandleRef bootstrapMethod, String name, ClassRef type, ConstantRef<?>[] bootstrapArgs) {
        this.bootstrapMethod = requireNonNull(bootstrapMethod);
        this.name = requireNonNull(name);
        this.type = requireNonNull(type);
        this.bootstrapArgs = requireNonNull(bootstrapArgs).clone();

        if (name.length() == 0)
            throw new IllegalArgumentException("Illegal invocation name: " + name);
    }

    /**
     * Construct a symbolic reference for a dynamic constant, whose bootstrap
     * takes no static arguments
     *
     * @param bootstrapMethod The bootstrap method for the constant
     * @param name The name that would appear in the {@code NameAndType} operand
     *             of the {@code LDC} for this constant
     * @param type The type that would appear in the {@code NameAndType} operand
     *             of the {@code LDC} for this constant
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if {@code name.length()} is zero
     */
    protected DynamicConstantRef(MethodHandleRef bootstrapMethod, String name, ClassRef type) {
        this(bootstrapMethod, name, type, EMPTY_ARGS);
    }

    /**
     * Return a symbolic reference for a dynamic constant whose bootstrap, invocation
     * name, and invocation type are the same as this one, but with the specified
     * bootstrap arguments
     *
     * @param bootstrapArgs the bootstrap arguments
     * @return the symbolic reference
     * @throws NullPointerException if any argument is null
     */
    @Foldable
    public DynamicConstantRef<T> withArgs(ConstantRef<?>... bootstrapArgs) {
        return new DynamicConstantRef<>(bootstrapMethod, name, type, bootstrapArgs);
    }

    /**
     * Return a symbolic reference for a dynamic constant.  If  the bootstrap
     * corresponds to a well-known bootstrap, for which a more specific symbolic
     * reference type (e.g., ClassRef) is available, then the more specific
     * symbolic reference will be returned.
     *
     * @param <T> the type of the dynamic constant
     * @param bootstrapMethod The bootstrap method for the constant
     * @param name The name that would appear in the {@code NameAndType} operand
     *             of the {@code LDC} for this constant
     * @param type The type that would appear in the {@code NameAndType} operand
     *             of the {@code LDC} for this constant
     * @param bootstrapArgs The static arguments to the bootstrap, that would
     *                      appear in the {@code BootstrapMethods} attribute
     * @return the symbolic reference
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if {@code name.length()} is zero
     */
    @Foldable
    public static<T> ConstantRef<T> ofCanonical(MethodHandleRef bootstrapMethod, String name, ClassRef type, ConstantRef<?>[] bootstrapArgs) {
        DynamicConstantRef<T> dcr = new DynamicConstantRef<>(bootstrapMethod, name, type, bootstrapArgs);
        return dcr.canonicalize();
    }

    private ConstantRef<T> canonicalize() {
        // @@@ Existing map-based approach is cute but not very robust; need to add more checking of target DCRef
        @SuppressWarnings("rawtypes")
        Function<DynamicConstantRef, ConstantRef<?>> f = canonicalMap.get(bootstrapMethod);
        if (f != null) {
            @SuppressWarnings("unchecked")
            ConstantRef<T> converted = (ConstantRef<T>) f.apply(this);
            return converted;
        }
        return this;
    }

    /**
     * Return a symbolic reference for a dynamic constant.
     *
     * @param <T> the type of the dynamic constant
     * @param bootstrapMethod The bootstrap method for the constant
     * @param name The name that would appear in the {@code NameAndType} operand
     *             of the {@code LDC} for this constant
     * @param type The type that would appear in the {@code NameAndType} operand
     *             of the {@code LDC} for this constant
     * @param bootstrapArgs The static arguments to the bootstrap, that would
     *                      appear in the {@code BootstrapMethods} attribute
     * @return the symbolic reference
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if {@code name.length()} is zero
     */
    @Foldable
    public static<T> DynamicConstantRef<T> of(MethodHandleRef bootstrapMethod, String name, ClassRef type, ConstantRef<?>[] bootstrapArgs) {
        return new DynamicConstantRef<>(bootstrapMethod, name, type, bootstrapArgs);
    }

    /**
     * Return a symbolic reference for a dynamic constant whose bootstrap has
     * no static arguments.
     *
     * @param <T> the type of the dynamic constant
     * @param bootstrapMethod The bootstrap method for the constant
     * @param name The name that would appear in the {@code NameAndType} operand
     *             of the {@code LDC} for this constant
     * @param type The type that would appear in the {@code NameAndType} operand
     *             of the {@code LDC} for this constant
     * @return the symbolic reference
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if {@code name.length()} is zero
     */
    @Foldable
    public static<T> DynamicConstantRef<T> of(MethodHandleRef bootstrapMethod, String name, ClassRef type) {
        return new DynamicConstantRef<>(bootstrapMethod, name, type);
    }

    /**
     * Return a symbolic reference for a dynamic constant whose bootstrap has
     * no static arguments, and whose name parameter is ignored by the bootstrap
     *
     * @param <T> the type of the dynamic constant
     * @param bootstrapMethod The bootstrap method for the constant
     * @param type The type that would appear in the {@code NameAndType} operand
     *             of the {@code LDC} for this constant
     * @return the symbolic reference
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if {@code name.length()} is zero
     */
    @Foldable
    public static<T> DynamicConstantRef<T> of(MethodHandleRef bootstrapMethod, ClassRef type) {
        return of(bootstrapMethod, "_", type);
    }

    /**
     * Return a symbolic reference for a dynamic constant whose bootstrap has
     * no static arguments, and whose type parameter is always the same as the
     * bootstrap method return type.
     *
     * @param <T> the type of the dynamic constant
     * @param bootstrapMethod The bootstrap method for the constant
     * @param name The name that would appear in the {@code NameAndType} operand
     *             of the {@code LDC} for this constant
     * @return the symbolic reference
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if {@code name.length()} is zero
     */
    @Foldable
    public static<T> DynamicConstantRef<T> of(MethodHandleRef bootstrapMethod, String name) {
        return of(bootstrapMethod, name, bootstrapMethod.type().returnType());
    }

    /**
     * Return a symbolic reference for a dynamic constant whose bootstrap has
     * no static arguments, whose name parameter is ignored, and whose type
     * parameter is always the same as the bootstrap method return type.
     *
     * @param <T> the type of the dynamic constant
     * @param bootstrapMethod The bootstrap method for the constant
     * @return the symbolic reference
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if {@code name.length()} is zero
     */
    @Foldable
    public static<T> DynamicConstantRef<T> of(MethodHandleRef bootstrapMethod) {
        return of(bootstrapMethod, "_");
    }

    /**
     * returns The name that would appear in the {@code NameAndType} operand
     *             of the {@code LDC} for this constant
     * @return the name
     */
    @Foldable
    public String name() {
        return name;
    }

    /**
     * returns The type that would appear in the {@code NameAndType} operand
     *             of the {@code LDC} for this constant
     * @return the type
     */
    @Foldable
    public ClassRef type() {
        return type;
    }

    /**
     * Returns the bootstrap method for this constant
     * @return the bootstrap method
     */
    @Foldable
    public MethodHandleRef bootstrapMethod() { return bootstrapMethod; }

    /**
     * Returns the bootstrap arguments for this constant
     * @return the bootstrap arguments
     */
    public ConstantRef<?>[] bootstrapArgs() { return bootstrapArgs.clone(); }

    private static Object[] resolveArgs(MethodHandles.Lookup lookup, ConstantRef<?>[] args) {
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

    @Override
    public Optional<? extends ConstantRef<? super ConstantRef<T>>> toSymbolicRef(MethodHandles.Lookup lookup) {
        ConstantRef<?>[] args = new ConstantRef<?>[bootstrapArgs.length + 4];
        args[0] = SymbolicRefs.MHR_DYNAMICCONSTANTREF_FACTORY;
        args[1] = bootstrapMethod;
        args[2] = name;
        args[3] = type;
        System.arraycopy(bootstrapArgs, 0, args, 4, bootstrapArgs.length);
        ConstantRef<ConstantRef<T>> ref = DynamicConstantRef.of(SymbolicRefs.BSM_INVOKE, name, SymbolicRefs.CR_DynamicConstantRef, args);
        return Optional.of(ref);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DynamicConstantRef)) return false;
        DynamicConstantRef<?> ref = (DynamicConstantRef<?>) o;
        return Objects.equals(bootstrapMethod, ref.bootstrapMethod) &&
               Arrays.equals(bootstrapArgs, ref.bootstrapArgs) &&
               Objects.equals(name, ref.name) &&
               Objects.equals(type, ref.type);
    }

    @Override
    public final int hashCode() {
        int result = Objects.hash(bootstrapMethod, name, type);
        result = 31 * result + Arrays.hashCode(bootstrapArgs);
        return result;
    }

    @Override
    public String toString() {
        return String.format("CondyRef[%s(%s), NameAndType[%s:%s]]",
                             bootstrapMethod, Arrays.toString(bootstrapArgs), name, type);
    }
}
