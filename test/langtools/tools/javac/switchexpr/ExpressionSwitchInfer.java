/**
 * @test /nodynamiccopyright/
 * @compile/fail/ref=ExpressionSwitchInfer.out -XDrawDiagnostics ExpressionSwitchInfer.java
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ExpressionSwitchInfer {

    private <T> T test(List<T> l, Class<T> c, String param) {
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

}
