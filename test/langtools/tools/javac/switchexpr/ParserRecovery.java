/**
 * @test /nodynamiccopyright/
 * @compile/fail/ref=ParserRecovery.out -XDrawDiagnostics ParserRecovery.java
 */

public class ParserRecovery {
    void t1(int e) {
         int i = switch (e) { case any; };
    }
    void t2(int e) {
         switch (e) { case any; }
    }
}
