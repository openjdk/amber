/*
 * @test /nodynamiccopyright/
 * @bug 8233038
 * @summary Tests need to be written for local methods feature
 * @compile/fail/ref=NameClashTest.out -XDrawDiagnostics NameClashTest.java
 */

public class NameClashTest {

    class X{}

    int foo() {
        return 10;
    }
    public static void main(String args []) {
        <T> int foo(T t) {
            return 10;
        }
        int foo(Object o) { // Error after erasure
            return 10;
        }
        if (args == null) {
            int foo(Object o, String s) { return 10; } // OK.
            int foo(Object o) { // Error after erasure
                return 10;
            }
        }

        int foo(String ... s) {
            return 10;
        }

        int foo(String [] s) {  // error varags signature same.
            return 10;
        }

        int foo() { return 10; }  // OK.

        void foo(X x) {}
        void foo(X x1, X x2) {} // OK.
        void foo(X x) {} // clash
    }

    void goo() {
        if (10 == 10) {
            void goo(int x) {} // OK
            void goo() { // OK.
                void goo() {} // Error.
                class Y {
                    void goo() {} // OK.
                }
            }
        } else {
            void goo(int x) {} // OK
            void goo() {} // OK.
        }
        void goo(int x) {} // OK
        void goo() {} // OK.
    }
}
