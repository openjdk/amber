/*
 * @test /nodynamiccopyright/
 * @bug 8187428
 * @summary javac fails to reject dominated pattern with the same erased type
 * @compile/fail/ref=ErasureDominationTest.out -XDrawDiagnostics ErasureDominationTest.java
 */
import java.util.ArrayList;

public class ErasureDominationTest {
    public static void main(String [] args) {
        Object o = new ArrayList<ErasureDominationTest>();
        switch (o) {
            case ArrayList<Integer> ai:
                System.out.println("ArrayList<Integer>");
                break;
            case ArrayList<String> as:
                System.out.println("ArrayList<String>");
                break;
        }
    }
}