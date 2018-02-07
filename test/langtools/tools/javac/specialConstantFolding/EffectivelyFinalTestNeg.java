/*
 * @test /nodynamiccopyright/
 * @summary checking that no intrinsification is possible if arguments are not constants
 * @compile/fail/ref=EffectivelyFinalTestNeg.out -XDdoConstantFold -XDrawDiagnostics EffectivelyFinalTestNeg.java
 * @compile/fail/ref=EffectivelyFinalTestNeg.out -XDdoConstantFold -XDrawDiagnostics -g EffectivelyFinalTestNeg.java
 */

import java.lang.invoke.*; import java.lang.sym.*;

import static java.lang.invoke.Intrinsics.*;

public class EffectivelyFinalTestNeg {
    String foo() {
        return "invoking EffectivelyFinalTest.foo()";
    }

    void test2() throws Throwable {
        ClassRef c1 = SymbolicRefs.CR_String;
        ClassRef c2 = SymbolicRefs.CR_Integer;
        c1 = null;
        MethodType mt = ldc(MethodTypeRef.of(c1, c2));
    }

    void test2_1() throws Throwable {
        ClassRef i = SymbolicRefs.CR_String;
        MethodType mt = ldc(MethodTypeRef.of(i));
        i = null;
    }

    void test3(EffectivelyFinalTestNeg f) throws Throwable {
        ClassRef c = SymbolicRefs.CR_String;
        // you can't trust m1 as it depends on c1 which is not effectively final
        MethodTypeRef mt = MethodTypeRef.of(c);
        MethodHandle mh = ldc(MethodHandleRef.of(MethodHandleRef.Kind.VIRTUAL, ClassRef.ofDescriptor("LEffectivelyFinalTestNeg;"), "foo", mt));
        c = null;
    }

    void test4(EffectivelyFinalTestNeg f) throws Throwable {
        ClassRef c = SymbolicRefs.CR_String;
        // you can't trust m1 as it depends on c1 which is not effectively final
        MethodTypeRef mt = MethodTypeRef.of(c);
        final MethodHandle mh = ldc(MethodHandleRef.of(MethodHandleRef.Kind.VIRTUAL, ClassRef.ofDescriptor("LEffectivelyFinalTestNeg;"), "foo", mt));
        c = null;
    }

    final ClassRef cField = ClassRef.ofDescriptor("LEffectivelyFinalTestNeg;");

    void test5(EffectivelyFinalTestNeg f) throws Throwable {
        ClassRef c = SymbolicRefs.CR_String;
        // you can't trust m1 as it depends on c1 which is not effectively final
        MethodTypeRef mt = MethodTypeRef.of(c);
        MethodHandle mh = ldc(MethodHandleRef.of(MethodHandleRef.Kind.VIRTUAL, cField, "foo", mt));
        c = null;
    }
}
