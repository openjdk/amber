/*
 * @test /nodynamiccopyright/
 * @bug 8177505
 * @summary wildcard is not allowed in enhanced enums
 * @compile/fail/ref=NoWildcardInEnumConstantsTest.out -XDrawDiagnostics NoWildcardInEnumConstantsTest.java
 */

enum NoWildcardInEnumConstantsTest<X> {
    A<?>
}
