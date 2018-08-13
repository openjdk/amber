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
package java.lang.constant;

import java.lang.Enum.EnumDesc;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.invoke.VarHandle.VarHandleDesc;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import jdk.internal.lang.annotation.Foldable;

import static java.lang.constant.ConstantDescs.BSM_DYNAMICCONSTANTDESC;
import static java.lang.constant.ConstantDescs.CR_Class;
import static java.lang.constant.ConstantDescs.CR_DynamicConstantDesc;
import static java.lang.constant.ConstantDescs.CR_VarHandle;
import static java.lang.constant.ConstantUtils.validateMemberName;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * A <a href="package-summary.html#nominal">nominal descriptor</a> for a
 * dynamic constant (one described in the constant pool with
 * {@code Constant_Dynamic_info}.)
 *
 * <p>Concrete subtypes of {@linkplain DynamicConstantDesc} must be
 * <a href="../doc-files/ValueBased.html">value-based</a>.
 *
 * @param <T> the type of the dynamic constant
 */
public abstract class DynamicConstantDesc<T>
        implements ConstantDesc<T>, Constable<ConstantDesc<T>> {

    private final DirectMethodHandleDesc bootstrapMethod;
    private final ConstantDesc<?>[] bootstrapArgs;
    private final String constantName;
    private final ClassDesc constantType;

    private static final Map<MethodHandleDesc, Function<DynamicConstantDesc<?>, ConstantDesc<?>>> canonicalMap
            = Map.ofEntries(Map.entry(ConstantDescs.BSM_PRIMITIVE_CLASS, DynamicConstantDesc::canonicalizePrimitiveClass),
                            Map.entry(ConstantDescs.BSM_ENUM_CONSTANT, DynamicConstantDesc::canonicalizeEnum),
                            Map.entry(ConstantDescs.BSM_NULL_CONSTANT, DynamicConstantDesc::canonicalizeNull),
                            Map.entry(ConstantDescs.BSM_VARHANDLE_STATIC_FIELD, DynamicConstantDesc::canonicalizeStaticFieldVarHandle),
                            Map.entry(ConstantDescs.BSM_VARHANDLE_FIELD, DynamicConstantDesc::canonicalizeFieldVarHandle),
                            Map.entry(ConstantDescs.BSM_VARHANDLE_ARRAY, DynamicConstantDesc::canonicalizeArrayVarHandle)
    );

    /**
     * Create a nominal descriptor for a dynamic constant.
     *
     * @param bootstrapMethod a {@link DirectMethodHandleDescImpl} describing the
     *                        bootstrap method for the constant
     * @param constantName The name that would appear in the {@code NameAndType}
     *                     operand of the {@code LDC} for this constant, as per
     *                     JVMS 4.2.2
     * @param constantType a {@link DirectMethodHandleDescImpl} describing the type
     *                     that would appear in the {@code NameAndType} operand
     *                     of the {@code LDC} for this constant
     * @param bootstrapArgs {@link ConstantDesc}s describing the static arguments
     *                      to the bootstrap, that would appear in the
     *                      {@code BootstrapMethods} attribute
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if the {@code name} has the incorrect
     * format
     * @jvms 4.2.2 Unqualified Names
     */
    protected DynamicConstantDesc(DirectMethodHandleDesc bootstrapMethod,
                                  String constantName,
                                  ClassDesc constantType,
                                  ConstantDesc<?>... bootstrapArgs) {
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
     * bootstrap arguments.
     *
     * @param bootstrapArgs {@link ConstantDesc}s describing the static arguments
     *                      to the bootstrap, that would appear in the
     *                      {@code BootstrapMethods} attribute
     * @return the nominal descriptor
     * @throws NullPointerException if any argument is null
     */
    @Foldable
    public DynamicConstantDesc<T> withArgs(ConstantDesc<?>... bootstrapArgs) {
        return DynamicConstantDesc.of(bootstrapMethod, constantName, constantType, bootstrapArgs);
    }

    /**
     * Return a nominal descriptor for a dynamic constant, transforming it into
     * a more specific type if the constant bootstrap is a well-known one and a
     * more specific nominal descriptor type (e.g., ClassDesc) is available.
     *
     * <p>Classes whose {@link Constable#describeConstable()} method produces
     * a {@linkplain DynamicConstantDesc} with a well-known bootstrap including
     * {@link Class} (for instances describing primitive types), {@link Enum},
     * and {@link VarHandle}.
     *
     * <p>Bytecode-reading APIs that process the constant pool and wish to expose
     * entries as {@link ConstantDesc} to their callers should generally use this
     * method in preference to {@link #of(DirectMethodHandleDesc, String, ClassDesc, ConstantDesc[])}
     * because this may result in a more specific type that can be provided to
     * callers.
     *
     * @param <T> the type of the dynamic constant
     * @param bootstrapMethod a {@link DirectMethodHandleDescImpl} describing the
     *                        bootstrap method for the constant
     * @param constantName The name that would appear in the {@code NameAndType}
     *                     operand of the {@code LDC} for this constant, as per
     *                     JVMS 4.2.2
     * @param constantType a {@link DirectMethodHandleDescImpl} describing the type
     *                     that would appear in the {@code NameAndType} operand
     *                     of the {@code LDC} for this constant
     * @param bootstrapArgs {@link ConstantDesc}s describing the static arguments
     *                      to the bootstrap, that would appear in the
     *                      {@code BootstrapMethods} attribute
     * @return the nominal descriptor
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if the {@code name} has the incorrect
     * format
     * @jvms 4.2.2 Unqualified Names
     */
    @Foldable
    public static<T> ConstantDesc<T> ofCanonical(DirectMethodHandleDesc bootstrapMethod,
                                                 String constantName,
                                                 ClassDesc constantType,
                                                 ConstantDesc<?>[] bootstrapArgs) {
        return DynamicConstantDesc.<T>of(bootstrapMethod, constantName, constantType, bootstrapArgs)
                .tryCanonicalize();
    }

    /**
     * Return a nominal descriptor for a dynamic constant.
     *
     * @param <T> the type of the dynamic constant
     * @param bootstrapMethod a {@link DirectMethodHandleDescImpl} describing the
     *                        bootstrap method for the constant
     * @param constantName The name that would appear in the {@code NameAndType}
     *                     operand of the {@code LDC} for this constant, as per
     *                     JVMS 4.2.2
     * @param constantType a {@link DirectMethodHandleDescImpl} describing the type
     *                     that would appear in the {@code NameAndType} operand
     *                     of the {@code LDC} for this constant
     * @param bootstrapArgs {@link ConstantDesc}s describing the static arguments
     *                      to the bootstrap, that would appear in the
     *                      {@code BootstrapMethods} attribute
     * @return the nominal descriptor
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if the {@code name} has the incorrect
     * format
     * @jvms 4.2.2 Unqualified Names
     */
    @Foldable
    public static<T> DynamicConstantDesc<T> of(DirectMethodHandleDesc bootstrapMethod,
                                               String constantName,
                                               ClassDesc constantType,
                                               ConstantDesc<?>[] bootstrapArgs) {
        return new DynamicConstantDesc<>(bootstrapMethod, constantName, constantType, bootstrapArgs) {};
    }

    /**
     * Return a nominal descriptor for a dynamic constant whose bootstrap has
     * no static arguments.
     *
     * @param <T> the type of the dynamic constant
     * @param bootstrapMethod a {@link DirectMethodHandleDescImpl} describing the
     *                        bootstrap method for the constant
     * @param constantName The name that would appear in the {@code NameAndType}
     *                     operand of the {@code LDC} for this constant, as per
     *                     JVMS 4.2.2
     * @param constantType a {@link DirectMethodHandleDescImpl} describing the type
     *                     that would appear in the {@code NameAndType} operand
     *                     of the {@code LDC} for this constant
     * @return the nominal descriptor
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if the {@code name} has the incorrect
     * format
     * @jvms 4.2.2 Unqualified Names
     */
    @Foldable
    public static<T> DynamicConstantDesc<T> of(DirectMethodHandleDesc bootstrapMethod,
                                               String constantName,
                                               ClassDesc constantType) {
        return DynamicConstantDesc.of(bootstrapMethod, constantName, constantType, ConstantUtils.EMPTY_CONSTANTDESC);
    }

    /**
     * Return a nominal descriptor for a dynamic constant whose bootstrap has
     * no static arguments, and for which the name parameter
     * is {@link ConstantDescs#DEFAULT_NAME}.
     *
     * @param <T> the type of the dynamic constant
     * @param bootstrapMethod a {@link DirectMethodHandleDescImpl} describing the
     *                        bootstrap method for the constant
     * @param constantType a {@link DirectMethodHandleDescImpl} describing the type
     *                     that would appear in the {@code NameAndType} operand
     *                     of the {@code LDC} for this constant
     * @return the nominal descriptor
     * @throws NullPointerException if any argument is null
     */
    @Foldable
    public static<T> DynamicConstantDesc<T> of(DirectMethodHandleDesc bootstrapMethod,
                                               ClassDesc constantType) {
        return of(bootstrapMethod, ConstantDescs.DEFAULT_NAME, constantType);
    }

    /**
     * Return a nominal descriptor for a dynamic constant whose bootstrap has
     * no static arguments, and whose type parameter is always the same as the
     * bootstrap method return type.
     *
     * @param <T> the type of the dynamic constant
     * @param bootstrapMethod a {@link DirectMethodHandleDescImpl} describing the
     *                        bootstrap method for the constant
     * @param constantName The name that would appear in the {@code NameAndType}
     *                     operand of the {@code LDC} for this constant, as per
     *                     JVMS 4.2.2
     * @return the nominal descriptor
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if the {@code name} has the incorrect
     * format
     * @jvms 4.2.2 Unqualified Names
     */
    @Foldable
    public static<T> DynamicConstantDesc<T> of(DirectMethodHandleDesc bootstrapMethod,
                                               String constantName) {
        return of(bootstrapMethod, constantName, bootstrapMethod.methodType().returnType());
    }

    /**
     * Return a nominal descriptor for a dynamic constant whose bootstrap has
     * no static arguments, whose name parameter is {@link ConstantDescs#DEFAULT_NAME},
     * and whose type parameter is always the same as the bootstrap method return type.
     *
     * @param <T> the type of the dynamic constant
     * @param bootstrapMethod a {@link DirectMethodHandleDescImpl} describing the
     *                        bootstrap method for the constant
     * @return the nominal descriptor
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if the {@code name} has the incorrect
     * format
     */
    @Foldable
    public static<T> DynamicConstantDesc<T> of(DirectMethodHandleDesc bootstrapMethod) {
        return of(bootstrapMethod, ConstantDescs.DEFAULT_NAME);
    }

    /**
     * Returns The name that would appear in the {@code NameAndType} operand
     *             of the {@code LDC} for this constant
     *
     * @return the constant name
     */
    @Foldable
    public String constantName() {
        return constantName;
    }

    /**
     * Returns a {@link ClassDesc} describing the type that would appear in the
     * {@code NameAndType} operand of the {@code LDC} for this constant.
     *
     * @return the constant type
     */
    @Foldable
    public ClassDesc constantType() {
        return constantType;
    }

    /**
     * Returns a {@link MethodHandleDesc} describing the bootstrap method for
     * this constant
     *
     * @return the bootstrap method
     */
    @Foldable
    public DirectMethodHandleDesc bootstrapMethod() {
        return bootstrapMethod;
    }

    /**
     * Returns the bootstrap arguments for this constant
     * @return the bootstrap arguments
     */
    public ConstantDesc<?>[] bootstrapArgs() {
        return bootstrapArgs.clone();
    }

    /**
     * Returns the bootstrap arguments for this constant as a {@link List}
     *
     * @return a {@link List} of the bootstrap arguments, described as {@link ConstantDesc}
     */
    public List<ConstantDesc<?>> bootstrapArgsList() {
        return List.of(bootstrapArgs);
    }

    @SuppressWarnings("unchecked")
    public T resolveConstantDesc(MethodHandles.Lookup lookup) throws ReflectiveOperationException {
        // TODO replace with public supported method
        try {
            MethodHandle bsm = bootstrapMethod.resolveConstantDesc(lookup);
            if (bsm.type().parameterCount() < 2 ||
                !MethodHandles.Lookup.class.isAssignableFrom(bsm.type().parameterType(0))) {
                throw new BootstrapMethodError(
                        "Invalid bootstrap method declared for resolving a dynamic constant: " + bootstrapMethod);
            }
            Object[] bsmArgs = new Object[3 + bootstrapArgs.length];
            bsmArgs[0] = lookup;
            bsmArgs[1] = constantName;
            bsmArgs[2] = constantType.resolveConstantDesc(lookup);
            for (int i = 0; i < bootstrapArgs.length; i++)
                bsmArgs[3 + i] = bootstrapArgs[i].resolveConstantDesc(lookup);

            return (T) bsm.invokeWithArguments(bsmArgs);
        } catch (Error e) {
            throw e;
        } catch (Throwable t) {
            throw new BootstrapMethodError(t);
        }
    }

    @Override
    public Optional<? extends ConstantDesc<ConstantDesc<T>>> describeConstable() {
        ConstantDesc<?>[] args =new ConstantDesc<?>[bootstrapArgs.length +5];
        args[0] =bootstrapMethod.owner().descriptorString();
        args[1] =bootstrapMethod.methodName();
        args[2] =bootstrapMethod.methodType().descriptorString();
        args[3] =constantName;
        args[4] =constantType.descriptorString();
        for (int i=0; i<bootstrapArgs.length; i++)
            args[i+5] = (ConstantDesc<?>) ((Constable)bootstrapArgs[i]).describeConstable().orElseThrow();
        return Optional.of(DynamicConstantDesc.of(BSM_DYNAMICCONSTANTDESC, ConstantDescs.DEFAULT_NAME,
                                                  CR_DynamicConstantDesc, args));
    }

    /**
     * Constant bootstrap method for representing a {@linkplain DynamicConstantDesc}
     * in the constant pool of a classfile.
     *
     * @param lookup ignored
     * @param name ignored
     * @param clazz ignored
     * @param bsmOwner A field type descriptor for the class declaring the
     *                 bootstrap method, as per JVMS 4.3.2
     * @param bsmName The name of the bootstrap method, as per JVMS 4.2.2
     * @param bsmDesc A method type descriptor for bootstrap method, as per JVMS 4.3.3
     * @param constantName The name that would appear in the {@code NameAndType}
     *                     operand of the {@code LDC} for this constant, as per
     *                     JVMS 4.2.2
     * @param constantType a field type descriptor string describing the type
     *                     that would appear in the {@code NameAndType} operand
     *                     of the {@code LDC} for this constant, as per JVMS 4.3.2
     * @param args The static arguments to the bootstrap method
     * @return the {@linkplain DynamicConstantDesc}
     * @jvms 4.2.2 Unqualified Names
     * @jvms 4.3.2 Field Descriptors
     * @jvms 4.3.3 Method Descriptors
     */
    public static DynamicConstantDesc<?> constantBootstrap(MethodHandles.Lookup lookup, String name, Class<ClassDesc> clazz,
                                                           String bsmOwner, String bsmName, String bsmDesc,
                                                           String constantName, String constantType,
                                                           ConstantDesc<?>... args) {
        return DynamicConstantDesc.of(MethodHandleDesc.of(DirectMethodHandleDesc.Kind.STATIC,
                                                          ClassDesc.ofDescriptor(bsmOwner), bsmName,
                                                          MethodTypeDesc.ofDescriptor(bsmDesc)),
                                      constantName, ClassDesc.ofDescriptor(constantType), args);

    }

    private ConstantDesc<T> tryCanonicalize() {
        Function<DynamicConstantDesc<?>, ConstantDesc<?>> f = canonicalMap.get(bootstrapMethod);
        if (f != null) {
            try {
                @SuppressWarnings("unchecked")
                ConstantDesc<T> converted = (ConstantDesc<T>) f.apply(this);
                return converted;
            }
            catch (Throwable t) {
                return this;
            }
        }
        return this;
    }

    private static ConstantDesc<?> canonicalizeNull(DynamicConstantDesc<?> desc) {
        if (desc.bootstrapArgs.length != 0)
            return desc;
        return ConstantDescs.NULL;
    }

    private static ConstantDesc<?> canonicalizeEnum(DynamicConstantDesc<?> desc) {
        if (desc.bootstrapArgs.length != 0
            || desc.constantName == null)
            return desc;
        return EnumDesc.of(desc.constantType, desc.constantName);
    }

    private static ConstantDesc<?> canonicalizePrimitiveClass(DynamicConstantDesc<?> desc) {
        if (desc.bootstrapArgs.length != 0
            || !desc.constantType().equals(CR_Class)
            || desc.constantName == null)
            return desc;
        return ClassDesc.ofDescriptor(desc.constantName);
    }

    private static ConstantDesc<?> canonicalizeStaticFieldVarHandle(DynamicConstantDesc<?> desc) {
        if (desc.bootstrapArgs.length != 3
            || !desc.constantType().equals(CR_VarHandle))
            return desc;
        return VarHandleDesc.ofStaticField((ClassDesc) desc.bootstrapArgs[0],
                                           (String) desc.bootstrapArgs[1],
                                           (ClassDesc) desc.bootstrapArgs[2]);
    }

    private static ConstantDesc<?> canonicalizeFieldVarHandle(DynamicConstantDesc<?> desc) {
        if (desc.bootstrapArgs.length != 3
            || !desc.constantType().equals(CR_VarHandle))
            return desc;
        return VarHandleDesc.ofField((ClassDesc) desc.bootstrapArgs[0],
                                     (String) desc.bootstrapArgs[1],
                                     (ClassDesc) desc.bootstrapArgs[2]);
    }

    private static ConstantDesc<?> canonicalizeArrayVarHandle(DynamicConstantDesc<?> desc) {
        if (desc.bootstrapArgs.length != 1
            || !desc.constantType().equals(CR_VarHandle))
            return desc;
        return VarHandleDesc.ofArray((ClassDesc) desc.bootstrapArgs[0]);
    }

    // @@@ To eventually support in canonicalization: DCR with BSM=MHR_METHODHANDLEDESC_ASTYPE becomes AsTypeMHDesc

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DynamicConstantDesc)) return false;
        DynamicConstantDesc<?> desc = (DynamicConstantDesc<?>) o;
        return Objects.equals(bootstrapMethod, desc.bootstrapMethod) &&
               Arrays.equals(bootstrapArgs, desc.bootstrapArgs) &&
               Objects.equals(constantName, desc.constantName) &&
               Objects.equals(constantType, desc.constantType);
    }

    @Override
    public final int hashCode() {
        int result = Objects.hash(bootstrapMethod, constantName, constantType);
        result = 31 * result + Arrays.hashCode(bootstrapArgs);
        return result;
    }

    @Override
    public String toString() {
        return String.format("DynamicConstantDesc[%s::%s(%s%s)%s]",
                             bootstrapMethod.owner().displayName(),
                             bootstrapMethod.methodName(),
                             constantName.equals(ConstantDescs.DEFAULT_NAME) ? "" : constantName + "/",
                             Stream.of(bootstrapArgs).map(Object::toString).collect(joining(",")),
                             constantType.displayName());
    }
}
