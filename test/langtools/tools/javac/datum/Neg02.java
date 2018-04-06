/*
 * @test /nodynamiccopyright/
 * @summary smoke negative test for datum classes
 * @compile/fail/ref=Neg02.out -XDrawDiagnostics Neg02.java
 */

public class Neg02 {
    record R(non_final int x);
}
