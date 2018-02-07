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
import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.sym.MethodHandleRef.Kind;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Predefined constants for common symbolic references.  Includes class references
 * for primitive types and common platform types, and method handle references
 * for standard bootstrap methods.
 *
 * @see SymbolicRef
 */
public final class SymbolicRefs {
    // No instances
    private SymbolicRefs() { }

    // Don't change the order of these declarations!

    /** {@link ClassRef} representing {@link Object} */
    @Foldable
    public static final ClassRef CR_Object = ClassRef.of("java.lang.Object");

    /** {@link ClassRef} representing {@link String} */
    @Foldable
    public static final ClassRef CR_String = ClassRef.of("java.lang.String");

    /** {@link ClassRef} representing {@link Class} */
    @Foldable
    public static final ClassRef CR_Class = ClassRef.of("java.lang.Class");

    /** {@link ClassRef} representing {@link Number} */
    @Foldable
    public static final ClassRef CR_Number = ClassRef.of("java.lang.Number");

    /** {@link ClassRef} representing {@link Integer} */
    @Foldable
    public static final ClassRef CR_Integer = ClassRef.of("java.lang.Integer");

    /** {@link ClassRef} representing {@link Long} */
    @Foldable
    public static final ClassRef CR_Long = ClassRef.of("java.lang.Long");

    /** {@link ClassRef} representing {@link Float} */
    @Foldable
    public static final ClassRef CR_Float = ClassRef.of("java.lang.Float");

    /** {@link ClassRef} representing {@link Double} */
    @Foldable
    public static final ClassRef CR_Double = ClassRef.of("java.lang.Double");

    /** {@link ClassRef} representing {@link Short} */
    @Foldable
    public static final ClassRef CR_Short = ClassRef.of("java.lang.Short");

    /** {@link ClassRef} representing {@link Byte} */
    @Foldable
    public static final ClassRef CR_Byte = ClassRef.of("java.lang.Byte");

    /** {@link ClassRef} representing {@link Character} */
    @Foldable
    public static final ClassRef CR_Character = ClassRef.of("java.lang.Character");

    /** {@link ClassRef} representing {@link Boolean} */
    @Foldable
    public static final ClassRef CR_Boolean = ClassRef.of("java.lang.Boolean");

    /** {@link ClassRef} representing {@link Void} */
    @Foldable
    public static final ClassRef CR_Void = ClassRef.of("java.lang.Void");

    /** {@link ClassRef} representing {@link Throwable} */
    @Foldable
    public static final ClassRef CR_Throwable = ClassRef.of("java.lang.Throwable");

    /** {@link ClassRef} representing {@link Exception} */
    @Foldable
    public static final ClassRef CR_Exception = ClassRef.of("java.lang.Exception");

    /** {@link ClassRef} representing {@link Enum} */
    @Foldable
    public static final ClassRef CR_Enum = ClassRef.of("java.lang.Enum");

    /** {@link ClassRef} representing {@link VarHandle} */
    @Foldable
    public static final ClassRef CR_VarHandle = ClassRef.of("java.lang.invoke.VarHandle");

    /** {@link ClassRef} representing {@link MethodHandles} */
    @Foldable
    public static final ClassRef CR_MethodHandles = ClassRef.of("java.lang.invoke.MethodHandles");

    /** {@link ClassRef} representing {@link MethodHandles.Lookup} */
    @Foldable
    public static final ClassRef CR_MethodHandles_Lookup = CR_MethodHandles.inner("Lookup");

    /** {@link ClassRef} representing {@link MethodHandle} */
    @Foldable
    public static final ClassRef CR_MethodHandle = ClassRef.of("java.lang.invoke.MethodHandle");

    /** {@link ClassRef} representing {@link MethodType} */
    @Foldable
    public static final ClassRef CR_MethodType = ClassRef.of("java.lang.invoke.MethodType");

    /** {@link ClassRef} representing {@link CallSite} */
    @Foldable
    public static final ClassRef CR_CallSite = ClassRef.of("java.lang.invoke.CallSite");

    /** {@link ClassRef} representing {@link Collection} */
    @Foldable
    public static final ClassRef CR_Collection = ClassRef.of("java.util.Collection");

    /** {@link ClassRef} representing {@link List} */
    @Foldable
    public static final ClassRef CR_List = ClassRef.of("java.util.List");

    /** {@link ClassRef} representing {@link Set} */
    @Foldable
    public static final ClassRef CR_Set = ClassRef.of("java.util.Set");

    /** {@link ClassRef} representing {@link Map} */
    @Foldable
    public static final ClassRef CR_Map = ClassRef.of("java.util.Map");

    /** {@link ClassRef} representing {@link SymbolicRef} */
    @Foldable
    static final ClassRef CR_SymbolicRef = ClassRef.of("java.lang.sym.SymbolicRef");

    /** {@link ClassRef} representing {@link ClassRef} */
    @Foldable
    static final ClassRef CR_ClassRef = ClassRef.of("java.lang.sym.ClassRef");

