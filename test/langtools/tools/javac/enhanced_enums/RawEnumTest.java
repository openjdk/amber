/*
 * @test /nodynamioccopyright/
 * @summary Smoke test for use of raw generic enums
 * @compile/fail/ref=RawEnumTest.out -Xlint:all -XDrawDiagnostics RawEnumTest.java
 */

import java.util.*;
import java.util.stream.Stream;

class RawEnumTest {
    interface A<X> {
        void m(X x);
    }

    enum Foo<X> implements A<String> {
        A<String>, B<Integer>;

        String m() { return ""; }
        public void m(String s) { }
        public List<X> list(List<X> l) { return null; }
        public List<String> list2(List<String> l) { return null; }
    }

    void test() {
        EnumSet<Foo> fooSet = EnumSet.noneOf(Foo.class); //ok
        Foo f = null; //no raw type warning
        f.m(""); //ok
        f.m(1); //error
        f.list(null); //unchecked
        f.list2(null); //ok
        Foo<?> foow = f; //ok
        Foo<String> fs = f; //unchecked
        fooSet = EnumSet.of(Foo.A, Foo.B); //ok
        Stream<Foo> fooStream = Arrays.stream(Foo.values());
    }
}
