/**
 * @test /nodymaticcopyright/
 * @compile/fail/ref=SwitchStatementArrow-old.out -source 9 -Xlint:-options -XDrawDiagnostics SwitchStatementArrow.java
 * @compile --enable-preview -source 12 SwitchStatementArrow.java
 * @run main/othervm --enable-preview SwitchStatementArrow
 */

import java.util.Objects;
import java.util.function.Function;

public class SwitchStatementArrow {
    public static void main(String... args) {
        new SwitchStatementArrow().run();
    }

    private void run() {
        runTest(this::statement1);
    }

    private void runTest(Function<T, String> print) {
        check(T.A,  print, "A");
        check(T.B,  print, "B-C");
        check(T.C,  print, "B-C");
        try {
            print.apply(T.D);
            throw new AssertionError();
        } catch (IllegalStateException ex) {
            if (!Objects.equals("D", ex.getMessage()))
                throw new AssertionError(ex);
        }
        check(T.E,  print, "other");
    }

    private String statement1(T t) {
        String res;

        switch (t) {
            case A -> { res = "A"; }
            case B, C -> res = "B-C";
            case D -> throw new IllegalStateException("D");
            default -> { res = "other"; break; }
        }

        return res;
    }

    private void check(T t, Function<T, String> print, String expected) {
        String result = print.apply(t);
        if (!Objects.equals(result, expected)) {
            throw new AssertionError("Unexpected result: " + result);
        }
    }

    enum T {
        A, B, C, D, E;
    }
}
