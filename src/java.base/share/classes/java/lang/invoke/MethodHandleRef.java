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
package java.lang.invoke;

import java.lang.annotation.TrackableConstant;
import java.util.Objects;

import static java.lang.invoke.MethodHandleInfo.REF_getField;
import static java.lang.invoke.MethodHandleInfo.REF_getStatic;
import static java.lang.invoke.MethodHandleInfo.REF_invokeInterface;
import static java.lang.invoke.MethodHandleInfo.REF_invokeSpecial;
import static java.lang.invoke.MethodHandleInfo.REF_invokeStatic;
import static java.lang.invoke.MethodHandleInfo.REF_invokeVirtual;
import static java.lang.invoke.MethodHandleInfo.REF_newInvokeSpecial;
import static java.lang.invoke.MethodHandleInfo.REF_putField;
import static java.lang.invoke.MethodHandleInfo.REF_putStatic;
import static java.lang.invoke.MethodHandleRef.Kind.STATIC;

/**
 * A descriptor for a {@linkplain MethodHandle} constant.
 */
public final class MethodHandleRef implements ConstantRef<MethodHandle> {
    private static final ClassRef[] INDY_BOOTSTRAP_ARGS = { ClassRef.of("java.lang.invoke.MethodHandles$Lookup"),
                                                            ClassRef.of("java.lang.String"),
                                                            ClassRef.of("java.lang.invoke.MethodType") };
    private static final ClassRef[] CONDY_BOOTSTRAP_ARGS = { ClassRef.of("java.lang.invoke.MethodHandles$Lookup"),
                                                             ClassRef.of("java.lang.String"),
                                                             ClassRef.of("java.lang.Class") };

    /**
     * Kinds of method handle refs
     */
    public enum Kind {
        @TrackableConstant STATIC(REF_invokeStatic),
        @TrackableConstant VIRTUAL(REF_invokeVirtual),
        @TrackableConstant INTERFACE_VIRTUAL(REF_invokeInterface),
        @TrackableConstant SPECIAL(REF_invokeSpecial),
        @TrackableConstant CONSTRUCTOR(REF_newInvokeSpecial),
        @TrackableConstant GETTER(REF_getField),
        @TrackableConstant SETTER(REF_putField),
        @TrackableConstant STATIC_GETTER(REF_getStatic),
        @TrackableConstant STATIC_SETTER(REF_putStatic);

        public final int refKind;

        Kind(int refKind) {
            this.refKind = refKind;
        }
    }

    private final Kind kind;
    private final ClassRef owner;
    private final String name;
    private final MethodTypeRef type;

