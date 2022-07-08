/**
 * @test /nodynamiccopyright/
 * @summary XXX
 * @compile --enable-preview -source ${jdk.version} DeconstructionDesugaring.java
 * @run main/othervm --enable-preview  DeconstructionDesugaring
 */

import java.util.function.ToIntFunction;

public class DeconstructionDesugaring {

    public static void main(String... args) throws Throwable {
        new DeconstructionDesugaring().test();
    }

    private void test() {
        test(this::runCheckStatement);
        test(this::runCheckExpression);
    }

    private void test(ToIntFunction<Object> task) {
        assertEquals(1, task.applyAsInt(new R1(new R2(""))));
        assertEquals(2, task.applyAsInt(new R1(new R2(1))));
        assertEquals(3, task.applyAsInt(new R1(new R2(1.0))));
        assertEquals(4, task.applyAsInt(new R1(new R2(new StringBuilder()))));
        assertEquals(5, task.applyAsInt(new R1(new R3(""))));
        assertEquals(6, task.applyAsInt(new R1(new R3(1))));
        assertEquals(7, task.applyAsInt(new R1(new R3(1.0))));
        assertEquals(8, task.applyAsInt(new R1(new R3(new StringBuilder()))));
        assertEquals(-1, task.applyAsInt(new R1(1.0f)));
        assertEquals(-1, task.applyAsInt("foo"));
    }

    private int runCheckStatement(Object o) {
        switch (o) {
            case R1(R2(String s)) -> { return 1; }
            case R1(R2(Integer i)) -> { return 2; }
            case R1(R2(Double d)) -> { return 3; }
            case R1(R2(CharSequence cs)) -> { return 4; }
            case R1(R3(String s)) -> { return 5; }
            case R1(R3(Integer i)) -> { return 6; }
            case R1(R3(Double f)) -> { return 7; }
            case R1(R3(CharSequence cs)) -> { return 8; }
            default -> { return -1; }
        }
    }

    private int runCheckExpression(Object o) {
        return switch (o) {
            case R1(R2(String s)) -> 1;
            case R1(R2(Integer i)) -> 2;
            case R1(R2(Double d)) -> 3;
            case R1(R2(CharSequence cs)) -> 4;
            case R1(R3(String s)) -> 5;
            case R1(R3(Integer i)) -> 6;
            case R1(R3(Double f)) -> 7;
            case R1(R3(CharSequence cs)) -> 8;
            default -> -1;
        };
    }

    private void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("expected: " + expected + ", " +
                                     "actual: " + actual);
        }
    }

    record R1(Object o) {}
    record R2(Object o) {}
    record R3(Object o) {}

}
