/**
 * @test nodynamiccopyright/
 * @summary Verify errors related to array patterns.
 * @compile/fail/ref=SimpleArrayPatternErrors.out -XDrawDiagnostics -XDshould-stop.at=FLOW --enable-preview -source ${jdk.version} SimpleArrayPatternErrors.java
 */
public class SimpleArrayPatternErrors {
    private boolean test(Object o) {
        return o instanceof {var s1} &&
               o instanceof String {var s2} &&
               o instanceof String[] {var s3, ..., var s4} &&
               o instanceof R({var s5}) &&
               o instanceof R(String {var s6}) &&
               o instanceof R(String[] {var s7, ..., var s8});
    }
    record R(String[] content) {}
}
