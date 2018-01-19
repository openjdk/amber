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

/**
 * Predefined constants for common symbolic references, including class references
 * for primitive types and common platform types, and method handle references
 * for standard bootstrap methods.
 */
public final class SymbolicRefs {
    // Warning -- don't change the order of these declarations!

    /**  ClassRef representing the class java.lang.Object */
    @Foldable
    public static final ClassRef CR_Object = ClassRef.of("java.lang.Object");

    /**  ClassRef representing the class java.lang.String */
    @Foldable
    public static final ClassRef CR_String = ClassRef.of("java.lang.String");

    /**  ClassRef representing the class java.lang.Class */
    @Foldable
    public static final ClassRef CR_Class = ClassRef.of("java.lang.Class");

    /**  ClassRef representing the class java.lang.Number */
    @Foldable
    public static final ClassRef CR_Number = ClassRef.of("java.lang.Number");

    /**  ClassRef representing the class java.lang.Integer */
    @Foldable
    public static final ClassRef CR_Integer = ClassRef.of("java.lang.Integer");

    /**  ClassRef representing the class java.lang.Long */
    @Foldable
    public static final ClassRef CR_Long = ClassRef.of("java.lang.Long");

    /**  ClassRef representing the class java.lang.Float */
    @Foldable
    public static final ClassRef CR_Float = ClassRef.of("java.lang.Float");

    /**  ClassRef representing the class java.lang.Double */
    @Foldable
    public static final ClassRef CR_Double = ClassRef.of("java.lang.Double");

    /**  ClassRef representing the class java.lang.Short */
    @Foldable
    public static final ClassRef CR_Short = ClassRef.of("java.lang.Short");

    /**  ClassRef representing the class java.lang.Byte */
    @Foldable
    public static final ClassRef CR_Byte = ClassRef.of("java.lang.Byte");

    /**  ClassRef representing the class java.lang.Character */
    @Foldable
    public static final ClassRef CR_Character = ClassRef.of("java.lang.Character");

    /**  ClassRef representing the class java.lang.Boolean */
    @Foldable
    public static final ClassRef CR_Boolean = ClassRef.of("java.lang.Boolean");

    /**  ClassRef representing the class java.lang.Void */
    @Foldable
    public static final ClassRef CR_Void = ClassRef.of("java.lang.Void");

    /**  ClassRef representing the class java.lang.Throwable */
    @Foldable
    public static final ClassRef CR_Throwable = ClassRef.of("java.lang.Throwable");

    /**  ClassRef representing the class java.lang.Exception */
    @Foldable
    public static final ClassRef CR_Exception = ClassRef.of("java.lang.Exception");

    /**  ClassRef representing the class java.lang.Enum */
    @Foldable
    public static final ClassRef CR_Enum = ClassRef.of("java.lang.Enum");

    /**  ClassRef representing the class java.lang.invoke.VarHandle */
    @Foldable
    public static final ClassRef CR_VarHandle = ClassRef.of("java.lang.invoke.VarHandle");

    /**  ClassRef representing the class java.lang.invoke.MethodHandles */
    @Foldable
    public static final ClassRef CR_MethodHandles = ClassRef.of("java.lang.invoke.MethodHandles");

    /**  ClassRef representing the class java.lang.invoke.MethodHandles.Lookup */
    @Foldable
    public static final ClassRef CR_Lookup = CR_MethodHandles.inner("Lookup");

    /**  ClassRef representing the class java.lang.invoke.MethodHandle */
    @Foldable
    public static final ClassRef CR_MethodHandle = ClassRef.of("java.lang.invoke.MethodHandle");

    /**  ClassRef representing the class java.lang.invoke.MethodType */
    @Foldable
    public static final ClassRef CR_MethodType = ClassRef.of("java.lang.invoke.MethodType");

    /**  ClassRef representing the class java.lang.invoke.CallSite */
    @Foldable
    public static final ClassRef CR_CallSite = ClassRef.of("java.lang.invoke.CallSite");

    /**  ClassRef representing the interface java.util.Collection */
    @Foldable
    public static final ClassRef CR_Collection = ClassRef.of("java.util.Collection");

    /**  ClassRef representing the interface java.util.List */
    @Foldable
    public static final ClassRef CR_List = ClassRef.of("java.util.List");

    /**  ClassRef representing the interface java.util.Set */
    @Foldable
    public static final ClassRef CR_Set = ClassRef.of("java.util.Set");

    /**  ClassRef representing the interface java.util.Map */
    @Foldable
    public static final ClassRef CR_Map = ClassRef.of("java.util.Map");

    @Foldable
    static final ClassRef CR_SymbolicRef = ClassRef.of("java.lang.sym.SymbolicRef");

    @Foldable
    static final ClassRef CR_ClassRef = ClassRef.of("java.lang.sym.ClassRef");

    @Foldable
    static final ClassRef CR_EnumRef = ClassRef.of("java.lang.sym.EnumRef");

    @Foldable
    static final ClassRef CR_MethodTypeRef = ClassRef.of("java.lang.sym.MethodTypeRef");

    @Foldable
    static final ClassRef CR_MethodHandleRef = ClassRef.of("java.lang.sym.MethodHandleRef");

    @Foldable
    static final ClassRef CR_VarHandleRef = ClassRef.of("java.lang.sym.VarHandleRef");

    @Foldable
    static final ClassRef CR_MethodHandleRef_Kind = CR_MethodHandleRef.inner("Kind");

    @Foldable
    static final ClassRef CR_DynamicConstantRef = ClassRef.of("java.lang.sym.DynamicConstantRef");

