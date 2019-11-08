/**
 * @test /nodynamiccopyright/
 * @bug 8233754
 * @compile/fail/ref=MultiFinderTest.out -Werror -XDrawDiagnostics -XDfind=all MultiFinderTest.java
 */

public class MultiFinderTest<X> {

    MultiFinderTest<String> mf = new MultiFinderTest<String>();

    interface SAM {
        void m();
    }

    SAM s = new SAM() { public void m() { } };

    static <Z> Z id(Z z) { return z; }

    private static void test(String a) {
        String s = MultiFinderTest.<String>id(a);
    }

    public static void main(String [] args) {
        for (String s : args) {
            test(s);
        }
    }
}
