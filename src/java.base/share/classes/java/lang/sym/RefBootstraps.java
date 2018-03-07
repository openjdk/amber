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

import jdk.internal.vm.annotation.Foldable;

import java.lang.invoke.MethodHandles;

import static java.lang.sym.ConstantRefs.CR_ClassRef;
import static java.lang.sym.ConstantRefs.CR_ConstantRef;
import static java.lang.sym.ConstantRefs.CR_DynamicConstantRef;
import static java.lang.sym.ConstantRefs.CR_EnumRef;
import static java.lang.sym.ConstantRefs.CR_MethodHandleRef;
import static java.lang.sym.ConstantRefs.CR_MethodTypeRef;
import static java.lang.sym.ConstantRefs.CR_String;

/**
 * RefBoostraps
 *
 * @author Brian Goetz
 */
public final class RefBootstraps {
    private RefBootstraps() { }

    @Foldable
    private static final ClassRef THIS_CLASS = ClassRef.of("java.lang.sym.RefBootstraps");

    /** Bootstrap for ClassRef */
    @Foldable
    public static final MethodHandleRef BSM_CLASSREF = MethodHandleRef.ofDynamicConstant(THIS_CLASS, "classRef", CR_ClassRef,
                                                                                         CR_String);

    /** Bootstrap for MethodTypeRef */
    @Foldable
    public static final MethodHandleRef BSM_METHODTYPEREF = MethodHandleRef.ofDynamicConstant(THIS_CLASS, "methodTypeRef", CR_MethodTypeRef,
                                                                                              CR_String);

    /** Bootstrap for MethodHandleRef */
    @Foldable
    public static final MethodHandleRef BSM_METHODHANDLEREF = MethodHandleRef.ofDynamicConstant(THIS_CLASS, "methodHandleRef", CR_MethodHandleRef,
                                                                                                CR_String, CR_String, CR_String, CR_String);

    /** Bootstrap for DynamicConstantRef */
    @Foldable
    public static final MethodHandleRef BSM_DYNAMICCONSTANTREF = MethodHandleRef.ofDynamicConstant(THIS_CLASS, "dynamicConstantRef", CR_DynamicConstantRef,
                                                                                                   CR_String, CR_String, CR_String, CR_String, CR_String,

                                                                                                   CR_ConstantRef.array());

    /** Bootstrap for ClassRef */
    @Foldable
    public static final MethodHandleRef BSM_ENUMREF = MethodHandleRef.ofDynamicConstant(THIS_CLASS, "enumRef", CR_EnumRef,
                                                                                        CR_String, CR_String);
    /**
     * Bootstrap for ClassRef
     *
     * @param lookup ignored
     * @param name ignored
     * @param clazz ignored
     * @param descriptor descriptor for class
     * @return the ClassRef
     */
    public static ClassRef classRef(MethodHandles.Lookup lookup, String name, Class<ClassRef> clazz,
                                    String descriptor) {
        // @@@ Can fold descriptor into name channel, with encoding from BytecodeName
        return ClassRef.ofDescriptor(descriptor);
    }

    /**
     * Bootstrap for MethodTypeRef
     *
     * @param lookup ignored
     * @param name ignored
     * @param clazz ignored
     * @param descriptor descriptor for method
     * @return the MethodTypeRef
     */
    public static MethodTypeRef methodTypeRef(MethodHandles.Lookup lookup, String name, Class<ClassRef> clazz,
                                              String descriptor) {
        // @@@ Can fold descriptor into name channel, with encoding from BytecodeName
        return MethodTypeRef.ofDescriptor(descriptor);
    }

    /**
     * Bootstrap for MethodHandleRef
     *
     * @param lookup ignored
     * @param name ignored
     * @param clazz ignored
     * @param bsmKindName kind
     * @param bsmOwner owner
     * @param bsmName name
     * @param bsmDesc desc
     * @return the MethodHandleRef
     */
    public static MethodHandleRef methodHandleRef(MethodHandles.Lookup lookup, String name, Class<ClassRef> clazz,
                                                  String bsmKindName, String bsmOwner, String bsmName, String bsmDesc) {
        return MethodHandleRef.of(MethodHandleRef.Kind.valueOf(bsmKindName), ClassRef.ofDescriptor(bsmOwner), bsmName, MethodTypeRef.ofDescriptor(bsmDesc));
    }

    /**
     * Bootstrap for DynamicConstantRef
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
     * @return the DynamicConstantRef
     */
    public static DynamicConstantRef<?> dynamicConstantRef(MethodHandles.Lookup lookup, String name, Class<ClassRef> clazz,
                                                           String bsmOwner, String bsmName, String bsmDesc,
                                                           String invName, String invType,
                                                           ConstantRef<?>... args) {
        return DynamicConstantRef.of(MethodHandleRef.of(MethodHandleRef.Kind.STATIC, ClassRef.ofDescriptor(bsmOwner), bsmName, MethodTypeRef.ofDescriptor(bsmDesc)),
                                     invName, ClassRef.ofDescriptor(invType), args);

    }

    /**
     * Bootstrap for EnumRef
     *
     * @param lookup ignored
     * @param name ignored
     * @param clazz ignored
     * @param classDescriptor enum class
     * @param constantName enum constant
     * @return the EnumRef
     */
    public static EnumRef<?> enumRef(MethodHandles.Lookup lookup, String name, Class<ClassRef> clazz,
                                     String classDescriptor, String constantName) {
        return EnumRef.of(ClassRef.ofDescriptor(classDescriptor), constantName);
    }
}
