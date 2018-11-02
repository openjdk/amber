/*
 * @test /nodynamiccopyright/
 * @summary Ensure that in type test patterns, the predicate is not trivially provable false.
 * @compile/fail/ref=PatternVariablesAreFinal.out -XDrawDiagnostics PatternVariablesAreFinal.java
 */
public class PatternVariablesAreFinal {
    public static void main(String[] args) {
        Object o = 32;
        if (o __matches String s) {
            s = "hello again";
            System.out.println(s);
        }
        switch (o) {
            case Integer i: i++; break;
        }
        int i = 42;
        i++;
        switch (o) {
            case String s: break;
            case Integer s: s = null; break;
        }
        System.out.println("test complete");
    }
}
