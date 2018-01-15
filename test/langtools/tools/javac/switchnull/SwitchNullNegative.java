/**
 * @test /nodynamiccopyright/
 * @compile/fail/ref=SwitchNullNegative.out -XDrawDiagnostics SwitchNullNegative.java
 */

public class SwitchNullNegative {
    private int notFirst(String str) {
        switch (str) {
            case "": return 1;
            case null: return 0;
            default: return 2;
        }
    }

    private int notReference(int i) {
        switch (i) {
            case null: return 0;
            case 1: return 1;
            default: return 2;
        }
    }
    private int notFirstExpression(String str) {
        return switch (str) {
            case "" -> 1;
            case null -> 0;
            default -> 2;
        };
    }

    private int notReferenceExpression(int i) {
        return switch (i) {
            case null -> 0;
            case 1 -> 1;
            default -> 2;
        };
    }
}
