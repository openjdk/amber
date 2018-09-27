/*
 * @test /nodynamiccopyright/
 * @summary smoke test for the concise method feature
 * @compile/fail/ref=ConciseMethodsNegTest01.out -XDrawDiagnostics ConciseMethodsNegTest01.java
 */

class ConciseMethodsNegTest01 {
    int length(String s) = s::length;
}
