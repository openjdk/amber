/*
 * @test /nodynamiccopyright/
 * @bug 8173059
 * @summary Underscore for unnamed method, constructor, lambda, and catch formals
 * @compile/fail/ref=UnderscoreInMethodsTest.out -XDrawDiagnostics UnderscoreInMethodsTest.java
 */

public class UnderscoreInMethodsTest {
    class Super {
        void m(String _) {}
    }

    class Child extends Super {
        void m(String _) {
            // error '_' is not in scope so it can't be used inside the method
            System.out.println(_);
        }
    }
}
