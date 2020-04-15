/*
 * @test /nodynamiccopyright/
 * @summary Checking for duplicate constant patterns test
 * @compile/fail/ref=DuplicateConstantPatternTests.out -XDrawDiagnostics DuplicateConstantPatternTests.java
 */

public class DuplicateConstantPatternTests {

    public static void main(String[] args) {
        int i = 47;
        switch (i) {
            case 45: throw new AssertionError("broken");
            case 45: throw new AssertionError("broken");
        }

        switch (i) {
            case 45: throw new AssertionError("broken");
            case 44+1: throw new AssertionError("broken");
        }

        switch (i) {
            case 45: throw new AssertionError("broken");
            case 46-1: throw new AssertionError("broken");
            default: System.out.println("Default");
        }

        Object o = "hello";
        switch (o) {
            case null: throw new AssertionError("broken");
            case null: throw new AssertionError("broken");
        }
    }

}
