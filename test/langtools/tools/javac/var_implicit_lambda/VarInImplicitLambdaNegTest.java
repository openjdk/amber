/*
 * @test /nodynamiccopyright/
 * @bug 8194892
 * @summary add compiler support for local-variable syntax for lambda parameters
 * @compile/fail/ref=VarInImplicitLambdaNegTest.out -XDrawDiagnostics VarInImplicitLambdaNegTest.java
 */

import java.util.function.*;

class VarInImplicitLambdaNegTest {
    IntBinaryOperator f1 = (x, var y) -> x + y;
    IntBinaryOperator f2 = (var x, y) -> x + y;
    IntBinaryOperator f3 = (int x, var y) -> x + y;
}
