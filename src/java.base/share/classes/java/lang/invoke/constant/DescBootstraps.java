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

import static java.lang.invoke.constant.ConstantDescs.CR_ClassDesc;
import static java.lang.invoke.constant.ConstantDescs.CR_ConstantDesc;
import static java.lang.invoke.constant.ConstantDescs.CR_DynamicConstantDesc;
import static java.lang.invoke.constant.ConstantDescs.CR_EnumDesc;
import static java.lang.invoke.constant.ConstantDescs.CR_MethodHandleDesc;
import static java.lang.invoke.constant.ConstantDescs.CR_MethodTypeDesc;
import static java.lang.invoke.constant.ConstantDescs.CR_String;

/**
 * DescBoostraps
 *
 * @author Brian Goetz
 */
public final class DescBootstraps {
    private DescBootstraps() { }

    @Foldable
    private static final ClassDesc THIS_CLASS = ClassDesc.of("java.lang.invoke.constant.DescBootstraps");

    /** Bootstrap for ClassDesc */
    @Foldable
    public static final ConstantMethodHandleDesc BSM_CLASSDESC
            = ConstantDescs.ofConstantBootstrap(THIS_CLASS, "classDesc", CR_ClassDesc,
                                                CR_String);

    /** Bootstrap for MethodTypeDesc */
    @Foldable
    public static final ConstantMethodHandleDesc BSM_METHODTYPEDESC
            = ConstantDescs.ofConstantBootstrap(THIS_CLASS, "methodTypeDesc", CR_MethodTypeDesc,
                                                CR_String);

    /** Bootstrap for MethodHandleDesc */
    @Foldable
    public static final ConstantMethodHandleDesc BSM_METHODHANDLEDESC
            = ConstantDescs.ofConstantBootstrap(THIS_CLASS, "methodHandleDesc", CR_MethodHandleDesc,
                                                CR_String, CR_String, CR_String, CR_String);

    /** Bootstrap for DynamicConstantDesc */
    @Foldable
    public static final ConstantMethodHandleDesc BSM_DYNAMICCONSTANTDESC
            = ConstantDescs.ofConstantBootstrap(THIS_CLASS, "dynamicConstantDesc", CR_DynamicConstantDesc,
                                                CR_String, CR_String, CR_String, CR_String, CR_String,
                                                CR_ConstantDesc.array());

    /** Bootstrap for ClassDesc */
    @Foldable
    public static final ConstantMethodHandleDesc BSM_ENUMDESC
            = ConstantDescs.ofConstantBootstrap(THIS_CLASS, "enumDesc", CR_EnumDesc,
                                                CR_String, CR_String);

    /**
     * Bootstrap for ClassDesc
     *
     * @param lookup ignored
     * @param name ignored
     * @param clazz ignored
     * @param descriptor descriptor for class
     * @return the ClassDesc
     */
    public static ClassDesc classDesc(MethodHandles.Lookup lookup, String name, Class<ClassDesc> clazz,
                                      String descriptor) {
        // @@@ Can fold descriptor into name channel, with encoding from BytecodeName
        return ClassDesc.ofDescriptor(descriptor);
    }

    /**
     * Bootstrap for MethodTypeDesc
     *
     * @param lookup ignored
     * @param name ignored
     * @param clazz ignored
     * @param descriptor descriptor for method
     * @return the MethodTypeDesc
     */
    public static MethodTypeDesc methodTypeDesc(MethodHandles.Lookup lookup, String name, Class<ClassDesc> clazz,
                                                String descriptor) {
        // @@@ Can fold descriptor into name channel, with encoding from BytecodeName
        return MethodTypeDesc.ofDescriptor(descriptor);
    }

    /**
     * Bootstrap for MethodHandleDesc
     *
     * @param lookup ignored
     * @param name ignored
     * @param clazz ignored
     * @param bsmKindName kind
     * @param bsmOwner owner
     * @param bsmName name
     * @param bsmDesc desc
     * @return the MethodHandleDesc
     */
    public static MethodHandleDesc methodHandleDesc(MethodHandles.Lookup lookup, String name, Class<ClassDesc> clazz,
                                                    String bsmKindName, String bsmOwner, String bsmName, String bsmDesc) {
        return MethodHandleDesc.of(MethodHandleDesc.Kind.valueOf(bsmKindName),
                                   ClassDesc.ofDescriptor(bsmOwner), bsmName,
                                   MethodTypeDesc.ofDescriptor(bsmDesc));
    }

    /**
     * Bootstrap for DynamicConstantDesc
     *
     * @param lookup ignored
     * @param name ignored
     * @param clazz ignored
     * @param bsmOwner owner
     * @param bsmName name
     * @param bsmDesc desc
     * @param invName invName
     * @param invType invType
     * @param args bsm args
     * @return the DynamicConstantDesc
     */
    public static DynamicConstantDesc<?> dynamicConstantDesc(MethodHandles.Lookup lookup, String name, Class<ClassDesc> clazz,
                                                             String bsmOwner, String bsmName, String bsmDesc,
                                                             String invName, String invType,
                                                             ConstantDesc<?>... args) {
        return DynamicConstantDesc.of(MethodHandleDesc.of(MethodHandleDesc.Kind.STATIC,
                                                          ClassDesc.ofDescriptor(bsmOwner), bsmName,
                                                          MethodTypeDesc.ofDescriptor(bsmDesc)),
                                      invName, ClassDesc.ofDescriptor(invType), args);

    }

    /**
     * Bootstrap for EnumDesc
     *
     * @param lookup ignored
     * @param name ignored
     * @param clazz ignored
     * @param classDescriptor enum class
     * @param constantName enum constant
     * @return the EnumDesc
     */
    public static EnumDesc<?> enumDesc(MethodHandles.Lookup lookup, String name, Class<ClassDesc> clazz,
                                       String classDescriptor, String constantName) {
        return EnumDesc.of(ClassDesc.ofDescriptor(classDescriptor), constantName);
    }
}
