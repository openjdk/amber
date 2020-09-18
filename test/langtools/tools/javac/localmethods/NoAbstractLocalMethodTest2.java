/*
 * @test /nodynamiccopyright/
 * @bug 8233038
 * @summary Tests need to be written for local methods feature
 * @compile/fail/ref=NoAbstractLocalMethodTest2.out -XDrawDiagnostics NoAbstractLocalMethodTest2.java
 */

public class NoAbstractLocalMethodTest2 {
    public static void main(String [] args) {
        void yyy (int y);
    }
}
