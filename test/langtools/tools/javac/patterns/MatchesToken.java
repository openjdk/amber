/**
 * @test
 * @compile MatchesToken.java
 */
public class MatchesToken {
    public void test(Object o) {
        final int __matches = 1;
        boolean b1 = __matches __matches __matches;
        boolean b2 = o __matches __matches;
        boolean b3 = __matches __matches Integer i;
    }
}

