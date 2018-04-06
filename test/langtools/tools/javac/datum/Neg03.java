/*
 * @test /nodynamiccopyright/
 * @summary smoke negative test for datum classes
 * @compile/fail/ref=Neg03.out -XDrawDiagnostics Neg03.java
 */

public class Neg03 {
    record R(int i) {
        int j;
    }
}
