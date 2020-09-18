/*
 * @test /nodynamiccopyright/
 * @bug 8173059
 * @summary Underscore for unnamed method, constructor, lambda, and catch formals
 * @compile/fail/ref=UnderscoreInCatchClauseTest.out -XDrawDiagnostics UnderscoreInCatchClauseTest.java
 */

public class UnderscoreInCatchClauseTest {
    void foo() {
        // this use should be allowed
        try { } catch (Throwable _) {
            // this use should be forbidden
            throw _;
        }
    }
}
