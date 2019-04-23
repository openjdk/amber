/*
 * @test /nodynamiccopyright/
 * @bug 8206986
 * @summary Verify reachability in switch expressions.
 * @compile/fail/ref=ExpressionSwitchUnreachable.out -XDrawDiagnostics --enable-preview -source ${jdk.version} ExpressionSwitchUnreachable.java
 */

public class ExpressionSwitchUnreachable {

    public static void main(String[] args) {
        int z = 42;
        int i = switch (z) {
            case 0 -> {
                break-with 42;
                System.out.println("Unreachable");  //Unreachable
            }
            default -> 0;
        };
        i = switch (z) {
            case 0 -> {
                break-with 42;
                break-with 42; //Unreachable
            }
            default -> 0;
        };
        i = switch (z) {
            case 0:
                System.out.println("0");
                break-with 42;
                System.out.println("1");    //Unreachable
            default : break-with 42;
        };
        i = switch (z) {
            case 0 -> 42;
            default -> {
                break-with 42;
                System.out.println("Unreachable"); //Unreachable
            }
        };
        i = switch (z) {
            case 0: break-with 42;
            default:
                System.out.println("0");
                break-with 42;
                System.out.println("1");    //Unreachable
        };
        i = switch (z) {
            case 0:
            default:
                System.out.println("0");
                break-with 42;
                System.out.println("1");    //Unreachable
        };
    }


}