    /** {@link ClassRef} representing {@link EnumRef} */
    @Foldable
    static final ClassRef CR_EnumRef = ClassRef.of("java.lang.sym.EnumRef");

    /** {@link ClassRef} representing {@link MethodTypeRef} */
    @Foldable
    static final ClassRef CR_MethodTypeRef = ClassRef.of("java.lang.sym.MethodTypeRef");

    /** {@link ClassRef} representing {@link MethodHandleRef} */
    @Foldable
    static final ClassRef CR_MethodHandleRef = ClassRef.of("java.lang.sym.MethodHandleRef");

    /** {@link ClassRef} representing {@link VarHandleRef} */
    @Foldable
    static final ClassRef CR_VarHandleRef = ClassRef.of("java.lang.sym.VarHandleRef");

    /** {@link ClassRef} representing {@link MethodHandleRef.Kind} */
    @Foldable
    static final ClassRef CR_MethodHandleRef_Kind = CR_MethodHandleRef.inner("Kind");

    /** {@link ClassRef} representing {@link DynamicConstantRef} */
    @Foldable
    static final ClassRef CR_CondyRef = ClassRef.of("java.lang.sym.DynamicConstantRef");

    /** {@link ClassRef} representing {@link DynamicCallSiteRef} */
    @Foldable
    static final ClassRef CR_DynamicCallSiteRef = ClassRef.of("java.lang.sym.DynamicCallSiteRef");

    /** {@link ClassRef} representing {@link ConstantBootstraps} */
    @Foldable
    static final ClassRef CR_ConstantBootstraps = ClassRef.of("java.lang.invoke.ConstantBootstraps");

    /** {@link MethodHandleRef} representing {@link ConstantBootstraps#primitiveClass(Lookup, String, Class)} */
    @Foldable
    public static final MethodHandleRef BSM_PRIMITIVE_CLASS
            = MethodHandleRef.ofDynamicConstant(CR_ConstantBootstraps, "primitiveClass", CR_Class);

    /** {@link MethodHandleRef} representing {@link ConstantBootstraps#enumConstant(Lookup, String, Class)} */
    @Foldable
    public static final MethodHandleRef BSM_ENUM_CONSTANT
            = MethodHandleRef.ofDynamicConstant(CR_ConstantBootstraps, "enumConstant", CR_Enum);

    /** {@link MethodHandleRef} representing {@link ConstantBootstraps#nullConstant(Lookup, String, Class)} */
    @Foldable
    public static final MethodHandleRef BSM_NULL_CONSTANT
            = MethodHandleRef.ofDynamicConstant(CR_ConstantBootstraps, "nullConstant", SymbolicRefs.CR_Object);

    /** {@link MethodHandleRef} representing {@link ConstantBootstraps#fieldVarHandle(Lookup, String, Class, Class, Class)} */
    @Foldable
    public static final MethodHandleRef BSM_VARHANDLE_FIELD
            = MethodHandleRef.ofDynamicConstant(CR_ConstantBootstraps, "fieldVarHandle", CR_VarHandle, CR_Class, CR_Class);

    /** {@link MethodHandleRef} representing {@link ConstantBootstraps#staticFieldVarHandle(Lookup, String, Class, Class, Class)} */
    @Foldable
    public static final MethodHandleRef BSM_VARHANDLE_STATIC_FIELD
            = MethodHandleRef.ofDynamicConstant(CR_ConstantBootstraps, "staticFieldVarHandle", CR_VarHandle, CR_Class, CR_Class);

    /** {@link MethodHandleRef} representing {@link ConstantBootstraps#arrayVarHandle(Lookup, String, Class, Class)} */
    @Foldable
    public static final MethodHandleRef BSM_VARHANDLE_ARRAY
            = MethodHandleRef.ofDynamicConstant(CR_ConstantBootstraps, "arrayVarHandle", CR_VarHandle, CR_Class);

    /** {@link MethodHandleRef} representing {@link ConstantBootstraps#invoke(Lookup, String, Class, MethodHandle, Object...)} */
    @Foldable
    public static final MethodHandleRef BSM_INVOKE
            = MethodHandleRef.ofDynamicConstant(CR_ConstantBootstraps, "invoke", CR_Object, CR_MethodHandle, CR_Object.array());

    /** {@link ClassRef} representing the primitive type {@code int} */
    @Foldable
    public static final ClassRef CR_int = ClassRef.ofDescriptor("I");

    /** {@link ClassRef} representing the primitive type {@code long} */
    @Foldable
    public static final ClassRef CR_long = ClassRef.ofDescriptor("J");

    /** {@link ClassRef} representing the primitive type {@code float} */
    @Foldable
    public static final ClassRef CR_float = ClassRef.ofDescriptor("F");

    /** {@link ClassRef} representing the primitive type {@code double} */
    @Foldable
    public static final ClassRef CR_double = ClassRef.ofDescriptor("D");

