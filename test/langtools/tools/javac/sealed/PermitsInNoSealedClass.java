/*
 * @test /nodynamiccopyright/
 * @summary smoke test for sealed classes
 * @compile/fail/ref=PermitsInNoSealedClass.out -XDrawDiagnostics PermitsInNoSealedClass.java
 */

import java.lang.annotation.*;

class PermitsInNoSealedClass {
    class NotSealed permits Sub3 {}

    class Sub3 extends NotSealed {}
}
