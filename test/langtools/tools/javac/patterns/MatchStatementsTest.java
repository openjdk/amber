/*
 * @test /nodynamiccopyright/
 * @summary
 * @enablePreview
 * @compile MatchStatementsTest.java
 * @run main MatchStatementsTest
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class MatchStatementsTest {
    public static void main(String[] args) {
        basicTest();
        assertMatchExceptionWithNested(MatchStatementsTest::raiseExceptionTest, TestPatternFailed.class);
    }

    static void basicTest() {
        Point p = new Point(1, 2);
        match Point(Integer a, Integer b) = p;
        assertEquals(3, a + b);

        IPoint ip   = new Point(3, 4);
        match Point(var c, var d) = ip;
        assertEquals(7, c + d);

        p = new Point(1, null);
        match Point(var e, var f) = p;
        assertEquals(null, f);

        PointP wp = new PointP(1, 2);
        match PointP(int ap, double bp) = wp;
        assertEquals(2.0d, bp);
    }

    static Integer raiseExceptionTest() {
        PointEx pointEx = new PointEx(1, 2);
        match PointEx(Integer a_ex, Integer b_noex) = pointEx;
        return a_ex;
    }

    sealed interface IPoint permits Point {}
    record Point(Integer x, Integer y) implements IPoint { }
    record PointP(int x, double y) { }
    record PointEx(Integer x, Integer y) {
        @Override
        public Integer x() {
            throw new TestPatternFailed(EXCEPTION_MESSAGE);
        }
    }
    static final String EXCEPTION_MESSAGE = "exception-message";
    public static class TestPatternFailed extends AssertionError {
        public TestPatternFailed(String message) {
            super(message);
        }
    }

    // error handling
    static void fail(String message) {
        throw new AssertionError(message);
    }

    static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected: " + expected + "," +
                    "got: " + actual);
        }
    }

    static <T> void assertMatchExceptionWithNested(Supplier<Integer> f, Class<?> nestedExceptionClass) {
        try {
            f.get();
            fail("Expected an exception, but none happened!");
        }
        catch(Exception ex) {
            assertEquals(MatchException.class, ex.getClass());

            MatchException me = (MatchException) ex;

            assertEquals(nestedExceptionClass, me.getCause().getClass());
        }
    }

    static <T> void assertEx(Supplier<Integer> f, Class<?> exceptionClass) {
        try {
            f.get();
            fail("Expected an exception, but none happened!");
        }
        catch(Exception ex) {
            assertEquals(exceptionClass, ex.getClass());
        }
    }
}
