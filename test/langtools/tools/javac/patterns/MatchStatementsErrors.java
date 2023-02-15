/*
 * @test /nodynamiccopyright/
 * @summary
 * @enablePreview
 * @compile/fail/ref=MatchStatementsErrors.out -XDrawDiagnostics -XDshould-stop.at=FLOW MatchStatementsErrors.java
 */

import java.util.List;

public class MatchStatementsErrors {

    static void exhaustivity_error1(Object point) {
        match Point(var x, var y) = point;
    }

    static void exhaustivity_error2(OPoint opoint) {
        match Point(var x, var y) = opoint;
    }

    static void parsing_error(Object point) {
        match Point p = point;
    }

    sealed interface IPoint permits Point {}
    record Point(Integer x, Integer y) implements IPoint { }
    record OPoint(Object x, Object y) { }
}
