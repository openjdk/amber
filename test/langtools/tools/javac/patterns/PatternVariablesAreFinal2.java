/*
 * @test
 * @summary Pattern variables are final so should be allowed to be referenced in an inner class
 * @compile --enable-preview -source ${jdk.version} PatternVariablesAreFinal2.java
 * @run main/othervm --enable-preview PatternVariablesAreFinal2
 */
public class PatternVariablesAreFinal2 {
    public static void main(String[] args) {
        Object o = "42";
        if (o instanceof String s) {
            new Object() {
                void run() { System.err.println(s); }
            }.run();
        }
    }
}
