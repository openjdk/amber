/*
 * @test /nodynamiccopyright/
 * @summary check that a datum can inherit from DataClass or an abstract datum class
 * @compile/fail/ref=DataClassAsSuper.out -XDrawDiagnostics DataClassAsSuper.java
 */

class DataClassAsSuper {

    // should extend DataClass or an abstract datum
    record D1(int x) extends Object { }

    record D2(int y) {}

    // D2 is datum but not abstract
    record D3(int y, int x) extends D2(y) {}
}
