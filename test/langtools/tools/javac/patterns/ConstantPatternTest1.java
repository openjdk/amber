/*
 * @test
 * @summary Basic tests for constant patterns
 * @compile ConstantPatternTest1.java
 * @run main ConstantPatternTest1
 */

public class ConstantPatternTest1 {
    static int counter = 0;

    public static void message() {
        System.out.println("Test "+ counter);
        counter++;
    }

    public static void fortyTwoTester(Object o) {
        switch (o) {
            case true: System.out.println("Boolean - wat no 42?"); break;
            case 42f: System.out.println("42 float");break;
            case '4': System.out.println("an f char"); break;
            case 42.0: System.out.println("42.0"); break;
            case 42: System.out.println("42"); break;
            case "42": System.out.println("42 string"); break;
            default: System.out.println("Something else"); break;
        }
    }
    public static double valueOfD(){
        return 41.0;
    }

    public static void main(String[] args) {

        int i = 41;
        i++;
        final int constantInt = 42;


        // Simple literal constant pattern
        if (i __matches 42) {
            message();
        } else {
            throw new AssertionError("Broken");
        }

        // Simple constant expression pattern
        if (i __matches 41+1) {
            message();
        } else {
            throw new AssertionError("Broken");
        }

        // Constant expression pattern using a final local
        if (i __matches constantInt) {
            message();
        } else {
            throw new AssertionError("Broken");
        }

        //--- Tests for match statement

        // Simple literal constant pattern
        switch (i){
            case 42: {
                message();
            }
        }

        // Simple constant expression pattern
        switch (i){
            case 41+1: {
                message();
            }
        }

        // Constant expression pattern using a final local
        switch (i){
            case constantInt: {
                message();
            }
        }

        // Multiple constant pattern clauses

        switch (i){
            case 41: {
                throw new AssertionError("Broken");
            }
            case 42: {
                message();
                break;
            }
        }

        switch (i){
            case 42: {
                message();
                break;
            }
            case 41: {
                throw new AssertionError("Broken");
            }
        }

        switch (i){
            case 42: {
                message();
                break;
            }
            case 41: {
                throw new AssertionError("Broken");
            }
            default: {
                throw new AssertionError("Broken");
            }
        }

        switch (i){
            case 41: {
                throw new AssertionError("Broken");
            }
            case 42: {
                message();
                break;
            }
            default: {
                throw new AssertionError("Broken");
            }
        }

        // Multiple constant expression pattern clauses

        switch (i){
            case 41-1: {
                throw new AssertionError("Broken");
            }
            case 42: {
                message();
                break;
            }
        }


//--- DOUBLE TYPED CONSTANT EXPRESSIONS

        double d = valueOfD();
        d++;
        final double constant = 42.0;
        Object o = d;

        //--- Tests for matches expression


        // Simple literal constant pattern
        if (d __matches 42.0) {
            message();
        } else {
            throw new AssertionError("Broken");
        }

        // Simple constant expression pattern
        if (d __matches 41.0+1.0) {
            message();
        } else {
            throw new AssertionError("Broken");
        }

        // Constant expression pattern using a final local
        if (d __matches constant) {
            message();
        } else {
            throw new AssertionError("Broken");
        }

        //--- Tests for match statement

        // Simple literal constant pattern
        switch (d) {
            case 42.0: {
                message();
            }
        }

        // Simple constant expression pattern
        switch (d) {
            case 41.0+1.0: {
                message();
                break;
            }
        }

        // Constant expression pattern using a final local
        switch (d) {
            case constant: {
                message();
                break;
            }
        }

        // Multiple constant pattern clauses

        switch (d) {
            case 41.0: {
                throw new AssertionError("Broken");
            }
            case 42.0: {
                message();
                break;
            }
        }

        switch (d) {
            case 42.0: {
                message();
                break;
            }
            case 41.0: {
                throw new AssertionError("Broken");
            }
        }

        switch (d) {
            case 42.0: {
                message();
                break;
            }
            case 41.0: {
                throw new AssertionError("Broken");
            }
            default: {
                throw new AssertionError("Broken");
            }
        }

        switch (d) {
            case 41.0: {
                throw new AssertionError("Broken");
            }
            case 42.0: {
                message();
                break;
            }
            default: {
                throw new AssertionError("Broken");
            }
        }

        // Multiple constant expression pattern clauses

        switch (d) {
            case 41.0-1.0: {
                throw new AssertionError("Broken");
            }
            case 42.0: {
                message();
            }
        }

//--- STRING TYPED CONSTANT EXPRESSIONS ----

        String s = "Hello";
        final String hello = "Hello";

        // Simple literal constant pattern
        if (s __matches "Hello") {
            message();
        } else {
            throw new AssertionError("Broken");
        }

        // Simple constant expression pattern
        if (s __matches "Hell"+"o") {
            message();
        } else {
            throw new AssertionError("Broken");
        }

        // Constant expression pattern using a final local
        if (s __matches hello) {
            message();
        } else {
            throw new AssertionError("Broken");
        }

        //--- Tests for match statement

        // Simple literal constant pattern
        switch (s) {
            case "Hello": {
                message();
            }
        }

        // Simple constant expression pattern
        switch (s) {
            case "Hell"+"o": {
                message();
            }
        }

        // Constant expression pattern using a final local
        switch (s) {
            case hello: {
                message();
            }
        }

        // Multiple constant pattern clauses

        switch (s) {
            case "Hi": {
                throw new AssertionError("Broken");
            }
            case "Hello": {
                message();
                break;
            }
        }

        switch (s) {
            case "Hello": {
                message();
                break;
            }
            case "Hi": {
                throw new AssertionError("Broken");
            }
        }

        switch (s) {
            case "Hello": {
                message();
                break;
            }
            case "Hi": {
                throw new AssertionError("Broken");
            }
            default: {
                throw new AssertionError("Broken");
            }
        }

        switch (s) {
            case "Hi": {
                throw new AssertionError("Broken");
            }
            case "Hello": {
                message();
                break;
            }
            default: {
                throw new AssertionError("Broken");
            }
        }

        // Multiple constant expression pattern clauses

        switch (s) {
            case "Hello"+"-": {
                throw new AssertionError("Broken");
            }
            case "Hello": {
                message();
                break;
            }
        }

        // Constant expression and a var pattern of the same type


        switch(s){
            case "Hello ": {
                throw new AssertionError("Broken");
            }
            case String s1: {
                message();
            }
        }

        switch(s){
            case "Hello": {
                message();
                break;
            }
            case String s1: {
                throw new AssertionError("Broken");
            }
        }
// ----- Constant expression patterns and reference typed expressions

        Object obj = "Hello";

        if (obj __matches "Hello") {
            message();
        } else {
            throw new AssertionError("Broken");
        }
        if (obj __matches null) {
            throw new AssertionError("Broken");
        }
        obj = 42;
        if (obj __matches 42) {
            message();
        } else {
            throw new AssertionError("Broken");
        }

        if (obj __matches var x) {
            if (obj __matches 42) {
                message();
            } else {
                throw new AssertionError("Broken");
            }
        } else {
            throw new AssertionError("Broken");
        }

        if (obj __matches 42) {
            if (obj __matches var x) {
                message();
            }
        }
        if (obj __matches null) {
            throw new AssertionError("Broken");
        }
        if (null __matches "Hello") {
            throw new AssertionError("Broken");
        }


        fortyTwoTester(42);
        fortyTwoTester(42.0);
        fortyTwoTester("42");
        fortyTwoTester(42L);
        fortyTwoTester(true);
        fortyTwoTester('4');
        fortyTwoTester('x');
        fortyTwoTester(0x2a);
    }

}
