/*
 * @test /nodynamiccopyright/
 * @summary smoke negative test for datum classes
 * @compile/fail/ref=SubDatumCannotPassDuplicateArgsToSuperTest.out -XDrawDiagnostics SubDatumCannotPassDuplicateArgsToSuperTest.java
 */

public class SubDatumCannotPassDuplicateArgsToSuperTest {
    abstract __datum D1(int x, int y) { }
    __datum D2(int x, int y, int z) extends D1(x, x) { }
}
