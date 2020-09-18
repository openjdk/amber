/**
 * @test /nodynamiccopyright/
 * @bug 8234792
 * @summary Finder should not recommend native methods as local method candidates
 * @compile/fail/ref=NativeMethodTest.out -Werror -XDrawDiagnostics -XDfind=local-methods NativeMethodTest.java
 */

public class NativeMethodTest {

    private static native void xxx();
    private static void yyy() {}
    private static void zzz() {}
    public static void main(String [] args) {
        xxx();
        yyy();
        zzz();
    }
    public static void main() {
        zzz();
    }
}
