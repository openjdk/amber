/*
 * @test
 * @summary testing various permutations of matches insides match etc.
 * @compile NestingMatchAndMatches.java
 * @run main NestingMatchAndMatches
 */
public class NestingMatchAndMatches {

    public static void main(String[] args) {

        Object o = "Hello";

        //matches inside a match
        switch (o) {
            case String s:
                if (s instanceof String t) {
                    System.out.println(s+"-"+t);
                }
        }
        switch (o) {
            case String s:
                if (o instanceof Integer t) {
                    System.out.println(s+"-"+t);
                    throw new AssertionError("broken");
                } else {
                    System.out.println(s);
                }
        }

        //match inside a match
        Object o2 = o;

        switch (o) {
            case String s:
                switch (o2) {
                    case String t:
                        System.out.println(s+"-"+t);
                        break;
                }
                break;
        }
        switch (o) {
            case String s:
                switch (o2) {
                    case String t:
                        System.out.println(s+"-"+t);
                        break;
                    default:
                        throw new AssertionError("broken");
                }
                break;
        }

        switch (o) {
            case String s:
                switch (o2) {
                    case Integer in:
                        throw new AssertionError("broken");
                    case String t:
                        System.out.println(s+"-"+t);
                        break;
                }
                break;
        }
        switch (o) {
            case String s:
                switch (o2) {
                    case Integer in:
                        throw new AssertionError("broken");
                    case String t:
                        System.out.println(s+"-"+t);
                        break;
                    default:
                        throw new AssertionError("broken");
                }
                break;
        }
        //matches inside a match inside a match

        switch (o) {
            case String s:
                switch (o2) {
                    case String t:
                        if (t instanceof String u) {
                            System.out.println(s+"-"+t+"--"+u);
                        } else {
                            throw new AssertionError("broken");
                        }
                }
                break;
        }
        System.out.println("> Test completed");
    }
}
