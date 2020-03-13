/**
 * @test /nodynamiccopyright/
 * @summary Verify error reports for erroneous deconstruction patterns are sensible
 * @compile/fail/ref=DeconstructionPatternErrors.out --enable-preview -source ${jdk.version} -XDrawDiagnostics -XDshould-stop.at=FLOW DeconstructionPatternErrors.java
 */

import java.util.ArrayList;
import java.util.List;

public class DeconstructionPatternErrors {

    public static void main(String... args) throws Throwable {
        Object p;
        p = new P(42);
        if (p instanceof P(_));
        if (p instanceof P3(ArrayList<Integer> l));
        if (p instanceof P4(ArrayList<Integer> l));
        if (p instanceof P5(int i));
        if (p instanceof P(String s));
        if (p instanceof P5(P(var v)));
    }

    public record P(int i) {
    }

    public record P2(Runnable r1, Runnable r2) {}
    public record P3(List<String> l) {}
    public record P4(Object o) {}
    public record P5(String s) {}

}
