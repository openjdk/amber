/* /nodynamiccopyright/ */

/** Common super class for most constant folding tests
 */

@IgnoreTest
public class ConstantFoldingTest {
    public static final int LDCOpCode = 0x12;               //  18
    public static final int POPOpCode = 0x57;               //  87
    public static final int GETSTATICOpCode = 0xb2;         // 178
    public static final int PUTSTATICOpCode = 0xb3;         // 179
    public static final int GETFIELDOpCode = 0xb4;          // 180
    public static final int ASTORE_2OpCode = 0x4d;          //  77

    public void check(boolean cond) {
        if (!cond)
            error();
    }

    public void error() {
        throw new AssertionError();
    }
}
