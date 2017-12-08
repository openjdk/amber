/**
 * @test /nodynamiccopyright/
 * @compile/fail/ref=ExpressionSwitchNotExhaustive.out -XDrawDiagnostics ExpressionSwitchNotExhaustive.java
 */

public class ExpressionSwitchNotExhaustive {
    private String print(int i) {
        return switch (i) {
            case 42 -> "42";
            case 43 -> "43";
        };
    }
    private String e(E e) {
        return switch (e) {
            case A -> "42";
        };
    }
    enum E {
        A, B;
    }
}
