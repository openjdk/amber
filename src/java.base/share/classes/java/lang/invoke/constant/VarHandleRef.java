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

import jdk.internal.lang.annotation.Foldable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import static java.lang.invoke.constant.ConstantRefs.CR_ClassRef;
import static java.lang.invoke.constant.ConstantRefs.CR_String;
import static java.lang.invoke.constant.ConstantRefs.CR_VarHandleRef;

/**
 * A symbolic reference for a {@link VarHandle} constant.
 */
public final class VarHandleRef extends DynamicConstantRef<VarHandle>
        implements Constable<ConstantRef<VarHandle>> {

    private static final ConstantRef<?>[] EMPTY_ARGS = new ConstantRef<?>[0];

    /**
     * Kinds of variable handle refs
     */
    private enum Kind {
        @Foldable
        FIELD(ConstantRefs.BSM_VARHANDLE_FIELD, ConstantRefs.MHR_VARHANDLEREF_OFFIELD),
        @Foldable
        STATIC_FIELD(ConstantRefs.BSM_VARHANDLE_STATIC_FIELD, ConstantRefs.MHR_VARHANDLEREF_OFSTATIC),
        @Foldable
        ARRAY(ConstantRefs.BSM_VARHANDLE_ARRAY, ConstantRefs.MHR_VARHANDLEREF_OFARRAY);

        final MethodHandleRef bootstrapMethod;
        final MethodHandleRef refFactory;

        Kind(MethodHandleRef bootstrapMethod, MethodHandleRef refFactory) {
            this.bootstrapMethod = bootstrapMethod;
            this.refFactory = refFactory;
        }

        List<ConstantRef<?>> toBSMArgs(ClassRef declaringClass, String name, ClassRef varType) {
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
    private final ClassRef declaringClass;
    private final ClassRef varType;

    /**
     * Construct a {@linkplain VarHandleRef}
     *
     * @param kind the kind of of the var handle
     * @param name the name of the field, for field var handles
     * @param declaringClass the name of the declaring class, for field var handles
     * @param varType the type of the variable
     * @throws NullPointerException if any required argument is null
     */
    private VarHandleRef(Kind kind, String name, ClassRef declaringClass, ClassRef varType) {
        super(kind.bootstrapMethod, name,
              ConstantRefs.CR_VarHandle,
              kind.toBSMArgs(declaringClass, name, varType).toArray(EMPTY_ARGS));
        this.kind = kind;
        this.declaringClass = declaringClass;
        this.varType = varType;
    }

    /**
     * Returns a {@code VarHandleRef} corresponding to a {@link VarHandle}
     * for an instance field.
     *
     * @param declaringClass the class in which the field is declared
     * @param name the name of the field
     * @param fieldType the type of the field
     * @return the {@code VarHandleRef}
     * @throws NullPointerException if any of the arguments are null
     */
    @Foldable
    public static VarHandleRef ofField(ClassRef declaringClass, String name, ClassRef fieldType) {
        Objects.requireNonNull(declaringClass);
        Objects.requireNonNull(name);
        Objects.requireNonNull(fieldType);
        return new VarHandleRef(Kind.FIELD, name, declaringClass, fieldType);
    }

    /**
     * Returns a {@code VarHandleRef} corresponding to a {@link VarHandle}
     * for a static field.
     *
     * @param declaringClass the class in which the field is declared
     * @param name the name of the field
     * @param fieldType the type of the field
     * @return the {@code VarHandleRef}
     * @throws NullPointerException if any of the arguments are null
     */
    @Foldable
    public static VarHandleRef ofStaticField(ClassRef declaringClass, String name, ClassRef fieldType) {
        Objects.requireNonNull(declaringClass);
        Objects.requireNonNull(name);
        Objects.requireNonNull(fieldType);
        return new VarHandleRef(Kind.STATIC_FIELD, name, declaringClass, fieldType);
    }

    /**
     * Returns a {@code VarHandleRef} corresponding to a {@link VarHandle}
     * for for an array type.
     *
     * @param arrayClass the type of the array
     * @return the {@code VarHandleRef}
     * @throws NullPointerException if any of the arguments are null
     */
    @Foldable
    public static VarHandleRef ofArray(ClassRef arrayClass) {
        Objects.requireNonNull(arrayClass);
        if (!arrayClass.isArray())
            throw new IllegalArgumentException("Array class argument not an array: " + arrayClass);
        return new VarHandleRef(Kind.ARRAY, "_", arrayClass, arrayClass.componentType());
    }

    /**
     * Returns the type of the variable described by this symbolic reference.
     *
     * @return the variable type
     */
    @Foldable
    public ClassRef varType() { return varType; }

    // @@@ should this be the of co-ordinate types? there by better mirroring
    // VarHandle this makes it slightly more involved since the array VH has
    // to inject it's index
    /**
     * Returns the declaring class of the variable described by this symbolic
     * reference.
     *
     * <p>If the declaring class is an array type then the declaring class
     * will be the component type of the array type.
     *
     * @return the declaring class
     */
    @Foldable
    public ClassRef declaringClass() { return declaringClass; }

    /* @@@
    MethodTypeRef accessModeTypeRef(AccessMode accessMode)
     */

    /* @@@
    MethodHandleRef toMethodHandleRef(AccessMode accessMode)
     */

    @Override
    public VarHandle resolveConstantRef(MethodHandles.Lookup lookup) throws ReflectiveOperationException {
        switch (kind) {
            case FIELD:
                return lookup.findVarHandle(declaringClass.resolveConstantRef(lookup),
                                            constantName(),
                                            varType.resolveConstantRef(lookup));
            case STATIC_FIELD:
                return lookup.findStaticVarHandle(declaringClass.resolveConstantRef(lookup),
                                                  constantName(),
                                                  varType.resolveConstantRef(lookup));
            case ARRAY:
                return MethodHandles.arrayElementVarHandle(declaringClass.resolveConstantRef(lookup));
            default:
                throw new InternalError("Cannot reach here");
        }
    }

    @Override
    public Optional<ConstantRef<ConstantRef<VarHandle>>> toConstantRef(MethodHandles.Lookup lookup) {
        try {
            ArrayList<ConstantRef<?>> args = new ArrayList<>();
            args.add(declaringClass.toConstantRef(lookup).orElseThrow());
            if (kind != Kind.ARRAY) {
                args.add(constantName());
                args.add(varType.toConstantRef(lookup).orElseThrow());
            }
            return Optional.of(DynamicConstantRef.ofInvoke(kind.refFactory, CR_VarHandleRef,
                                                           args.toArray(EMPTY_ARGS)));
        } catch (NoSuchElementException e) {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        switch (kind) {
            case FIELD:
            case STATIC_FIELD:
                return String.format("VarHandleRef[%s%s.%s:%s]",
                                     (kind == Kind.STATIC_FIELD) ? "static " : "",
                                     declaringClass.simpleName(), constantName(), varType.simpleName());
            case ARRAY:
                return String.format("VarHandleRef[%s[]]", declaringClass.simpleName());
            default:
                throw new InternalError("Cannot reach here");
        }
    }
}
