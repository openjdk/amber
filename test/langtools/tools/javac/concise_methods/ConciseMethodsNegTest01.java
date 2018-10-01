/*
 * @test /nodynamiccopyright/
 * @summary smoke test for the concise method feature
 * @compile/fail/ref=ConciseMethodsNegTest01.out -XDrawDiagnostics ConciseMethodsNegTest01.java
 */

class ConciseMethodsNegTest01 {
    int length(String s) = s::length;

    abstract int length2(String s) = String::length;

    native int length3(String s) = String::length;

    static ConciseMethodsNegTest01 make() {
        return new ConciseMethodsNegTest01();
    }

    ConciseMethodsNegTest01() = ConciseMethodsNegTest01::make;

    private static void all() {}

    public static void foo() = ConciseMethodsNegTest01.all();
}
