/*
 * @test /nodynamiccopyright/
 * @summary smoke negative test for datum classes
 * @compile/fail/ref=NoAddFieldsCanBeDeclaredInDatumTest.out -XDrawDiagnostics NoAddFieldsCanBeDeclaredInDatumTest.java
 */

public class NoAddFieldsCanBeDeclaredInDatumTest {
    __datum Bad1(int i) {
        int y;
    }

    __datum Bad2(int i) {
        interface I {}
    }

    __datum Bad3(int i) {
        static {}
    }
}