    /** {@link ClassRef} representing the primitive type {@code short} */
    @Foldable
    public static final ClassRef CR_short = ClassRef.ofDescriptor("S");

    /** {@link ClassRef} representing the primitive type {@code byte} */
    @Foldable
    public static final ClassRef CR_byte = ClassRef.ofDescriptor("B");

    /** {@link ClassRef} representing the primitive type {@code char} */
    @Foldable
    public static final ClassRef CR_char = ClassRef.ofDescriptor("C");

    /** {@link ClassRef} representing the primitive type {@code boolean} */
    @Foldable
    public static final ClassRef CR_boolean = ClassRef.ofDescriptor("Z");

    /** {@link ClassRef} representing the primitive type {@code void} */
    @Foldable
    public static final ClassRef CR_void = ClassRef.ofDescriptor("V");

    /** Symbolic reference representing the constant {@code null} */
    @Foldable
    public static final SymbolicRef<?> NULL = DynamicConstantRef.of(SymbolicRefs.BSM_NULL_CONSTANT, SymbolicRefs.CR_Object);

    /** {@link MethodHandleRef} representing the factory method {@link ClassRef#ofDescriptor(String)} */
    @Foldable
    static final MethodHandleRef MHR_CLASSREF_FACTORY
            = MethodHandleRef.of(Kind.STATIC, CR_ClassRef, "ofDescriptor", CR_ClassRef, CR_String);

    /** {@link MethodHandleRef} representing the factory method {@link EnumRef#of(MethodHandleRef)} */
    @Foldable
    static final MethodHandleRef MHR_ENUMREF_FACTORY
            = MethodHandleRef.of(Kind.STATIC, CR_EnumRef, "of", CR_EnumRef, CR_ClassRef, CR_String);

    /** {@link MethodHandleRef} representing the factory method {@link MethodTypeRef#ofDescriptor(String)} */
    @Foldable
    static final MethodHandleRef MHR_METHODTYPEREF_FACTORY
            = MethodHandleRef.of(Kind.STATIC, CR_MethodTypeRef, "ofDescriptor", CR_MethodTypeRef, CR_String);

    /** {@link MethodHandleRef} representing the factory method {@link MethodHandleRef#of(Kind, ClassRef, String, MethodTypeRef)} */
    @Foldable
    static final MethodHandleRef MHR_METHODHANDLEREF_FACTORY
            = MethodHandleRef.of(Kind.STATIC, CR_MethodHandleRef, "of",
                                 CR_MethodHandleRef, CR_MethodHandleRef_Kind, CR_ClassRef, CR_String, CR_MethodTypeRef);

    /** {@link MethodHandleRef} representing the factory method {@link VarHandleRef#ofField(ClassRef, String, ClassRef)} */
    @Foldable
    static final MethodHandleRef MHR_VARHANDLEREF_FIELD_FACTORY
            = MethodHandleRef.of(Kind.STATIC, CR_VarHandleRef, "ofField", CR_VarHandleRef, CR_ClassRef, CR_String, CR_ClassRef);

    /** {@link MethodHandleRef} representing the factory method {@link VarHandleRef#ofStaticField(ClassRef, String, ClassRef)} */
    @Foldable
    static final MethodHandleRef MHR_VARHANDLEREF_STATIC_FIELD_FACTORY
            = MethodHandleRef.of(Kind.STATIC, CR_VarHandleRef, "ofStaticField", CR_VarHandleRef, CR_ClassRef, CR_String, CR_ClassRef);

    /** {@link MethodHandleRef} representing the factory method {@link VarHandleRef#ofArray(ClassRef)} */
    @Foldable
    static final MethodHandleRef MHR_VARHANDLEREF_ARRAY_FACTORY
            = MethodHandleRef.of(Kind.STATIC, CR_VarHandleRef, "ofArray", CR_VarHandleRef, CR_ClassRef);

    /** {@link MethodHandleRef} representing the factory method {@link DynamicConstantRef#of(MethodHandleRef, String, ClassRef, SymbolicRef[])} */
    @Foldable
    static final MethodHandleRef MHR_DYNAMICCONSTANTREF_FACTORY
            = MethodHandleRef.of(Kind.STATIC, CR_CondyRef, "of",
                                 CR_CondyRef, CR_MethodHandleRef, CR_String, CR_ClassRef, CR_SymbolicRef.array());

    /** {@link MethodHandleRef} representing the factory method {@link DynamicCallSiteRef#of(MethodHandleRef, String, MethodTypeRef, SymbolicRef[])} */
    @Foldable
    static final MethodHandleRef MHR_DYNAMICCALLSITEREF_FACTORY
            = MethodHandleRef.of(Kind.STATIC, CR_DynamicCallSiteRef, "of",
                                 CR_DynamicCallSiteRef, CR_MethodHandleRef, CR_String, CR_MethodTypeRef, CR_SymbolicRef.array());
}
