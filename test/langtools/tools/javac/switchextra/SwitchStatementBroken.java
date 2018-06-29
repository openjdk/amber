/**
 * @test /nodynamiccopyright/
 * @compile/fail/ref=SwitchStatementBroken.out -XDrawDiagnostics SwitchStatementBroken.java
 */

public class SwitchStatementBroken {

    private void statementBroken(int i) {
        String res;

        switch (i) {
            case 0 -> { res = "NULL-A"; }
            case 1: { res = "NULL-A"; break; }
        }
    }

}
