/*
 * @test /nodynamiccopyright/
 * @summary check that the compiler doesn't accept canonical constructors with name mismatch
 * @compile/fail/ref=MismatchTest.out -XDrawDiagnostics MismatchTest.java
 */

import java.util.*;

public class MismatchTest {
    record R1(int i, int j) {
        public R1(int j, int i) {} // doesn't match by name
    }

    record R2(int i, List<String> ls) {
        public R2(int i, List ls) {}
    }
}
