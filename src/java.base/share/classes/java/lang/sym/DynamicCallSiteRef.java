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
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Objects;

import static java.lang.sym.ConstantRefs.CR_String;

/**
 * A nominal reference for an {@code invokedynamic} call site.
 */
@SuppressWarnings("rawtypes")
public final class DynamicCallSiteRef {
    private static final ConstantRef<?>[] EMPTY_ARGS = new ConstantRef<?>[0];

    private final MethodHandleRef bootstrapMethod;
    private final ConstantRef<?>[] bootstrapArgs;
    private final String name;
    private final MethodTypeRef type;

    /**
     * Construct a nominal reference for an {@code invokedynamic} call site
     *
     * @param bootstrapMethod The bootstrap method for the {@code invokedynamic}
     * @param name The name that would appear in the {@code NameAndType} operand
     *             of the {@code invokedynamic}
     * @param type The invocation type that would appear in the {@code NameAndType} operand
     *             of the {@code invokedynamic}
     * @param bootstrapArgs The static arguments to the bootstrap, that would
     *                      appear in the {@code BootstrapMethods} attribute
     * @throws NullPointerException if any parameter is null
     */
    private DynamicCallSiteRef(MethodHandleRef bootstrapMethod,
                               String name,
                               MethodTypeRef type,
                               ConstantRef<?>[] bootstrapArgs) {
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
        this.bootstrapMethod = Objects.requireNonNull(bootstrapMethod);
        this.bootstrapArgs = Objects.requireNonNull(bootstrapArgs.clone());
    }

    /**
     * Return a nominal reference for an {@code invokedynamic} call site.  If
     * the bootstrap method corresponds to a well-known bootstrap, for which a
     * more specific nominal reference type exists, the more specific nominal
     * reference type is returned.
     *
     * @param bootstrapMethod The bootstrap method for the {@code invokedynamic}
     * @param name The name that would appear in the {@code NameAndType} operand
     *             of the {@code invokedynamic}
     * @param type The invocation type that would appear in the {@code NameAndType} operand
     *             of the {@code invokedynamic}
     * @param bootstrapArgs The static arguments to the bootstrap, that would
     *                      appear in the {@code BootstrapMethods} attribute
     * @return the nominal reference
     * @throws NullPointerException if any parameter is null
     */
    @Foldable
    public static DynamicCallSiteRef ofCanonical(MethodHandleRef bootstrapMethod, String name, MethodTypeRef type, ConstantRef<?>... bootstrapArgs) {
        DynamicCallSiteRef ref = new DynamicCallSiteRef(bootstrapMethod, name, type, bootstrapArgs);
        return ref.canonicalize();
    }

    /**
     * Return a nominal reference for an {@code invokedynamic} call site.
     *
     * @param bootstrapMethod The bootstrap method for the {@code invokedynamic}
     * @param name The name that would appear in the {@code NameAndType} operand
     *             of the {@code invokedynamic}
     * @param type The invocation type that would appear in the {@code NameAndType} operand
     *             of the {@code invokedynamic}
     * @param bootstrapArgs The static arguments to the bootstrap, that would
     *                      appear in the {@code BootstrapMethods} attribute
     * @return the nominal reference
     * @throws NullPointerException if any parameter is null
     */
    @Foldable
    public static DynamicCallSiteRef of(MethodHandleRef bootstrapMethod, String name, MethodTypeRef type, ConstantRef<?>... bootstrapArgs) {
        return new DynamicCallSiteRef(bootstrapMethod, name, type, bootstrapArgs);
    }

    /**
     * Return a nominal reference for an {@code invokedynamic} call site whose
     * bootstrap method has no static arguments.
     *
     * @param bootstrapMethod The bootstrap method for the {@code invokedynamic}
     * @param name The name that would appear in the {@code NameAndType} operand
     *             of the {@code invokedynamic}
     * @param type The invocation type that would appear in the {@code NameAndType} operand
     *             of the {@code invokedynamic}
     * @return the nominal reference
     * @throws NullPointerException if any parameter is null
     */
    @Foldable
    public static DynamicCallSiteRef of(MethodHandleRef bootstrapMethod, String name, MethodTypeRef type) {
        return new DynamicCallSiteRef(bootstrapMethod, name, type, EMPTY_ARGS);
    }

