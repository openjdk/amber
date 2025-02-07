/*
 * @test /nodynamiccopyright/
 * @summary Verify error related to annotations and patterns
 * @enablePreview
 * @compile/fail/ref=OverloadedPatternDeclarationErrors.out -XDrawDiagnostics -XDdev OverloadedPatternDeclarationErrors.java
 */
public class OverloadedPatternDeclarationErrors {
    interface I {}
    static class I1 implements I {}
    static class I2 implements I {}

    private static int test(D o) {
        if (o instanceof D(String data, Integer out)) {                        // matching arity exists, no applicable overload found
            return out;
        }
        return -1;
    }

    private static void test2(D o) {
        if (o instanceof D(I data)) {                                          // matching arity exists, multiple applicable exist, ambiguous
        }
    }

    private static void test3(D o) {
        if (o instanceof D(I data, Integer i1, Integer i2)) {                  // no matching arity matcher exists
        }
    }

    public static class D {
        public pattern D(I1 v) {
            match D(new I1());
        }

        public pattern D(I2 v) {
            match D(new I2());
        }

        public pattern D(Object v1, Float out) {
            match D(10.0f, 10.0f);
        }

        public pattern D(Float out, Integer v1) {
            match D(10.0f, 2);
        }
    }
}
