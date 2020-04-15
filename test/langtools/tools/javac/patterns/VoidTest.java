/*
 * @test /nodynamiccopyright/
 * @summary Ensure that void expressions passed to switch statement are rejected
 * @compile/fail/ref=VoidTest.out -XDrawDiagnostics VoidTest.java
 */
public class VoidTest {
    public static void test() {
        switch (test()) {
        }
    }
}
