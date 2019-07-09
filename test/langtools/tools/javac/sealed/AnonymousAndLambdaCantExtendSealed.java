/*
 * @test /nodynamiccopyright/
 * @summary smoke test for sealed classes
 * @compile/fail/ref=AnonymousAndLambdaCantExtendSealed.out -XDrawDiagnostics AnonymousAndLambdaCantExtendSealed.java
 */

class AnonymousAndLambdaCantExtendSealed {
    sealed interface I1 extends Runnable {
        public static I1 i = () -> {};
    }

    sealed interface I2 extends Runnable {
        public static void foo() { new I2() { public void run() { } }; }
    }
}
