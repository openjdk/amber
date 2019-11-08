/**
 * @test /nodynamiccopyright/
 * @bug 8233754
 * @compile/fail/ref=GenericInvocationTest.out -Werror -XDrawDiagnostics -XDfind=local-methods GenericInvocationTest.java
 */

public class GenericInvocationTest {

    private static <T> T id(T t ) {
        return t;
    } // can be local

    private static <T> T id(T t1, T t2) {
        return null;
    }  // can be local.

    private static <T> T id(T ... t) {
        return null;
    }  // cannot be local since is invoked with explicit type witness.

    public static void main(String [] args) {
        GenericInvocationTest.<String> id (id(id("hello "), id("world ")), "is ", "the ", "message!");
    }
}
