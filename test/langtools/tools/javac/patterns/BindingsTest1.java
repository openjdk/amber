/*
 * @test
 * @summary Basic tests for bindings from instanceof
 * @compile BindingsTest1.java
 * @run main BindingsTest1
 */

public class BindingsTest1 {
    public static boolean Ktrue() { return true; }
    public static void main(String[] args) {
        Object o1 = "hello";
        Integer i = 42;
        Object o2 = i;
        Object o3 = "there";


        // Test for (e matches P).T = { binding variables in P }
        if (o1 instanceof String s) {
            s.length();
        }

        // Test for e1 && e2.T = union(e1.T, e2.T)
        if (o1 instanceof String s && o2 instanceof Integer in) {
            s.length();
            in.intValue();
        }
        // Test for e1 && e2.F = intersect(e1.F, e2.F)
        if (!(o1 instanceof String s) && !(o1 instanceof String s)) {

        } else {
            s.length();
        }

        // test for e1&&e2 - include e1.T in e2
        if (o1 instanceof String s && s.length()>0) {
            System.out.print("done");
        }

        // Test for (e1 || e2).T = intersect(e1.T, e2.T)
        if (o1 instanceof String s || o3 instanceof String s){
            System.out.println(s); // ?
        }

        // Test for (e1 || e2).F = union(e1.F, e2.F)
        if (!(o1 instanceof String s) || !(o3 instanceof Integer in)){
        } else {
            s.length();
            i.intValue();
        }

        // Test for e1||e2 - include e1.F in e2

        if (!(o1 instanceof String s) || s.length()>0) {
            System.out.println("done");
        }

        // Test for (e1 ? e2 : e3).T contains intersect(e2.T, e3.T)
        if (Ktrue() ? o2 instanceof Integer x : o2 instanceof Integer x) {
            x.intValue();
        }

        // Test for (e1 ? e2 : e3).T contains intersect(e1.T, e3.T)
        if (o1 instanceof String s ? true : o1 instanceof String s) {
            s.length();
        }

        // Test for (e1 ? e2 : e3).T contains intersect(e1.F, e2.T)
        if (!(o1 instanceof String s) ? (o1 instanceof String s) : true) {
            s.length();
        }

        // Test for (e1 ? e2 : e3).F contains intersect(e2.F, e3.F)
        if (Ktrue() ? !(o2 instanceof Integer x) : !(o2 instanceof Integer x)){
        } else {
            x.intValue();
        }

        // Test for (e1 ? e2 : e3).F contains intersect(e1.T, e3.F)
        if (o1 instanceof String s ? true : !(o1 instanceof String s)){
        } else {
            s.length();
        }

        // Test for (e1 ? e2 : e3).F contains intersect(e1.F, e2.F)
        if (!(o1 instanceof String s) ? !(o1 instanceof String s) : true){
        } else {
            s.length();
        }

        // Test for e1 ? e2: e3 - include e1.T in e2
        if (o1 instanceof String s ? s.length()>0 : false) {
            System.out.println("done");
        }

        // Test for e1 ? e2 : e3 - include e1.F in e3
        if (!(o1 instanceof String s) ? false : s.length()>0){
            System.out.println("done");
        }

        // Test for (!e).T = e.F

        if (!(!(o1 instanceof String s) || !(o3 instanceof Integer in))){
            s.length();
            i.intValue();
        }

        // Test for (!e).F = e.T
        if (!(o1 instanceof String s)) {

        } else {
            s.length();
        }



        System.out.println("BindingsTest1 complete");
    }
}
