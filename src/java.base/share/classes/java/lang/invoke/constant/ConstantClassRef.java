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
import java.lang.reflect.Array;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.lang.invoke.constant.ConstantRefs.CR_ClassRef;
import static java.lang.invoke.constant.ConstantUtils.dropFirstAndLastChar;
import static java.lang.invoke.constant.ConstantUtils.internalToBinary;
import static java.util.Objects.requireNonNull;

/**
 * A nominal descriptor for a class, interface, or array type.  A
 * {@linkplain ConstantClassRef} corresponds to a {@code Constant_Class_info}
 * entry in the constant pool of a classfile.
 */
public class ConstantClassRef implements ClassRef {
    private static final Pattern TYPE_DESC = Pattern.compile("(\\[*)(V|I|J|S|B|C|F|D|Z|L[^/.\\[;][^.\\[;]*;)");

    private final String descriptor;

    /**
     * Create a {@linkplain ClassRef} from a descriptor string for a class or
     * interface type
     *
     * @param descriptor a field descriptor string for a class or interface type,
     *                   as per JVMS 4.3.2
     * @throws IllegalArgumentException if the descriptor string is not a valid
     * field descriptor string, or does not describe a class or interface type
     */
    ConstantClassRef(String descriptor) {
        // @@@ Replace validation with a lower-overhead mechanism than regex
        // Follow the trail from MethodType.fromMethodDescriptorString to
        // parsing code in sun/invoke/util/BytecodeDescriptor.java which could
        // be extracted and/or shared
        // @@@ regex actually permits primitive types
        requireNonNull(descriptor);
        if (!TYPE_DESC.matcher(descriptor).matches())
            throw new IllegalArgumentException(String.format("not a valid type descriptor: %s", descriptor));
        this.descriptor = descriptor;
    }

    @Override
    public String descriptorString() {
        return descriptor;
    }

    @Override
    public Class<?> resolveConstantRef(MethodHandles.Lookup lookup)
            throws ReflectiveOperationException {
        ClassRef c = this;
        int depth = ConstantUtils.arrayDepth(descriptorString());
        for (int i=0; i<depth; i++)
            c = c.componentType();

        if (c.descriptorString().length() == 1)
            return Class.forName(descriptorString(), true, lookup.lookupClass().getClassLoader());
        else {
            Class<?> clazz = Class.forName(internalToBinary(dropFirstAndLastChar(c.descriptorString())),
                                           true, lookup.lookupClass().getClassLoader());
            for (int i = 0; i < depth; i++)
                clazz = Array.newInstance(clazz, 0).getClass();
            return clazz;
        }
    }

    @Override
    public Optional<? extends ConstantRef<? super ConstantRef<Class<?>>>> toConstantRef(MethodHandles.Lookup lookup) {
        return Optional.of(DynamicConstantRef.of(RefBootstraps.BSM_CLASSREF, CR_ClassRef).withArgs(descriptor));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClassRef constant = (ClassRef) o;
        return Objects.equals(descriptor, constant.descriptorString());
    }

    @Override
    public int hashCode() {
        return descriptor != null ? descriptor.hashCode() : 0;
    }

    @Override
    public String toString() {
        return String.format("ClassRef[%s]", displayName());
    }
}
