/*
 * @test /nodynamiccopyright/
 * @summary smoke negative test for datum classes
 * @compile/fail/ref=SubDatumFieldsMustBeAPrefixOfParentTest.out -XDrawDiagnostics SubDatumFieldsMustBeAPrefixOfParentTest.java
 */

public class SubDatumFieldsMustBeAPrefixOfParentTest {
    abstract record D1(int x, int y) { }
    record D2(int x, int y, int z) extends D1(y, x) { }
}
