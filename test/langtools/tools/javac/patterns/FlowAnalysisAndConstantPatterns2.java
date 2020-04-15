/*
 * @test /nodynamiccopyright/
 * @summary Stress test flow analysis with constant patterns
 * @compile/fail/ref=FlowAnalysisAndConstantPatterns2.out -XDrawDiagnostics FlowAnalysisAndConstantPatterns2.java
 */
public class FlowAnalysisAndConstantPatterns2 {
    public static int getInt(){
        return 42;
    }
    public static void main(String[] args) {

        int i = getInt();
        switch (i) {
            case 42+1: System.out.println("42+1!");
            case 43: System.out.println("43!");
        }

        switch (i) {
            case 43: System.out.println("43!");
            case 42+1: System.out.println("42+1!");
        }

        switch (i) {
            case 43: System.out.println("43!");
            case 42+1: System.out.println("42+1!");
            case int j: System.out.println("it's an int!");
        }

        switch (i) {
            case 43: System.out.println("43!");
            case int j: System.out.println("it's an int!");
            case 42+1: System.out.println("42+1!");
        }
    }
}
