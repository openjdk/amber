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
package java.lang.sym;

import java.lang.annotation.Foldable;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A descriptor for a {@link VarHandle} constant.
 */
public final class VarHandleRef extends DynamicConstantRef<VarHandle> {

    /**
     * Kinds of variable handle refs
     */
    private enum Kind {
        @Foldable FIELD,
        @Foldable STATIC_FIELD,
        @Foldable ARRAY;
    }

    private final Kind kind;
    private final ClassRef declaringClass;
    private final ClassRef varType;

    private VarHandleRef(Kind kind, String name, ClassRef declaringClass, ClassRef varType) {
        super(kindToBSM(kind), name,
              SymbolicRefs.CR_VarHandle,
              kindToBSMArgs(kind, declaringClass, name, varType).toArray(new SymbolicRef<?>[0]));
        this.kind = kind;
        this.declaringClass = declaringClass;
        this.varType = varType;
    }

    private static MethodHandleRef kindToBSM(Kind kind) {
        switch (kind) {
            case FIELD:
                return SymbolicRefs.BSM_VARHANDLE_FIELD;
            case STATIC_FIELD:
                return SymbolicRefs.BSM_VARHANDLE_STATIC_FIELD;
            case ARRAY:
                return SymbolicRefs.BSM_VARHANDLE_ARRAY;
            default:
                throw new InternalError("Cannot reach here");
        }
    }

    private static List<SymbolicRef<?>> kindToBSMArgs(Kind kind, ClassRef declaringClass, String name, ClassRef varType) {
        switch (kind) {
            case FIELD:
            case STATIC_FIELD:
                return List.of(declaringClass, name, varType);
            case ARRAY:
                return List.of(declaringClass);
            default:
                throw new InternalError("Cannot reach here");
        }
    }

    private static MethodHandleRef kindToMethodHandleRefFactory(Kind kind) {
        switch (kind) {
            case FIELD:
                return SymbolicRefs.MHR_VARHANDLEREF_FIELD_FACTORY;
            case STATIC_FIELD:
                return SymbolicRefs.MHR_VARHANDLEREF_STATIC_FIELD_FACTORY;
            case ARRAY:
                return SymbolicRefs.MHR_VARHANDLEREF_ARRAY_FACTORY;
            default:
                throw new InternalError("Cannot reach here");
        }
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
    public static VarHandleRef fieldVarHandle(ClassRef declaringClass, String name, ClassRef fieldType) {
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
    public static VarHandleRef staticFieldVarHandle(ClassRef declaringClass, String name, ClassRef fieldType) {
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
    public static VarHandleRef arrayVarHandle(ClassRef arrayClass) {
        Objects.requireNonNull(arrayClass);
        if (!arrayClass.isArray())
            throw new IllegalArgumentException("Array class argument not an array: " + arrayClass);
        return new VarHandleRef(Kind.STATIC_FIELD, "_", arrayClass, arrayClass.componentType());
    }

    /**
     * Returns the variable type of this variable handle.
     *
     * @return the variable type.
     */
    @Foldable
    public ClassRef varType() { return varType; }

    /**
     * Returns the declaring class of this of variable handle.
     *
     * <p>If the declaring class is an array type then the variable type
     * will be the component type of the array type.
     *
     * @@@ should this be the of co-ordinate types? there by better mirroring
     * VarHandle this makes it slightly more involved since the array VH has
     * to inject it's index
     *
     * @return the declaring class.
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
    public VarHandle resolveRef(MethodHandles.Lookup lookup) throws ReflectiveOperationException {
        switch (kind) {
            case FIELD:
                return lookup.findVarHandle(declaringClass.resolveRef(lookup),
                                            name(),
                                            varType.resolveRef(lookup));
            case STATIC_FIELD:
                return lookup.findStaticVarHandle(declaringClass.resolveRef(lookup),
                                                  name(),
                                                  varType.resolveRef(lookup));
            case ARRAY:
                return MethodHandles.arrayElementVarHandle(declaringClass.resolveRef(lookup));
            default:
                throw new InternalError("Cannot reach here");
        }
    }

    @Override
    public Optional<? extends SymbolicRef<VarHandle>> toSymbolicRef(MethodHandles.Lookup lookup) {
        var declaringClassRefRef = declaringClass.toSymbolicRef(lookup);
        if (!declaringClassRefRef.isPresent())
            return Optional.empty();

        var args = new ArrayList<SymbolicRef<?>>();
        args.add(kindToMethodHandleRefFactory(kind));
        args.add(declaringClassRefRef.get());
        if (kind != Kind.ARRAY) {
            args.add(name());
            var varTypeRefRef = varType.toSymbolicRef(lookup);
            if (!varTypeRefRef.isPresent())
                return Optional.empty();
            args.add(varTypeRefRef.get());
        }
        return Optional.of(DynamicConstantRef.<VarHandle>of(SymbolicRefs.BSM_INVOKE)
                                   .withArgs(args.toArray(new SymbolicRef<?>[0])));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var ref = (VarHandleRef) o;
        return kind == ref.kind &&
               Objects.equals(declaringClass, ref.declaringClass) &&
               Objects.equals(name(), ref.name()) &&
               Objects.equals(varType, ref.varType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, declaringClass, name(), varType);
    }

    @Override
    public String toString() {
        switch (kind) {
            case FIELD:
            case STATIC_FIELD:
                return String.format("VarHandleRef[kind=%s, declaringClass=%s, name=%s, fieldType=%s]", kind, declaringClass, name(), varType);
            case ARRAY:
                return String.format("VarHandleRef[kind=%s, arrayClass=%s]", kind, declaringClass);
            default:
                throw new InternalError("Cannot reach here");
        }
    }
}
