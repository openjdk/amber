/**
 * @test /nodynamiccopyright/
 * @bug 8233754
 * @compile/fail/ref=AuxilliaryClassTest.out -Werror -XDrawDiagnostics -XDfind=local-methods AuxilliaryClassTest.java
 */

class AuxilliaryClassTest {

    private static void run() {
        run();
        run();
        run();
        run();
    } // can be local since recursion does not count.

    public static void main(String [] args) {
        run();
        run();
        run();
        run();  // multiple call sites from the same method is OK.
    }
}

class AuxilliaryClassTest01 {

    private static void run() {
        run();
        run();
        run();
        run();
    } // can be local since recursion does not count.

    public static void main(String [] args) {
        run();
        run();
        run();
        run();  // multiple call sites from the same method is OK.
    }
}
