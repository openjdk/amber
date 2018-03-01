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
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.sym.ConstantRefs.CR_ClassRef;
import static java.lang.sym.ConstantRefs.CR_ConstantRef;
import static java.lang.sym.ConstantRefs.CR_DynamicConstantRef;
import static java.lang.sym.ConstantRefs.CR_MethodHandleRef;
import static java.lang.sym.ConstantRefs.CR_String;
import static java.util.Objects.requireNonNull;

/**
 * A symbolic reference for a dynamic constant (one described in the constant
 * pool with {@code Constant_Dynamic_info}.)
 *
 * <p>Concrete subtypes of {@linkplain DynamicConstantRef} must be
 * <a href="../doc-files/ValueBased.html">value-based</a>.
 *
 * @param <T> the type of the dynamic constant
 */
public abstract class DynamicConstantRef<T> implements ConstantRef<T>, Constable<ConstantRef<T>> {
    private final MethodHandleRef bootstrapMethod;
    private final ConstantRef<?>[] bootstrapArgs;
    private final String name;
    private final ClassRef type;

    @SuppressWarnings("rawtypes")
    private static final Map<MethodHandleRef, Function<DynamicConstantRef, ConstantRef<?>>> canonicalMap
            = Map.ofEntries(Map.entry(ConstantRefs.BSM_PRIMITIVE_CLASS, d -> ClassRef.ofDescriptor(d.name)),
                            Map.entry(ConstantRefs.BSM_ENUM_CONSTANT, d -> EnumRef.of(d.type, d.name)),
                            Map.entry(ConstantRefs.BSM_NULL_CONSTANT, d -> ConstantRefs.NULL),
                            Map.entry(ConstantRefs.BSM_VARHANDLE_STATIC_FIELD,
                                      d -> VarHandleRef.ofStaticField((ClassRef) d.bootstrapArgs[0],
                                                                      (String) d.bootstrapArgs[1],
                                                                      (ClassRef) d.bootstrapArgs[2])),
                            Map.entry(ConstantRefs.BSM_VARHANDLE_FIELD,
                                      d -> VarHandleRef.ofField((ClassRef) d.bootstrapArgs[0],
                                                                (String) d.bootstrapArgs[1],
                                                                (ClassRef) d.bootstrapArgs[2])),
                            Map.entry(ConstantRefs.BSM_VARHANDLE_ARRAY,
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
    protected DynamicConstantRef(MethodHandleRef bootstrapMethod, String name, ClassRef type, ConstantRef<?>... bootstrapArgs) {
        this.bootstrapMethod = requireNonNull(bootstrapMethod);
        this.name = requireNonNull(name);
        this.type = requireNonNull(type);
        this.bootstrapArgs = requireNonNull(bootstrapArgs).clone();

        if (name.length() == 0)
            throw new IllegalArgumentException("Illegal invocation name: " + name);
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
        return new DynamicConstantRefImpl<>(bootstrapMethod, name, type, bootstrapArgs);
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
        DynamicConstantRef<T> dcr = new DynamicConstantRefImpl<>(bootstrapMethod, name, type, bootstrapArgs);
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
        return new DynamicConstantRefImpl<>(bootstrapMethod, name, type, bootstrapArgs);
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
        return new DynamicConstantRefImpl<>(bootstrapMethod, name, type);
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
        return of(bootstrapMethod, name, bootstrapMethod.methodType().returnType());
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
    public String constantName() {
        return name;
    }

    /**
     * returns The type that would appear in the {@code NameAndType} operand
     *             of the {@code LDC} for this constant
     * @return the type
     */
    @Foldable
    public ClassRef constantType() {
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

    private static Object[] resolveArgs(MethodHandles.Lookup lookup, ConstantRef<?>[] args)
            throws ReflectiveOperationException {
        try {
            return Stream.of(args)
                    .map(arg -> {
                        try {
                            return arg.resolveConstantRef(lookup);
                        }
                        catch (ReflectiveOperationException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toArray();
        }
        catch (RuntimeException e) {
            if (e.getCause() instanceof ReflectiveOperationException) {
                throw (ReflectiveOperationException) e.getCause();
            }
            else {
                throw e;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public T resolveConstantRef(MethodHandles.Lookup lookup) throws ReflectiveOperationException {
        return (T) ConstantBootstraps.makeConstant(bootstrapMethod.resolveConstantRef(lookup),
                                                   name,
                                                   type.resolveConstantRef(lookup),
                                                   resolveArgs(lookup, bootstrapArgs),
                                                   // TODO pass lookup
                                                   lookup.lookupClass());
    }

    @Override
    public Optional<? extends ConstantRef<? super ConstantRef<T>>> toConstantRef(MethodHandles.Lookup lookup) {
        try {
            ConstantRef<?>[] args = new ConstantRef<?>[bootstrapArgs.length + 1];
            args[0] = DynamicConstantRef.ofInvoke(ConstantRefs.MHR_DYNAMICCONSTANTREF_FACTORY, CR_DynamicConstantRef,
                                          bootstrapMethod.toConstantRef().orElseThrow(),
                                          name, type.toConstantRef().orElseThrow());
            for (int i=0; i<bootstrapArgs.length; i++)
                args[i+1] = ((Constable<?>) bootstrapArgs[i]).toConstantRef(lookup).orElseThrow();
            return Optional.of(DynamicConstantRef.ofInvoke(ConstantRefs.MHR_DYNAMICCONSTANTREF_WITHARGS, CR_DynamicConstantRef, args));
        }
        catch (NoSuchElementException e) {
            return Optional.empty();
        }
    }

    /**
     * Produce a {@linkplain DynamicConstantRef} describing the invocation of
     * the specified bootstrap with the specified arguments.
     *
     * @param bootstrap symbolic reference for the bootstrap method
     * @param type symbolic reference for the type of the resulting constant
     * @param args symbolic references for the bootstrap arguments
     * @param <T> the type of the resulting constant
     * @return the dynamic constant reference
     */
    protected static<T> DynamicConstantRef<T> ofInvoke(MethodHandleRef bootstrap,
                                                       ClassRef type,
                                                       ConstantRef<?>... args) {
        ConstantRef<?>[] quotedArgs = new ConstantRef<?>[args.length + 1];
        quotedArgs[0] = bootstrap;
        System.arraycopy(args, 0, quotedArgs, 1, args.length);
        return DynamicConstantRef.of(ConstantRefs.BSM_INVOKE, "_", type, quotedArgs);
    }

    /**
     * Produce an {@code Optional<DynamicConstantRef<T>>} describing the invocation
     * of the specified bootstrap with the specified arguments.  The arguments will
     * be converted to symbolic references using the provided lookup.
     *
     * @param lookup A {@link MethodHandles.Lookup} to be used to perform
     *               access control determinations
     * @param bootstrap symbolic reference for the bootstrap method
     * @param type symbolic reference for the type of the resulting constant
     * @param args symbolic references for the bootstrap arguments
     * @param <T> the type of the resulting constant
     * @return the dynamic constant reference
     */
    static<T> Optional<DynamicConstantRef<T>> symbolizeHelper(MethodHandles.Lookup lookup,
                                                              MethodHandleRef bootstrap,
                                                              ClassRef type,
                                                              Constable<?>... args) {
        try {
            ConstantRef<?>[] quotedArgs = new ConstantRef<?>[args.length + 1];
            quotedArgs[0] = bootstrap;
            for (int i=0; i<args.length; i++)
                quotedArgs[i+1] = args[i].toConstantRef(lookup).orElseThrow();
            return Optional.of(DynamicConstantRef.of(ConstantRefs.BSM_INVOKE, "_", type, quotedArgs));
        }
        catch (NoSuchElementException e) {
            return Optional.empty();
        }
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
        // @@@ Too verbose.  Prefer something like DynamicRef[Foo::bootstrap(name/static args)type]
        return String.format("DynamicConstantRef[%s(%s), NameAndType[%s:%s]]",
                             bootstrapMethod, Arrays.toString(bootstrapArgs), name, type.simpleName());
    }

    private static final class DynamicConstantRefImpl<T> extends DynamicConstantRef<T> {
        public DynamicConstantRefImpl(MethodHandleRef bootstrapMethod, String name, ClassRef type, ConstantRef<?>... bootstrapArgs) {
            super(bootstrapMethod, name, type, bootstrapArgs);
        }
    }
}
