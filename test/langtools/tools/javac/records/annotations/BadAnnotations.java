/*
 * @test /nodynamiccopyright/
 * @summary bad declaration annotations on records
 * @compile/fail/ref=BadAnnotations.out -XDrawDiagnostics BadAnnotations.java
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

class BadAnnotations {
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

    @Target({ ElementType.METHOD })
    @interface Foo7 {}
    record R7(@Foo7 int i) {}

    @Target({ ElementType.PARAMETER })
    @interface Foo8 {}
    record R8(@Foo8 int i) {}

    // no target applies to all
    @interface Foo9 {}
    record R9(@Foo9 int i) {}
}
