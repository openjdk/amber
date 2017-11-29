/*
 * @test /nodynamiccopyright/
 * @summary smoke negative test for datum classes
 * @compile/fail/ref=Neg01.out -XDrawDiagnostics Neg01.java
 */

public class Neg01 {
    static abstract record Sup1(int x, int y) { }

    record Bad1(int x, int y, int z) extends Sup1(x, y, z) { } //too many super args

    record Bad2(int x, int z) extends Sup1(x) { } //too few super args

    record Bad3(int x, int z) extends Sup1(x, z) { } //name mismatch

    record Bad4(int x, double y) extends Sup1(x, y) { } //type mismatch

    record Bad5(int x, int y) extends Sup1(x, y) {
        Bad5(int x, int y) { super(x, y); } //error: explicit constructor and super header
    }

    static class Sup2 { }

    record Bad6(int x, int y) extends Object(x) { } //bad super header

    record Bad7(int x, int y) extends Sup2 { } //non-datum superclass

    static record Sup3(int x, int y) { }

    record Bad7(int x, int y) extends Sup2 { } //non-abstract datum superclass

    record Test(int x) {
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