    @Foldable
    static final ClassRef CR_IndyRef = ClassRef.of("java.lang.sym.IndyRef");

    @Foldable
    static final ClassRef CR_ConstantBootstraps = ClassRef.of("java.lang.invoke.ConstantBootstraps");

    @Foldable
    public static final MethodHandleRef BSM_PRIMITIVE_CLASS
            = MethodHandleRef.ofCondyBootstrap(CR_ConstantBootstraps, "primitiveClass", CR_Class);

    @Foldable
    public static final MethodHandleRef BSM_ENUM_CONSTANT
            = MethodHandleRef.ofCondyBootstrap(CR_ConstantBootstraps, "enumConstant", CR_Enum);

    @Foldable
    public static final MethodHandleRef BSM_NULL_CONSTANT
            = MethodHandleRef.ofCondyBootstrap(CR_ConstantBootstraps, "nullConstant", SymbolicRefs.CR_Object);

    @Foldable
    public static final MethodHandleRef BSM_VARHANDLE_FIELD
            = MethodHandleRef.ofCondyBootstrap(CR_ConstantBootstraps, "fieldVarHandle", CR_VarHandle, CR_Class, CR_Class);

    @Foldable
    public static final MethodHandleRef BSM_VARHANDLE_STATIC_FIELD
            = MethodHandleRef.ofCondyBootstrap(CR_ConstantBootstraps, "staticFieldVarHandle", CR_VarHandle, CR_Class, CR_Class);

    @Foldable
    public static final MethodHandleRef BSM_VARHANDLE_ARRAY
            = MethodHandleRef.ofCondyBootstrap(CR_ConstantBootstraps, "arrayVarHandle", CR_VarHandle, CR_Class);

    @Foldable
    public static final MethodHandleRef BSM_INVOKE
            = MethodHandleRef.ofCondyBootstrap(CR_ConstantBootstraps, "invoke", CR_Object, CR_MethodHandle, CR_Object.array());

    /**  ClassRef representing the primitive type int */
    @Foldable
    public static final ClassRef CR_int = ClassRef.ofDescriptor("I");

    /**  ClassRef representing the primitive type long */
    @Foldable
    public static final ClassRef CR_long = ClassRef.ofDescriptor("J");

    /**  ClassRef representing the primitive type float */
    @Foldable
    public static final ClassRef CR_float = ClassRef.ofDescriptor("F");

    /**  ClassRef representing the primitive type double */
    @Foldable
    public static final ClassRef CR_double = ClassRef.ofDescriptor("D");

    /**  ClassRef representing the primitive type short */
    @Foldable
    public static final ClassRef CR_short = ClassRef.ofDescriptor("S");

    /**  ClassRef representing the primitive type byte */
    @Foldable
    public static final ClassRef CR_byte = ClassRef.ofDescriptor("B");

    /**  ClassRef representing the primitive type char */
    @Foldable
    public static final ClassRef CR_char = ClassRef.ofDescriptor("C");

    /**  ClassRef representing the primitive type boolean */
    @Foldable
    public static final ClassRef CR_boolean = ClassRef.ofDescriptor("Z");

    /**  ClassRef representing the void type */
    @Foldable
    public static final ClassRef CR_void = ClassRef.ofDescriptor("V");

    /** Symbolic reference representing null */
    @Foldable
    public static final SymbolicRef<?> NULL = DynamicConstantRef.of(SymbolicRefs.BSM_NULL_CONSTANT, SymbolicRefs.CR_Object);

    @Foldable
    static final MethodHandleRef MHR_CLASSREF_FACTORY
            = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, CR_ClassRef, "ofDescriptor", CR_ClassRef, CR_String);

    @Foldable
    static final MethodHandleRef MHR_ENUMREF_FACTORY
            = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, CR_EnumRef, "of", CR_EnumRef, CR_ClassRef, CR_String);

    @Foldable
    static final MethodHandleRef MHR_METHODTYPEREF_FACTORY
            = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, CR_MethodTypeRef, "ofDescriptor", CR_MethodTypeRef, CR_String);

    @Foldable
    static final MethodHandleRef MHR_METHODHANDLEREF_FACTORY
            = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, CR_MethodHandleRef, "of",
                                 CR_MethodHandleRef, CR_MethodHandleRef_Kind, CR_ClassRef, CR_String, CR_MethodTypeRef);

    @Foldable
    static final MethodHandleRef MHR_VARHANDLEREF_FIELD_FACTORY
            = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, CR_VarHandleRef, "fieldVarHandle", CR_VarHandleRef, CR_ClassRef, CR_String, CR_ClassRef);

    @Foldable
    static final MethodHandleRef MHR_VARHANDLEREF_STATIC_FIELD_FACTORY
            = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, CR_VarHandleRef, "staticFieldVarHandle", CR_VarHandleRef, CR_ClassRef, CR_String, CR_ClassRef);

    @Foldable
    static final MethodHandleRef MHR_VARHANDLEREF_ARRAY_FACTORY
            = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, CR_VarHandleRef, "arrayVarHandle", CR_VarHandleRef, CR_ClassRef);

    @Foldable
    static final MethodHandleRef MHR_DYNAMICCONSTANTREF_FACTORY
            = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, CR_DynamicConstantRef, "of",
                                 CR_DynamicConstantRef, CR_MethodHandleRef, CR_String, CR_ClassRef, CR_SymbolicRef.array());

    @Foldable
    static final MethodHandleRef MHR_INDYREF_FACTORY
            = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, CR_IndyRef, "of",
                                 CR_IndyRef, CR_MethodHandleRef, CR_String, CR_MethodTypeRef, CR_SymbolicRef.array());
}
