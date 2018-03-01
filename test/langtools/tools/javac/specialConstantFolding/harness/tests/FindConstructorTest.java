/* /nodynamiccopyright/ */

import java.lang.invoke.*;
import java.lang.sym.ClassRef;
import java.lang.sym.MethodHandleRef;
import java.lang.sym.MethodTypeRef;
import java.lang.sym.ConstantRefs;

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
        final MethodHandle mhNewFindConstructorTest = ldc(MethodHandleRef.of(MethodHandleRef.Kind.CONSTRUCTOR, ClassRef.ofDescriptor("LFindConstructorTest;"), "<init>",
                                                                             MethodTypeRef.of(ConstantRefs.CR_void)));
        FindConstructorTest foo = (FindConstructorTest) mhNewFindConstructorTest.invokeExact();
        check(foo.toString().equals("invoking FindConstructorTest.toString()"));
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_newInvokeSpecial"})
    void test2() throws Throwable {
        MethodHandle mhNewFindConstructorTest = ldc(MethodHandleRef.of(MethodHandleRef.Kind.CONSTRUCTOR, ClassRef.ofDescriptor("LFindConstructorTest;"), "<init>",
                                                                       MethodTypeRef.of(ConstantRefs.CR_void)));
        FindConstructorTest foo = (FindConstructorTest) mhNewFindConstructorTest.invokeExact();
        check(foo.toString().equals("invoking FindConstructorTest.toString()"));
    }
}
