/*
 * @test /nodynamiccopyright/
 * @summary check that record is not used as a type name
 * @compile/fail/ref=BadUseOfRecordKeywordTest.out -XDrawDiagnostics BadUseOfRecordKeywordTest.java
 */

class BadUseOfRecordKeywordTest {

    interface record {
        void m();
    }

    class record {}

    enum record {A, B}

    record record(int x);
}
