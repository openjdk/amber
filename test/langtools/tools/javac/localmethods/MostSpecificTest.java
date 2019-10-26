/*
 * @test /nodynamiccopyright/
 * @bug 8233038
 * @summary Tests need to be written for local methods feature
 * @compile/fail/ref=MostSpecificTest.out -XDrawDiagnostics MostSpecificTest.java
 */

public class MostSpecificTest {
    public static void main(String args []) {
        void foo(String s, Object o) {
        }
        void foo(Object o, String s) {
        }

        foo(new String(), new String()); // ambigious
    }
}
