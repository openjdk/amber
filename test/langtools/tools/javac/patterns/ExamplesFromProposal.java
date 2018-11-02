/*
 * @test
 * @summary All example code from "Pattern Matching for Java" document, released April 2017 (with switch renamed as match)
 * @compile ExamplesFromProposal.java
 * @run main ExamplesFromProposal
 */

interface Node {
}

class IntNode implements Node {
    int value;

    IntNode(int value) {
        this.value = value;
    }
}

class NegNode implements Node {
    Node node;

    NegNode(Node node) {
        this.node = node;
    }
}

class MulNode implements Node {
    Node left, right;

    MulNode(Node left, Node right) {
        this.left = left;
        this.right = right;
    }
}

class AddNode implements Node {
    Node left, right;

    AddNode(Node left, Node right) {
        this.left = left;
        this.right = right;
    }
}

public class ExamplesFromProposal {

    public static Object getSomething() {
        return new Long(42);
    }

    public static int eval(Node n) {
        switch (n) {
            case IntNode in: return in.value;
            case NegNode nn: return -eval(nn.node);
            case AddNode an: return eval(an.left) + eval(an.right);
            case MulNode mn: return eval(mn.left) * eval(mn.right);
        };
        // should never happen
        throw new AssertionError("broken");
    }

    public static String toString(Node n) {
        switch (n) {
            case IntNode in: return String.valueOf(in.value);
            case NegNode nn: return "-"+eval(nn.node);
            case AddNode an: return eval(an.left) + " + " + eval(an.right);
            case MulNode mn: return eval(mn.left) + " * " + eval(mn.right);
        };
        // should never happen
        throw new AssertionError("broken");
    }

    public static Node simplify(Node n) {
        switch (n) {
            case IntNode in:
                return n;

            case NegNode nn:
                return new NegNode(simplify(nn.node));

            case AddNode ad: switch (simplify(ad.left)) {
                case IntNode intn:
                    if (intn.value == 0)
                        return simplify(ad.right);
                    else
                        return new AddNode(intn, simplify(ad.right));
                default:
                    return new AddNode(simplify(ad.left), simplify(ad.right));
            }

            case MulNode mn:
                return new MulNode(simplify(mn.left), simplify(mn.right));
        }
        //should never happen
        throw new AssertionError("broken");
    }

    public static void testNode(Node n, int expected) {
        if (eval(n) != expected)
            throw new AssertionError("broken");
    }

    public static void main(String[] args) {
        Object x = new Integer(42);

        if (x __matches Integer i) {
            // can use i here
            System.out.println(i.intValue());
        }

        Object obj = getSomething();

        String formatted = "unknown";
        if (obj __matches Integer i) {
            formatted = String.format("int %d", i);
        }
        else if (obj __matches Byte b) {
            formatted = String.format("byte %d", b);
        }
        else if (obj __matches Long l) {
            formatted = String.format("long %d", l);
        }
        else if (obj __matches Double d) {
            formatted = String.format("double %f", d);
        }
        else if (obj __matches String s) {
            formatted = String.format("String %s", s);
        }
        System.out.println(formatted);

        formatted="";
        switch (obj) {
            case Integer i: formatted = String.format("int %d", i); break;
            case Byte b:    formatted = String.format("byte %d", b); break;
            case Long l:    formatted = String.format("long %d", l); break;
            case Double d:  formatted = String.format("double %f", d); break;
            case String s:  formatted = String.format("String %s", s); break;
            default:        formatted = String.format("Something else "+ obj.toString()); break;
        }
        System.out.println(formatted);

        // Rewritten from an expression switch
        String s="";
        short srt = 100;
        int num = (int)srt;

        switch (num) {
            case 0: s = "zero"; break;
            case 1: s = "one"; break;
        //    case int i: s = "some other integer";
            default: s = "not an Integer"; break;
        }
        System.out.println(s);


        Node zero = new IntNode(0);
        Node one = new IntNode(1);
        Node ft = new IntNode(42);

        Node temp = new AddNode(zero,ft);

        testNode(temp,42);



        if (toString(simplify(temp)).equals(toString(ft)))
            System.out.println("Simplify worked!");
        else
            throw new AssertionError("broken");


        if (toString(simplify(new AddNode(zero,temp))).equals(toString(ft)))
            System.out.println("Simplify worked!");
        else
            throw new AssertionError("broken");


        temp = new AddNode(zero,ft);
        temp = new AddNode(one,temp);
        temp = new AddNode(zero,temp);

        Node fortythree = new AddNode(one,ft);

        if (toString(simplify(temp)).equals(toString(fortythree)))
            System.out.println("Simplify worked!");
        else
            throw new AssertionError("broken");


        x = "Hello";

        if (x __matches String s1) {
            System.out.println(s1);
        }
        if (x __matches String s1 && s1.length() > 0) {
            System.out.println(s1);
        }
        if (x __matches String s1) {
            System.out.println(s1 + " is a string");
        } else {
            System.out.println("not a string");
        }

        if (!(x __matches String s1)) {
            System.out.println("not a string");
        } else {
            System.out.println(s1 + " is a string");
        }
    }
}
