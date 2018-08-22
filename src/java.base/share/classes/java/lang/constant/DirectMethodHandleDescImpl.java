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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.Optional;

import static java.lang.constant.ConstantDescs.BSM_METHODHANDLEDESC;
import static java.lang.constant.ConstantDescs.CR_DirectMethodHandleDesc;
import static java.lang.constant.ConstantDescs.DEFAULT_NAME;
import static java.lang.constant.ConstantUtils.validateClassOrInterface;
import static java.lang.constant.ConstantUtils.validateMemberName;
import static java.lang.constant.DirectMethodHandleDesc.Kind.CONSTRUCTOR;
import static java.util.Objects.requireNonNull;

/**
 * A <a href="package-summary.html#nominal">nominal descriptor</a> for a direct
 * {@link MethodHandle}.  A {@linkplain DirectMethodHandleDescImpl} corresponds to
 * a {@code Constant_MethodHandle_info} entry in the constant pool of a classfile.
 */
final class DirectMethodHandleDescImpl implements DirectMethodHandleDesc {

    private final Kind kind;
    private final ClassDesc owner;
    private final String name;
    private final MethodTypeDesc type;

    /**
     * Construct a {@linkplain DirectMethodHandleDescImpl} for a method or field
     * from a kind, owner, name, and type
     *
     * @param kind the kind of the method handle
     * @param owner the declaring class or interface for the method
     * @param name the name of the method (ignored if {@code kind} is
     * {@code CONSTRUCTOR}), as per JVMS 4.2.2
     * @param type the type of the method
     * @throws NullPointerException if any non-ignored argument is null
     * @throws IllegalArgumentException if {@code kind} describes a field accessor,
     * and {@code type} is not consistent with that kind of field accessor, or if
     * {@code kind} describes a constructor, and the return type of {@code type}
     * is not {@code void}
     * @jvms 4.2.2 Unqualified Names
     */
    DirectMethodHandleDescImpl(Kind kind, ClassDesc owner, String name, MethodTypeDesc type) {
        if (kind == CONSTRUCTOR)
            name = "<init>";

        requireNonNull(kind);
        validateClassOrInterface(requireNonNull(owner));
        validateMemberName(requireNonNull(name));
        requireNonNull(type);

        switch (kind) {
            case CONSTRUCTOR: validateConstructor(type); break;
            case GETTER: validateFieldType(type, false, true); break;
            case SETTER: validateFieldType(type, true, true); break;
            case STATIC_GETTER: validateFieldType(type, false, false); break;
            case STATIC_SETTER: validateFieldType(type, true, false); break;
        }

        this.kind = kind;
        this.owner = owner;
        this.name = name;
        this.type = type;
    }

    private static void validateFieldType(MethodTypeDesc type, boolean isSetter, boolean isVirtual) {
        boolean isVoid = type.returnType().descriptorString().equals("V");
        int expectedParams = (isSetter ? 1 : 0) + (isVirtual ? 1 : 0);
        if (isVoid != isSetter
            || type.parameterCount() != expectedParams
            || (isVirtual && type.parameterType(0).isPrimitive())) {
            String expectedType = String.format("(%s%s)%s", (isVirtual ? "R" : ""),
                                                (isSetter ? "T" : ""), (isSetter ? "V" : "T"));
            throw new IllegalArgumentException(String.format("Expected type of %s for getter, found %s", expectedType, type));
        }
    }

    private static void validateConstructor(MethodTypeDesc type) {
        if (!type.returnType().descriptorString().equals("V")) {
            throw new IllegalArgumentException(String.format("Expected type of (T*)V for constructor, found %s", type));
        }
    }

    @Override
    public Kind kind() { return kind; }

    @Override
    public int refKind() { return kind.refKind; }

    @Override
    public boolean isOwnerInterface() { return kind.isInterface; }

    @Override
    public ClassDesc owner() {
        return owner;
    }

    @Override
    public String methodName() {
        return name;
    }

    @Override
    public MethodTypeDesc methodType() {
        return type;
    }

    public MethodHandle resolveConstantDesc(MethodHandles.Lookup lookup)
            throws ReflectiveOperationException {
        Class<?> resolvedOwner = owner.resolveConstantDesc(lookup);
        MethodType resolvedType = this.type.resolveConstantDesc(lookup);
        switch (kind) {
            case STATIC:
            case INTERFACE_STATIC:
                return lookup.findStatic(resolvedOwner, name, resolvedType);
            case INTERFACE_VIRTUAL:
            case VIRTUAL:
                return lookup.findVirtual(resolvedOwner, name, resolvedType);
            case SPECIAL:
            case INTERFACE_SPECIAL:
                return lookup.findSpecial(resolvedOwner, name, resolvedType, lookup.lookupClass());
            case CONSTRUCTOR:
                return lookup.findConstructor(resolvedOwner, resolvedType);
            case GETTER:
                return lookup.findGetter(resolvedOwner, name, resolvedType.returnType());
            case STATIC_GETTER:
                return lookup.findStaticGetter(resolvedOwner, name, resolvedType.returnType());
            case SETTER:
                return lookup.findSetter(resolvedOwner, name, resolvedType.parameterType(1));
            case STATIC_SETTER:
                return lookup.findStaticSetter(resolvedOwner, name, resolvedType.parameterType(0));
            default:
                throw new IllegalStateException(kind.name());
        }
    }

    @Override
    public Optional<? extends ConstantDesc<ConstantDesc<MethodHandle>>> describeConstable() {
        return Optional.of(DynamicConstantDesc.of(BSM_METHODHANDLEDESC,
                                                  kind.toString(),
                                                  owner.descriptorString(),
                                                  name,
                                                  type.descriptorString()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DirectMethodHandleDescImpl desc = (DirectMethodHandleDescImpl) o;
        return kind == desc.kind &&
               Objects.equals(owner, desc.owner) &&
               Objects.equals(name, desc.name) &&
               Objects.equals(type, desc.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, owner, name, type);
    }

    @Override
    public String toString() {
        return String.format("MethodHandleDesc[%s/%s::%s%s]", kind, owner.displayName(), name, type.displayDescriptor());
    }
}
