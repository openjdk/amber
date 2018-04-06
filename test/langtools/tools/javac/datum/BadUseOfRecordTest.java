/*
 * @test /nodynamiccopyright/
 * @summary check that record is not used as a type name
 * @compile/fail/ref=BadUseOfRecordTest.out -XDrawDiagnostics BadUseOfRecordTest.java
 */

class BadUseOfRecordTest {

    interface record {
        void m();
    }

    class record {}

    enum record {A, B}

    record record(int x);
}
