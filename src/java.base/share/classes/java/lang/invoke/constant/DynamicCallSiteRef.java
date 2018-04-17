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

import jdk.internal.lang.annotation.Foldable;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.invoke.constant.ConstantRefs.CR_String;
import static java.lang.invoke.constant.ConstantUtils.EMPTY_CONSTANTREF;
import static java.lang.invoke.constant.ConstantUtils.validateMemberName;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * A nominal descriptor for an {@code invokedynamic} call site.
 */
@SuppressWarnings("rawtypes")
public final class DynamicCallSiteRef {

    private final ConstantMethodHandleRef bootstrapMethod;
    private final ConstantRef<?>[] bootstrapArgs;
    private final String invocationName;
    private final MethodTypeRef invocationType;

    /**
     * Construct a nominal descriptor for an {@code invokedynamic} call site
     *
     * @param bootstrapMethod The bootstrap method for the {@code invokedynamic}
     * @param invocationName The name that would appear in the {@code NameAndType}
     *                       operand of the {@code invokedynamic}
     * @param invocationType The invocation type that would appear in the
     * {@code NameAndType} operand of the {@code invokedynamic}
     * @param bootstrapArgs The static arguments to the bootstrap, that would
     *                      appear in the {@code BootstrapMethods} attribute
     * @throws NullPointerException if any parameter is null
     * @throws IllegalArgumentException if the bootstrap method is not a
     * {@link ConstantMethodHandleRef}
     * @throws IllegalArgumentException if {@code name.length()} is zero
     */
    private DynamicCallSiteRef(ConstantMethodHandleRef bootstrapMethod,
                               String invocationName,
                               MethodTypeRef invocationType,
                               ConstantRef<?>[] bootstrapArgs) {
        this.invocationName = validateMemberName(requireNonNull(invocationName));
        this.invocationType = requireNonNull(invocationType);
        this.bootstrapMethod = requireNonNull(bootstrapMethod);
        this.bootstrapArgs = requireNonNull(bootstrapArgs.clone());
        if (invocationName.length() == 0)
            throw new IllegalArgumentException("Illegal invocation name: " + invocationName);
    }

    /**
     * Return a nominal descriptor for an {@code invokedynamic} call site.  If
     * the bootstrap method corresponds to a well-known bootstrap, for which a
     * more specific nominal descriptor invocationType exists, the more specific nominal
     * descriptor invocationType is returned.
     *
     * @param bootstrapMethod The bootstrap method for the {@code invokedynamic}
     * @param invocationName The invocationName that would appear in the
     * {@code NameAndType} operand of the {@code invokedynamic}
     * @param invocationType The invocation invocationType that would appear in
     * the {@code NameAndType} operand of the {@code invokedynamic}
     * @param bootstrapArgs The static arguments to the bootstrap, that would
     *                      appear in the {@code BootstrapMethods} attribute
     * @return the nominal descriptor
     * @throws NullPointerException if any parameter is null
     */
    @Foldable
    public static DynamicCallSiteRef ofCanonical(ConstantMethodHandleRef bootstrapMethod,
                                                 String invocationName,
                                                 MethodTypeRef invocationType,
                                                 ConstantRef<?>... bootstrapArgs) {
        DynamicCallSiteRef ref = new DynamicCallSiteRef(bootstrapMethod,
                                                        invocationName, invocationType,
                                                        bootstrapArgs);
        return ref.canonicalize();
    }

    /**
     * Return a nominal descriptor for an {@code invokedynamic} call site.
     *
     * @param bootstrapMethod The bootstrap method for the {@code invokedynamic}
     * @param invocationName The invocationName that would appear in the
     * {@code NameAndType} operand of the {@code invokedynamic}
     * @param invocationType The invocation invocationType that would appear in
     * the {@code NameAndType} operand of the {@code invokedynamic}
     * @param bootstrapArgs The static arguments to the bootstrap, that would
     *                      appear in the {@code BootstrapMethods} attribute
     * @return the nominal descriptor
     * @throws NullPointerException if any parameter is null
     */
    @Foldable
    public static DynamicCallSiteRef of(ConstantMethodHandleRef bootstrapMethod,
                                        String invocationName,
                                        MethodTypeRef invocationType,
                                        ConstantRef<?>... bootstrapArgs) {
        return new DynamicCallSiteRef(bootstrapMethod, invocationName, invocationType, bootstrapArgs);
    }

