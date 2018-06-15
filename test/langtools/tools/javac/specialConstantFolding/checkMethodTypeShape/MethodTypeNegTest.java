/*
 * @test /nodynamiccopyright/
 * @summary check that method type's shape matches the reference kind
 * @compile/fail/ref=MethodTypeNegTest.out -Werror -XDrawDiagnostics MethodTypeNegTest.java
 */

import java.lang.constant.*;

public class MethodTypeNegTest {
    private static final ClassDesc CR_THIS = ClassDesc.of("MethodTypeNegTest");

    void test() {
        // for setters result must be void...
        MethodHandleDesc r1 = MethodHandleDesc.of(DirectMethodHandleDesc.Kind.SETTER, CR_THIS, "nonexistent", ConstantDescs.CR_int);
        MethodHandleDesc r2 = MethodHandleDesc.of(DirectMethodHandleDesc.Kind.STATIC_SETTER, CR_THIS, "nonexistent", ConstantDescs.CR_int);

        // ...and static setters should have only one argument and instance setters two
        MethodHandleDesc r3 = MethodHandleDesc.of(DirectMethodHandleDesc.Kind.SETTER, CR_THIS, "nonexistent", ConstantDescs.CR_void, ConstantDescs.CR_int);
        MethodHandleDesc r4 = MethodHandleDesc.of(DirectMethodHandleDesc.Kind.STATIC_SETTER, CR_THIS, "nonexistent", ConstantDescs.CR_void, ConstantDescs.CR_int, ConstantDescs.CR_int);

        // for constructors result type must be void
        MethodHandleDesc r5 = MethodHandleDesc.of(DirectMethodHandleDesc.Kind.CONSTRUCTOR, CR_THIS, "", ConstantDescs.CR_int);

        // for getters result must be different from void...
        MethodHandleDesc r6 = MethodHandleDesc.of(DirectMethodHandleDesc.Kind.GETTER, CR_THIS, "nonexistent", ConstantDescs.CR_void);
        MethodHandleDesc r7 = MethodHandleDesc.of(DirectMethodHandleDesc.Kind.STATIC_GETTER, CR_THIS, "nonexistent", ConstantDescs.CR_void);

        // ...and instance setters should have only one argument, static ones none
        MethodHandleDesc r8 = MethodHandleDesc.of(DirectMethodHandleDesc.Kind.GETTER, CR_THIS, "nonexistent", ConstantDescs.CR_int, ConstantDescs.CR_int, ConstantDescs.CR_int);
        MethodHandleDesc r9 = MethodHandleDesc.of(DirectMethodHandleDesc.Kind.STATIC_GETTER, CR_THIS, "nonexistent", ConstantDescs.CR_int, ConstantDescs.CR_int, ConstantDescs.CR_int);

        // no argument can be void
        MethodHandleDesc r10 = MethodHandleDesc.of(DirectMethodHandleDesc.Kind.VIRTUAL, CR_THIS, "nonexistent", ConstantDescs.CR_int, ConstantDescs.CR_void);
    }
}
