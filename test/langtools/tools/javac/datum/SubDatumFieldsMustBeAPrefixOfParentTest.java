/*
 * @test /nodynamiccopyright/
 * @summary smoke negative test for datum classes
 * @compile/fail/ref=SubDatumFieldsMustBeAPrefixOfParentTest.out -XDrawDiagnostics SubDatumFieldsMustBeAPrefixOfParentTest.java
 */

public class SubDatumFieldsMustBeAPrefixOfParentTest {
    abstract __datum D1(int x, int y) { }
    __datum D2(int x, int y, int z) extends D1(y, x) { }
}
