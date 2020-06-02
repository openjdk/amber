/*
 * @test /nodynamiccopyright/
 * @summary smoke test for the concise method finder
 * @compile/fail/ref=ConciseMethodsFinderTest.out -Werror -XDrawDiagnostics -XDfind=concise_method ConciseMethodsFinderTest.java
 */

class ConciseMethodsFinderTest {

    class Inner {
        String si;
    }

    Inner inner;

    Inner getInner() { return inner; }

    ConciseMethodsFinderTest(String s, String p) {}

    // this one is a candidate
    int length(String s) {
        return s.length();
    }

    // this one is a candidate
    int length2() {
        return inner.si.length();
    }

    // this one is a candidate
    int length3() {
        return getInner().si.length();
    }

    // this one is a candidate
    static ConciseMethodsFinderTest makeTest1(String s, String k) {
        return new ConciseMethodsFinderTest(s, k);
    }

    // not a candidate
    static ConciseMethodsFinderTest makeTest2(String s) {
        return new ConciseMethodsFinderTest(s, "");
    }

    // not a candidate
    int length2(String s, boolean b) { return s.length(); }

    // this one is a candidate
    String test() {
        return new Object(){}.toString();
    }
}
