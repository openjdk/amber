/*
 * @test /nodynamiccopyright/
 * @summary Ensure that in a matching switch statement, the default clause must be the last.
 * @compile/fail/ref=DefaultClauseMustBeLast.out -XDrawDiagnostics DefaultClauseMustBeLast.java
 */
public class DefaultClauseMustBeLast {
    public static void main(String[] args) {
        Object o = (Long)42L;
        switch (o) {
            default: System.out.println("default"); break;
            case Long l: System.out.println("It's a long"); break;
        }
        switch (o) {
            case Long l: System.out.println("It's a long"); break;
            default : System.out.println("default"); break;
            case Double d: System.out.println("It's a double"); break;
        }
    }
}
