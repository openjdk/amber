/*
 * @test
 * @summary Basic tests for constant patterns
 * @compile/fail/ref=ConstantPatternTest2.out -XDrawDiagnostics ConstantPatternTest2.java
 */

public class ConstantPatternTest2 {

    public static double valueOfD(){
        return 41.0;
    }

    public static void main(String[] args) {

        int i = 41;
        i++;
        final int constantInt = 42;


        // Simple literal constant pattern
        if (i instanceof 42) {
            throw new AssertionError("Broken");
        }

        // Simple constant expression pattern
        if (i instanceof 41+1) {
            throw new AssertionError("Broken");
        }

        // Constant expression pattern using a final local
        if (i instanceof constantInt) {
            throw new AssertionError("Broken");
        }

//--- DOUBLE TYPED CONSTANT EXPRESSIONS

        double d = valueOfD();
        d++;
        final double constant = 42.0;

        // Simple literal constant pattern
        if (d instanceof 42.0) {
            throw new AssertionError("Broken");
        }

        // Simple constant expression pattern
        if (d instanceof 41.0+1.0) {
            throw new AssertionError("Broken");
        }

        // Constant expression pattern using a final local
        if (d instanceof constant) {
            throw new AssertionError("Broken");
        }

//--- STRING TYPED CONSTANT EXPRESSIONS ----

        String s = "Hello";
        final String hello = "Hello";

        // Simple literal constant pattern
        if (s instanceof "Hello") {
            throw new AssertionError("Broken");
        }

        // Simple constant expression pattern
        if (s instanceof "Hell"+"o") {
            throw new AssertionError("Broken");
        }

        // Constant expression pattern using a final local
        if (s instanceof hello) {
            throw new AssertionError("Broken");
        }

// ----- Constant expression patterns and reference typed expressions

        Object obj = "Hello";

        if (obj instanceof "Hello") {
            throw new AssertionError("Broken");
        }
        if (obj instanceof null) {
            throw new AssertionError("Broken");
        }
        obj = 42;
        if (obj instanceof 42) {
            throw new AssertionError("Broken");
        }

        if (obj instanceof var x) {
            throw new AssertionError("Broken");
        }

        if (obj instanceof 42) {
        }
        if (obj instanceof null) {
            throw new AssertionError("Broken");
        }
        if (null instanceof "Hello") {
            throw new AssertionError("Broken");
        }

    }

}
