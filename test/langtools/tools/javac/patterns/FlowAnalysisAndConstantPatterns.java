/*
 * @test /nodynamiccopyright/
 * @summary Stress test flow analysis with constant patterns
 * @compile/fail/ref=FlowAnalysisAndConstantPatterns.out -XDrawDiagnostics FlowAnalysisAndConstantPatterns.java
 */
public class FlowAnalysisAndConstantPatterns {
    public static int getInt(){
        return 43;
    }
    public static void main(String[] args) {
        int i = getInt();
        switch (i) {
            case int j: System.out.println("it's an int!"); break;
            case 42: System.out.println("42!"); break;
        }
    }
}
