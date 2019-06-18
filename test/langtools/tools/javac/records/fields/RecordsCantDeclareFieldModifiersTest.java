/*
 * @test /nodynamiccopyright/
 * @summary smoke negative test for datum classes
 * @compile/fail/ref=RecordsCantDeclareFieldModifiersTest.out -XDrawDiagnostics RecordsCantDeclareFieldModifiersTest.java
 */

public class RecordsCantDeclareFieldModifiersTest {
    record R(public int i);
}
