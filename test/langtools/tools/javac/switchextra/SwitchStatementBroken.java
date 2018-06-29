/**
 * @test /nodynamiccopyright/
 * @compile/fail/ref=SwitchStatementBroken.out -XDrawDiagnostics --enable-preview -source 12 SwitchStatementBroken.java
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
