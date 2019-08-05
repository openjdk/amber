/*
 * @test /nodynamiccopyright/
 * @summary bad declaration annotations on records
 * @compile/fail/ref=BadAnnotations.out -XDrawDiagnostics BadAnnotations.java
 */

import java.lang.annotation.*;

class BadAnnotations {
    // negative cases
    @Target({ ElementType.CONSTRUCTOR })
    @interface Foo1 {}
    record R1(@Foo1 int i) {}

    @Target({ ElementType.TYPE })
    @interface Foo2 {}
    record R2(@Foo2 int i) {}

    @Target({ ElementType.LOCAL_VARIABLE })
    @interface Foo3 {}
    record R3(@Foo3 int i) {}

    @Target({ ElementType.PACKAGE })
    @interface Foo4 {}
    record R4(@Foo4 int i) {}

    @Target({ ElementType.MODULE })
    @interface Foo5 {}
    record R5(@Foo5 int i) {}

    // positive cases
    @Target({ ElementType.FIELD })
    @interface Foo6 {}
    record R6(@Foo6 int i) {}

    @Target({ ElementType.RECORD_COMPONENT })
    @interface Foo7 {}
    record R7(@Foo7 int i) {}

    @Target({ ElementType.METHOD })
    @interface Foo8 {}
    record R8(@Foo8 int i) {}

    @Target({ ElementType.PARAMETER })
    @interface Foo9 {}
    record R9(@Foo9 int i) {}

    // no target applies to all
    @interface Foo10 {}
    record R10(@Foo10 int i) {}

    // type annotations are allowed too
    @Target({ ElementType.TYPE_USE })
    @interface Foo11 {}
    record R11(@Foo11 int i) {}

    @Target({ ElementType.TYPE_PARAMETER })
    @interface Foo12 {}
    record R12(@Foo12 int i) {}
}
