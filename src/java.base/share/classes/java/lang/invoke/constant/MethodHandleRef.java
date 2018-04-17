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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodType;

import jdk.internal.lang.annotation.Foldable;

import static java.lang.invoke.MethodHandleInfo.REF_getField;
import static java.lang.invoke.MethodHandleInfo.REF_getStatic;
import static java.lang.invoke.MethodHandleInfo.REF_invokeInterface;
import static java.lang.invoke.MethodHandleInfo.REF_invokeSpecial;
import static java.lang.invoke.MethodHandleInfo.REF_invokeStatic;
import static java.lang.invoke.MethodHandleInfo.REF_invokeVirtual;
import static java.lang.invoke.MethodHandleInfo.REF_newInvokeSpecial;
import static java.lang.invoke.MethodHandleInfo.REF_putField;
import static java.lang.invoke.MethodHandleInfo.REF_putStatic;
import static java.lang.invoke.constant.ConstantRefs.CR_void;
import static java.lang.invoke.constant.MethodHandleRef.Kind.CONSTRUCTOR;
import static java.lang.invoke.constant.MethodHandleRef.Kind.STATIC;

/**
 * A nominal descriptor for a {@link MethodHandle} constant.
 */
public interface MethodHandleRef
        extends ConstantRef<MethodHandle>, Constable<ConstantRef<MethodHandle>> {
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


    /**
     * Return a {@code MethodHandleRef} corresponding to an invocation of a
     * declared method, invocation of a constructor, or access to a field.
     *
     * <p>If {@code kind} is {@code CONSTRUCTOR}, the name is ignored and the return
     * type of the invocation type must be {@code void}.  If {@code kind} corresponds
     * to a field access, the invocation type must be consistent with that kind
     * of field access and the type of the field; instance field accessors must
     * take a leading receiver parameter, getters must return the type of the
     * field, setters must take a new value for the field and return {@code void}.
     *
     * <p>For constructor and field access, the methods {@link #ofField(Kind, ClassRef, String, ClassRef)}
     * and {@link #ofConstructor(ClassRef, ClassRef...)} may be more convenient.
     *
     * @param kind The kind of method handle to be described
     * @param clazz the class declaring the method, constructor, or field
     * @param name the name of the method or field (ignored if {@code kind} is
     * {@code CONSTRUCTOR}), as per JVMS 4.2.2
     * @param type the invocation type of the method handle
     * @return the {@code MethodHandleRef}
     * @throws NullPointerException if any of the non-ignored arguments are null
     */
    @Foldable
    static ConstantMethodHandleRef of(Kind kind,
                                      ClassRef clazz,
                                      String name,
                                      MethodTypeRef type) {
        return new ConstantMethodHandleRef(kind, clazz, name, type);
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to an invocation of a
     * declared method, invocation of a constructor, or access to a field.
     *
     * <p>If {@code kind} is {@code CONSTRUCTOR}, the name is ignored and the return
     * type of the invocation type must be {@code void}.  If {@code kind} corresponds
     * to a field access, the invocation type must be consistent with that kind
     * of field access and the type of the field; instance field accessors must
     * take a leading receiver parameter, getters must return the type of the
     * field, setters must take a new value for the field and return {@code void}.
     * The method {@link #ofField(Kind, ClassRef, String, ClassRef)} will construct
     * the appropriate invocation given the type of the field.
     *
     * @param kind The kind of method handle to be described
     * @param clazz the class declaring the method, constructor, or field
     * @param name the name of the method or field (ignored if {@code kind} is
     * {@code CONSTRUCTOR}), as per JVMS 4.2.2
     * @param descriptorString method descriptor string for the invocation type
     * of the method handle, as per JVMS 4.3.3
     * @return the {@code MethodHandleRef}
     * @throws NullPointerException if any of the non-ignored arguments are null
     */
    @Foldable
    static ConstantMethodHandleRef of(Kind kind,
                                      ClassRef clazz,
                                      String name,
                                      String descriptorString) {
        return of(kind, clazz, name, MethodTypeRef.ofDescriptor(descriptorString));
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to an invocation of a
     * declared method, invocation of a constructor, or access to a field.
     *
     * <p>If {@code kind} is {@code CONSTRUCTOR}, the name is ignored and the return
     * type of the invocation type must be {@code void}.  If {@code kind} corresponds
     * to a field access, the invocation type must be consistent with that kind
     * of field access and the type of the field; instance field accessors must
     * take a leading receiver parameter, getters must return the type of the
     * field, setters must take a new value for the field and return {@code void}.
     * The method {@link #ofField(Kind, ClassRef, String, ClassRef)} will construct
     * the appropriate invocation given the type of the field.
     *
     * @param kind The kind of method handle to be described
     * @param clazz the class declaring the method, constructor, or field
     * @param name the name of the method or field (ignored if {@code kind} is
     * {@code CONSTRUCTOR}), as per JVMS 4.2.2
     * @param returnType the return type of the method handle
     * @param paramTypes the parameter types of the method handle
     * @return the {@code MethodHandleRef}
     * @throws NullPointerException if any of the non-ignored arguments are null
     */
    @Foldable
    static ConstantMethodHandleRef of(Kind kind,
                                      ClassRef clazz,
                                      String name,
                                      ClassRef returnType,
                                      ClassRef... paramTypes) {
        return of(kind, clazz, name, MethodTypeRef.of(returnType, paramTypes));
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to accessing a field
     *
     * @param kind the kind of the method handle; must be one of {@code GETTER},
     *             {@code SETTER}, {@code STATIC_GETTER}, or {@code STATIC_SETTER}
     * @param clazz the class declaring the field
     * @param fieldName the name of the field, as per JVMS 4.2.2
     * @param fieldType the type of the field
     * @return the {@code MethodHandleRef}
     * @throws NullPointerException if any of the arguments are null
     */
    @Foldable
    static ConstantMethodHandleRef ofField(Kind kind,
                                           ClassRef clazz,
                                           String fieldName,
                                           ClassRef fieldType) {
        MethodTypeRef mtr;
        switch (kind) {
            case GETTER: mtr = MethodTypeRef.of(fieldType, clazz); break;
            case SETTER: mtr = MethodTypeRef.of(CR_void, clazz, fieldType); break;
            case STATIC_GETTER: mtr = MethodTypeRef.of(fieldType); break;
            case STATIC_SETTER: mtr = MethodTypeRef.of(CR_void, fieldType); break;
            default:
                throw new IllegalArgumentException(kind.toString());
        }
        return MethodHandleRef.of(kind, clazz, fieldName, mtr);
    }

    /**
     * Return a {@code MethodHandleRef} corresponding to invocation of a constructor
     *
     * @param clazz the class declaring the constuctor
     * @param paramTypes the parameter types of the constructor
     * @return the {@code MethodHandleRef}
     * @throws NullPointerException if any of the arguments are null
     */
    @Foldable
    static ConstantMethodHandleRef ofConstructor(ClassRef clazz,
                                                 ClassRef... paramTypes) {
        return MethodHandleRef.of(CONSTRUCTOR, clazz, ConstantRefs.DEFAULT_NAME,
                                  MethodTypeRef.of(CR_void, paramTypes));
    }

    /**
     * Return the type of the method handle described by this nominal descriptor
     * @return the method type
     */
    @Foldable
    MethodTypeRef methodType();

    /**
     * Return a {@linkplain MethodHandleRef} that describes this method handle
     * adapted to a different type, as if by {@link MethodHandle#asType(MethodType)}.
     *
     * @param type the new type
     * @return the adapted descriptor
     */
    @Foldable
    default MethodHandleRef asType(MethodTypeRef type) {
        return (methodType().equals(type)) ? this : new AsTypeMethodHandleRef(this, type);
    }
}
