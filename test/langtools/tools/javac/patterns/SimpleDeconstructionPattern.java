/**
 * @test
 * @compile -doe SimpleDeconstructionPattern.java
 * @run main SimpleDeconstructionPattern
 */

public class SimpleDeconstructionPattern {

    public static void main(String... args) throws Throwable {
        if (!test1(new P(42))) {
            throw new IllegalStateException();
        }
        if (test1(new P(41))) {
            throw new IllegalStateException();
        }
        if (!test2(new P(42))) {
            throw new IllegalStateException();
        }
        if (test2(new P(41))) {
            throw new IllegalStateException();
        }
        if (!test3(new P2(new P(42), ""))) {
            throw new IllegalStateException();
        }
        if (test3(new P2(new P(41), ""))) {
            throw new IllegalStateException();
        }
        if (test3(new P2(new P(42), "a"))) {
            throw new IllegalStateException();
        }
        if (!test4(new P2(new P(42), ""))) {
            throw new IllegalStateException();
        }
        if (test4(new P2(new P(41), ""))) {
            throw new IllegalStateException();
        }
        if (test4(new P2(new P(42), "a"))) {
            throw new IllegalStateException();
        }
        if (!test5(new P(42))) {
            throw new IllegalStateException();
        }
        if (test5(new P(41))) {
            throw new IllegalStateException();
        }
        if (!test6(new P(42))) {
            throw new IllegalStateException();
        }
        if (!test6(new P(41))) {
            throw new IllegalStateException();
        }
    }

    private static boolean test1(Object o) throws Throwable {
        return o instanceof P(42);
    }

    private static boolean test2(Object o) throws Throwable {
        return o instanceof P(int i) && i == 42;
    }

    private static boolean test3(Object o) throws Throwable {
        return o instanceof P2(P(42), "");
    }

    private static boolean test4(Object o) throws Throwable {
        return o instanceof P2(P(int i), String s) && i == 42 && "".equals(s);
    }

    private static boolean test5(Object o) throws Throwable {
        return o instanceof P(var i) && i == 42;
    }

    private static boolean test6(Object o) throws Throwable {
        return o instanceof P(_);
    }

    public record P(int i) {
    }

    public record P2(P p, String s) {
    }    
}