    /**
     * Return a nominal descriptor for an {@code invokedynamic} call site whose
     * bootstrap method has no static arguments.
     *
     * @param bootstrapMethod The bootstrap method for the {@code invokedynamic}
     * @param invocationName The invocationName that would appear in the
     * {@code NameAndType} operand of the {@code invokedynamic}
     * @param invocationType The invocation invocationType that would appear
     * in the {@code NameAndType} operand of the {@code invokedynamic}
     * @return the nominal descriptor
     * @throws NullPointerException if any parameter is null
     */
    @Foldable
    public static DynamicCallSiteRef of(ConstantMethodHandleRef bootstrapMethod,
                                        String invocationName,
                                        MethodTypeRef invocationType) {
        return new DynamicCallSiteRef(bootstrapMethod, invocationName, invocationType, EMPTY_CONSTANTREF);
    }

    /**
     * Return a nominal descriptor for an {@code invokedynamic} call site whose
     * bootstrap method has no static arguments and for which the name parameter
     * is {@link ConstantRefs#DEFAULT_NAME}.
     *
     * @param bootstrapMethod The bootstrap method for the {@code invokedynamic}
     * @param invocationType The invocation type that would appear in
     * the {@code NameAndType} operand of the {@code invokedynamic}
     * @return the nominal descriptor
     * @throws NullPointerException if any parameter is null
     */
    @Foldable
    public static DynamicCallSiteRef of(ConstantMethodHandleRef bootstrapMethod,
                                        MethodTypeRef invocationType) {
        return of(bootstrapMethod, ConstantRefs.DEFAULT_NAME, invocationType);
    }

    /**
     * Return a nominal descriptor for an {@code invokedynamic} call site whose
     * bootstrap method, name, and invocation type are the same as this one, but
     * with the specified bootstrap arguments.
     *
     * @param bootstrapArgs The static arguments to the bootstrap, that would
     *                      appear in the {@code BootstrapMethods} attribute
     * @return the nominal descriptor
     * @throws NullPointerException if any parameter is null
     */
    @Foldable
    public DynamicCallSiteRef withArgs(ConstantRef<?>... bootstrapArgs) {
        return new DynamicCallSiteRef(bootstrapMethod, invocationName, invocationType, bootstrapArgs);
    }

    /**
     * Return a nominal descriptor for an {@code invokedynamic} call site whose
     * bootstrap and bootstrap arguments are the same as this one, but with the
     * specified invocationName and invocation invocationType
     *
     * @param invocationName The invocationName that would appear in the
     * {@code NameAndType} operand of the {@code invokedynamic}
     * @param invocationType The invocation invocationType that would appear in
     * the {@code NameAndType} operand of the {@code invokedynamic}
     * @return the nominal descriptor
     * @throws NullPointerException if any parameter is null
     */
    @Foldable
    public DynamicCallSiteRef withNameAndType(String invocationName,
                                              MethodTypeRef invocationType) {
        return new DynamicCallSiteRef(bootstrapMethod, invocationName, invocationType, bootstrapArgs);
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
    public String invocationName() {
        return invocationName;
    }

    /**
     * Returns the invocation type that would appear in the {@code NameAndType} operand
     * of the {@code invokedynamic}
     * @return the invocation type
     */
    @Foldable
    public MethodTypeRef invocationType() {
        return invocationType;
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
     * Reflectively invokes the bootstrap method with the specified arguments,
     * and return the resulting {@link CallSite}
     *
     * @param lookup The {@link MethodHandles.Lookup} used to resolve class names
     * @return the {@link CallSite}
     * @throws Throwable if any exception is thrown by the bootstrap method
     */
    public CallSite resolveCallSiteRef(MethodHandles.Lookup lookup) throws Throwable {
        assert bootstrapMethod.methodType().parameterType(1).equals(CR_String);
        MethodHandle bsm = bootstrapMethod.resolveConstantRef(lookup);
        Object[] args = new Object[bootstrapArgs.length + 3];
        args[0] = lookup;
        args[1] = invocationName;
        args[2] = invocationType.resolveConstantRef(lookup);
        System.arraycopy(bootstrapArgs, 0, args, 3, bootstrapArgs.length);
        return (CallSite) bsm.invokeWithArguments(args);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DynamicCallSiteRef specifier = (DynamicCallSiteRef) o;
        return Objects.equals(bootstrapMethod, specifier.bootstrapMethod) &&
               Arrays.equals(bootstrapArgs, specifier.bootstrapArgs) &&
               Objects.equals(invocationName, specifier.invocationName) &&
               Objects.equals(invocationType, specifier.invocationType);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(bootstrapMethod, invocationName, invocationType);
        result = 31 * result + Arrays.hashCode(bootstrapArgs);
        return result;
    }

    @Override
    public String toString() {
        return String.format("DynamicCallSiteRef[%s::%s(%s%s):%s]",
                             bootstrapMethod.owner().displayName(),
                             bootstrapMethod.methodName(),
                             invocationName.equals(ConstantRefs.DEFAULT_NAME) ? "" : invocationName + "/",
                             Stream.of(bootstrapArgs).map(Object::toString).collect(joining(",")),
                             invocationType.displayDescriptor());
    }
}