    private MethodHandleRef(Kind kind, ClassRef owner, String name, MethodTypeRef type) {
        this.kind = kind;
        this.owner = owner;
        this.name = name;
        this.type = type;
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to an
     * invocation of a static method
     * @param kind One of: STATIC, VIRTUAL, INTERFACE_VIRTUAL, SPECIAL, CONSTRUCTOR
     * @param clazz the class containing the method
     * @param name the name of the method (ignored if kind=CONSTRUCTOR)
     * @param type the method type of the method
     * @return the {@code MethodHandleRef}
     */
    @TrackableConstant
    public static MethodHandleRef of(Kind kind, ClassRef clazz, String name, MethodTypeRef type) {
        switch (kind) {
            case STATIC:
            case VIRTUAL:
            case INTERFACE_VIRTUAL:
            case SPECIAL:
                return new MethodHandleRef(kind, clazz, name, type);
            case CONSTRUCTOR:
                return new MethodHandleRef(kind, clazz, "<init>", type);
            default:
                throw new IllegalArgumentException(kind.toString());
        }
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to an
     * invocation of a static method
     * @param kind One of: STATIC, VIRTUAL, INTERFACE_VIRTUAL, SPECIAL, CONSTRUCTOR
     * @param clazz the class containing the method
     * @param name the name of the method (ignored if kind=CONSTRUCTOR)
     * @param descriptorString descriptor string of the method
     * @return the {@code MethodHandleRef}
     */
    @TrackableConstant
    public static MethodHandleRef of(Kind kind, ClassRef clazz, String name, String descriptorString) {
        return of(kind, clazz, name, MethodTypeRef.ofDescriptor(descriptorString));
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to an
     * invocation of a static method
     * @param kind One of: STATIC, VIRTUAL, INTERFACE_VIRTUAL, SPECIAL, CONSTRUCTOR
     * @param clazz the class containing the method
     * @param name the name of the method (ignored if kind=CONSTRUCTOR)
     * @param returnType the return type of the method
     * @param paramTypes the parameter types of the method
     * @return the {@code MethodHandleRef}
     */
    @TrackableConstant
    public static MethodHandleRef of(Kind kind, ClassRef clazz, String name, ClassRef returnType, ClassRef... paramTypes) {
        return of(kind, clazz, name, MethodTypeRef.of(returnType, paramTypes));
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to an invokedynamic bootstrap,
     * which is a static method whose leading arguments are {@code Lookup}, {@code String}, and {@code MethodType}
     * @param clazz the class containing the method
     * @param name the name of the method
     * @param returnType the return type of the method
     * @param paramTypes the parameter types of the method
     * @return the {@code MethodHandleRef}
     */
    @TrackableConstant
    public static MethodHandleRef ofIndyBootstrap(ClassRef clazz, String name, ClassRef returnType, ClassRef... paramTypes) {
        return of(STATIC, clazz, name, MethodTypeRef.of(returnType, paramTypes).insertParameterTypes(0, INDY_BOOTSTRAP_ARGS));
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to a constantdynamic bootstrap,
     * which is a static method whose leading arguments are {@code Lookup}, {@code String}, and {@code Class}
     * @param clazz the class containing the method
     * @param name the name of the method
     * @param returnType the return type of the method
     * @param paramTypes the parameter types of the method
     * @return the {@code MethodHandleRef}
     */
    @TrackableConstant
    public static MethodHandleRef ofCondyBootstrap(ClassRef clazz, String name, ClassRef returnType, ClassRef... paramTypes) {
        return of(STATIC, clazz, name, MethodTypeRef.of(returnType, paramTypes).insertParameterTypes(0, CONDY_BOOTSTRAP_ARGS));
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to invocation
     * of an instance field getter
     * @param clazz the class containing the field
     * @param name the name of the field
     * @param type the type of the field
     * @return the {@code MethodHandleRef}
     */
    @TrackableConstant
    public static MethodHandleRef ofField(Kind kind, ClassRef clazz, String name, ClassRef type) {
        switch (kind) {
            case GETTER: return new MethodHandleRef(Kind.GETTER, clazz, name, MethodTypeRef.of(type, clazz));
            case SETTER:
                return new MethodHandleRef(Kind.SETTER, clazz, name, MethodTypeRef.of(ClassRef.CR_void, clazz, type));
            case STATIC_GETTER: return new MethodHandleRef(Kind.STATIC_GETTER, clazz, name, MethodTypeRef.of(type));
            case STATIC_SETTER:
                return new MethodHandleRef(Kind.STATIC_SETTER, clazz, name, MethodTypeRef.of(ClassRef.CR_void, type));
            default: throw new IllegalArgumentException(kind.toString());
        }
    }

    /**
     * Resolve to a MethodHandle
     * @param lookup the lookup
     * @return the MethodHandle
     * @throws ReflectiveOperationException exception
     */
    public MethodHandle resolve(MethodHandles.Lookup lookup) throws ReflectiveOperationException {
        switch (kind) {
            case STATIC: return lookup.findStatic(owner.resolve(lookup), name, type.resolve(lookup));
            case INTERFACE_VIRTUAL:
            case VIRTUAL:
                return lookup.findVirtual(owner.resolve(lookup), name, type.resolve(lookup));
            case SPECIAL: return lookup.findSpecial(owner.resolve(lookup), name, type.resolve(lookup), lookup.lookupClass());
            case CONSTRUCTOR: return lookup.findConstructor(owner.resolve(lookup), type.resolve(lookup));
            case GETTER: return lookup.findGetter(owner.resolve(lookup), name, type.resolve(lookup).returnType());
            case STATIC_GETTER: return lookup.findStaticGetter(owner.resolve(lookup), name, type.resolve(lookup).returnType());
            case SETTER: return lookup.findSetter(owner.resolve(lookup), name, type.resolve(lookup).parameterType(1));
            case STATIC_SETTER: return lookup.findStaticSetter(owner.resolve(lookup), name, type.resolve(lookup).parameterType(0));
            default: throw new IllegalStateException(kind.name());
        }
    }

    /**
     * Return the {@code refKind} of the method handle described by this descriptor,
     * as defined by {@link MethodHandleInfo}
     * @return the reference kind
     */
    @TrackableConstant
    public int refKind() { return kind.refKind; }

    /**
     * Return the class in which the method described by this descriptor is
     * declared
     *
     * @return the class in which the method is declared
     */
    @TrackableConstant
    public ClassRef owner() {
        return owner;
    }

    /**
     * Return the name of the method described by this descriptor
     * @return the name of the method
     */
    @TrackableConstant
    public String name() {
        return name;
    }

    /**
     * Return the method type of the method described by this descriptor
     * @return the method type
     */
    @TrackableConstant
    public MethodTypeRef type() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodHandleRef ref = (MethodHandleRef) o;
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
        return String.format("MethodHandleRef[kind=%s, owner=%s, name=%s, type=%s]", kind, owner, name, type);
    }
}
