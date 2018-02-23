/*
 * @test /nodynamiccopyright/
 * @summary check that method type's shape matches the reference kind
 * @compile/fail/ref=MethodTypeNegTest.out -XDdoConstantFold -Werror -XDrawDiagnostics MethodTypeNegTest.java
 */

import java.lang.invoke.MethodHandle;
import java.lang.sym.ClassRef;
import java.lang.sym.MethodHandleRef;
import java.lang.sym.SymbolicRefs;

import static java.lang.invoke.Intrinsics.ldc;
import static java.lang.sym.MethodHandleRef.Kind.*;

public class MethodTypeNegTest {
    private static final ClassRef CR_THIS = ClassRef.of("MethodTypeNegTest");

    void test() {
        // for setters result must be void...
        MethodHandleRef r1 = MethodHandleRef.of(MethodHandleRef.Kind.SETTER, CR_THIS, "nonexistent", SymbolicRefs.CR_int);
        MethodHandleRef r2 = MethodHandleRef.of(MethodHandleRef.Kind.STATIC_SETTER, CR_THIS, "nonexistent", SymbolicRefs.CR_int);

        // ...and static setters should have only one argument and instance setters two
        MethodHandleRef r3 = MethodHandleRef.of(MethodHandleRef.Kind.SETTER, CR_THIS, "nonexistent", SymbolicRefs.CR_void, SymbolicRefs.CR_int);
        MethodHandleRef r4 = MethodHandleRef.of(MethodHandleRef.Kind.STATIC_SETTER, CR_THIS, "nonexistent", SymbolicRefs.CR_void, SymbolicRefs.CR_int, SymbolicRefs.CR_int);

        // for constructors result type must be void
        MethodHandleRef r5 = MethodHandleRef.of(MethodHandleRef.Kind.CONSTRUCTOR, CR_THIS, "", SymbolicRefs.CR_int);

        // for getters result must be different from void...
        MethodHandleRef r6 = MethodHandleRef.of(MethodHandleRef.Kind.GETTER, CR_THIS, "nonexistent", SymbolicRefs.CR_void);
        MethodHandleRef r7 = MethodHandleRef.of(MethodHandleRef.Kind.STATIC_GETTER, CR_THIS, "nonexistent", SymbolicRefs.CR_void);

        // ...and instance setters should have only one argument, static ones none
        MethodHandleRef r8 = MethodHandleRef.of(MethodHandleRef.Kind.GETTER, CR_THIS, "nonexistent", SymbolicRefs.CR_int, SymbolicRefs.CR_int, SymbolicRefs.CR_int);
        MethodHandleRef r9 = MethodHandleRef.of(MethodHandleRef.Kind.STATIC_GETTER, CR_THIS, "nonexistent", SymbolicRefs.CR_int, SymbolicRefs.CR_int, SymbolicRefs.CR_int);

        // no argument can be void
        MethodHandleRef r10 = MethodHandleRef.of(MethodHandleRef.Kind.VIRTUAL, CR_THIS, "nonexistent", SymbolicRefs.CR_int, SymbolicRefs.CR_void);
    }
}
