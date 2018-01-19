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
package java.lang.sym;

import java.lang.annotation.Foldable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * A descriptor for an {@code invokedynamic} invocation
 */
@SuppressWarnings("rawtypes")
public final class IndyRef implements SymbolicRef {
    private static final SymbolicRef<?>[] EMPTY_ARGS = new SymbolicRef<?>[0];

    private final MethodHandleRef bootstrapMethod;
    private final SymbolicRef<?>[] bootstrapArgs;
    private final String name;
    private final MethodTypeRef type;

    private IndyRef(MethodHandleRef bootstrapMethod,
                    String name,
                    MethodTypeRef type,
                    SymbolicRef<?>[] bootstrapArgs) {
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
        this.bootstrapMethod = Objects.requireNonNull(bootstrapMethod);
        this.bootstrapArgs = Objects.requireNonNull(bootstrapArgs.clone());
    }

    /**
     * Return a descriptor for an invokedynamic.  If  the bootstrap corresponds
     * to a well-known bootstrap, for which a higher-level symbolic ref
     * is available, then the higher-level ref will be returned
     * @param bootstrapMethod the bootstrap method
     * @param name the name for the invokedynamic
     * @param type the type of the invokedynamic
     * @param bootstrapArgs the bootstrap arguments
     * @return the descriptor for the invokedynamic
     */
    @Foldable
    public static IndyRef ofCanonical(MethodHandleRef bootstrapMethod, String name, MethodTypeRef type, SymbolicRef<?>... bootstrapArgs) {
        IndyRef ref = new IndyRef(bootstrapMethod, name, type, bootstrapArgs);
        return ref.canonicalize();
    }

    /**
     * Return a descriptor for an invokedynamic
     * @param bootstrapMethod the bootstrap method for the {@code invokedynamic}
     * @param name the name for the invokedynamic
     * @param type the type of the invokedynamic
     * @param bootstrapArgs the bootstrap arguments
     * @return the descriptor for the invokedynamic
     */
    @Foldable
    public static IndyRef of(MethodHandleRef bootstrapMethod, String name, MethodTypeRef type, SymbolicRef<?>... bootstrapArgs) {
        return new IndyRef(bootstrapMethod, name, type, bootstrapArgs);
    }

    /**
     * Return a descriptor for an invokedynamic whose bootstrap has no static arguments.
     * @param bootstrapMethod the bootstrap method for the {@code invokedynamic}
     * @param name the name for the invokedynamic
     * @param type the type of the invokedynamic
     * @return the descriptor for the invokedynamic
     */
    @Foldable
    public static IndyRef of(MethodHandleRef bootstrapMethod, String name, MethodTypeRef type) {
        return new IndyRef(bootstrapMethod, name, type, EMPTY_ARGS);
    }

    /**
     * Return a descriptor for an invokedynamic whose bootstrap has no static arguments,
     * and whose name is ignored by the bootstrap
     * @param bootstrapMethod the bootstrap method for the {@code invokedynamic}
     * @param type the type of the invokedynamic
     * @return the descriptor for the invokedynamic
     */
    @Foldable
    public static IndyRef of(MethodHandleRef bootstrapMethod, MethodTypeRef type) {
        return of(bootstrapMethod, "_", type);
    }

    /**
     * Return a descriptor for an invokedynamic whose bootstrap, invocation
     * name, and invocation type are the same as this one, but with the specified
     * bootstrap arguments
     *
     * @param bootstrapArgs the bootstrap arguments
     * @return the descriptor for the invokedynamic
     */
    @Foldable
    public IndyRef withArgs(SymbolicRef<?>... bootstrapArgs) {
        return new IndyRef(bootstrapMethod, name, type, bootstrapArgs);
    }

    /**
     * Return a descriptor for an invokedynamic whose bootstrap and arguments
     * are the same as this one, but with the specified name and type
     *
     * @param name the name for the invokedynamic
     * @param type the type for the invokedyanmic
     * @return the descriptor for the invokedynamic
     */
    @Foldable
    public IndyRef withNameAndType(String name, MethodTypeRef type) {
        return new IndyRef(bootstrapMethod, name, type, bootstrapArgs);
    }

    private IndyRef canonicalize() {
        // @@@ MethodRef
        return this;
    }

    /**
     * returns the name
     * @return the name
     */
    @Foldable
    public String name() {
        return name;
    }

    /**
     * returns the type
     * @return the type
     */
    @Foldable
    public MethodTypeRef type() {
        return type;
    }

    /**
     * Returns the bootstrap method for the {@code invokedynamic}
     * @return the bootstrap method for the {@code invokedynamic}
     */
    @Foldable
    public MethodHandleRef bootstrapMethod() { return bootstrapMethod; }

    /**
     * Returns the bootstrap arguments for the {@code invokedynamic}
     * @return the bootstrap arguments for the {@code invokedynamic}
     */
    public SymbolicRef<?>[] bootstrapArgs() { return bootstrapArgs.clone(); }

    public MethodHandle dynamicInvoker(MethodHandles.Lookup lookup) throws ReflectiveOperationException {
        // @@@ resolve BSM, adapt as appropriate, figure out how to invoke, invoke, get call site, ask for dynamic invoker
        return null;
    }

    @Override
    public Object resolveRef(MethodHandles.Lookup lookup) throws ReflectiveOperationException {
        throw new UnsupportedOperationException("IndyRef");
    }

    @Override
    public Optional<? extends SymbolicRef<?>> toSymbolicRef(MethodHandles.Lookup lookup) {
        SymbolicRef<?>[] args = new SymbolicRef<?>[bootstrapArgs.length + 4];
        args[0] = SymbolicRefs.MHR_INDYREF_FACTORY;
        args[1] = bootstrapMethod;
        args[2] = name;
        args[3] = type;
        System.arraycopy(bootstrapArgs, 0, args, 4, bootstrapArgs.length);
        return Optional.of(DynamicConstantRef.of(SymbolicRefs.BSM_INVOKE).withArgs(args));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndyRef specifier = (IndyRef) o;
        return Objects.equals(bootstrapMethod, specifier.bootstrapMethod) &&
               Arrays.equals(bootstrapArgs, specifier.bootstrapArgs) &&
               Objects.equals(name, specifier.name) &&
               Objects.equals(type, specifier.type);
    }

    @Override
    public int hashCode() {

        int result = Objects.hash(bootstrapMethod, name, type);
        result = 31 * result + Arrays.hashCode(bootstrapArgs);
        return result;
    }

    @Override
    public String toString() {
        return String.format("IndyRef[%s(%s) %s%s]", bootstrapMethod, Arrays.toString(bootstrapArgs), name, type);
    }
}
