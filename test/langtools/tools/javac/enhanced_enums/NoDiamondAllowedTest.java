/*
 * @test /nodynamiccopyright/
 * @bug 8177505
 * @summary diamond is not allowed in enhanced enums
 * @compile/fail/ref=NoDiamondAllowedTest.out -XDrawDiagnostics NoDiamondAllowedTest.java
 */

enum NoDiamondAllowedTest<X> {
    A<>("");

    NoDiamondAllowedTest(X x) {}
}
