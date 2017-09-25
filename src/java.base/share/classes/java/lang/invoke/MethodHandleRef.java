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

    private enum Kind {
        STATIC(REF_invokeStatic),
        VIRTUAL(REF_invokeVirtual),
        INTERFACE_VIRTUAL(REF_invokeInterface),
        SPECIAL(REF_invokeSpecial),
        CTOR(REF_newInvokeSpecial),
        GETTER(REF_getField),
        SETTER(REF_putField),
        STATIC_GETTER(REF_getStatic),
        STATIC_SETTER(REF_putStatic);

        final int refKind;

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

    // @@@ Consider special combinator: asIndyBootstrap, asCondyBootstrap, asWhizzyBootstrap

    /**
     * Return a {@code MethodHandleRef} corresponding to an
     * invocation of a static method
     * @param clazz the class containing the method
     * @param name the name of the method
     * @param type the method type of the method
     * @return the {@code MethodHandleRef}
     */
    @TrackableConstant
    public static MethodHandleRef ofStatic(ClassRef clazz, String name, MethodTypeRef type) {
        return new MethodHandleRef(Kind.STATIC, clazz, name, type);
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to an
     * invocation of a static method
     * @param clazz the class containing the method
     * @param name the name of the method
     * @param descriptorString descriptor string of the method
     * @return the {@code MethodHandleRef}
     */
    @TrackableConstant
    public static MethodHandleRef ofStatic(ClassRef clazz, String name, String descriptorString) {
        return ofStatic(clazz, name, MethodTypeRef.ofDescriptor(descriptorString));
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to an
     * invocation of a static method
     * @param clazz the class containing the method
     * @param name the name of the method
     * @param returnType the return type of the method
     * @param paramTypes the parameter types of the method
     * @return the {@code MethodHandleRef}
     */
    @TrackableConstant
    public static MethodHandleRef ofStatic(ClassRef clazz, String name, ClassRef returnType, ClassRef... paramTypes) {
        return ofStatic(clazz, name, MethodTypeRef.of(returnType, paramTypes));
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to an invokedynamic bootstrap,
     * which is a static method whose leading arguments are {@code Lookup}, {@code String}, and {@code MethodType}
     * @param clazz the class containing the method
     * @param name the name of the method
     * @param type the method type of the method
     * @return the {@code MethodHandleRef}
     */
    @TrackableConstant
    public static MethodHandleRef ofIndyBootstrap(ClassRef clazz, String name, MethodTypeRef type) {
        return ofStatic(clazz, name, type.insertParameterTypes(0, INDY_BOOTSTRAP_ARGS));
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to an invokedynamic bootstrap,
     * which is a static method whose leading arguments are {@code Lookup}, {@code String}, and {@code MethodType}
     * @param clazz the class containing the method
     * @param name the name of the method
     * @param descriptorString the descriptor string of the method
     * @return the {@code MethodHandleRef}
     */
    @TrackableConstant
    public static MethodHandleRef ofIndyBootstrap(ClassRef clazz, String name, String descriptorString) {
        return ofIndyBootstrap(clazz, name, MethodTypeRef.ofDescriptor(descriptorString));
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
        return ofIndyBootstrap(clazz, name, MethodTypeRef.of(returnType, paramTypes));
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to a constantdynamic bootstrap,
     * which is a static method whose leading arguments are {@code Lookup}, {@code String}, and {@code Class}
     * @param clazz the class containing the method
     * @param name the name of the method
     * @param type the method type of the method
     * @return the {@code MethodHandleRef}
     */

    @TrackableConstant
    public static MethodHandleRef ofCondyBootstrap(ClassRef clazz, String name, MethodTypeRef type) {
        return ofStatic(clazz, name, type.insertParameterTypes(0, CONDY_BOOTSTRAP_ARGS));
    }
    /**
     * Return a {@code MethodHandleRef} corresponding to a constantdynamic bootstrap,
     * which is a static method whose leading arguments are {@code Lookup}, {@code String}, and {@code Class}
     * @param clazz the class containing the method
     * @param name the name of the method
     * @param descriptorString the descriptor string of the method
     * @return the {@code MethodHandleRef}
     */

    @TrackableConstant
    public static MethodHandleRef ofCondyBootstrap(ClassRef clazz, String name, String descriptorString) {
        return ofCondyBootstrap(clazz, name, MethodTypeRef.ofDescriptor(descriptorString));
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
        return ofCondyBootstrap(clazz, name, MethodTypeRef.of(returnType, paramTypes));
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to invocation
     * of a virtual method via {@code invokevirtual}
     * @param clazz the class containing the method
     * @param name the name of the method
     * @param type the method type of the method
     * @return the {@code MethodHandleRef}
     */
    @TrackableConstant
    public static MethodHandleRef ofVirtual(ClassRef clazz, String name, MethodTypeRef type) {
        return new MethodHandleRef(Kind.VIRTUAL, clazz, name, type);
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to invocation
     * of a virtual method via {@code invokevirtual}
     * @param clazz the class containing the method
     * @param name the name of the method
     * @param descriptorString the descriptor string of the method
     * @return the {@code MethodHandleRef}
     */

    @TrackableConstant
    public static MethodHandleRef ofVirtual(ClassRef clazz, String name, String descriptorString) {
        return ofVirtual(clazz, name, MethodTypeRef.ofDescriptor(descriptorString));
    }
    /**
     * Return a {@code MethodHandleRef} corresponding to invocation
     * of a virtual method via {@code invokevirtual}
     * @param clazz the class containing the method
     * @param name the name of the method
     * @param returnType the return type of the method
     * @param paramTypes the parameter types of the method
     * @return the {@code MethodHandleRef}
     */
    @TrackableConstant
    public static MethodHandleRef ofVirtual(ClassRef clazz, String name, ClassRef returnType, ClassRef... paramTypes) {
        return ofVirtual(clazz, name, MethodTypeRef.of(returnType, paramTypes));
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to invocation
     * of a virtual method via {@code invokeinterface}
     * @param clazz the class containing the method
     * @param name the name of the method
     * @param type the method type of the method
     * @return the {@code MethodHandleRef}
     */
    @TrackableConstant
    public static MethodHandleRef ofInterface(ClassRef clazz, String name, MethodTypeRef type) {
        return new MethodHandleRef(Kind.INTERFACE_VIRTUAL, clazz, name, type);
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to invocation
     * of a virtual method via {@code invokeinterface}
     * @param clazz the class containing the method
     * @param name the name of the method
     * @param descriptorString the descriptor string of the method
     * @return the {@code MethodHandleRef}
     */

    @TrackableConstant
    public static MethodHandleRef ofInterface(ClassRef clazz, String name, String descriptorString) {
        return ofInterface(clazz, name, MethodTypeRef.ofDescriptor(descriptorString));
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to invocation
     * of a virtual method via {@code invokeinterface}
     * @param clazz the class containing the method
     * @param name the name of the method
     * @param returnType the return type of the method
     * @param paramTypes the parameter types of the method
     * @return the {@code MethodHandleRef}
     */
    @TrackableConstant
    public static MethodHandleRef ofInterface(ClassRef clazz, String name, ClassRef returnType, ClassRef... paramTypes) {
        return ofInterface(clazz, name, MethodTypeRef.of(returnType, paramTypes));
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to invocation
     * of a virtual method via {@code invokespecial}
     * @param clazz the class containing the method
     * @param name the name of the method
     * @param type the method type of the method
     * @return the {@code MethodHandleRef}
     */
    @TrackableConstant
    public static MethodHandleRef ofSpecial(ClassRef clazz, String name, MethodTypeRef type) {
        return new MethodHandleRef(Kind.SPECIAL, clazz, name, type);
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to invocation
     * of a virtual method via {@code invokespecial}
     * @param clazz the class containing the method
     * @param name the name of the method
     * @param descriptorString the descriptor string of the method
     * @return the {@code MethodHandleRef}
     */
    @TrackableConstant
    public static MethodHandleRef ofSpecial(ClassRef clazz, String name, String descriptorString) {
        return ofSpecial(clazz, name, MethodTypeRef.ofDescriptor(descriptorString));
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to invocation
     * of a virtual method via {@code invokespecial}
     * @param clazz the class containing the method
     * @param name the name of the method
     * @param returnType the return type of the method
     * @param paramTypes the parameter types of the method
     * @return the {@code MethodHandleRef}
     */
    @TrackableConstant
    public static MethodHandleRef ofSpecial(ClassRef clazz, String name, ClassRef returnType, ClassRef... paramTypes) {
        return ofSpecial(clazz, name, MethodTypeRef.of(returnType, paramTypes));
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to invocation
     * of a constructor
     * @param clazz the class containing the method
     * @param type the method type of the method
     * @return the {@code MethodHandleRef}
     */
    @TrackableConstant
    public static MethodHandleRef ofConstructor(ClassRef clazz, MethodTypeRef type) {
        return new MethodHandleRef(Kind.CTOR, clazz, "<init>", type);
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to invocation
     * of a constructor
     * @param clazz the class containing the method
     * @param descriptorString the descriptor string of the method
     * @return the {@code MethodHandleRef}
     */
    @TrackableConstant
    public static MethodHandleRef ofConstructor(ClassRef clazz, String descriptorString) {
        return ofConstructor(clazz, MethodTypeRef.ofDescriptor(descriptorString));
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to invocation
     * of a constructor
     * @param clazz the class containing the method
     * @param returnType the return type of the method
     * @param paramTypes the parameter types of the method
     * @return the {@code MethodHandleRef}
     */
    @TrackableConstant
    public static MethodHandleRef ofConstructor(ClassRef clazz, ClassRef returnType, ClassRef... paramTypes) {
        return ofConstructor(clazz, MethodTypeRef.of(returnType, paramTypes));
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
    public static MethodHandleRef ofGetter(ClassRef clazz, String name, ClassRef type) {
        return new MethodHandleRef(Kind.GETTER, clazz, name, MethodTypeRef.of(type, clazz));
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to invocation
     * of an instance field setter
     * @param clazz the class containing the field
     * @param name the name of the field
     * @param type the type of the field
     * @return the {@code MethodHandleRef}
     */
    @TrackableConstant
    public static MethodHandleRef ofSetter(ClassRef clazz, String name, ClassRef type) {
        return new MethodHandleRef(Kind.SETTER, clazz, name, MethodTypeRef.of(ClassRef.ofVoid(), clazz, type));
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to invocation
     * of a static field getter
     * @param clazz the class containing the field
     * @param name the name of the field
     * @param type the type of the field
     * @return the {@code MethodHandleRef}
     */
    @TrackableConstant
    public static MethodHandleRef ofStaticGetter(ClassRef clazz, String name, ClassRef type) {
        return new MethodHandleRef(Kind.STATIC_GETTER, clazz, name, MethodTypeRef.of(type));
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to invocation
     * of a static field setter
     * @param clazz the class containing the field
     * @param name the name of the field
     * @param type the type of the field
     * @return the {@code MethodHandleRef}
     */
    @TrackableConstant
    public static MethodHandleRef ofStaticSetter(ClassRef clazz, String name, ClassRef type) {
        return new MethodHandleRef(Kind.STATIC_SETTER, clazz, name, MethodTypeRef.of(ClassRef.ofVoid(), type));
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
            case CTOR: return lookup.findConstructor(owner.resolve(lookup), type.resolve(lookup));
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
