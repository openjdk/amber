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

import java.lang.annotation.Foldable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.Optional;

import static java.lang.invoke.MethodHandleInfo.REF_getField;
import static java.lang.invoke.MethodHandleInfo.REF_getStatic;
import static java.lang.invoke.MethodHandleInfo.REF_invokeInterface;
import static java.lang.invoke.MethodHandleInfo.REF_invokeSpecial;
import static java.lang.invoke.MethodHandleInfo.REF_invokeStatic;
import static java.lang.invoke.MethodHandleInfo.REF_invokeVirtual;
import static java.lang.invoke.MethodHandleInfo.REF_newInvokeSpecial;
import static java.lang.invoke.MethodHandleInfo.REF_putField;
import static java.lang.invoke.MethodHandleInfo.REF_putStatic;
import static java.lang.sym.MethodHandleRef.Kind.CONSTRUCTOR;
import static java.lang.sym.MethodHandleRef.Kind.STATIC;
import static java.lang.sym.SymbolicRefs.CR_void;
import static java.util.Objects.requireNonNull;

/**
 * A symbolic reference for a {@link MethodHandle} constant.
 */
public final class MethodHandleRef implements ConstantRef<MethodHandle>, Constable<ConstantRef<MethodHandle>> {
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
        /** A method handle for a method invoked as with {@code invokestatic} */
        @Foldable STATIC(REF_invokeStatic),
        /** A method handle for a method invoked as with {@code invokevirtual} */
        @Foldable VIRTUAL(REF_invokeVirtual),
        /** A method handle for a method invoked as with {@code invokeinterface} */
        @Foldable INTERFACE_VIRTUAL(REF_invokeInterface),
        /** A method handle for a method invoked as with {@code invokespecial} */
        @Foldable SPECIAL(REF_invokeSpecial),
        /** A method handle for a constructor */
        @Foldable CONSTRUCTOR(REF_newInvokeSpecial),
        /** A method handle for a read accessor for an instance field  */
        @Foldable GETTER(REF_getField),
        /** A method handle for a write accessor for an instance field  */
        @Foldable SETTER(REF_putField),
        /** A method handle for a read accessor for a static field  */
        @Foldable STATIC_GETTER(REF_getStatic),
        /** A method handle for a write accessor for a static field  */
        @Foldable STATIC_SETTER(REF_putStatic);

        /** The corresponding {@code refKind} value for this kind of method handle,
         * as defined by {@link MethodHandleInfo}
         */
        public final int refKind;

