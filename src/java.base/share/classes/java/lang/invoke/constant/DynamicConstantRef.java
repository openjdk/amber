/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package java.lang.invoke.constant;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import jdk.internal.lang.annotation.Foldable;

import static java.lang.invoke.constant.ConstantRefs.CR_DynamicConstantRef;
import static java.lang.invoke.constant.ConstantUtils.validateMemberName;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * A nominal descriptor for a dynamic constant (one described in the constant
 * pool with {@code Constant_Dynamic_info}.)
 *
 * <p>Concrete subtypes of {@linkplain DynamicConstantRef} must be
 * <a href="../doc-files/ValueBased.html">value-based</a>.
 *
 * @param <T> the type of the dynamic constant
 */
public abstract class DynamicConstantRef<T> implements ConstantRef<T>, Constable<ConstantRef<T>> {

    private final ConstantMethodHandleRef bootstrapMethod;
    private final ConstantRef<?>[] bootstrapArgs;
    private final String constantName;
    private final ClassRef constantType;

    @SuppressWarnings("rawtypes")
    private static final Map<MethodHandleRef, Function<DynamicConstantRef, ConstantRef<?>>> canonicalMap
            = Map.ofEntries(Map.entry(ConstantRefs.BSM_PRIMITIVE_CLASS, d -> ClassRef.ofDescriptor(d.constantName)),
                            Map.entry(ConstantRefs.BSM_ENUM_CONSTANT, d -> EnumRef.of(d.constantType, d.constantName)),
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
     * Construct a nominal descriptor for a dynamic constant
     *
     * @param bootstrapMethod The bootstrap method for the constant
     * @param constantName The name that would appear in the {@code NameAndType}
     *                     operand of the {@code LDC} for this constant
     * @param constantType The type that would appear in the {@code NameAndType}
     *                     operand of the {@code LDC} for this constant
     * @param bootstrapArgs The static arguments to the bootstrap, that would
     *                      appear in the {@code BootstrapMethods} attribute
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if the bootstrap method is not a
     * {@link ConstantMethodHandleRef}
     * @throws IllegalArgumentException if {@code name.length()} is zero
     */
    protected DynamicConstantRef(ConstantMethodHandleRef bootstrapMethod,
                                 String constantName,
                                 ClassRef constantType,
                                 ConstantRef<?>... bootstrapArgs) {
        this.bootstrapMethod = requireNonNull(bootstrapMethod);
        this.constantName = validateMemberName(requireNonNull(constantName));
        this.constantType = requireNonNull(constantType);
        this.bootstrapArgs = requireNonNull(bootstrapArgs).clone();

        if (constantName.length() == 0)
            throw new IllegalArgumentException("Illegal invocation name: " + constantName);
    }

    /**
     * Return a nominal descriptor for a dynamic constant whose bootstrap, invocation
     * name, and invocation type are the same as this one, but with the specified
     * bootstrap arguments
     *
     * @param bootstrapArgs the bootstrap arguments
     * @return the nominal descriptor
     * @throws NullPointerException if any argument is null
     */
    @Foldable
    public DynamicConstantRef<T> withArgs(ConstantRef<?>... bootstrapArgs) {
        return DynamicConstantRef.of(bootstrapMethod, constantName, constantType, bootstrapArgs);
    }

    // TODO Better description needed
    /**
     * Return a nominal descriptor for a dynamic constant.  If the bootstrap
     * corresponds to a well-known bootstrap, for which a more specific nominal
     * descriptor type (e.g., ClassRef) is available, then the more specific
     * nominal descriptor will be returned.
     *
     * @param <T> the type of the dynamic constant
     * @param bootstrapMethod The bootstrap method for the constant
     * @param name The name that would appear in the {@code NameAndType} operand
     *             of the {@code LDC} for this constant
     * @param type The type that would appear in the {@code NameAndType} operand
     *             of the {@code LDC} for this constant
     * @param bootstrapArgs The static arguments to the bootstrap, that would
     *                      appear in the {@code BootstrapMethods} attribute
     * @return the nominal descriptor
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if {@code name.length()} is zero
     */
    @Foldable
    public static<T> ConstantRef<T> ofCanonical(ConstantMethodHandleRef bootstrapMethod,
                                                String name,
                                                ClassRef type,
                                                ConstantRef<?>[] bootstrapArgs) {
        return DynamicConstantRef.<T>of(bootstrapMethod, name, type, bootstrapArgs)
                .canonicalize();
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
     * Return a nominal descriptor for a dynamic constant.
     *
     * @param <T> the type of the dynamic constant
     * @param bootstrapMethod The bootstrap method for the constant
     * @param name The name that would appear in the {@code NameAndType} operand
     *             of the {@code LDC} for this constant
     * @param type The type that would appear in the {@code NameAndType} operand
     *             of the {@code LDC} for this constant
     * @param bootstrapArgs The static arguments to the bootstrap, that would
     *                      appear in the {@code BootstrapMethods} attribute
     * @return the nominal descriptor
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if {@code name.length()} is zero
     */
    @Foldable
    public static<T> DynamicConstantRef<T> of(ConstantMethodHandleRef bootstrapMethod,
                                              String name,
                                              ClassRef type,
                                              ConstantRef<?>[] bootstrapArgs) {
        return new DynamicConstantRef<>(bootstrapMethod, name, type, bootstrapArgs) { };
    }

    /**
     * Return a nominal descriptor for a dynamic constant whose bootstrap has
     * no static arguments.
     *
     * @param <T> the type of the dynamic constant
     * @param bootstrapMethod The bootstrap method for the constant
     * @param name The name that would appear in the {@code NameAndType} operand
     *             of the {@code LDC} for this constant
     * @param type The type that would appear in the {@code NameAndType} operand
     *             of the {@code LDC} for this constant
     * @return the nominal descriptor
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if {@code name.length()} is zero
     */
    @Foldable
    public static<T> DynamicConstantRef<T> of(ConstantMethodHandleRef bootstrapMethod,
                                              String name,
                                              ClassRef type) {
        return DynamicConstantRef.of(bootstrapMethod, name, type, ConstantUtils.EMPTY_CONSTANTREF);
    }

    /**
     * Return a nominal descriptor for a dynamic constant whose bootstrap has
     * no static arguments, and for which the name parameter
     * is {@link ConstantRefs#DEFAULT_NAME}
     *
     * @param <T> the type of the dynamic constant
     * @param bootstrapMethod The bootstrap method for the constant
     * @param type The type that would appear in the {@code NameAndType} operand
     *             of the {@code LDC} for this constant
     * @return the nominal descriptor
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if {@code name.length()} is zero
     */
    @Foldable
    public static<T> DynamicConstantRef<T> of(ConstantMethodHandleRef bootstrapMethod,
                                              ClassRef type) {
        return of(bootstrapMethod, ConstantRefs.DEFAULT_NAME, type);
    }

    /**
     * Return a nominal descriptor for a dynamic constant whose bootstrap has
     * no static arguments, and whose type parameter is always the same as the
     * bootstrap method return type.
     *
     * @param <T> the type of the dynamic constant
     * @param bootstrapMethod The bootstrap method for the constant
     * @param name The name that would appear in the {@code NameAndType} operand
     *             of the {@code LDC} for this constant
     * @return the nominal descriptor
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if {@code name.length()} is zero
     */
    @Foldable
    public static<T> DynamicConstantRef<T> of(ConstantMethodHandleRef bootstrapMethod,
                                              String name) {
        return of(bootstrapMethod, name, bootstrapMethod.methodType().returnType());
    }

    /**
     * Return a nominal descriptor for a dynamic constant whose bootstrap has
     * no static arguments, whose name parameter is {@link ConstantRefs#DEFAULT_NAME},
     * and whose type parameter is always the same as the bootstrap method return type.
     *
     * @param <T> the type of the dynamic constant
     * @param bootstrapMethod The bootstrap method for the constant
     * @return the nominal descriptor
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if {@code name.length()} is zero
     */
    @Foldable
    public static<T> DynamicConstantRef<T> of(ConstantMethodHandleRef bootstrapMethod) {
        return of(bootstrapMethod, ConstantRefs.DEFAULT_NAME);
    }

    /**
     * returns The name that would appear in the {@code NameAndType} operand
     *             of the {@code LDC} for this constant
     * @return the name
     */
    @Foldable
    public String constantName() {
        return constantName;
    }

    /**
     * returns The type that would appear in the {@code NameAndType} operand
     *             of the {@code LDC} for this constant
     * @return the type
     */
    @Foldable
    public ClassRef constantType() {
        return constantType;
    }

    /**
     * Returns the bootstrap method for this constant
     * @return the bootstrap method
     */
    @Foldable
    public ConstantMethodHandleRef bootstrapMethod() {
        return bootstrapMethod;
    }

    /**
     * Returns the bootstrap arguments for this constant
     * @return the bootstrap arguments
     */
    public ConstantRef<?>[] bootstrapArgs() {
        return bootstrapArgs.clone();
    }

    /**
     * Returns the bootstrap arguments for this constant as a {@link List}
     * @return the bootstrap arguments
     */
    public List<ConstantRef<?>> bootstrapArgsList() {
        return List.of(bootstrapArgs);
    }

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
        // TODO replace with public supported method
        try {
            MethodHandle bsm = bootstrapMethod.resolveConstantRef(lookup);
            if (bsm.type().parameterCount() < 2 ||
                !MethodHandles.Lookup.class.isAssignableFrom(bsm.type().parameterType(0))) {
                throw new BootstrapMethodError(
                        "Invalid bootstrap method declared for resolving a dynamic constant: " + bootstrapMethod);
            }
            Object[] staticArgs = resolveArgs(lookup, bootstrapArgs);
            Object[] bsmArgs = new Object[3 + staticArgs.length];
            bsmArgs[0] = lookup;
            bsmArgs[1] = constantName;
            bsmArgs[2] = constantType.resolveConstantRef(lookup);
            System.arraycopy(staticArgs, 0, bsmArgs, 3, staticArgs.length);
            return (T) bsm.invokeWithArguments(bsmArgs);
        } catch (Error e) {
            throw e;
        } catch (Throwable t) {
            throw new BootstrapMethodError(t);
        }
    }

    @Override
    public Optional<? extends ConstantRef<? super ConstantRef<T>>> toConstantRef(MethodHandles.Lookup lookup) {
        ConstantRef<?>[] args = new ConstantRef<?>[bootstrapArgs.length + 5];
        args[0] = bootstrapMethod.owner().descriptorString();
        args[1] = bootstrapMethod.methodName();
        args[2] = bootstrapMethod.methodType().descriptorString();
        args[3] = constantName;
        args[4] = constantType.descriptorString();
        System.arraycopy(bootstrapArgs, 0, args, 5, bootstrapArgs.length);
        return Optional.of(DynamicConstantRef.of(RefBootstraps.BSM_DYNAMICCONSTANTREF, ConstantRefs.DEFAULT_NAME,
                                                 CR_DynamicConstantRef, args));
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DynamicConstantRef)) return false;
        DynamicConstantRef<?> ref = (DynamicConstantRef<?>) o;
        return Objects.equals(bootstrapMethod, ref.bootstrapMethod) &&
               Arrays.equals(bootstrapArgs, ref.bootstrapArgs) &&
               Objects.equals(constantName, ref.constantName) &&
               Objects.equals(constantType, ref.constantType);
    }

    @Override
    public final int hashCode() {
        int result = Objects.hash(bootstrapMethod, constantName, constantType);
        result = 31 * result + Arrays.hashCode(bootstrapArgs);
        return result;
    }

    @Override
    public String toString() {
        return String.format("DynamicConstantRef[%s::%s(%s%s)%s]",
                             bootstrapMethod.owner().displayName(),
                             bootstrapMethod.methodName(),
                             constantName.equals(ConstantRefs.DEFAULT_NAME) ? "" : constantName + "/",
                             Stream.of(bootstrapArgs).map(Object::toString).collect(joining(",")),
                             constantType.displayName());
    }
}
