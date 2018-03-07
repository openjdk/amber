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

import jdk.internal.lang.annotation.Foldable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.Optional;

import static java.lang.sym.ConstantRefs.CR_MethodTypeRef;
import static java.lang.sym.MethodHandleRef.Kind.CONSTRUCTOR;
import static java.lang.sym.ConstantRefs.CR_MethodHandleRef;
import static java.util.Objects.requireNonNull;

/**
 * DirectMethodHandleRef
 *
 * @author Brian Goetz
 */
public class ConstantMethodHandleRef implements MethodHandleRef {

    private final Kind kind;
    private final ClassRef owner;
    private final String name;
    private final MethodTypeRef type;

    /**
     * Construct a {@linkplain ConstantMethodHandleRef} from a kind, owner, name, and type
     * @param kind the kind of the method handle
     * @param owner the declaring class for the method
     * @param name the name of the method (ignored if {@code kind} is {@code CONSTRUCTOR})
     * @param type the type of the method
     * @throws NullPointerException if any non-ignored argument is null
     * @throws IllegalArgumentException if {@code kind} describes a field accessor,
     * and {@code type} is not consistent with that kind of field accessor
     */
    ConstantMethodHandleRef(Kind kind, ClassRef owner, String name, MethodTypeRef type) {
        super();
        if (kind == CONSTRUCTOR)
            name = "<init>";

        requireNonNull(kind);
        requireNonNull(owner);
        requireNonNull(name);
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

    private static void validateFieldType(MethodTypeRef type, boolean isSetter, boolean isVirtual) {
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

    private static void validateConstructor(MethodTypeRef type) {
        if (!type.returnType().descriptorString().equals("V")) {
            throw new IllegalArgumentException(String.format("Expected type of (T)V for constructor, found %s", type));
        }
    }

    /**
     * Return the {@code refKind} of the method handle described by this nominal reference,
     * as defined by {@link MethodHandleInfo}
     * @return the reference kind
     */
    @Foldable
    public int refKind() { return kind.refKind; }

    /**
     * Return the {@code kind} of the method handle described by this nominal reference
     * @return the {@link Kind}
     */
    @Foldable
    public Kind kind() { return kind; }

    /**
     * Return the class which declares the method or field described by
     * this nominal reference
     *
     * @return the class in which the method or field is declared
     */
    @Foldable
    public ClassRef owner() {
        return owner;
    }

    /**
     * Return the name of the method described by this nominal reference
     *
     * @return the name of the method
     */
    @Foldable
    public String methodName() {
        return name;
    }

    /**
     * Return the type of the method described by this nominal reference
     * @return the method type
     */
    @Foldable
    public MethodTypeRef methodType() {
        return type;
    }

    public MethodHandle resolveConstantRef(MethodHandles.Lookup lookup) throws ReflectiveOperationException {
        Class<?> resolvedOwner = owner.resolveConstantRef(lookup);
        MethodType resolvedType = this.type.resolveConstantRef(lookup);
        switch (kind) {
            case STATIC:
                return lookup.findStatic(resolvedOwner, name, resolvedType);
            case INTERFACE_VIRTUAL:
            case VIRTUAL:
                return lookup.findVirtual(resolvedOwner, name, resolvedType);
            case SPECIAL:
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
    public Optional<? extends ConstantRef<? super ConstantRef<MethodHandle>>> toConstantRef(MethodHandles.Lookup lookup) {
        return Optional.of(DynamicConstantRef.of(RefBootstraps.BSM_METHODHANDLEREF, CR_MethodHandleRef)
                                             .withArgs(kind.toString(), owner.descriptorString(), name, type.descriptorString()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConstantMethodHandleRef ref = (ConstantMethodHandleRef) o;
        return kind == ref.kind &&
               Objects.equals(owner, ref.owner) &&
               Objects.equals(name, ref.name) &&
               Objects.equals(type, ref.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, owner, name, type);
    }

    @Override
    public String toString() {
        return String.format("MethodHandleRef[%s/%s::%s%s]", kind, owner.simpleName(), name, type.simpleDescriptor());
    }
}
