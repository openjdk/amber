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

import jdk.internal.lang.annotation.Foldable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.sym.ConstantRefs.CR_ClassRef;
import static java.lang.sym.ConstantRefs.CR_MethodTypeRef;
import static java.util.Objects.requireNonNull;

/**
 * ConstantMethodTypeRef
 *
 * @author Brian Goetz
 */
public final class ConstantMethodTypeRef implements MethodTypeRef {

    private static final Pattern TYPE_DESC = Pattern.compile("(\\[*)(V|I|J|S|B|C|F|D|Z|L[^/.\\[;][^.\\[;]*;)");
    private static final Pattern pattern = Pattern.compile("\\((.*)\\)(.*)");

    private final ClassRef returnType;
    private final ClassRef[] argTypes;

    /**
     * Construct a {@linkplain MethodTypeRef} with the specified return type
     * and parameter types
     *
     * @param returnType a {@link ClassRef} describing the return type
     * @param argTypes {@link ClassRef}s describing the parameter types
     */
    ConstantMethodTypeRef(ClassRef returnType, ClassRef[] argTypes) {
        this.returnType = requireNonNull(returnType);
        this.argTypes = requireNonNull(argTypes);

        for (ClassRef cr : argTypes)
            if (cr.isPrimitive() && cr.descriptorString().equals("V"))
                throw new IllegalArgumentException("Void parameters not permitted");
    }

    /**
     * Create a {@linkplain ConstantMethodTypeRef} from a method descriptor string
     *
     * @param descriptor the method descriptor string
     * @return a {@linkplain ConstantMethodTypeRef} describing the desired method type
     * @throws IllegalArgumentException if the descriptor string is not a valid
     * method descriptor
     */
    @Foldable
    static MethodTypeRef ofDescriptor(String descriptor) {
        // @@@ Replace validation with a lower-overhead mechanism than regex
        // Follow the trail from MethodType.fromMethodDescriptorString to
        // parsing code in sun/invoke/util/BytecodeDescriptor.java which could
        // be extracted and/or shared
        Matcher matcher = pattern.matcher(descriptor);
        if (!matcher.matches())
            throw new IllegalArgumentException(String.format("%s is not a valid method descriptor", descriptor));
        String paramTypes = matcher.group(1);
        String returnType = matcher.group(2);
        if (!TYPE_DESC.matcher(returnType).matches())
            throw new IllegalArgumentException(String.format("Invalid return type %s", returnType));
        List<String> params = new ArrayList<>();
        matcher = TYPE_DESC.matcher(paramTypes);
        while (matcher.regionStart() < paramTypes.length()) {
            if (matcher.lookingAt()) {
                params.add(matcher.group());
                matcher.region(matcher.end(), matcher.regionEnd());
            }
            else
                throw new IllegalArgumentException(String.format("Invalid parameter type: %s", paramTypes.substring(matcher.regionStart(), matcher.regionEnd())));
        }
        return new ConstantMethodTypeRef(ClassRef.ofDescriptor(returnType), params.stream().map(ClassRef::ofDescriptor).toArray(ClassRef[]::new));
    }

    @Foldable
    @Override
    public ClassRef returnType() {
        return returnType;
    }

    @Foldable
    @Override
    public int parameterCount() {
        return argTypes.length;
    }

    @Foldable
    @Override
    public ClassRef parameterType(int index) {
        return argTypes[index];
    }

    @Override
    public List<ClassRef> parameterList() {
        return List.of(argTypes);
    }

    @Override
    public ClassRef[] parameterArray() {
        return argTypes.clone();
    }

    @Foldable
    @Override
    public MethodTypeRef changeReturnType(ClassRef returnType) {
        return MethodTypeRef.of(returnType, argTypes);
    }

    @Foldable
    @Override
    public MethodTypeRef changeParameterType(int index, ClassRef paramType) {
        ClassRef[] newArgs = argTypes.clone();
        newArgs[index] = paramType;
        return MethodTypeRef.of(returnType, newArgs);
    }

    @Foldable
    @Override
    public MethodTypeRef dropParameterTypes(int start, int end) {
        if (start < 0 || start >= argTypes.length || end < 0 || end > argTypes.length)
            throw new IndexOutOfBoundsException();
        else if (start > end)
            throw new IllegalArgumentException(String.format("Range (%d, %d) not valid for size %d", start, end, argTypes.length));
        ClassRef[] newArgs = new ClassRef[argTypes.length - (end - start)];
        System.arraycopy(argTypes, 0, newArgs, 0, start);
        System.arraycopy(argTypes, end, newArgs, start, argTypes.length - end);
        return MethodTypeRef.of(returnType, newArgs);
    }

    @Foldable
    @Override
    public MethodTypeRef insertParameterTypes(int pos, ClassRef... paramTypes) {
        if (pos < 0 || pos > argTypes.length)
            throw new IndexOutOfBoundsException(pos);
        ClassRef[] newArgs = new ClassRef[argTypes.length + paramTypes.length];
        System.arraycopy(argTypes, 0, newArgs, 0, pos);
        System.arraycopy(paramTypes, 0, newArgs, pos, paramTypes.length);
        System.arraycopy(argTypes, pos, newArgs, pos+paramTypes.length, argTypes.length - pos);
        return MethodTypeRef.of(returnType, newArgs);
    }

    @Override
    public MethodType resolveConstantRef(MethodHandles.Lookup lookup) {
        return MethodType.fromMethodDescriptorString(descriptorString(), lookup.lookupClass().getClassLoader());
    }

    @Override
    public Optional<? extends ConstantRef<? super ConstantRef<MethodType>>> toConstantRef(MethodHandles.Lookup lookup) {
        return Optional.of(DynamicConstantRef.of(RefBootstraps.BSM_METHODTYPEREF, CR_MethodTypeRef).withArgs(descriptorString()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConstantMethodTypeRef constant = (ConstantMethodTypeRef) o;

        return returnType.equals(constant.returnType)
               && Arrays.equals(argTypes, constant.argTypes);
    }

    @Override
    public int hashCode() {
        int result = returnType.hashCode();
        result = 31 * result + Arrays.hashCode(argTypes);
        return result;
    }

    @Override
    public String toString() {
        return String.format("MethodTypeRef[%s]", simpleDescriptor());
    }
}
