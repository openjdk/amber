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
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * A symbolic reference for a {@linkplain MethodType} constant.
 */
public final class MethodTypeRef implements ConstantRef.WithTypeDescriptor<MethodType>, Constable<ConstantRef<MethodType>> {
    private static final Pattern TYPE_DESC = Pattern.compile("(\\[*)(V|I|J|S|B|C|F|D|Z|L[^/.\\[;][^.\\[;]*;)");
    private static Pattern pattern = Pattern.compile("\\((.*)\\)(.*)");

    private final ClassRef returnType;
    private final ClassRef[] argTypes;

    /**
     * Construct a {@linkplain MethodTypeRef} with the specified return type
     * and parameter types
     *
     * @param returnType a {@link ClassRef} describing the return type
     * @param argTypes {@link ClassRef}s describing the parameter types
     */
    private MethodTypeRef(ClassRef returnType, ClassRef[] argTypes) {
        this.returnType = requireNonNull(returnType);
        this.argTypes = requireNonNull(argTypes);

        for (ClassRef cr : argTypes)
            if (cr.isPrimitive() && cr.descriptorString().equals("V"))
                throw new IllegalArgumentException("Void parameters not permitted");
    }

    /**
     * Create a {@linkplain MethodTypeRef} from a method descriptor string
     *
     * @param descriptor the method descriptor string
     * @return a {@linkplain MethodTypeRef} describing the desired method type
     * @throws IllegalArgumentException if the descriptor string is not a valid
     * method descriptor
     */
    @Foldable
    public static MethodTypeRef ofDescriptor(String descriptor) {
        // @@@ Find a lower-overhead way of validating the descriptor
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
        return new MethodTypeRef(ClassRef.ofDescriptor(returnType), params.stream().map(ClassRef::ofDescriptor).toArray(ClassRef[]::new));
    }

    /**
     * Returns a {@linkplain MethodTypeRef} for the specified return type and
     * parameter types.
     *
     * @param returnDescriptor a {@linkplain ClassRef} describing the return type
     * @param paramDescriptors {@linkplain ClassRef}s describing the argument types
     * @return a {@linkplain MethodTypeRef} describing the desired method type
     */
    @Foldable
    public static MethodTypeRef of(ClassRef returnDescriptor, ClassRef... paramDescriptors) {
        return new MethodTypeRef(returnDescriptor, paramDescriptors);
    }

    /**
     * Get the return type of the method type described by this {@linkplain MethodTypeRef}
     * @return the return type
     */
    @Foldable
    public ClassRef returnType() {
        return returnType;
    }

    /**
     * Get the number of parameters of the method type described by
     * this {@linkplain MethodTypeRef}
     * @return the number of parameters
     */
    @Foldable
    public int parameterCount() {
        return argTypes.length;
    }

    /**
     * Get the parameter type of the {@code index}'th parameter of the method type
     * described by this {@linkplain MethodTypeRef}
     *
     * @param index the index of the parameter to retrieve
     * @return the parameter type
     * @throws IndexOutOfBoundsException if the index is outside the half-open
     * range {[0, parameterCount())}
     */
    @Foldable
    public ClassRef parameterType(int index) {
        return argTypes[index];
    }

    /**
     * Get the parameter types as a {@link List}
     *
     * @return the parameter types
     */
    public List<ClassRef> parameterList() {
        return Arrays.asList(argTypes);
    }

    /**
     * Get the parameter types as an array
     *
     * @return the parameter types
     */
    public ClassRef[] parameterArray() {
        return argTypes.clone();
    }

    /**
     * Return a {@linkplain MethodTypeRef} that is identical to
     * this one, except with the specified return type
     *
     * @param returnType the new return type
     * @return the new method type descriptor
     */
    @Foldable
    public MethodTypeRef changeReturnType(ClassRef returnType) {
        return of(returnType, argTypes);
    }

