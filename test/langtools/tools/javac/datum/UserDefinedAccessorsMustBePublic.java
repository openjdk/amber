/*
 * @test /nodynamiccopyright/
 * @summary smoke negative test for record classes
 * @compile/fail/ref=UserDefinedAccessorsMustBePublic.out -XDrawDiagnostics UserDefinedAccessorsMustBePublic.java
 */

public record UserDefinedAccessorsMustBePublic(int i) {
    int i() { return i; }
}
