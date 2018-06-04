/*
 * @test /nodynamiccopyright/
 * @summary checking that no intrinsification is possible if arguments are not constants
 * @compile/fail/ref=EffectivelyFinalTestNeg.out -XDrawDiagnostics EffectivelyFinalTestNeg.java
 * @compile/fail/ref=EffectivelyFinalTestNeg.out -XDrawDiagnostics -g EffectivelyFinalTestNeg.java
 */

import java.lang.invoke.*; import java.lang.constant.*;

import static java.lang.invoke.Intrinsics.*;

public class EffectivelyFinalTestNeg {
    String foo() {
        return "invoking EffectivelyFinalTest.foo()";
    }

    void test2() throws Throwable {
        ClassDesc c1 = ConstantDescs.CR_String;
        ClassDesc c2 = ConstantDescs.CR_Integer;
        c1 = null;
        MethodType mt = ldc(MethodTypeDesc.of(c1, c2));
    }

    void test2_1() throws Throwable {
        ClassDesc i = ConstantDescs.CR_String;
        MethodType mt = ldc(MethodTypeDesc.of(i));
        i = null;
    }

    void test3(EffectivelyFinalTestNeg f) throws Throwable {
        ClassDesc c = ConstantDescs.CR_String;
        // you can't trust m1 as it depends on c1 which is not effectively final
        MethodTypeDesc mt = MethodTypeDesc.of(c);
        MethodHandle mh = ldc(MethodHandleDesc.of(MethodHandleDesc.Kind.VIRTUAL, ClassDesc.ofDescriptor("LEffectivelyFinalTestNeg;"), "foo", mt));
        c = null;
    }

    void test4(EffectivelyFinalTestNeg f) throws Throwable {
        ClassDesc c = ConstantDescs.CR_String;
        // you can't trust m1 as it depends on c1 which is not effectively final
        MethodTypeDesc mt = MethodTypeDesc.of(c);
        final MethodHandle mh = ldc(MethodHandleDesc.of(MethodHandleDesc.Kind.VIRTUAL, ClassDesc.ofDescriptor("LEffectivelyFinalTestNeg;"), "foo", mt));
        c = null;
    }

    final ClassDesc cField = ClassDesc.ofDescriptor("LEffectivelyFinalTestNeg;");

    void test5(EffectivelyFinalTestNeg f) throws Throwable {
        ClassDesc c = ConstantDescs.CR_String;
        // you can't trust m1 as it depends on c1 which is not effectively final
        MethodTypeDesc mt = MethodTypeDesc.of(c);
        MethodHandle mh = ldc(MethodHandleDesc.of(MethodHandleDesc.Kind.VIRTUAL, cField, "foo", mt));
        c = null;
    }
}