    /**
     * Return a {@linkplain MethodTypeRef} that is identical to this one,
     * except that a single parameter type has been changed to the provided
     * value
     *
     * @param index the index of the parameter to change
     * @param paramType the new parameter type
     * @return the new method type descriptor
     * @throws IndexOutOfBoundsException if the index is outside the half-open
     * range {[0, parameterCount)}
     */
    @Foldable
    public MethodTypeRef changeParameterType(int index, ClassRef paramType) {
        ClassRef[] newArgs = argTypes.clone();
        newArgs[index] = paramType;
        return of(returnType, newArgs);
    }

    /**
     * Return a {@linkplain MethodTypeRef} that is identical to this one,
     * except that a range of parameter types have been removed
     *
     * @param start the index of the first parameter to remove
     * @param end the index after the last parameter to remove
     * @return the new method type descriptor
     * @throws IndexOutOfBoundsException if {@code start} is outside the half-open
     * range {[0, parameterCount)}, or {@code end} is outside the closed range
     * {@code [0, parameterCount]}
     */
    @Foldable
    public MethodTypeRef dropParameterTypes(int start, int end) {
        if (start < 0 || start >= argTypes.length || end < 0 || end > argTypes.length)
            throw new IndexOutOfBoundsException();
        else if (start > end)
            throw new IllegalArgumentException(String.format("Range (%d, %d) not valid for size %d", start, end, argTypes.length));
        ClassRef[] newArgs = new ClassRef[argTypes.length - (end - start)];
        System.arraycopy(argTypes, 0, newArgs, 0, start);
        System.arraycopy(argTypes, end, newArgs, start, argTypes.length - end);
        return of(returnType, newArgs);
    }

    /**
     * Return a {@linkplain MethodTypeRef} that is identical to this one,
     * except that a range of additional parameter types have been inserted
     *
     * @param pos the index at which to insert the first inserted parameter
     * @param paramTypes the new parameter types to insert
     * @return the new method type descriptor
     * @throws IndexOutOfBoundsException if {@code pos} is outside the closed
     * range {[0, parameterCount]}
     */
    @Foldable
    public MethodTypeRef insertParameterTypes(int pos, ClassRef... paramTypes) {
        if (pos < 0 || pos > argTypes.length)
            throw new IndexOutOfBoundsException(pos);
        ClassRef[] newArgs = new ClassRef[argTypes.length + paramTypes.length];
        System.arraycopy(argTypes, 0, newArgs, 0, pos);
        System.arraycopy(paramTypes, 0, newArgs, pos, paramTypes.length);
        System.arraycopy(argTypes, pos, newArgs, pos+paramTypes.length, argTypes.length - pos);
        return of(returnType, newArgs);
    }

    public MethodType resolveRef(MethodHandles.Lookup lookup) throws ReflectiveOperationException {
        return MethodType.fromMethodDescriptorString(descriptorString(), lookup.lookupClass().getClassLoader());
    }

    @Override
    public Optional<ConstantRef<ConstantRef<MethodType>>> toSymbolicRef(MethodHandles.Lookup lookup) {
        ConstantRef<?>[] args = new ConstantRef<?>[] { SymbolicRefs.MHR_METHODTYPEREF_FACTORY, descriptorString() };
        return Optional.of(DynamicConstantRef.of(SymbolicRefs.BSM_INVOKE, "_", SymbolicRefs.CR_MethodTypeRef, args));
    }

    @Override
    public String descriptorString() {
        return String.format("(%s)%s",
                             Stream.of(argTypes)
                                   .map(ClassRef::descriptorString)
                                   .collect(Collectors.joining()),
                             returnType.descriptorString());
    }

    /**
     * Return a human-readable descriptor for this method type, using the
     * canonical names for parameter and return types
     * @return the human-readable descriptor for this method type
     */
    public String canonicalDescriptor() {
        return String.format("(%s)%s",
                             Stream.of(argTypes)
                                   .map(ClassRef::canonicalName)
                                   .collect(Collectors.joining()),
                             returnType.canonicalName());
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MethodTypeRef constant = (MethodTypeRef) o;

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
        return String.format("MethodTypeRef[%s]", canonicalDescriptor());
    }
}
