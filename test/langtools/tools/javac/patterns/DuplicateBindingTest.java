/*
 * @test /nodynamiccopyright/
 * @summary Basic pattern bindings scope test
 * @compile/fail/ref=DuplicateBindingTest.out -XDrawDiagnostics DuplicateBindingTest.java
 */

public class DuplicateBindingTest {

    int f;

    public static void main(String[] args) {

        if (args != null) {
            int s;
            if (args[0] __matches String s) { // NOT OK. Redef same scope.
            }
            int k;
            switch(args[0]) {
                case String k: // NOT OK: Redef outer scope.
                    break;
                case var args: // NOT OK: Redef outer scope.
                    break;
                case Integer f: // OK to redef field, but BAD type.
                    break;
                default:
                    break;
            }
        }
    }
}