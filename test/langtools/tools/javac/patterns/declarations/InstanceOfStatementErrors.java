/*
 * @test /nodynamiccopyright/
 * @summary
 * @enablePreview
 * @compile/fail/ref=InstanceOfStatementErrors.out -XDrawDiagnostics -XDshould-stop.at=FLOW InstanceOfStatementErrors.java
 */

import java.util.List;

public class InstanceOfStatementErrors {
    static void exhaustivity_error1(Object point) {
        point instanceof Point(var x, var y);
    }

    sealed interface IPoint permits Point {}
    record Point(Integer x, Integer y) implements IPoint { }
    record OPoint(Object x, Object y) { }
}