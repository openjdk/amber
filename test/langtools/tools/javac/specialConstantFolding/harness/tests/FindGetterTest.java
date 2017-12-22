/* /nodynamiccopyright/ */

import java.lang.invoke.*;
import java.lang.sym.ClassRef;
import java.lang.sym.MethodHandleRef;

import static java.lang.invoke.Intrinsics.*;
import static java.lang.sym.MethodHandleRef.Kind.GETTER;

public class FindGetterTest extends ConstantFoldingTest {
    String strField = "instance field";

    public static void main(String[] args) throws Throwable {
        new FindGetterTest().run();
    }

    void run() throws Throwable {
        test1(this);
        test2(this);
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_getField"})
    void test1(FindGetterTest f) throws Throwable {
        final MethodHandle mhInstanceGetter = ldc(MethodHandleRef.ofField(GETTER, ClassRef.ofDescriptor("LFindGetterTest;"), "strField", ClassRef.ofDescriptor("Ljava/lang/String;")));
        check(mhInstanceGetter.invoke(f).toString().equals("instance field"));
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_getField"})
    void test2(FindGetterTest f) throws Throwable {
        MethodHandle mhInstanceGetter = ldc(MethodHandleRef.ofField(GETTER, ClassRef.ofDescriptor("LFindGetterTest;"), "strField", ClassRef.ofDescriptor("Ljava/lang/String;")));
        check(mhInstanceGetter.invoke(f).toString().equals("instance field"));
    }
}
