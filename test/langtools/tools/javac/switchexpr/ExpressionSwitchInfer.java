/**
 * @test /nodynamiccopyright/
 * @compile/fail/ref=ExpressionSwitchInfer.out -XDrawDiagnostics ExpressionSwitchInfer.java
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ExpressionSwitchInfer {

    private <T> T test(List<T> l, Class<T> c, Object param) {
        test(param == null ? new ArrayList<>() : new ArrayList<>(), CharSequence.class, param).charAt(0);
        test(param == null ? new ArrayList<>() : new ArrayList<>(), CharSequence.class, param).substring(0);

        test(switch (param) {
            case null -> new ArrayList<>();
            default -> new ArrayList<>();
        }, CharSequence.class, param).charAt(0);
        test(switch (param) {
            case null -> new ArrayList<>();
            default -> new ArrayList<>();
        }, CharSequence.class, param).substring(0);

        return null;
    }

    private String print(Object o) {
        return switch (o) {
            case Integer i -> String.format("int %d", i);
            case Double d  -> String.format("double %2.1f", d);
            default        -> String.format("String %s", o);
        };
    }

    private void check(Object input, String expected) {
        String result = print(input);
        if (!Objects.equals(result, expected)) {
            throw new AssertionError("Unexpected result: " + result);
        }
    }
}
