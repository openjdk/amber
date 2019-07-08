/*
 * @test /nodynamiccopyright/
 * @summary check that a datum can inherit from DataClass or an abstract datum class
 * @compile/fail/ref=RecordsCantHaveExtendsClause.out -XDrawDiagnostics RecordsCantHaveExtendsClause.java
 */

import java.io.Serializable;

class RecordsCantHaveExtendsClause {

    // even Object which is the implicit super class for records
    record R1(int x) extends Object {}

    record R2(int y) {}

    // can't extend other records either
    record R3(int y, int x) extends R2(y) {}

    // records can implement interfaces
    record R4() implements Serializable, Runnable {
        public void run() {}
    }
}
