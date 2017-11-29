/*
 * @test /nodynamiccopyright/
 * @summary smoke negative test for datum classes
 * @compile/fail/ref=NoAddFieldsCanBeDeclaredInDatumTest.out -XDrawDiagnostics NoAddFieldsCanBeDeclaredInDatumTest.java
 */

public class NoAddFieldsCanBeDeclaredInDatumTest {
    record Bad1(int i) {
        int y;
    }

    record Bad2(int i) {
        interface I {}
    }

    record Bad3(int i) {
        static {}
    }
}
