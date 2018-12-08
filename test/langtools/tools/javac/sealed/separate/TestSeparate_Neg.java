/*
 * @test /nodynamiccopyright/
 * @summary smoke test for sealed and separate compilation
 * @compile/fail/ref=TestSeparate_Neg.out -XDrawDiagnostics TestSeparate_Neg.java SealedClasses.java
 */

public class TestSeparate_Neg {
    static class C_SCN extends SealedClasses.SC { }
    static class C_SACN extends SealedClasses.SAC { }
    static class C_SIN implements SealedClasses.SI { }
    static interface I_SIN extends SealedClasses.SI { }
}
