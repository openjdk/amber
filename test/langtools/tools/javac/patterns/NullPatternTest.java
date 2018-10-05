/*
 * @test
 * @summary Basic positive test for match statement
 * @compile NullPatternTest.java
 * @run main NullPatternTest
 */

public class NullPatternTest {
    public static void main(String[] args) {
        Object o = "Hello world";
        switch (o) {
            case null: throw new AssertionError("broken");
            case String s : System.out.println("String"); break;
            default: throw new AssertionError("broken");
        }

        o = null;

        switch (o) {
            case null: System.out.println("null"); break;
            default: throw new AssertionError("broken");
        }
    }
}
