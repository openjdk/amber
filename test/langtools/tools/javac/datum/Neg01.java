/*
 * @test /nodynamiccopyright/
 * @summary smoke negative test for datum classes
 * @compile/fail/ref=Neg01.out -XDrawDiagnostics Neg01.java
 */

public class Neg01 {
    static abstract record Sup1(int x, int y) { }

    record Bad1(int x, int y, int z) extends Sup1(x, y) { } //can't extend

    static class Sup2 { }

    static record Sup3(int x, int y) { }
}
