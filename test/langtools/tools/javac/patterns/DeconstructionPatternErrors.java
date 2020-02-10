/**
 * @test /nodynamiccopyright/
 * @summary Verify error reports for erroneous deconstruction patterns are sensible
 * @compile/fail/ref=DeconstructionPatternErrors.out --enable-preview -source ${jdk.version} -XDrawDiagnostics DeconstructionPatternErrors.java
 */

public class DeconstructionPatternErrors {

    public static void main(String... args) throws Throwable {
        Object p;
        p = new P(42);
        if (p instanceof P(String s));
        p = new P2(() -> {}, () -> {});
        if (p instanceof P2(String s, Runnable r)) {
            System.err.println("s=" + s);
            System.err.println("r=" + r);
        }
    }

    public record P(int i) {
    }

    public record P2(Runnable r1, Runnable r2) {}
}
