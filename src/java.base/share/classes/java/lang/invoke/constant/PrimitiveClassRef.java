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
package java.lang.invoke.constant;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import sun.invoke.util.Wrapper;

import static java.util.Objects.requireNonNull;

/**
 * PrimitiveClassRef
 *
 * @author Brian Goetz
 */
final class PrimitiveClassRef extends DynamicConstantRef<Class<?>> implements ClassRef {
    private final String descriptor;

    /**
     * Create a {@linkplain ClassRef} from a descriptor string for a primitive type
     *
     * @param descriptor the descriptor string
     * @throws IllegalArgumentException if the descriptor string does not
     * describe a valid primitive type
     */
    PrimitiveClassRef(String descriptor) {
        super(ConstantRefs.BSM_PRIMITIVE_CLASS, requireNonNull(descriptor), ConstantRefs.CR_Class);
        if (descriptor.length() != 1
            || "VIJCSBFDZ".indexOf(descriptor.charAt(0)) < 0)
            throw new IllegalArgumentException(String.format("%s is not a valid primitive type descriptor", descriptor));
        this.descriptor = descriptor;
    }

    @Override
    public String descriptorString() {
        return descriptor;
    }

    @Override
    public Class<?> resolveConstantRef(MethodHandles.Lookup lookup) {
        return Wrapper.forBasicType(descriptorString().charAt(0)).primitiveType();
    }

    @Override
    public Optional<? extends ConstantRef<? super ConstantRef<Class<?>>>> toConstantRef(MethodHandles.Lookup lookup) {
        return DynamicConstantRef.symbolizeHelper(lookup, ConstantRefs.MHR_CLASSREF_FACTORY, ConstantRefs.CR_ClassRef, descriptorString());
    }

    @Override
    public String toString() {
        return String.format("PrimitiveClassRef[%s]", simpleName());
    }
}
