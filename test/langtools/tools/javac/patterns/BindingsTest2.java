/*
 * @test /nodynamiccopyright/
 * @summary Ensure that scopes arising from conditionalExpressions are handled corrected.
 * @compile/fail/ref=BindingsTest2.out -XDrawDiagnostics BindingsTest2.java
 */
public class BindingsTest2 {
    public static boolean Ktrue() { return true; }
    public static void main(String[] args) {
        Object o1 = "hello";
        Integer in = 42;
        Object o2 = in;
        Object o3 = "there";


        if (Ktrue() ? o2 __matches Integer x : o2 __matches String x) {
            x.intValue();
        }
        if (Ktrue() ? o2 __matches Integer x : true) {
            x.intValue();
        }

        if (o1 __matches String s ? true : true) {
            s.length();
        }
        if (o1 __matches String s ? true : o2 __matches Integer s) {
            s.length();
        }
        if (o1 __matches String s ? true : o2 __matches Integer i) {
            s.length();
        }

        // Test for (e1 ? e2 : e3).T contains intersect(e1.F, e2.T)
        if (!(o1 __matches String s) ? true : true) {
            s.length();
        }
        if (!(o1 __matches String s) ? (o2 __matches Integer s) : true) {
            s.length();
        }
        if (!(o1 __matches String s) ? (o2 __matches Integer i) : true) {
            s.length();
            i.intValue();
        }
        if (!(o1 __matches String s) ? (o1 __matches String s2) : true) {
            s.length();
            s2.length();
        }


        // Test for (e1 ? e2 : e3).F contains intersect(e2.F, e3.F)
        if (Ktrue() ? !(o2 __matches Integer x) : !(o1 __matches String x)){
        } else {
            x.intValue();
        }
        if (Ktrue() ? !(o2 __matches Integer x) : !(o1 __matches String s)){
        } else {
            x.intValue();
        }
        if (Ktrue() ? !(o2 __matches Integer x) : !(o2 __matches Integer x1)){
        } else {
            x.intValue();
            x1.intValue();
        }
        if (Ktrue() ? !(o2 __matches Integer x) : false){
        } else {
            x.intValue();
        }

        // Test for (e1 ? e2 : e3).F contains intersect(e1.T, e3.F)
        if (o1 __matches String s ? true : !(o2 __matches Integer s)){
        } else {
            s.length();
        }
        if (o1 __matches String s ? true : !(o2 __matches Integer i)){
        } else {
            s.length();
            i.intValue();
        }
        if (o1 __matches String s ? true : !(o2 __matches String s1)){
        } else {
            s.length();
            s1.length();
        }
        // Test for (e1 ? e2 : e3).F contains intersect(e1.F, e2.F)
        if (!(o1 __matches String s) ? !(o1 __matches String s1) : true){
        } else {
            s.length();
            s1.length();
        }
        if (!(o1 __matches String s) ? !(o2 __matches Integer s) : true){
        } else {
            s.length();
        }
        if (!(o1 __matches String s) ? !(o2 __matches Integer i) : true){
        } else {
            s.length();
            i.intValue();
        }

        // Test for e1 ? e2: e3 - include e1.T in e2
        if (o1 __matches String s ? false : s.length()>0) {
            System.out.println("done");
        }
        if (o1 __matches String s ? false : s.intValue!=0) {
            System.out.println("done");
        }

        // Test for e1 ? e2 : e3 - include e1.F in e3
        if (!(o1 __matches String s) ? s.length()>0 : false){
            System.out.println("done");
        }
        if (!(o1 __matches String s) ? s.intValue>0 : false){
            System.out.println("done");
        }

    }
}
