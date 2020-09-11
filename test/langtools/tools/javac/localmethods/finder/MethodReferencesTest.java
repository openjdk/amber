/**
 * @test /nodynamiccopyright/
 * @bug 8233754
 * @compile/fail/ref=MethodReferencesTest.out -Werror -XDrawDiagnostics -XDfind=local-methods MethodReferencesTest.java
 */

public class MethodReferencesTest {

    private static void run(Runnable r) {} // can be local
    private static void run() {} // cannot be local since a method reference targets

    public static void main(String [] args) {
        run();
        run(MethodReferencesTest::run);
    }
}
