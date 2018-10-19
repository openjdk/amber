/**
 * @test
 * @compile MatchesToken.java
 */
public class MatchesToken {
    public void test(Object o) {
        final int matches = 1;
        boolean b1 = matches matches matches;
        boolean b2 = o matches matches;
        boolean b3 = matches matches Integer i;
        boolean b4 = (matches matches matches) matches true;
    }

    private void test() {
        I1 i1 = (int matches) -> {};
        I2 i2 = (int matches, int other) -> {};
    }

    interface I1 {
        public void m(int i);
    }

    interface I2 {
        public void m(int i1, int i2);
    }
}

