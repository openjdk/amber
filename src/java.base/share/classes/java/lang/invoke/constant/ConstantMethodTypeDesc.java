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
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.invoke.constant.ConstantDescs.CR_MethodTypeDesc;
import static java.lang.invoke.constant.ConstantDescs.CR_String;
import static java.util.Objects.requireNonNull;

/**
 * A <a href="package-summary.html#nominal">nominal descriptor</a> for a
 * {@link MethodType}.  A {@linkplain ConstantMethodTypeDesc} corresponds to a
 * {@code Constant_MethodType_info} entry in the constant pool of a classfile.
 */
public final class ConstantMethodTypeDesc implements MethodTypeDesc {
    @Foldable
    private static final ConstantMethodHandleDesc BSM_METHODTYPEDESC
            = ConstantDescs.ofConstantBootstrap(ClassDesc.of("java.lang.invoke.constant", "ConstantMethodTypeDesc"),
                                                "constantBootstrap", CR_MethodTypeDesc, CR_String);

    private static final Pattern TYPE_DESC = Pattern.compile("(\\[*)(V|I|J|S|B|C|F|D|Z|L[^/.\\[;][^.\\[;]*;)");
    private static final Pattern pattern = Pattern.compile("\\((.*)\\)(.*)");

    private final ClassDesc returnType;
    private final ClassDesc[] argTypes;

    /**
     * Construct a {@linkplain MethodTypeDesc} with the specified return type
     * and parameter types
     *
     * @param returnType a {@link ClassDesc} describing the return type
     * @param argTypes {@link ClassDesc}s describing the parameter types
     */
    ConstantMethodTypeDesc(ClassDesc returnType, ClassDesc[] argTypes) {
        this.returnType = requireNonNull(returnType);
        this.argTypes = requireNonNull(argTypes);

        for (ClassDesc cr : argTypes)
            if (cr.isPrimitive() && cr.descriptorString().equals("V"))
                throw new IllegalArgumentException("Void parameters not permitted");
    }

    /**
     * Create a {@linkplain ConstantMethodTypeDesc} given a method descriptor string.
     *
     * @param descriptor the method descriptor string, as per JVMS 4.3.3
     * @return a {@linkplain ConstantMethodTypeDesc} describing the desired method type
     * @throws IllegalArgumentException if the descriptor string is not a valid
     * method descriptor
     */
    @Foldable
    static ConstantMethodTypeDesc ofDescriptor(String descriptor) {
        // @@@ Replace validation with a lower-overhead mechanism than regex
        // Follow the trail from MethodType.fromMethodDescriptorString to
        // parsing code in sun/invoke/util/BytecodeDescriptor.java which could
        // be extracted and/or shared
        Matcher matcher = pattern.matcher(descriptor);
        if (!matcher.matches())
            throw new IllegalArgumentException(String.format("%s is not a valid method descriptor", descriptor));
        String paramTypesStr = matcher.group(1);
        String returnTypeStr = matcher.group(2);
        if (!TYPE_DESC.matcher(returnTypeStr).matches())
            throw new IllegalArgumentException(String.format("Invalid return type %s", returnTypeStr));
        List<String> params = new ArrayList<>();
        matcher = TYPE_DESC.matcher(paramTypesStr);
        while (matcher.regionStart() < paramTypesStr.length()) {
            if (matcher.lookingAt()) {
                params.add(matcher.group());
                matcher.region(matcher.end(), matcher.regionEnd());
            }
            else
                throw new IllegalArgumentException(String.format("Invalid parameter type: %s",
                                                                 paramTypesStr.substring(matcher.regionStart(), matcher.regionEnd())));
        }
        ClassDesc[] paramTypes = params.stream().map(ClassDesc::ofDescriptor).toArray(ClassDesc[]::new);
        return new ConstantMethodTypeDesc(ClassDesc.ofDescriptor(returnTypeStr), paramTypes);
    }

    @Foldable
    @Override
    public ClassDesc returnType() {
        return returnType;
    }

    @Foldable
    @Override
    public int parameterCount() {
        return argTypes.length;
    }

    @Foldable
    @Override
    public ClassDesc parameterType(int index) {
        return argTypes[index];
    }

    @Override
    public List<ClassDesc> parameterList() {
        return List.of(argTypes);
    }

    @Override
    public ClassDesc[] parameterArray() {
        return argTypes.clone();
    }

    @Foldable
    @Override
    public MethodTypeDesc changeReturnType(ClassDesc returnType) {
        return MethodTypeDesc.of(returnType, argTypes);
    }

    @Foldable
    @Override
    public MethodTypeDesc changeParameterType(int index, ClassDesc paramType) {
        ClassDesc[] newArgs = argTypes.clone();
        newArgs[index] = paramType;
        return MethodTypeDesc.of(returnType, newArgs);
    }

    @Foldable
    @Override
    public MethodTypeDesc dropParameterTypes(int start, int end) {
        if (start < 0 || start >= argTypes.length || end < 0 || end > argTypes.length)
            throw new IndexOutOfBoundsException();
        else if (start > end)
            throw new IllegalArgumentException(String.format("Range (%d, %d) not valid for size %d", start, end, argTypes.length));
        ClassDesc[] newArgs = new ClassDesc[argTypes.length - (end - start)];
        System.arraycopy(argTypes, 0, newArgs, 0, start);
        System.arraycopy(argTypes, end, newArgs, start, argTypes.length - end);
        return MethodTypeDesc.of(returnType, newArgs);
    }

    @Foldable
    @Override
    public MethodTypeDesc insertParameterTypes(int pos, ClassDesc... paramTypes) {
        if (pos < 0 || pos > argTypes.length)
            throw new IndexOutOfBoundsException(pos);
        ClassDesc[] newArgs = new ClassDesc[argTypes.length + paramTypes.length];
        System.arraycopy(argTypes, 0, newArgs, 0, pos);
        System.arraycopy(paramTypes, 0, newArgs, pos, paramTypes.length);
        System.arraycopy(argTypes, pos, newArgs, pos+paramTypes.length, argTypes.length - pos);
        return MethodTypeDesc.of(returnType, newArgs);
    }

    @Override
    public MethodType resolveConstantDesc(MethodHandles.Lookup lookup) {
        return MethodType.fromMethodDescriptorString(descriptorString(), lookup.lookupClass().getClassLoader());
    }

    @Override
    public Optional<? extends ConstantDesc<? super ConstantDesc<MethodType>>> describeConstable(MethodHandles.Lookup lookup) {
        return Optional.of(DynamicConstantDesc.of(BSM_METHODTYPEDESC, CR_MethodTypeDesc).withArgs(descriptorString()));
    }

    /**
     * Constant bootstrap method for representing a {@linkplain MethodTypeDesc} in
     * the constant pool of a classfile.
     *
     * @param lookup ignored
     * @param name ignored
     * @param clazz ignored
     * @param descriptor a method descriptor string for the method type, as per JVMS 4.3.3
     * @return the {@linkplain MethodTypeDesc}
     */
    public static MethodTypeDesc constantBootstrap(MethodHandles.Lookup lookup, String name, Class<ClassDesc> clazz,
                                                   String descriptor) {
        return MethodTypeDesc.ofDescriptor(descriptor);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConstantMethodTypeDesc constant = (ConstantMethodTypeDesc) o;

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
        return String.format("MethodTypeDesc[%s]", displayDescriptor());
    }
}
