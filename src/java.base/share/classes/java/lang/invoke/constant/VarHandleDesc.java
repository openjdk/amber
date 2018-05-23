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

import static java.lang.invoke.constant.ConstantDescs.CR_VarHandleDesc;

/**
 * A <a href="package-summary.html#nominal">nominal descriptor</a> for a
 * {@link VarHandle} constant.
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
     * Construct a {@linkplain VarHandleDesc} given a kind, name, and declaring
     * class.
     *
     * @param kind the kind of of the var handle
     * @param name the name of the field, , as per JVMS 4.2.2, for field var
     *             handles; otherwise ignored
     * @param declaringClass a {@link ClassDesc} describing the declaring class,
     *                       for field var handles
     * @param varType a {@link ClassDesc} describing the type of the variable
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
     * Returns a {@linkplain VarHandleDesc} corresponding to a {@link VarHandle}
     * for an instance field.
     *
     * @param name the name of the field, as per JVMS 4.2.2
     * @param declaringClass a {@link ClassDesc} describing the declaring class,
     *                       for field var handles
     * @param fieldType a {@link ClassDesc} describing the type of the field
     * @return the {@linkplain VarHandleDesc}
     * @throws NullPointerException if any of the arguments are null
     */
    public static VarHandleDesc ofField(ClassDesc declaringClass, String name, ClassDesc fieldType) {
        Objects.requireNonNull(declaringClass);
        Objects.requireNonNull(name);
        Objects.requireNonNull(fieldType);
        return new VarHandleDesc(Kind.FIELD, name, declaringClass, fieldType);
    }

    /**
     * Returns a {@linkplain VarHandleDesc} corresponding to a {@link VarHandle}
     * for a static field.
     *
     * @param name the name of the field, as per JVMS 4.2.2
     * @param declaringClass a {@link ClassDesc} describing the declaring class,
     *                       for field var handles
     * @param fieldType a {@link ClassDesc} describing the type of the field
     * @return the {@linkplain VarHandleDesc}
     * @throws NullPointerException if any of the arguments are null
     */
    public static VarHandleDesc ofStaticField(ClassDesc declaringClass, String name, ClassDesc fieldType) {
        Objects.requireNonNull(declaringClass);
        Objects.requireNonNull(name);
        Objects.requireNonNull(fieldType);
        return new VarHandleDesc(Kind.STATIC_FIELD, name, declaringClass, fieldType);
    }

    /**
     * Returns a {@linkplain VarHandleDesc} corresponding to a {@link VarHandle}
     * for for an array type.
     *
     * @param arrayClass a {@link ClassDesc} describing the type of the array
     * @return the {@linkplain VarHandleDesc}
     * @throws NullPointerException if any of the arguments are null
     */
    public static VarHandleDesc ofArray(ClassDesc arrayClass) {
        Objects.requireNonNull(arrayClass);
        if (!arrayClass.isArray())
            throw new IllegalArgumentException("Array class argument not an array: " + arrayClass);
        return new VarHandleDesc(Kind.ARRAY, ConstantDescs.DEFAULT_NAME, arrayClass, arrayClass.componentType());
    }

    /**
     * Returns a {@link ClassDesc} describing the type of the variable described
     * by this descriptor.
     *
     * @return the variable type
     */
    public ClassDesc varType() {
        return varType;
    }

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
    public Optional<? extends ConstantDesc<? super ConstantDesc<VarHandle>>> describeConstable() {
        Constable<?>[] args =
                (kind == Kind.ARRAY)
                ? new Constable<?>[] { declaringClass }
                : new Constable<?>[] { declaringClass, constantName(), varType };
        return ConstantUtils.symbolizeHelper(kind.descFactory, CR_VarHandleDesc, args);
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
