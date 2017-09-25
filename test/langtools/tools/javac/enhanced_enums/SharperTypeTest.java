/*
 * @test /nodynamiccopyright/
 * @bug 8177505
 * @summary testing the sharper type of enhanced enum constants
 * @compile/fail/ref=SharperTypeTest.out -XDrawDiagnostics SharperTypeTest.java
 */

public class SharperTypeTest {
    enum Primitive<X> {
        INT<Integer>(Integer.class, 0) {
            int mod(int x, int y) { return x % y; }
            int add(int x, int y) { return x + y; }
        },
        FLOAT<Float>(Float.class, 0f)  {
            long add(long x, long y) { return x + y; }
        };

        final Class<X> boxClass;
        final X defaultValue;

        Primitive(Class<X> boxClass, X defaultValue) {
           this.boxClass = boxClass;
           this.defaultValue = defaultValue;
        }
    }

    void m() {
        int zero_int = Primitive.INT.mod(50, 2);           // ok
        int zero_float = Primitive.FLOAT.mod(50, 2);       // error
    }
}
