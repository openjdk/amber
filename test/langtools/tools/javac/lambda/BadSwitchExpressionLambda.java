/*
 * @test /nodynamiccopyright/
 * @bug 8206986
 * @summary Adding switch expressions
 * @compile/fail/ref=BadSwitchExpressionLambda.out -XDrawDiagnostics --enable-preview -source 12 BadSwitchExpressionLambda.java
 */

class BadSwitchExpressionLambda {

    interface SAM {
        void invoke();
    }

    public static void m() {}

    void test(int i) {
        SAM sam1 = () -> m(); //ok
        SAM sam2 = () -> switch (i) { case 0 -> m(); default -> m(); }; //not ok
    }
}