    /**
     * Return a nominal reference for an {@code invokedynamic} call site whose
     * bootstrap method has no static arguments and whose name parameter is ignored.
     *
     * @param bootstrapMethod The bootstrap method for the {@code invokedynamic}
     * @param type The invocation type that would appear in the {@code NameAndType} operand
     *             of the {@code invokedynamic}
     * @return the nominal reference
     * @throws NullPointerException if any parameter is null
     */
    @Foldable
    public static DynamicCallSiteRef of(MethodHandleRef bootstrapMethod, MethodTypeRef type) {
        return of(bootstrapMethod, "_", type);
    }

    /**
     * Return a nominal reference for an {@code invokedynamic} call site whose
     * bootstrap method, name, and invocation type are the same as this one, but
     * with the specified bootstrap arguments.
     *
     * @param bootstrapArgs The static arguments to the bootstrap, that would
     *                      appear in the {@code BootstrapMethods} attribute
     * @return the nominal reference
     * @throws NullPointerException if any parameter is null
     */
    @Foldable
    public DynamicCallSiteRef withArgs(ConstantRef<?>... bootstrapArgs) {
        return new DynamicCallSiteRef(bootstrapMethod, name, type, bootstrapArgs);
    }

    /**
     * Return a nominal reference for an {@code invokedynamic} call site whose
     * bootstrap and bootstrap arguments are the same as this one, but with the
     * specified name and invocation type
     *
     * @param name The name that would appear in the {@code NameAndType} operand
     *             of the {@code invokedynamic}
     * @param type The invocation type that would appear in the {@code NameAndType} operand
     *             of the {@code invokedynamic}
     * @return the nominal reference
     * @throws NullPointerException if any parameter is null
     */
    @Foldable
    public DynamicCallSiteRef withNameAndType(String name, MethodTypeRef type) {
        return new DynamicCallSiteRef(bootstrapMethod, name, type, bootstrapArgs);
    }

    private DynamicCallSiteRef canonicalize() {
        // @@@ MethodRef
        return this;
    }

    /**
     * Returns the invocation name that would appear in the {@code NameAndType} operand
     * of the {@code invokedynamic}
     * @return the invocation name
     */
    @Foldable
    public String name() {
        return name;
    }

    /**
     * Returns the invocation type that would appear in the {@code NameAndType} operand
     * of the {@code invokedynamic}
     * @return the invocation type
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
    public ConstantRef<?>[] bootstrapArgs() { return bootstrapArgs.clone(); }

    /**
     * Reflectively invokes the bootstrap method, and returns a dynamic invoker
     * {@link MethodHandle} for the returned {@link CallSite}
     *
     * @param lookup The {@link MethodHandles.Lookup} used to resolve class names
     * @return the dynamic invoker
     * @throws Throwable if any exception is thrown by the bootstrap method
     *
     * @see CallSite#dynamicInvoker()
     */
    public MethodHandle dynamicInvoker(MethodHandles.Lookup lookup) throws Throwable {
        assert bootstrapMethod.methodType().parameterType(1).equals(CR_String);
        MethodHandle bsm = bootstrapMethod.resolveConstantRef(lookup);
        Object[] args = new Object[bootstrapArgs.length + 3];
        args[0] = lookup;
        args[1] = name;
        args[2] = type.resolveConstantRef(lookup);
        System.arraycopy(bootstrapArgs, 0, args, 3, bootstrapArgs.length);
        CallSite callSite = (CallSite) bsm.invokeWithArguments(args);
        return callSite.dynamicInvoker();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DynamicCallSiteRef specifier = (DynamicCallSiteRef) o;
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
        return String.format("DynamicCallSiteRef[%s(%s) %s%s]", bootstrapMethod, Arrays.toString(bootstrapArgs), name, type);
    }
}
