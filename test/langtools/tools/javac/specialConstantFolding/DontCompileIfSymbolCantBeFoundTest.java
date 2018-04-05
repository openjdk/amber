/*
 * @test /nodynamiccopyright/
 * @summary checking that the compiler issues an error if the symbol cant be found
 * @ignore generate warnings instead of errors
 * @compile/fail/ref=DontCompileIfSymbolCantBeFoundTest.out -XDdoConstantFold -XDrawDiagnostics DontCompileIfSymbolCantBeFoundTest.java
 */

import java.lang.invoke.*;
import java.lang.invoke.constant.ClassRef;
import java.lang.invoke.constant.MethodHandleRef;
import java.lang.invoke.constant.MethodTypeRef;
import java.lang.invoke.constant.ConstantRefs;

import static java.lang.invoke.Intrinsics.*;
import static java.lang.invoke.constant.MethodHandleRef.Kind.GETTER;
import static java.lang.invoke.constant.MethodHandleRef.Kind.SETTER;
import static java.lang.invoke.constant.MethodHandleRef.Kind.STATIC_GETTER;
import static java.lang.invoke.constant.MethodHandleRef.Kind.STATIC_SETTER;

public class DontCompileIfSymbolCantBeFoundTest {
    void test() {
        final MethodHandle mhVirtual = ldc(MethodHandleRef.of(MethodHandleRef.Kind.VIRTUAL, ClassRef.ofDescriptor("DontCompileIfSymbolCantBeFoundTest"), "noSuchMethod", MethodTypeRef.ofDescriptor("()Ljava/lang/String;")));
        final MethodHandle mhStatic = ldc(MethodHandleRef.of(MethodHandleRef.Kind.STATIC, ClassRef.ofDescriptor("DontCompileIfSymbolCantBeFoundTest"), "noSuchMethod", "()Ljava/lang/String;"));
        final MethodHandle mhSpecial = ldc(MethodHandleRef.ofSpecial(ClassRef.ofDescriptor("DontCompileIfSymbolCantBeFoundTest"), "noSuchMethod", MethodTypeRef.ofDescriptor("()Ljava/lang/String;"), ClassRef.ofDescriptor("DontCompileIfSymbolCantBeFoundTest")));

        final MethodHandle mhStaticSetter = ldc(MethodHandleRef.ofField(STATIC_SETTER, ClassRef.ofDescriptor("DontCompileIfSymbolCantBeFoundTest"), "noSuchField", ConstantRefs.CR_String));
        final MethodHandle mhSetter = ldc(MethodHandleRef.ofField(SETTER, ClassRef.ofDescriptor("DontCompileIfSymbolCantBeFoundTest"), "noSuchField", ConstantRefs.CR_String));
        final MethodHandle mhGetter = ldc(MethodHandleRef.ofField(GETTER, ClassRef.ofDescriptor("DontCompileIfSymbolCantBeFoundTest"), "noSuchField", ConstantRefs.CR_String));
        final MethodHandle mhStaticGetter = ldc(MethodHandleRef.ofField(STATIC_GETTER, ClassRef.ofDescriptor("DontCompileIfSymbolCantBeFoundTest"), "noSuchField", ConstantRefs.CR_String));

        final MethodHandle mhNewFindConstructorTest = ldc(MethodHandleRef.ofConstructor(ClassRef.ofDescriptor("DontCompileIfSymbolCantBeFoundTest"), VOID, ConstantRefs.CR_String));
    }
}
