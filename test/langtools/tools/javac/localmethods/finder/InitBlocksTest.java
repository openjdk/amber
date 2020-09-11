/**
 * @test /nodynamiccopyright/
 * @bug 8233754
 * @compile/fail/ref=InitBlocksTest.out -Werror -XDrawDiagnostics -XDfind=local-methods InitBlocksTest.java
 */

public class InitBlocksTest {

    private static void goo() {}  // can't be localized
    private static void soo() {}  // can't be localized
    private static void boo() {}  // can't be localized
    private static void loo() {}  // can be 

    private void goo(int i) {}    // can't be localized
    private void soo(int i) {}    // can't be localized
    private void loo(int i) {}    // can be

    static {
        goo();
        loo();
        boo();
    }

    static {
        goo();
        soo();
    }

    {
        goo(42);
        loo(42);
        boo();
    }

    {
        goo(42);
        soo(42);
    }

    public static void main(String [] args) {
        soo();
    }
    public void main() {
        soo(42);
    }
}
