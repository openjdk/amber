/*
 * @test /nodynamiccopyright/
 * @summary smoke negative test for record classes
 * @compile/fail/ref=PrimaryConstructorMustBePublic.out -XDrawDiagnostics PrimaryConstructorMustBePublic.java
 */

public record PrimaryConstructorMustBePublic(int i) {
    PrimaryConstructorMustBePublic {}
}
