/*
 * @test /nodynamiccopyright/
 * @summary check that record is not used as a type name
 * @compile/fail/ref=UnexpectedKindRecordTest.out -XDrawDiagnostics UnexpectedKindRecordTest.java
 */

class UnexpectedKindRecordTest {

    record R(int i) {
        public int i() { return i; }
        public int i() { return i; }
    }
}
