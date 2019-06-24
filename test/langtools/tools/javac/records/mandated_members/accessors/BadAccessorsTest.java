/*
 * @test /nodynamiccopyright/
 * @summary check that the compiler doesn't accept incorrect accessors
 * @compile/fail/ref=BadAccessorsTest.out -XDrawDiagnostics BadAccessorsTest.java
 */

import java.util.List;

public class BadAccessorsTest {
    record R1(int i, int j, int k, List<String> ls) {
        // accessors has to be public
        int i() { return i; }
        private int j() { return j; }
        protected int k() { return k; }
        // must match type exactly
        public List ls() { return ls; }
    }
}
