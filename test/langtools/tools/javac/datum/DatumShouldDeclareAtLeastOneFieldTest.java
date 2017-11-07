/*
 * @test /nodynamiccopyright/
 * @summary smoke negative test for datum classes
 * @compile/fail/ref=DatumShouldDeclareAtLeastOneFieldTest.out -XDrawDiagnostics DatumShouldDeclareAtLeastOneFieldTest.java
 */

public class DatumShouldDeclareAtLeastOneFieldTest {
    static abstract __datum D1() { }

    static __datum D2() { }
}