        Kind(int refKind) {
            this.refKind = refKind;
        }
    }

    private final Kind kind;
    private final ClassRef owner;
    private final String name;
    private final MethodTypeRef type;

    /**
     * Construct a {@linkplain MethodHandleRef} from a kind, owner, name, and type
     * @param kind the kind of the method handle
     * @param owner the declaring class for the method
     * @param name the name of the method (ignored if {@code kind} is {@code CONSTRUCTOR})
     * @param type the type of the method
     * @throws NullPointerException if any non-ignored argument is null
     * @throws IllegalArgumentException if {@code kind} describes a field accessor,
     * and {@code type} is not consistent with that kind of field accessor
     */
    private MethodHandleRef(Kind kind, ClassRef owner, String name, MethodTypeRef type) {
        if (kind == CONSTRUCTOR)
            name = "<init>";

        requireNonNull(kind);
        requireNonNull(owner);
        requireNonNull(name);
        requireNonNull(type);

        switch (kind) {
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

    /**
     * Return a {@code MethodHandleRef} corresponding to an invocation of a
     * declared method or an accessor for a field
     *
     * @param kind The kind of method handle to be described
     * @param clazz the class declaring the method
     * @param name the name of the method (ignored if {@code kind} is {@code CONSTRUCTOR})
     * @param type the method type of the method
     * @return the {@code MethodHandleRef}
     * @throws NullPointerException if any of the non-ignored arguments are null
     */
    @Foldable
    public static MethodHandleRef of(Kind kind, ClassRef clazz, String name, MethodTypeRef type) {
        return new MethodHandleRef(kind, clazz, name, type);
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to an invocation of a
     * declared method or an accessor for a field
     *
     * @param kind The kind of method handle to be described
     * @param clazz the class declaring the method
     * @param name the name of the method (ignored if {@code kind} is {@code CONSTRUCTOR})
     * @param descriptorString descriptor string of the method
     * @return the {@code MethodHandleRef}
     * @throws NullPointerException if any of the non-ignored arguments are null
     */
    @Foldable
    public static MethodHandleRef of(Kind kind, ClassRef clazz, String name, String descriptorString) {
        return of(kind, clazz, name, MethodTypeRef.ofDescriptor(descriptorString));
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to an invocation of a
     * declared method or an accessor for a field
     *
     * @param kind The kind of method handle to be described
     * @param clazz the class declaring the method
     * @param name the name of the method (ignored if {@code kind} is {@code CONSTRUCTOR})
     * @param returnType the return type of the method
     * @param paramTypes the parameter types of the method
     * @return the {@code MethodHandleRef}
     * @throws NullPointerException if any of the non-ignored arguments are null
     */
    @Foldable
    public static MethodHandleRef of(Kind kind, ClassRef clazz, String name, ClassRef returnType, ClassRef... paramTypes) {
        return of(kind, clazz, name, MethodTypeRef.of(returnType, paramTypes));
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to a bootstrap method for
     * an {@code invokedynamic} callsite, which is a static method whose leading
     * parameter types are {@code Lookup}, {@code String}, and {@code MethodType}
     * @param clazz the class declaring the method
     * @param name the name of the method
     * @param returnType the return type of the method
     * @param paramTypes the parameter types of the method that follow the three
     *                   standard leading arguments, if any
     * @return the {@code MethodHandleRef}
     * @throws NullPointerException if any of the arguments are null
     */
    @Foldable
    public static MethodHandleRef ofDynamicCallsite(ClassRef clazz, String name, ClassRef returnType, ClassRef... paramTypes) {
        return of(STATIC, clazz, name, MethodTypeRef.of(returnType, paramTypes).insertParameterTypes(0, INDY_BOOTSTRAP_ARGS));
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to a bootstrap method for a
     * dynamic constant, which is a static method whose leading arguments are
     * {@code Lookup}, {@code String}, and {@code Class}
     * @param clazz the class declaring the method
     * @param name the name of the method
     * @param returnType the return type of the method
     * @param paramTypes the parameter types of the method that follow the three
     *                   standard leading arguments, if any
     * @return the {@code MethodHandleRef}
     * @throws NullPointerException if any of the arguments are null
     */
    @Foldable
    public static MethodHandleRef ofDynamicConstant(ClassRef clazz, String name, ClassRef returnType, ClassRef... paramTypes) {
        return of(STATIC, clazz, name, MethodTypeRef.of(returnType, paramTypes).insertParameterTypes(0, CONDY_BOOTSTRAP_ARGS));
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to accessing a field
     * @param kind the kind of the method handle; must be one of {@code GETTER},
     *             {@code SETTER}, {@code STATIC_GETTER}, or {@code STATIC_SETTER}
     * @param clazz the class declaring the field
     * @param name the name of the field
     * @param type the type of the field
     * @return the {@code MethodHandleRef}
     * @throws NullPointerException if any of the arguments are null
     */
    @Foldable
    public static MethodHandleRef ofField(Kind kind, ClassRef clazz, String name, ClassRef type) {
        MethodTypeRef mtr;
        switch (kind) {
            case GETTER: mtr = MethodTypeRef.of(type, clazz); break;
            case SETTER: mtr = MethodTypeRef.of(CR_void, clazz, type); break;
            case STATIC_GETTER: mtr = MethodTypeRef.of(type); break;
            case STATIC_SETTER: mtr = MethodTypeRef.of(CR_void, type); break;
            default:
                throw new IllegalArgumentException(kind.toString());
        }
        return new MethodHandleRef(kind, clazz, name, mtr);
    }

    public MethodHandle resolveRef(MethodHandles.Lookup lookup) throws ReflectiveOperationException {
        Class<?> resolvedOwner = owner.resolveRef(lookup);
        MethodType resolvedType = this.type.resolveRef(lookup);
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

    /**
     * Return the {@code refKind} of the method handle described by this symbolic reference,
     * as defined by {@link MethodHandleInfo}
     * @return the reference kind
     */
    @Foldable
    public int refKind() { return kind.refKind; }

    /**
     * Return the {@code kind} of the method handle described by this symbolic reference
     * @return the {@link Kind}
     */
    @Foldable
    public Kind kind() { return kind; }

    /**
     * Return the class which declares the method or field described by
     * this symbolic reference
     *
     * @return the class in which the method or field is declared
     */
    @Foldable
    public ClassRef owner() {
        return owner;
    }

    /**
     * Return the name of the method described by this symbolic reference
     *
     * @return the name of the method
     */
    @Foldable
    public String name() {
        return name;
    }

    /**
     * Return the type of the method described by this symbolic reference
     * @return the method type
     */
    @Foldable
    public MethodTypeRef type() {
        return type;
    }

    @Override
    public Optional<ConstantRef<ConstantRef<MethodHandle>>> toSymbolicRef(MethodHandles.Lookup lookup) {
        Optional<EnumRef<Kind>> kindRef = kind.toSymbolicRef(lookup);
        Optional<ConstantRef<ConstantRef<Class<?>>>> classRefRef = owner.toSymbolicRef(lookup);
        Optional<ConstantRef<ConstantRef<MethodType>>> typeRefRef = type.toSymbolicRef(lookup);
        if (!kindRef.isPresent() || !classRefRef.isPresent() || !typeRefRef.isPresent())
            return Optional.empty();
        ConstantRef<?>[] args = {SymbolicRefs.MHR_METHODHANDLEREF_FACTORY, kindRef.get(),
                                 classRefRef.get(), name, typeRefRef.get()};
        return Optional.of(DynamicConstantRef.of(SymbolicRefs.BSM_INVOKE, name,
                                                 SymbolicRefs.CR_MethodHandleRef, args));
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
        return String.format("MethodHandleRef[%s/%s::%s%s]", kind, owner.canonicalName(), name, type.canonicalDescriptor());
    }
}
