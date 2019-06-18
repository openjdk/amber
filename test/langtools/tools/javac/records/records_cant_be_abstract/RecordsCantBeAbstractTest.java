/*
 * @test /nodynamiccopyright/
 * @summary smoke negative test for datum classes
 * @compile/fail/ref=RecordsCantBeAbstractTest.out -XDrawDiagnostics RecordsCantBeAbstractTest.java
 */

abstract record RecordsCantBeAbstractTest(int i);
