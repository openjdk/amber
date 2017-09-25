/*
 * @test /nodynamiccopyright/
 * @bug 8173059
 * @summary Underscore for unnamed method, constructor, lambda, and catch formals
 * @compile/fail/ref=UnderscoreInLambdaTest.out -XDrawDiagnostics UnderscoreInLambdaTest.java
 */

import java.util.function.*;

public class UnderscoreInLambdaTest {
    void foo() {
        // error
        BiFunction<Integer, String, String> biss1 =
                (_, _) ->
                    String.valueOf(_); // this use will be detected and banned by the parser
        // ok implicit
        BiFunction<Integer, String, String> biss2 =
                (_, _) -> "";
        // ok explicit
        BiFunction<Integer, String, String> biss2 =
                (int _, String _) -> "";
    }
}
