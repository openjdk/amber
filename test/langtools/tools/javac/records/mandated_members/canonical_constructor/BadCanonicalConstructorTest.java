/*
 * @test /nodynamiccopyright/
 * @summary check that a datum can inherit from DataClass or an abstract datum class
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
