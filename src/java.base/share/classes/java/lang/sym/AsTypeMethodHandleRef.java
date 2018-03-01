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
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Optional;

import static java.lang.sym.ConstantRefs.BSM_INVOKE;
import static java.lang.sym.ConstantRefs.CR_MethodHandle;
import static java.util.Objects.requireNonNull;

/**
 * AsTypeMethodHandleRef
 *
 * @author Brian Goetz
 */
final class AsTypeMethodHandleRef extends DynamicConstantRef<MethodHandle>
        implements MethodHandleRef {

    private final MethodHandleRef underlying;
    private final MethodTypeRef type;

    AsTypeMethodHandleRef(MethodHandleRef underlying, MethodTypeRef type) {
        super(BSM_INVOKE, "_", CR_MethodHandle,
              ConstantRefs.MHR_METHODHANDLE_ASTYPE, underlying, type);
        // Any type checking we can do?
        this.underlying = requireNonNull(underlying);
        this.type = requireNonNull(type);
    }

    @Override
    @Foldable
    public MethodTypeRef methodType() {
        return type;
    }

    @Override
    public MethodHandle resolveConstantRef(MethodHandles.Lookup lookup) throws ReflectiveOperationException {
        MethodHandle handle = underlying.resolveConstantRef(lookup);
        MethodType methodType = type.resolveConstantRef(lookup);
        return handle.asType(methodType);
    }

    @Override
    public Optional<? extends ConstantRef<? super ConstantRef<MethodHandle>>> toConstantRef(MethodHandles.Lookup lookup) {
        return DynamicConstantRef.symbolizeHelper(lookup, ConstantRefs.MHR_METHODHANDLEREF_ASTYPE, ConstantRefs.CR_MethodHandleRef,
                                                  underlying, type);
    }

    @Override
    public String toString() {
        return underlying.toString() + String.format(".asType%s", type.simpleDescriptor());
    }

    // @@@ canonical?
}
