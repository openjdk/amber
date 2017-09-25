/* /nodynamiccopyright/ */

import java.lang.invoke.*;

import static java.lang.invoke.Intrinsics.*;

public class FindConstructorTest extends ConstantFoldingTest {
    @Override
    public String toString() {
        return "invoking FindConstructorTest.toString()";
    }

    public static void main(String[] args) throws Throwable {
        new FindConstructorTest().run();
    }

    void run() throws Throwable {
        test1();
        test2();
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_newInvokeSpecial"})
    void test1() throws Throwable {
        final MethodHandle mhNewFindConstructorTest = ldc(MethodHandleRef.ofConstructor(ClassRef.ofDescriptor("LFindConstructorTest;"), ClassRef.ofVoid()));
        FindConstructorTest foo = (FindConstructorTest) mhNewFindConstructorTest.invokeExact();
        check(foo.toString().equals("invoking FindConstructorTest.toString()"));
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_newInvokeSpecial"})
    void test2() throws Throwable {
        MethodHandle mhNewFindConstructorTest = ldc(MethodHandleRef.ofConstructor(ClassRef.ofDescriptor("LFindConstructorTest;"), ClassRef.ofVoid()));
        FindConstructorTest foo = (FindConstructorTest) mhNewFindConstructorTest.invokeExact();
        check(foo.toString().equals("invoking FindConstructorTest.toString()"));
    }
}
