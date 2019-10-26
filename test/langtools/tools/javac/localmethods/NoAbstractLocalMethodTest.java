/*
 * @test /nodynamiccopyright/
 * @bug 8233038
 * @summary Tests need to be written for local methods feature
 * @compile/fail/ref=NoAbstractLocalMethodTest.out -XDrawDiagnostics NoAbstractLocalMethodTest.java
 */

public class NoAbstractLocalMethodTest {
    public static void main(String [] args) {
        abstract void xxx (int y);
    }
}
