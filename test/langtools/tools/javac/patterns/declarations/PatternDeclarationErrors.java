/*
 * @test /nodynamiccopyright/
 * @summary Verify error related to annotations and patterns
 * @enablePreview
 * @compile/fail/ref=PatternDeclarationErrors.out -XDrawDiagnostics -XDdev --should-stop=at=FLOW PatternDeclarationErrors.java
 */
public class PatternDeclarationErrors {
    public record D() {
        public pattern D(Object v1, Float out) {
            match D(10.0f);
        }

        public pattern D(Float out, Integer v1) {
            match D2(10.0f, 2);
        }

        public pattern D(Float out, Double v1) {
            match D(10.0f, "2");
        }

        public void D3(Float out, Integer v1) {
            match D3(10.0f, 2);
        }

        public pattern D(Float out, Character v1) {
            match D(10.0f, '2');
            System.out.println("unreachable");
        }
    }
}
