/*
 * @test /nodynamiccopyright/
 * @summary smoke negative test for datum classes
 * @compile/fail/ref=DatumShouldDeclareAtLeastOneFieldTest.out -XDrawDiagnostics DatumShouldDeclareAtLeastOneFieldTest.java
 */

public class DatumShouldDeclareAtLeastOneFieldTest {
    static abstract record D1() { }

    static record D2() { }
}
