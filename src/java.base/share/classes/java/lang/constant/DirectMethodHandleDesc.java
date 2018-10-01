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
import java.util.OptionalInt;
import java.util.stream.Stream;

import jdk.internal.vm.annotation.Stable;

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
 * A <a href="package-summary.html#nominal">nominal descriptor</a> for a direct
 * {@link MethodHandle}.  A {@linkplain DirectMethodHandleDescImpl} corresponds to
 * a {@code Constant_MethodHandle_info} entry in the constant pool of a classfile.
 *
 * @apiNote In the future, if the Java language permits, {@linkplain DirectMethodHandleDesc}
 * may become a {@code sealed} interface, which would prohibit subclassing except
 * by explicitly permitted types.  Non-platform classes should not implement
 * {@linkplain DirectMethodHandleDesc} directly.
 */
public interface DirectMethodHandleDesc extends MethodHandleDesc {
    /**
     * Kinds of method handles that can be described with {@linkplain DirectMethodHandleDesc}.
     */
    enum Kind {
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

        /**
         * Find the enumeration member with the given {@code refKind} field.
         * Behaves as if {@code valueOf(refKind, false)}.  As a special case,
         * if {@code refKind} is {@code REF_invokeInterface} (9) then the
         * {@code isInterface} field will be true.
         *
         * @param refKind refKind of desired member
         * @return the matching enumeration member
         * @throws IllegalArgumentException if there is no such member
         */
        public static Kind valueOf(int refKind) {
            return valueOf(refKind, false);
        }

        /**
         * Find the enumeration member with the given {@code refKind} and
         * {@code isInterface} fields. If {@code isInterface} is true and there
         * is no such enumeration member, return the member, if any, with the
         * same {@code refKind} and a false {@code isInterface} field. If
         * {@code isInterface} is true but there is no such enumeration member,
         * then the result of {@code valueOf(refKind, false)} is returned.  As
         * a special case, if {@code refKind} is {@code REF_invokeVirtual} (5) and
         * {@code isInterface} is true, then the result of
         * {@code valueOf(REF_invokeInterface, false)} is returned, and if
         * {@code isInterface} is false and {@code refKind} is {@code REF_invokeInterface},
         * {@code INTERFACE_VIRTUAL} is returned.
         *
         * @param refKind refKind of desired member
         * @param isInterface whether desired member is for interface methods
         * @return the matching enumeration member
         * @throws IllegalArgumentException if there is no such member
         */
        public static Kind valueOf(int refKind, boolean isInterface) {
            int i = tableIndex(refKind, isInterface);
            if (i >= 0 && i < TABLE.length) {
                Kind kind = TABLE[i];
                if (kind.refKind == refKind && kind.isInterface == isInterface) {
                    return kind;
                }
            }
            if (isInterface)
                return valueOf(refKind);
            else if (refKind == REF_invokeInterface)
                return INTERFACE_VIRTUAL;
            else
                throw new IllegalArgumentException(String.format("refKind=%d", refKind));
        }

        private static int tableIndex(int refKind, boolean isInterface) {
            if (refKind < 0)  return refKind;
            return (refKind * 2) + (isInterface ? 1 : 0);
        }

        private static final @Stable Kind[] TABLE;

        static {
            // Pack the static table.
            int max = 0;
            for (Kind k : values())
                max = Math.max(max, tableIndex(k.refKind, k.isInterface));

            TABLE = new Kind[max+1];
            for (Kind kind : values()) {
                int i = tableIndex(kind.refKind, kind.isInterface);
                if (i >= TABLE.length || TABLE[i] != null)
                    throw new AssertionError("TABLE entry for " + kind);
                TABLE[i] = kind;
            }

            // Pack in some aliases also.
            int ii = tableIndex(REF_invokeInterface, false);
            if (TABLE[ii] != null)
                throw new AssertionError("TABLE entry for (invokeInterface, false) used by " + TABLE[ii]);
            TABLE[ii] = INTERFACE_VIRTUAL;

            for (Kind kind : values()) {
                if (!kind.isInterface) {
                    // Add extra cache entry to alias the isInterface case.
                    // For example, (REF_getStatic, X) will produce STATIC_GETTER
                    // for either truth value of X.
                    int i = tableIndex(kind.refKind, true);
                    if (TABLE[i] == null) {
                        // There is not a specific Kind for interfaces
                        if (kind == VIRTUAL)  kind = INTERFACE_VIRTUAL;
                        if (TABLE[i] == null)  TABLE[i] = kind;
                    }
                }
            }
        }
    }

    /**
     * Return the {@code kind} of the method handle described by this nominal
     * descriptor.
     *
     * @return the {@link Kind}
     */
    Kind kind();

    /**
     * Return the {@code refKind} of the method handle described by this nominal
     * reference, as defined by {@link MethodHandleInfo}.
     *
     * @return the reference kind
     */
    int refKind();

    /**
     * Indicates if the method is declared by an interface
     *
     * @return true if the method is declared by an interface
     */
    boolean isOwnerInterface();

    /**
     * Return a {@link ClassDesc} describing the class declaring the
     * method or field described by this nominal descriptor.
     *
     * @return the class declaring the method or field
     */
    ClassDesc owner();

    /**
     * Return the name of the method or field described by this nominal descriptor.
     *
     * @return the name of the method or field
     */
    String methodName();

    /**
     * Return a {@link MethodTypeDesc} describing the invocation type of the
     * method handle described by this nominal descriptor
     *
     * @return the method type
     */
    MethodTypeDesc methodType();
}
