/*
 * @test /nodynamiccopyright/
 * @summary smoke test for sealed classes
 * @compile/fail/ref=NotSealedTest.out --enable-preview -source ${jdk.version} -XDrawDiagnostics NotSealedTest.java
 */

import java.lang.annotation.*;

class NotSealedTest {
    sealed class Super {}
    sealed non-sealed class Sub extends Super {}

    final sealed class Super2 {}

    final non-sealed class Super3 {}

    non-sealed class NoSealedSuper {}

    sealed public void m() {}
}
