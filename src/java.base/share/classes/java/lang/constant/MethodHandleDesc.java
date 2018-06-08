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
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodType;

import static java.lang.invoke.MethodHandleInfo.REF_getField;
import static java.lang.invoke.MethodHandleInfo.REF_getStatic;
import static java.lang.invoke.MethodHandleInfo.REF_invokeInterface;
import static java.lang.invoke.MethodHandleInfo.REF_invokeSpecial;
import static java.lang.invoke.MethodHandleInfo.REF_invokeStatic;
import static java.lang.invoke.MethodHandleInfo.REF_invokeVirtual;
import static java.lang.invoke.MethodHandleInfo.REF_newInvokeSpecial;
import static java.lang.invoke.MethodHandleInfo.REF_putField;
import static java.lang.invoke.MethodHandleInfo.REF_putStatic;
import static java.lang.constant.ConstantDescs.CR_void;
import static java.lang.constant.MethodHandleDesc.Kind.CONSTRUCTOR;

/**
 * A <a href="package-summary.html#nominal">nominal descriptor</a> for a
 * {@link MethodHandle} constant.
 */
public interface MethodHandleDesc
        extends ConstantDesc<MethodHandle>, Constable<ConstantDesc<MethodHandle>> {
    /**
     * Kinds of method handles that can be described with {@linkplain MethodHandleDesc}.
     */
    public enum Kind {
        /** A method handle for a method invoked as with {@code invokestatic} */
        STATIC(REF_invokeStatic),
        /** A method handle for a method invoked as with {@code invokestatic} */
        INTERFACE_STATIC(REF_invokeStatic, true),
        /** A method handle for a method invoked as with {@code invokevirtual} */
        VIRTUAL(REF_invokeVirtual),
        /** A method handle for a method invoked as with {@code invokeinterface} */
        INTERFACE_VIRTUAL(REF_invokeInterface, true),
        /** A method handle for a method invoked as with {@code invokespecial} */
        SPECIAL(REF_invokeSpecial),
        /** A method handle for an interface method invoked as with {@code invokespecial} */
        INTERFACE_SPECIAL(REF_invokeSpecial, true),
        /** A method handle for a constructor */
        CONSTRUCTOR(REF_newInvokeSpecial),
        /** A method handle for a read accessor for an instance field  */
        GETTER(REF_getField),
        /** A method handle for a write accessor for an instance field  */
        SETTER(REF_putField),
        /** A method handle for a read accessor for a static field  */
        STATIC_GETTER(REF_getStatic),
        /** A method handle for a write accessor for a static field  */
        STATIC_SETTER(REF_putStatic);

        /** The corresponding {@code refKind} value for this kind of method handle,
         * as defined by {@link MethodHandleInfo}
         */
        public final int refKind;
        /** Is this an interface
         */
        public final boolean isInterface;

        Kind(int refKind) {
            this(refKind, false);
        }
        Kind(int refKind, boolean isInterface) { this.refKind = refKind; this.isInterface = isInterface; }
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
     *
     * <p>For constructor and field access, the methods {@link #ofField(Kind, ClassDesc, String, ClassDesc)}
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
    static ConstantMethodHandleDesc of(Kind kind,
                                       ClassDesc clazz,
                                       String name,
                                       MethodTypeDesc type) {
        return new ConstantMethodHandleDesc(kind, clazz, name, type);
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
     * The method {@link #ofField(Kind, ClassDesc, String, ClassDesc)} will construct
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
    static ConstantMethodHandleDesc of(Kind kind,
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
     * The method {@link #ofField(Kind, ClassDesc, String, ClassDesc)} will construct
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
    static ConstantMethodHandleDesc of(Kind kind,
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
    static ConstantMethodHandleDesc ofField(Kind kind,
                                            ClassDesc clazz,
                                            String fieldName,
                                            ClassDesc fieldType) {
        MethodTypeDesc mtr;
        switch (kind) {
            case GETTER: mtr = MethodTypeDesc.of(fieldType, clazz); break;
            case SETTER: mtr = MethodTypeDesc.of(CR_void, clazz, fieldType); break;
            case STATIC_GETTER: mtr = MethodTypeDesc.of(fieldType); break;
            case STATIC_SETTER: mtr = MethodTypeDesc.of(CR_void, fieldType); break;
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
    static ConstantMethodHandleDesc ofConstructor(ClassDesc clazz,
                                                  ClassDesc... paramTypes) {
        return MethodHandleDesc.of(CONSTRUCTOR, clazz, ConstantDescs.DEFAULT_NAME,
                                   MethodTypeDesc.of(CR_void, paramTypes));
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
