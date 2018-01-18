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
        MethodHandle mh1 = ldc(r1);
        MethodHandleRef r2 = MethodHandleRef.of(MethodHandleRef.Kind.STATIC_SETTER, CR_THIS, "nonexistent", SymbolicRefs.CR_int);
        MethodHandle mh2 = ldc(r2);

        // ...and static setters should have only one argument and instance setters two
        MethodHandleRef r3 = MethodHandleRef.of(MethodHandleRef.Kind.SETTER, CR_THIS, "nonexistent", SymbolicRefs.CR_void, SymbolicRefs.CR_int);
        MethodHandle mh3 = ldc(r3);
        MethodHandleRef r4 = MethodHandleRef.of(MethodHandleRef.Kind.STATIC_SETTER, CR_THIS, "nonexistent", SymbolicRefs.CR_void, SymbolicRefs.CR_int, SymbolicRefs.CR_int);
        MethodHandle mh4 = ldc(r4);

        // for constructors result type must be void
        MethodHandleRef r5 = MethodHandleRef.of(MethodHandleRef.Kind.CONSTRUCTOR, CR_THIS, "", SymbolicRefs.CR_int);
        MethodHandle mh5 = ldc(r5);

        // for getters result must be different from void...
        MethodHandleRef r6 = MethodHandleRef.of(MethodHandleRef.Kind.GETTER, CR_THIS, "nonexistent", SymbolicRefs.CR_void);
        MethodHandle mh6 = ldc(r6);
        MethodHandleRef r7 = MethodHandleRef.of(MethodHandleRef.Kind.STATIC_GETTER, CR_THIS, "nonexistent", SymbolicRefs.CR_void);
        MethodHandle mh7 = ldc(r7);

        // ...and instance setters should have only one argument, static ones none
        MethodHandleRef r8 = MethodHandleRef.of(MethodHandleRef.Kind.GETTER, CR_THIS, "nonexistent", SymbolicRefs.CR_int, SymbolicRefs.CR_int, SymbolicRefs.CR_int);
        MethodHandle mh8 = ldc(r8);
        MethodHandleRef r9 = MethodHandleRef.of(MethodHandleRef.Kind.STATIC_GETTER, CR_THIS, "nonexistent", SymbolicRefs.CR_int, SymbolicRefs.CR_int, SymbolicRefs.CR_int);
        MethodHandle mh9 = ldc(r9);

        // no argument can be void
        MethodHandleRef r10 = MethodHandleRef.of(MethodHandleRef.Kind.VIRTUAL, CR_THIS, "nonexistent", SymbolicRefs.CR_int, SymbolicRefs.CR_void);
        MethodHandle mh10 = ldc(r10);
    }
}
