/*
 * @test /nodynamiccopyright/
 * @bug 8177513
 * @summary underscore can't be followed by dimensions
 * @compile/fail/ref=UnderscoreCantBeFollowedByDimsTest.out -XDrawDiagnostics UnderscoreCantBeFollowedByDimsTest.java
 */

public class UnderscoreCantBeFollowedByDimsTest {
    void m(String _[]) {}
}
