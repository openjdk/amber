/*
 * @test /nodynamiccopyright/
 * @summary smoke negative test for datum classes
 * @compile/fail/ref=SubDatumCannotPassDuplicateArgsToSuperTest.out -XDrawDiagnostics SubDatumCannotPassDuplicateArgsToSuperTest.java
 */

public class SubDatumCannotPassDuplicateArgsToSuperTest {
    abstract record D1(int x, int y) { }
    record D2(int x, int y, int z) extends D1(x, x) { }
}
