/*
 * @test /nodynamiccopyright/
 * @summary Ensure that unreachable statements in match clauses are detected corrected.
 * @compile/fail/ref=UnreachableStatementInMatchClause.out -XDrawDiagnostics UnreachableStatementInMatchClause.java
 */
public class UnreachableStatementInMatchClause {
    public static void main(String[] args) {
        Object o = "Hello world";
        switch (o) {
            case String s:
                System.out.println(s);
                break;
                throw new AssertionError("oops");
            default:
                System.out.println("hmm");
                break;
                throw new AssertionError("oops once more");
        }
    }
}
