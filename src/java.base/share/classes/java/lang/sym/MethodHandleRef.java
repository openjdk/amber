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

import jdk.internal.vm.annotation.Foldable;

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
import static java.lang.sym.MethodHandleRef.Kind.STATIC;
import static java.lang.sym.ConstantRefs.CR_void;
import static java.util.Objects.requireNonNull;

/**
 * A symbolic reference for a {@link MethodHandle} constant.
 */
public interface MethodHandleRef
        extends ConstantRef<MethodHandle>, Constable<ConstantRef<MethodHandle>> {
    /**
     * Kinds of method handle refs
     */
    enum Kind {
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
    static ConstantMethodHandleRef of(Kind kind, ClassRef clazz, String name, MethodTypeRef type) {
        return new ConstantMethodHandleRef(kind, clazz, name, type);
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
    static ConstantMethodHandleRef of(Kind kind, ClassRef clazz, String name, String descriptorString) {
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
    static ConstantMethodHandleRef of(Kind kind, ClassRef clazz, String name, ClassRef returnType, ClassRef... paramTypes) {
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
    static ConstantMethodHandleRef ofDynamicCallsite(ClassRef clazz, String name, ClassRef returnType, ClassRef... paramTypes) {
        return of(STATIC, clazz, name, MethodTypeRef.of(returnType, paramTypes).insertParameterTypes(0, ConstantRefs.INDY_BOOTSTRAP_ARGS));
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
    static ConstantMethodHandleRef ofDynamicConstant(ClassRef clazz, String name, ClassRef returnType, ClassRef... paramTypes) {
        return of(STATIC, clazz, name, MethodTypeRef.of(returnType, paramTypes).insertParameterTypes(0, ConstantRefs.CONDY_BOOTSTRAP_ARGS));
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
    static ConstantMethodHandleRef ofField(Kind kind, ClassRef clazz, String name, ClassRef type) {
        MethodTypeRef mtr;
        switch (kind) {
            case GETTER: mtr = MethodTypeRef.of(type, clazz); break;
            case SETTER: mtr = MethodTypeRef.of(CR_void, clazz, type); break;
            case STATIC_GETTER: mtr = MethodTypeRef.of(type); break;
            case STATIC_SETTER: mtr = MethodTypeRef.of(CR_void, type); break;
            default:
                throw new IllegalArgumentException(kind.toString());
        }
        return new ConstantMethodHandleRef(kind, clazz, name, mtr);
    }

    /**
     * Return the type of the method described by this symbolic reference
     * @return the method type
     */
    @Foldable
    MethodTypeRef methodType();

    /**
     * Return a {@linkplain MethodHandleRef} that describes this method handle
     * adapted to a different type, as if by {@link MethodHandle#asType(MethodType)}.
     *
     * @param type the new type
     * @return the adapted method handle reference
     */
    @Foldable
    default MethodHandleRef asType(MethodTypeRef type) {
        return (methodType().equals(type)) ? this : new AsTypeMethodHandleRef(this, type);
    }
}
