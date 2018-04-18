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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jdk.internal.lang.annotation.Foldable;

import static java.lang.invoke.constant.ConstantDescs.CR_VarHandleDesc;

/**
 * A nominal descriptor for a {@link VarHandle} constant.
 */
public final class VarHandleDesc extends DynamicConstantDesc<VarHandle>
        implements Constable<ConstantDesc<VarHandle>> {

    /**
     * Kinds of variable handle descs
     */
    private enum Kind {
        FIELD(ConstantDescs.BSM_VARHANDLE_FIELD, ConstantDescs.MHR_VARHANDLEDESC_OFFIELD),
        STATIC_FIELD(ConstantDescs.BSM_VARHANDLE_STATIC_FIELD, ConstantDescs.MHR_VARHANDLEDESC_OFSTATIC),
        ARRAY(ConstantDescs.BSM_VARHANDLE_ARRAY, ConstantDescs.MHR_VARHANDLEDESC_OFARRAY);

        final ConstantMethodHandleDesc bootstrapMethod;
        final ConstantMethodHandleDesc descFactory;

        Kind(ConstantMethodHandleDesc bootstrapMethod,
             ConstantMethodHandleDesc descFactory) {
            this.bootstrapMethod = bootstrapMethod;
            this.descFactory = descFactory;
        }

        List<ConstantDesc<?>> toBSMArgs(ClassDesc declaringClass, String name, ClassDesc varType) {
            switch (this) {
                case FIELD:
                case STATIC_FIELD:
                    return List.of(declaringClass, name, varType);
                case ARRAY:
                    return List.of(declaringClass);
                default:
                    throw new InternalError("Cannot reach here");
            }
        }
    }

    private final Kind kind;
    private final ClassDesc declaringClass;
    private final ClassDesc varType;

    /**
     * Construct a {@linkplain VarHandleDesc}
     *
     * @param kind the kind of of the var handle
     * @param name the name of the field, for field var handles
     * @param declaringClass the name of the declaring class, for field var handles
     * @param varType the type of the variable
     * @throws NullPointerException if any required argument is null
     */
    private VarHandleDesc(Kind kind, String name, ClassDesc declaringClass, ClassDesc varType) {
        super(kind.bootstrapMethod, name,
              ConstantDescs.CR_VarHandle,
              kind.toBSMArgs(declaringClass, name, varType).toArray(ConstantUtils.EMPTY_CONSTANTDESC));
        this.kind = kind;
        this.declaringClass = declaringClass;
        this.varType = varType;
    }

    /**
     * Returns a {@code VarHandleDesc} corresponding to a {@link VarHandle}
     * for an instance field.
     *
     * @param declaringClass the class in which the field is declared
     * @param name the name of the field
     * @param fieldType the type of the field
     * @return the {@code VarHandleDesc}
     * @throws NullPointerException if any of the arguments are null
     */
    @Foldable
    public static VarHandleDesc ofField(ClassDesc declaringClass, String name, ClassDesc fieldType) {
        Objects.requireNonNull(declaringClass);
        Objects.requireNonNull(name);
        Objects.requireNonNull(fieldType);
        return new VarHandleDesc(Kind.FIELD, name, declaringClass, fieldType);
    }

    /**
     * Returns a {@code VarHandleDesc} corresponding to a {@link VarHandle}
     * for a static field.
     *
     * @param declaringClass the class in which the field is declared
     * @param name the name of the field
     * @param fieldType the type of the field
     * @return the {@code VarHandleDesc}
     * @throws NullPointerException if any of the arguments are null
     */
    @Foldable
    public static VarHandleDesc ofStaticField(ClassDesc declaringClass, String name, ClassDesc fieldType) {
        Objects.requireNonNull(declaringClass);
        Objects.requireNonNull(name);
        Objects.requireNonNull(fieldType);
        return new VarHandleDesc(Kind.STATIC_FIELD, name, declaringClass, fieldType);
    }

    /**
     * Returns a {@code VarHandleDesc} corresponding to a {@link VarHandle}
     * for for an array type.
     *
     * @param arrayClass the type of the array
     * @return the {@code VarHandleDesc}
     * @throws NullPointerException if any of the arguments are null
     */
    @Foldable
    public static VarHandleDesc ofArray(ClassDesc arrayClass) {
        Objects.requireNonNull(arrayClass);
        if (!arrayClass.isArray())
            throw new IllegalArgumentException("Array class argument not an array: " + arrayClass);
        return new VarHandleDesc(Kind.ARRAY, ConstantDescs.DEFAULT_NAME, arrayClass, arrayClass.componentType());
    }

    /**
     * Returns the type of the variable described by this descriptor
     *
     * @return the variable type
     */
    @Foldable
    public ClassDesc varType() {
        return varType;
    }

    // @@@ should this be the of co-ordinate types? there by better mirroring
    // VarHandle this makes it slightly more involved since the array VH has
    // to inject it's index
    /**
     * Returns the declaring class of the variable described by this descriptor.
     *
     * <p>If the declaring class is an array type then the declaring class
     * will be the component type of the array type.
     *
     * @return the declaring class
     */
    @Foldable
    public ClassDesc declaringClass() {
        return declaringClass;
    }

    /* @@@
    MethodTypeDesc accessModeTypeRef(AccessMode accessMode)
     */

    /* @@@
    MethodHandleDesc toMethodHandleRef(AccessMode accessMode)
     */

    @Override
    public VarHandle resolveConstantDesc(MethodHandles.Lookup lookup)
            throws ReflectiveOperationException {
        switch (kind) {
            case FIELD:
                return lookup.findVarHandle(declaringClass.resolveConstantDesc(lookup),
                                            constantName(),
                                            varType.resolveConstantDesc(lookup));
            case STATIC_FIELD:
                return lookup.findStaticVarHandle(declaringClass.resolveConstantDesc(lookup),
                                                  constantName(),
                                                  varType.resolveConstantDesc(lookup));
            case ARRAY:
                return MethodHandles.arrayElementVarHandle(declaringClass.resolveConstantDesc(lookup));
            default:
                throw new InternalError("Cannot reach here");
        }
    }

    @Override
    public Optional<? extends ConstantDesc<? super ConstantDesc<VarHandle>>> describeConstable(MethodHandles.Lookup lookup) {
        Constable<?>[] args =
                (kind == Kind.ARRAY)
                ? new Constable<?>[] { declaringClass }
                : new Constable<?>[] { declaringClass, constantName(), varType };
        return ConstantUtils.symbolizeHelper(lookup, kind.descFactory, CR_VarHandleDesc, args);
    }

    @Override
    public String toString() {
        switch (kind) {
            case FIELD:
            case STATIC_FIELD:
                return String.format("VarHandleDesc[%s%s.%s:%s]",
                                     (kind == Kind.STATIC_FIELD) ? "static " : "",
                                     declaringClass.displayName(), constantName(), varType.displayName());
            case ARRAY:
                return String.format("VarHandleDesc[%s[]]", declaringClass.displayName());
            default:
                throw new InternalError("Cannot reach here");
        }
    }
}
