/*
 * @test /nodynamiccopyright/
 * @summary checking that the compiler issues an error if the symbol cant be found
 * @ignore generate warnings instead of errors
 * @compile/fail/ref=DontCompileIfSymbolCantBeFoundTest.out -XDrawDiagnostics DontCompileIfSymbolCantBeFoundTest.java
 */

import java.lang.invoke.*;
import java.lang.invoke.constant.ClassDesc;
import java.lang.invoke.constant.ConstantDescs;
import java.lang.invoke.constant.MethodHandleDesc;
import java.lang.invoke.constant.MethodTypeDesc;

import static java.lang.invoke.Intrinsics.*;
import static java.lang.invoke.constant.MethodHandleDesc.Kind.GETTER;
import static java.lang.invoke.constant.MethodHandleDesc.Kind.SETTER;
import static java.lang.invoke.constant.MethodHandleDesc.Kind.STATIC_GETTER;
import static java.lang.invoke.constant.MethodHandleDesc.Kind.STATIC_SETTER;

public class DontCompileIfSymbolCantBeFoundTest {
    void test() {
        final MethodHandle mhVirtual = ldc(MethodHandleDesc.of(MethodHandleDesc.Kind.VIRTUAL, ClassDesc.ofDescriptor("DontCompileIfSymbolCantBeFoundTest"), "noSuchMethod", MethodTypeDesc.ofDescriptor("()Ljava/lang/String;")));
        final MethodHandle mhStatic = ldc(MethodHandleDesc.of(MethodHandleDesc.Kind.STATIC, ClassDesc.ofDescriptor("DontCompileIfSymbolCantBeFoundTest"), "noSuchMethod", "()Ljava/lang/String;"));
        final MethodHandle mhSpecial = ldc(MethodHandleDesc.ofSpecial(ClassDesc.ofDescriptor("DontCompileIfSymbolCantBeFoundTest"), "noSuchMethod", MethodTypeDesc.ofDescriptor("()Ljava/lang/String;"), ClassDesc.ofDescriptor("DontCompileIfSymbolCantBeFoundTest")));

        final MethodHandle mhStaticSetter = ldc(MethodHandleDesc.ofField(STATIC_SETTER, ClassDesc.ofDescriptor("DontCompileIfSymbolCantBeFoundTest"), "noSuchField", ConstantDescs.CR_String));
        final MethodHandle mhSetter = ldc(MethodHandleDesc.ofField(SETTER, ClassDesc.ofDescriptor("DontCompileIfSymbolCantBeFoundTest"), "noSuchField", ConstantDescs.CR_String));
        final MethodHandle mhGetter = ldc(MethodHandleDesc.ofField(GETTER, ClassDesc.ofDescriptor("DontCompileIfSymbolCantBeFoundTest"), "noSuchField", ConstantDescs.CR_String));
        final MethodHandle mhStaticGetter = ldc(MethodHandleDesc.ofField(STATIC_GETTER, ClassDesc.ofDescriptor("DontCompileIfSymbolCantBeFoundTest"), "noSuchField", ConstantDescs.CR_String));

        final MethodHandle mhNewFindConstructorTest = ldc(MethodHandleDesc.ofConstructor(ClassDesc.ofDescriptor("DontCompileIfSymbolCantBeFoundTest"), VOID, ConstantDescs.CR_String));
    }
}
