/**
 * @test
 * @compile SwitchNull.java
 * @run main SwitchNull
 * @compile/fail/ref=SwitchNull.out -XDdisableIndySwitch=true -XDrawDiagnostics SwitchNull.java
 */

public class SwitchNull {
    public static void main(String... args) {
        new SwitchNull().run();
    }

    private void run() {
        assertEquals(0, stringNoNPE(null));
        assertEquals(1, stringNoNPE(""));
        assertEquals(2, stringNoNPE("other"));
        assertEquals(0, stringNoNPENotFirst1(null));
        assertEquals(0, stringNoNPENotFirst2(null));
        assertEquals(0, boxNoNPE(null));
        assertEquals(1, boxNoNPE(1));
        assertEquals(2, boxNoNPE(2));
        assertEquals(0, boxByteNoNPE(null));
        assertEquals(1, boxByteNoNPE((byte) 1));
        assertEquals(2, boxByteNoNPE((byte) 2));
        assertEquals(0, enumNoNPE(null));
        assertEquals(1, enumNoNPE(E.A));
        assertEquals(2, enumNoNPE(E.B));
    }

    private int stringNoNPE(String str) {
        switch (str) {
            case null: return 0;
            case "": return 1;
            default: return 2;
        }
    }

    private int stringNoNPENotFirst1(String str) {
        switch (str) {
            case "": return 1;
            case null: return 0;
            default: return 2;
        }
    }

    private int stringNoNPENotFirst2(String str) {
        switch (str) {
            case "": return 1;
            default: return 2;
            case null: return 0;
        }
    }

    private int boxNoNPE(Integer i) {
        switch (i) {
            case null: return 0;
            case 1: return 1;
            default: return 2;
        }
    }

    private int boxByteNoNPE(Byte i) {
        switch (i) {
            case null: return 0;
            case 1: return 1;
            default: return 2;
        }
    }

    private int enumNoNPE(E e) {
        switch (e) {
            case null: return 0;
            case A: return 1;
            default: return 2;
        }
    }

    private static enum E {
        A, B;
    }

    private void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError();
        }
    }
}
