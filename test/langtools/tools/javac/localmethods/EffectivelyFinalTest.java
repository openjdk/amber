/*
 * @test /nodynamiccopyright/
 * @bug 8233038
 * @summary Tests need to be written for local methods feature
 * @compile/fail/ref=EffectivelyFinalTest.out -XDrawDiagnostics EffectivelyFinalTest.java
 */

public class EffectivelyFinalTest {
    {
        int x = 10;
        x = 30;
        void foo() {
            x = 40;
        }
    }
    public static void main(String args []) {
       void foo() {
           args = null;
       }
       int p;
       if (args == null)
           p = 10;
       else
           p = 10;
       int goo() {
           return p;
       }
       int q;
       if (args == null)
           q = 10;
       else
           q = 10;
       int qoo() {
           return q;
       }
       q = 50;
    }
}
