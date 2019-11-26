/**
 * @test /nodynamiccopyright/
 * @bug 8234800
 * @summary Finder crashes when encountering private method calls in field initializer
 * @compile/fail/ref=CallInFieldInit.out -Werror -XDrawDiagnostics -XDfind=local-methods CallInFieldInit.java
 */

public class CallInFieldInit {

    int x = getInt();
    int y = getAnotherInt();

    private int getInt() { return 0; }
    private int getAnotherInt() { return 0; }

    private int getIntLocalizable() { return 0; }

    void foo() {
        getIntLocalizable();
        getAnotherInt();
    }
}
