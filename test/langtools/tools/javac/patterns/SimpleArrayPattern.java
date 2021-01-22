/**
 * @test
 * @compile --enable-preview -source ${jdk.version} SimpleArrayPattern.java
 * @run main/othervm --enable-preview SimpleArrayPattern
 */

// * @compile/fail/ref=SimpleDeconstructionPatternNoPreview.out -XDrawDiagnostics SimpleDeconstructionPattern.java
import java.util.List;
import java.util.Objects;

public class SimpleArrayPattern {

    public static void main(String... args) throws Throwable {
        if (!Objects.equals(List.of("a", "b"), simple(new String[] {"a", "b"}))) {
            throw new IllegalStateException();
        }
        if (!Objects.equals(null, simple(new String[] {"a", "b", "c"}))) {
            throw new IllegalStateException();
        }
        if (!Objects.equals(null, simple(new String[] {"a"}))) {
            throw new IllegalStateException();
        }
        if (!Objects.equals(List.of("a", "b"), nested1(new R(new String[] {"a", "b"})))) {
            throw new IllegalStateException();
        }
        if (!Objects.equals(null, nested1(new R(new String[] {"a"})))) {
            throw new IllegalStateException();
        }
        if (!Objects.equals(List.of("a", "b"), nested2(new RInfer(new String[] {"a", "b"})))) {
            throw new IllegalStateException();
        }
        if (!Objects.equals(null, nested2(new RInfer(new String[] {"a"})))) {
            throw new IllegalStateException();
        }
        if (!Objects.equals(List.of("a", "b"), orMore1(new String[] {"a", "b"}))) {
            throw new IllegalStateException();
        }
        if (!Objects.equals(List.of("a", "b"), orMore1(new String[] {"a", "b", "c"}))) {
            throw new IllegalStateException();
        }
        if (!Objects.equals(null, orMore1(new String[] {"a"}))) {
            throw new IllegalStateException();
        }
        if (!Objects.equals(List.of("a", "b"), orMore2(new R(new String[] {"a", "b"})))) {
            throw new IllegalStateException();
        }
        if (!Objects.equals(List.of("a", "b"), orMore2(new R(new String[] {"a", "b", "c"})))) {
            throw new IllegalStateException();
        }
        if (!Objects.equals(null, orMore2(new R(new String[] {"a"})))) {
            throw new IllegalStateException();
        }
        if (!Objects.equals(List.of("a", "b"), orMore3(new RInfer(new String[] {"a", "b"})))) {
            throw new IllegalStateException();
        }
        if (!Objects.equals(List.of("a", "b"), orMore3(new RInfer(new String[] {"a", "b", "c"})))) {
            throw new IllegalStateException();
        }
        if (!Objects.equals(null, orMore3(new RInfer(new String[] {"a"})))) {
            throw new IllegalStateException();
        }
    }

    private static List<String> simple(Object o) throws Throwable {
        if (o instanceof String[] {String s1, String s2}) {
            return List.of(s1, s2);
        }
        return null;
    }

    private static List<String> nested1(Object o) throws Throwable {
        if (o instanceof R(String[] {String s1, String s2})) {
            return List.of(s1, s2);
        }
        return null;
    }

    private static List<String> nested2(Object o) throws Throwable {
        if (o instanceof RInfer({String s1, String s2})) {
            return List.of(s1, s2);
        }
        return null;
    }

    private static List<String> orMore1(Object o) throws Throwable {
        if (o instanceof String[] {String s1, String s2, ...}) {
            return List.of(s1, s2);
        }
        return null;
    }

    private static List<String> orMore2(Object o) throws Throwable {
        if (o instanceof R(String[] {String s1, String s2, ...})) {
            return List.of(s1, s2);
        }
        return null;
    }

    private static List<String> orMore3(Object o) throws Throwable {
        if (o instanceof RInfer({String s1, String s2, ...})) {
            return List.of(s1, s2);
        }
        return null;
    }

    record R(Object o) {}

    record RInfer(Object[] o) {}
}
