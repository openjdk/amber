/*
 * @test /nodynamiccopyright/
 * @summary smoke test for sealed classes
 * @compile/fail/ref=NotSealedTest.out -XDrawDiagnostics NotSealedTest.java
 * @ignore
 */

import java.lang.annotation.*;

class NotSealedTest {
    @Sealed @NotSealed class AB {}
    class NS {}
    interface NSI {}
    @NotSealed class SNS extends NS {}
    @NotSealed class SNSI implements NSI {}
}
