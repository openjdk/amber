/*
 * @test /nodynamiccopyright/
 * @summary smoke test for sealed classes
 * @compile/fail/ref=SealedTest01.out -XDrawDiagnostics SealedTest01.java
 */

class SealedTest01 {
    static final class SC permits C_SC { }
    static final abstract class SAC permits C_SAC { }
    static final interface SI permits C_SI, I_SI { }

    static class C_SC extends SC { }
    static class C_SAC extends SAC { }
    static class C_SI implements SI { }
    static interface I_SI extends SI { }
}

class SealedTest01_Other {
    static class C_SC extends SealedTest01.SC { }
    static class C_SAC extends SealedTest01.SAC { }
    static class C_SI implements SealedTest01.SI { }
    static interface I_SI extends SealedTest01.SI { }
}
