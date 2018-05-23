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

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import static java.lang.invoke.constant.ConstantDescs.BSM_ENUMDESC;
import static java.lang.invoke.constant.ConstantDescs.CR_EnumDesc;
import static java.lang.invoke.constant.ConstantUtils.validateMemberName;
import static java.util.Objects.requireNonNull;

/**
 * A <a href="package-summary.html#nominal">nominal descriptor</a> for an
 * {@code enum} constant.
 *
 * @param <E> the type of the enum constant
 */
public final class EnumDesc<E extends Enum<E>>
        extends DynamicConstantDesc<E> {

    /**
     * Construct a nominal descriptor for the specified {@code enum} class and name.
     *
     * @param constantType a {@link ClassDesc} describing the {@code enum} class
     * @param constantName the name of the enum constant, as per JVMS 4.2.2
     * @throws NullPointerException if any argument is null
     */
    private EnumDesc(ClassDesc constantType, String constantName) {
        super(ConstantDescs.BSM_ENUM_CONSTANT, requireNonNull(constantName), requireNonNull(constantType));
    }

    /**
     * Return a nominal descriptor for the specified {@code enum} class and name
     *
     * @param <E> the type of the enum constant
     * @param enumClass a {@link ClassDesc} describing the {@code enum} class
     * @param constantName the name of the enum constant, as per JVMS 4.2.2
     * @return the nominal descriptor
     * @throws NullPointerException if any argument is null
     */
    public static<E extends Enum<E>> EnumDesc<E> of(ClassDesc enumClass,
                                                    String constantName) {
        return new EnumDesc<>(enumClass, validateMemberName(constantName));
    }

    @Override
    @SuppressWarnings("unchecked")
    public E resolveConstantDesc(MethodHandles.Lookup lookup)
            throws ReflectiveOperationException {
        return Enum.valueOf((Class<E>) constantType().resolveConstantDesc(lookup), constantName());
    }

    @Override
    public Optional<? extends ConstantDesc<? super ConstantDesc<E>>> describeConstable() {
        return Optional.of(DynamicConstantDesc.of(BSM_ENUMDESC, CR_EnumDesc)
                                              .withArgs(constantType().descriptorString(), constantName()));
    }

    /**
     * Constant bootstrap method for representing an {@linkplain EnumDesc} in
     * the constant pool of a classfile.
     *
     * @param lookup ignored
     * @param name ignored
     * @param clazz ignored
     * @param classDescriptor A field type descriptor for the enum class, as
     *                        per JVMS 4.3.2
     * @param constantName The name of the {@code enum} constant
     * @return the {@linkplain EnumDesc}
     */
    public static EnumDesc<?> constantBootstrap(MethodHandles.Lookup lookup, String name, Class<ClassDesc> clazz,
                                                String classDescriptor, String constantName) {
        return EnumDesc.of(ClassDesc.ofDescriptor(classDescriptor), constantName);
    }

    @Override
    public String toString() {
        return String.format("EnumDesc[%s.%s]", constantType().displayName(), constantName());
    }
}
