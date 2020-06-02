/*
 * @test
 * @summary testing break inside match clauses
 * @compile BreakInsideMatch.java
 * @run main BreakInsideMatch
 */

public class BreakInsideMatch {
    static int i = 42;

    public static void test(Object o) {
        switch (o) {
            case String s:
                i++;
                break;
            case Integer in:
                i = 0;
                break;
            default:
                i = 100;
                break;
        }
        System.out.println(i);
    }

    public static void main(String[] args) {

        System.out.println("< Tests started");

        test("Hello");
        test(42);
        test(42.0);


        System.out.println("> Tests completed");

    }
}