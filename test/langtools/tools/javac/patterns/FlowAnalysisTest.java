/*
 * @test /nodynamiccopyright/
 * @summary Stress test flow analysis with patterns
 * @compile/fail/ref=FlowAnalysisTest.out -XDrawDiagnostics FlowAnalysisTest.java
 */

class FlowAnalysisTest {
    void foo(Object object) {
        
        switch (object) {
            case Object o: break;
            case String s: break;
        }
        switch (object) {
            case var o: break;
            case String s: break;
            case "Hello" : break;
        }
    }
}
