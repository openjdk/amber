/*
 * @test /nodynamiccopyright/
 * @compile/fail/ref=SwitchObject.out -XDrawDiagnostics SwitchObject.java
 */
public class SwitchObject {

    private int longSwitch(Object o) {
        switch (o) {
            case -1: return 0;
            case "": return 1;
            default: return 3;
        }
    }

}