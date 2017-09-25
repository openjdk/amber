/*
 * @test /nodynamiccopyright/
 * @summary checking that the compiler issues an error if the symbol cant be found
 * @ignore generate warnings instead of errors
 * @compile/fail/ref=DontCompileIfSymbolCantBeFoundTest.out -XDdoConstantFold -XDrawDiagnostics DontCompileIfSymbolCantBeFoundTest.java
 */

import java.lang.invoke.*;

import static java.lang.invoke.Intrinsics.*;

public class DontCompileIfSymbolCantBeFoundTest {
    void test() {
        final MethodHandle mhVirtual = ldc(MethodHandleRef.ofVirtual(ClassRef.ofDescriptor("DontCompileIfSymbolCantBeFoundTest"), "noSuchMethod", "()Ljava/lang/String;"));
        final MethodHandle mhStatic = ldc(MethodHandleRef.ofStatic(ClassRef.ofDescriptor("DontCompileIfSymbolCantBeFoundTest"), "noSuchMethod", "()Ljava/lang/String;"));
        final MethodHandle mhSpecial = ldc(MethodHandleRef.ofSpecial(ClassRef.ofDescriptor("DontCompileIfSymbolCantBeFoundTest"), "noSuchMethod", MethodTypeRef.ofDescriptor("()Ljava/lang/String;"), ClassRef.ofDescriptor("DontCompileIfSymbolCantBeFoundTest")));

        final MethodHandle mhStaticSetter = ldc(MethodHandleRef.ofStaticSetter(ClassRef.ofDescriptor("DontCompileIfSymbolCantBeFoundTest"), "noSuchField", ClassRef.CR_String));
        final MethodHandle mhSetter = ldc(MethodHandleRef.ofSetter(ClassRef.ofDescriptor("DontCompileIfSymbolCantBeFoundTest"), "noSuchField", ClassRef.CR_String));
        final MethodHandle mhGetter = ldc(MethodHandleRef.ofGetter(ClassRef.ofDescriptor("DontCompileIfSymbolCantBeFoundTest"), "noSuchField", ClassRef.CR_String));
        final MethodHandle mhStaticGetter = ldc(MethodHandleRef.ofStaticGetter(ClassRef.ofDescriptor("DontCompileIfSymbolCantBeFoundTest"), "noSuchField", ClassRef.CR_String));

        final MethodHandle mhNewFindConstructorTest = ldc(MethodHandleRef.ofConstructor(ClassRef.ofDescriptor("DontCompileIfSymbolCantBeFoundTest"), VOID, ClassRef.CR_String));
    }
}
