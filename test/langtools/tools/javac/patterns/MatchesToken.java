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
        boolean b4 = (__matches __matches __matches) __matches true;
    }

    private void test() {
        I1 i1 = (int __matches) -> {};
        I2 i2 = (int __matches, int other) -> {};
    }

    interface I1 {
        public void m(int i);
    }

    interface I2 {
        public void m(int i1, int i2);
    }
}

