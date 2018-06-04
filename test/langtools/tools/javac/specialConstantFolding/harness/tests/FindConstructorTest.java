/* /nodynamiccopyright/ */

import java.lang.invoke.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;

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
        final MethodHandle mhNewFindConstructorTest = ldc(MethodHandleDesc.of(MethodHandleDesc.Kind.CONSTRUCTOR, ClassDesc.ofDescriptor("LFindConstructorTest;"), "<init>",
                                                                              MethodTypeDesc.of(ConstantDescs.CR_void)));
        FindConstructorTest foo = (FindConstructorTest) mhNewFindConstructorTest.invokeExact();
        check(foo.toString().equals("invoking FindConstructorTest.toString()"));
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_newInvokeSpecial"})
    void test2() throws Throwable {
        MethodHandle mhNewFindConstructorTest = ldc(MethodHandleDesc.of(MethodHandleDesc.Kind.CONSTRUCTOR, ClassDesc.ofDescriptor("LFindConstructorTest;"), "<init>",
                                                                        MethodTypeDesc.of(ConstantDescs.CR_void)));
        FindConstructorTest foo = (FindConstructorTest) mhNewFindConstructorTest.invokeExact();
        check(foo.toString().equals("invoking FindConstructorTest.toString()"));
    }
}
