/*
 * @test /nodynamiccopyright/
 * @summary Ensure that unreachable cases of a pattern switch are diagnosed.
 * @compile/fail/ref=UnreachableCasesTest.out -XDrawDiagnostics UnreachableCasesTest.java
 */

public class UnreachableCasesTest {

    class X {}
    class Y extends X {}
    class Z extends Y {}

    public static void main(String [] args) {

        switch (10) {
            case Integer i:
                System.out.println("i = " + i);
                break;
            default:
                System.out.println("Unreachable");
                break;
        }

        Y y = null;
        switch (y) {
            case Z z1: break;
            case Y y1: break;
            case X x1: break;
        }
    }
}