/*
 * @test /nodynamiccopyright/
 * @summary checking that no intrinsification is possible if arguments are not constants
 * @compile/fail/ref=EffectivelyFinalTestNeg.out -XDdoConstantFold -XDrawDiagnostics EffectivelyFinalTestNeg.java
 */

import java.lang.invoke.*;

import static java.lang.invoke.Intrinsics.*;

public class EffectivelyFinalTestNeg {
    String foo() {
        return "invoking EffectivelyFinalTest.foo()";
    }

    void test2() throws Throwable {
        ClassRef c1 = ClassRef.CR_String;
        ClassRef c2 = ClassRef.CR_Integer;
        c1 = null;
        MethodType mt = ldc(MethodTypeRef.of(c1, c2));
    }

    void test2_1() throws Throwable {
        ClassRef i = ClassRef.CR_String;
        MethodType mt = ldc(MethodTypeRef.of(i));
        i = null;
    }

    void test3(EffectivelyFinalTestNeg f) throws Throwable {
        ClassRef c = ClassRef.CR_String;
        // you can't trust m1 as it depends on c1 which is not effectively final
        MethodTypeRef mt = MethodTypeRef.of(c);
        MethodHandle mh = ldc(MethodHandleRef.ofVirtual(ClassRef.ofDescriptor("LEffectivelyFinalTestNeg;"), "foo", mt));
        c = null;
    }

    void test4(EffectivelyFinalTestNeg f) throws Throwable {
        ClassRef c = ClassRef.CR_String;
        // you can't trust m1 as it depends on c1 which is not effectively final
        MethodTypeRef mt = MethodTypeRef.of(c);
        final MethodHandle mh = ldc(MethodHandleRef.ofVirtual(ClassRef.ofDescriptor("LEffectivelyFinalTestNeg;"), "foo", mt));
        c = null;
    }

    final ClassRef cField = ClassRef.ofDescriptor("LEffectivelyFinalTestNeg;");

    void test5(EffectivelyFinalTestNeg f) throws Throwable {
        ClassRef c = ClassRef.CR_String;
        // you can't trust m1 as it depends on c1 which is not effectively final
        MethodTypeRef mt = MethodTypeRef.of(c);
        MethodHandle mh = ldc(MethodHandleRef.ofVirtual(cField, "foo", mt));
        c = null;
    }
}
