/*
 * @test /nodynamiccopyright/
 * @summary smoke negative test for datum classes
 * @compile/fail/ref=Neg01.out -XDrawDiagnostics Neg01.java
 */

public class Neg01 {
    static abstract __datum Sup1(int x, int y) { }

    __datum Bad1(int x, int y, int z) extends Sup1(x, y, z) { } //too many super args

    __datum Bad2(int x, int z) extends Sup1(x) { } //too few super args

    __datum Bad3(int x, int z) extends Sup1(x, z) { } //name mismatch

    __datum Bad4(int x, double y) extends Sup1(x, y) { } //type mismatch

    __datum Bad5(int x, int y) extends Sup1(x, y) {
        Bad5(int x, int y) { super(x, y); } //error: explicit constructor and super header
    }

    static class Sup2 { }

    __datum Bad6(int x, int y) extends Object(x) { } //bad super header

    __datum Bad7(int x, int y) extends Sup2 { } //non-datum superclass

    static __datum Sup3(int x, int y) { }

    __datum Bad7(int x, int y) extends Sup2 { } //non-abstract datum superclass

    __datum Test(int x) {
        Test(int x, int y) {
            default(); //too few
            default(x); //ok
            default(x, y); //too many
        }

        void test() {
           default(); //error - not in a constructor
        }
    }
}
