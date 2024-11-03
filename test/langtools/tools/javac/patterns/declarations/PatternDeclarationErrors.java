import java.io.IOException;

/*
 * @test /nodynamiccopyright/
 * @summary Verify error related to annotations and patterns
 * @enablePreview
 * @compile/fail/ref=PatternDeclarationErrors.out -XDrawDiagnostics -XDdev --should-stop=at=FLOW PatternDeclarationErrors.java
 */
public class PatternDeclarationErrors {
    public class D {
        public pattern D(Object v1, Float out) {
            match D(10.0f); // not matching signature
        }

        public pattern D(Float out, Integer v1) {
            match D2(10.0f, 2); // no matching name of pattern declaration
        }

        public pattern D(Float out, Double v1) {
            match D(10.0f, "2"); // inconvertible types
        }

        public void D3(Float out, Integer v1) {
            match D3(10.0f, 2); // match in regular method
        }

        public pattern D(Float out, Character v1) {
            match D(10.0f, '2');
            System.out.println("unreachable"); // unreachable
        }
    }

    public class ExitErrors {
        public pattern ExitErrors(int out) {
           break; // no break
        }

        public pattern ExitErrors(float out) {
            return Integer.valueOf(42); // no return
        }

        public pattern ExitErrors(char out) {
            yield Integer.valueOf(42); // no yield
        }
    }

    public class ExceptionErrors {
        public pattern ExceptionErrors(int out) {
            throw new Error(); // no throws in patter declaration body
        }

        public pattern ExceptionErrors(float out) throws IOException { // no throws in pattern signature
            throw new IOException(); // no throws in patter declaration body
        }
    }

    public record R(int i) {
        public pattern R(int i) { match R(42); }
    }

    public class R1 {
        public pattern R1(int i) { match R1(42); }
        public pattern R1(int i) { match R1(42); }
    }

    public class R2 {
        pattern R2(int i) { match R2(42); }
        pattern R2(int i) { match R2(42); }
    }


    public class Point {
        final int x;
        final int y;

        protected Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        protected pattern Point(int x, int y) {
            match Point(this.x, this.y);
        }
    }

    public class GreatPoint extends Point {
        final int magnitude;

        public GreatPoint(int x, int y, int magnitude) {
            super(x, y);
            if (magnitude < 0) throw new IllegalArgumentException();
            this.magnitude = magnitude;
        }

        public pattern GreatPoint(int x, int y, int magnitude) {
            if (this instanceof Point(var X, var Y)) {
                x = X;
                y = Y;
            }
            match GreatPoint(x, y, this.magnitude);
        }
    }
}
