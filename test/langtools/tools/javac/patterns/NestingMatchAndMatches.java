/*
 * @test
 * @summary testing various permutations of matches insides match etc.
 * @compile NestingMatchAndMatches.java
 * @run main NestingMatchAndMatches
 */
public class NestingMatchAndMatches {

    public static void main(String[] args) {

        Object o = "Hello";

        //Nested matches
        if ((o matches String s) matches boolean b) {
            System.out.println("String!");
        } else {
            throw new AssertionError("broken");
        }
        if ((o matches String s) matches false) {
            throw new AssertionError("broken");
        } else {
            System.out.println("String ");
        }

        if ((o matches String s) matches true) {
            System.out.println("String!");
        } else {
            throw new AssertionError("broken");
        }

        boolean b = (o matches String s) ? (s matches "Hello") : false;

        if (b) {
            System.out.println("yes!");
        } else {
            throw new AssertionError("broken");
        }

        //matches inside a match
        switch (o) {
            case String s:
                if (s matches String t) {
                    System.out.println(s+"-"+t);
                }
        }
        switch (o) {
            case String s:
                if (o matches Integer t) {
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
                        if (t matches String u) {
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
