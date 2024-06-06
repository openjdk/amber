/*
 * @test /nodynamiccopyright/
 * @summary Verify error related to annotations and patterns
 * @enablePreview
 * @compile/fail/ref=OverloadedPatternDeclarationErrors.out -XDrawDiagnostics -XDdev OverloadedPatternDeclarationErrors.java
 */
public class OverloadedPatternDeclarationErrors {
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

    public static class D {
        public pattern D(Object v1, Float out) {
            match D(10.0f, 10.0f);
        }

        public pattern D(Float out, Integer v1) {
            match D(10.0f, 2);
        }
    }

    public static class D2 {
        public pattern D2(Object v, Integer out) {
            match D2("", 1);
        }

        public pattern D2(CharSequence v, Integer out) {
            match D2("", 2);
        }
    }
}
