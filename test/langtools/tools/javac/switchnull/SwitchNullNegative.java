/**
 * @test /nodynamiccopyright/
 * @compile/fail/ref=SwitchNullNegative.out -XDrawDiagnostics SwitchNullNegative.java
 */

public class SwitchNullNegative {
    private int notReference(int i) {
        switch (i) {
            case null: return 0;
            case 1: return 1;
            default: return 2;
        }
    }

    private int notReferenceExpression(int i) {
        return switch (i) {
            case null -> 0;
            case 1 -> 1;
            default -> 2;
        };
    }

    private int repeatedStatement(Integer i) {
        return switch (i) {
            case 0: break 0;
            case null: break -1;
            case null: break -1;
            case 1: break 1;
        };
    }

    private int repeatedExpression(Integer i) {
        return switch (i) {
            case 0 -> 0;
            case null -> -1;
            case null -> -1;
            case 1 -> 1;
        };
    }
}
