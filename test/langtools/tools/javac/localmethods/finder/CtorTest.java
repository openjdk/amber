/**
 * @test /nodynamiccopyright/
 * @bug 8233754
 * @compile/fail/ref=CtorTest.out -Werror -XDrawDiagnostics -XDfind=local-methods CtorTest.java
 */

public class CtorTest {

    private static int foo() { // can be local.
        return 42;
    }

    private void goo() {} // can be local.

    private CtorTest(int i) { goo(); } // never flag a constructor.

    public static void main(String [] args) {
        new CtorTest(foo());
    }
}
