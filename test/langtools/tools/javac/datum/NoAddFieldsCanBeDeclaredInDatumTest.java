/*
 * @test /nodynamiccopyright/
 * @summary smoke negative test for datum classes
 * @compile/fail/ref=NoAddFieldsCanBeDeclaredInDatumTest.out -XDrawDiagnostics NoAddFieldsCanBeDeclaredInDatumTest.java
 */

public class NoAddFieldsCanBeDeclaredInDatumTest {
    record Bad1(int i) {
        int y;
    }

    record Good1(int i) {
        interface I {}
    }

    record Good2(int i) {
        static {}
    }

    record Good3(int i) {
        enum E {A, B}
    }

    record Good4(int i) {
        {}
    }
}
