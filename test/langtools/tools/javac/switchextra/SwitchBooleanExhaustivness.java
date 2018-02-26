/*
 * @test /nodynamiccopyright/
 * @compile/fail/ref=SwitchBooleanExhaustivness.out -XDrawDiagnostics SwitchBooleanExhaustivness.java
 */
public class SwitchBooleanExhaustivness {

    private int exhaustive1(boolean b) {
        switch (b) {
            case false: return 0;
            case true: return 1;
        }
    }

    private int exhaustive2(boolean b) {
        switch (b) {
            case false: return 0;
            default: return 1;
        }
    }

    private int exhaustive3(boolean b) {
        return switch (b) {
            case false: break 0;
            case true: break 1;
        };
    }

    private int exhaustive4(boolean b) {
        return switch (b) {
            case false: break 0;
            default: break 1;
        };
    }

    private int notExhaustive1(boolean b) {
        switch (b) {
            case false: return 0;
        }
    }

    private int notExhaustive2(boolean b) {
        return switch (b) {
            case false: break 0;
        };
    }

}