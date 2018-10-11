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
import java.lang.invoke.MethodType;

import static java.lang.constant.ConstantDescs.CD_void;
import static java.lang.constant.DirectMethodHandleDesc.Kind.CONSTRUCTOR;

/**
 * A <a href="package-summary.html#nominal">nominal descriptor</a> for a
 * {@link MethodHandle} constant.
 *
 * @apiNote In the future, if the Java language permits, {@linkplain MethodHandleDesc}
 * may become a {@code sealed} interface, which would prohibit subclassing except
 * by explicitly permitted types.  Non-platform classes should not implement
 * {@linkplain MethodHandleDesc} directly.
 */
public interface MethodHandleDesc
        extends ConstantDesc<MethodHandle> {

    /**
     * Create a {@linkplain MethodHandleDesc} corresponding to an invocation of a
     * declared method, invocation of a constructor, or access to a field.
     *
     * <p>If {@code kind} is {@code CONSTRUCTOR}, the name is ignored and the return
     * type of the invocation type must be {@code void}.  If {@code kind} corresponds
     * to a field access, the invocation type must be consistent with that kind
     * of field access and the type of the field; instance field accessors must
     * take a leading receiver parameter, getters must return the type of the
     * field, setters must take a new value for the field and return {@code void}.
     *
     * <p>For constructor and field access, the methods {@link #ofField(DirectMethodHandleDesc.Kind, ClassDesc, String, ClassDesc)}
     * and {@link #ofConstructor(ClassDesc, ClassDesc...)} may be more convenient.
     *
     * @param kind The kind of method handle to be described
     * @param clazz a {@link ClassDesc} describing the class containing the
     *              method, constructor, or field
     * @param name the name of the method or field (ignored if {@code kind} is
     * {@code CONSTRUCTOR}), as per JVMS 4.2.2
     * @param type a {@link MethodTypeDesc} describing the invocation type of
     *             the method handle
     * @return the {@linkplain MethodHandleDesc}
     * @throws NullPointerException if any non-ignored arguments are null
     * @throws IllegalArgumentException if the {@code name} has the incorrect
     * format
     * @jvms 4.2.2 Unqualified Names
     */
    static DirectMethodHandleDesc of(DirectMethodHandleDesc.Kind kind,
                                     ClassDesc clazz,
                                     String name,
                                     MethodTypeDesc type) {
        return new DirectMethodHandleDescImpl(kind, clazz, name, type);
    }

    /**
     * Create a {@linkplain MethodHandleDesc} corresponding to an invocation of a
     * declared method, invocation of a constructor, or access to a field.
     *
     * <p>If {@code kind} is {@code CONSTRUCTOR}, the name is ignored and the return
     * type of the invocation type must be {@code void}.  If {@code kind} corresponds
     * to a field access, the invocation type must be consistent with that kind
     * of field access and the type of the field; instance field accessors must
     * take a leading receiver parameter, getters must return the type of the
     * field, setters must take a new value for the field and return {@code void}.
     * The method {@link #ofField(DirectMethodHandleDesc.Kind, ClassDesc, String, ClassDesc)} will construct
     * the appropriate invocation given the type of the field.
     *
     * @param kind The kind of method handle to be described
     * @param clazz a {@link ClassDesc} describing the class containing the
     *              method, constructor, or field
     * @param name the name of the method or field (ignored if {@code kind} is
     * {@code CONSTRUCTOR}), as per JVMS 4.2.2
     * @param descriptorString a method descriptor string for the invocation type
     * of the method handle, as per JVMS 4.3.3
     * @return the {@linkplain MethodHandleDesc}
     * @throws NullPointerException if any of the non-ignored arguments are null
     * @jvms 4.2.2 Unqualified Names
     * @jvms 4.3.3 Method Descriptors
     */
    static DirectMethodHandleDesc of(DirectMethodHandleDesc.Kind kind,
                                     ClassDesc clazz,
                                     String name,
                                     String descriptorString) {
        return of(kind, clazz, name, MethodTypeDesc.ofDescriptor(descriptorString));
    }

    /**
     * Create a {@linkplain MethodHandleDesc} corresponding to an invocation of a
     * declared method, invocation of a constructor, or access to a field.
     *
     * <p>If {@code kind} is {@code CONSTRUCTOR}, the name is ignored and the return
     * type of the invocation type must be {@code void}.  If {@code kind} corresponds
     * to a field access, the invocation type must be consistent with that kind
     * of field access and the type of the field; instance field accessors must
     * take a leading receiver parameter, getters must return the type of the
     * field, setters must take a new value for the field and return {@code void}.
     * The method {@link #ofField(DirectMethodHandleDesc.Kind, ClassDesc, String, ClassDesc)} will construct
     * the appropriate invocation given the type of the field.
     *
     * @param kind The kind of method handle to be described
     * @param clazz a {@link ClassDesc} describing the class containing the
     *              method, constructor, or field
     * @param name the name of the method or field (ignored if {@code kind} is
     * {@code CONSTRUCTOR}), as per JVMS 4.2.2
     * @param returnType a {@link ClassDesc} describing the return type of the
     *                   method handle
     * @param paramTypes {@link ClassDesc}s describing the parameter types of
     *                                    the method handle
     * @return the {@linkplain MethodHandleDesc}
     * @throws NullPointerException if any of the non-ignored arguments are null
     * @jvms 4.2.2 Unqualified Names
     */
    static DirectMethodHandleDesc of(DirectMethodHandleDesc.Kind kind,
                                     ClassDesc clazz,
                                     String name,
                                     ClassDesc returnType,
                                     ClassDesc... paramTypes) {
        return of(kind, clazz, name, MethodTypeDesc.of(returnType, paramTypes));
    }

    /**
     * Create a {@linkplain MethodHandleDesc} corresponding to a method handle
     * that accesses a field.
     *
     * @param kind the kind of the method handle to be described; must be one of {@code GETTER},
     *             {@code SETTER}, {@code STATIC_GETTER}, or {@code STATIC_SETTER}
     * @param clazz a {@link ClassDesc} describing the class containing the
     *              method, constructor, or field
     * @param fieldName the name of the field, as per JVMS 4.2.2
     * @param fieldType a {@link ClassDesc} describing the type of the field
     * @return the {@linkplain MethodHandleDesc}
     * @throws NullPointerException if any of the arguments are null
     * @jvms 4.2.2 Unqualified Names
     */
    static DirectMethodHandleDesc ofField(DirectMethodHandleDesc.Kind kind,
                                          ClassDesc clazz,
                                          String fieldName,
                                          ClassDesc fieldType) {
        MethodTypeDesc mtr;
        switch (kind) {
            case GETTER: mtr = MethodTypeDesc.of(fieldType, clazz); break;
            case SETTER: mtr = MethodTypeDesc.of(CD_void, clazz, fieldType); break;
            case STATIC_GETTER: mtr = MethodTypeDesc.of(fieldType); break;
            case STATIC_SETTER: mtr = MethodTypeDesc.of(CD_void, fieldType); break;
            default:
                throw new IllegalArgumentException(kind.toString());
        }
        return MethodHandleDesc.of(kind, clazz, fieldName, mtr);
    }

    /**
     * Return a {@linkplain MethodHandleDesc} corresponding to invocation of a constructor
     *
     * @param clazz a {@link ClassDesc} describing the class containing the
     *              method, constructor, or field
     * @param paramTypes {@link ClassDesc}s describing the parameter types of
     *                   the constructor
     * @return the {@linkplain MethodHandleDesc}
     * @throws NullPointerException if any of the arguments are null
     */
    static DirectMethodHandleDesc ofConstructor(ClassDesc clazz,
                                                ClassDesc... paramTypes) {
        return MethodHandleDesc.of(CONSTRUCTOR, clazz, ConstantDescs.DEFAULT_NAME,
                                   MethodTypeDesc.of(CD_void, paramTypes));
    }

    /**
     * Return a {@linkplain MethodHandleDesc} that describes this method handle
     * adapted to a different type, as if by {@link MethodHandle#asType(MethodType)}.
     *
     * @param type a {@link MethodHandleDesc} describing the new method type
     * @return a {@linkplain MethodHandleDesc} for the adapted method handle
     */
    default MethodHandleDesc asType(MethodTypeDesc type) {
        return (methodType().equals(type)) ? this : new AsTypeMethodHandleDesc(this, type);
    }

    /**
     * Return a {@link MethodTypeDesc} describing the type of the method handle
     * described by this nominal descriptor
     *
     * @return a {@linkplain MethodHandleDesc} describing the method handle type
     */
    MethodTypeDesc methodType();
}
