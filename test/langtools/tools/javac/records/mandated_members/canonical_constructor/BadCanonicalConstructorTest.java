/*
 * @test /nodynamiccopyright/
 * @summary check that the compiler doesn't accept canonical constructors with wrong accessibility
 * @compile/fail/ref=BadCanonicalConstructorTest.out -XDrawDiagnostics BadCanonicalConstructorTest.java
 */

public class BadCanonicalConstructorTest {
    record R1() {
        private R1 {}
    }

    record R2() {
        protected R2 {}
    }

    record R3() {
        R3 {}
    }
}
