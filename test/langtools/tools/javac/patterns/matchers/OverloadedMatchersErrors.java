/*
 * @test /nodynamiccopyright/
 * @summary Verify error related to annotations and patterns
 * @enablePreview
 * @compile/fail/ref=OverloadedMatchersErrors.out -XDrawDiagnostics OverloadedMatchersErrors.java
 */
public class OverloadedMatchersErrors {
    private static int test(D o) {
        if (o instanceof D(String data, Integer out)) { // no compatible matcher found
            return out;
        }
        return -1;
    }

    private static int test2(D2 o) {
        if (o instanceof D2(String data, Integer out)) { // ambiguous
            return out;
        }
        return -1;
    }

    public record D() {
        public __matcher D(Object v1, Float out) {
            out = 10.0f;
        }

        public __matcher D(Float out, Integer v1) {
            out = 2;
        }
    }

    public record D2() {
        public __matcher D2(Object v, Integer out) {
            out = 1;
        }

        public __matcher D2(CharSequence v, Integer out) {
            out = 2;
        }
    }
}
