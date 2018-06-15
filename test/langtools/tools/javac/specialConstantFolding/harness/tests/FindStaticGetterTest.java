/* /nodynamiccopyright/ */

import java.lang.invoke.*;
import java.lang.constant.*;
import static java.lang.invoke.Intrinsics.*;
import static java.lang.constant.DirectMethodHandleDesc.Kind.STATIC_GETTER;

public class FindStaticGetterTest extends ConstantFoldingTest {
    static String staticStrField = "class field";

    public static void main(String[] args) throws Throwable {
        new FindStaticGetterTest().run();
    }

    void run() throws Throwable {
        test1();
        test2();
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_getStatic"})
    void test1() throws Throwable {
        final MethodHandle mhStaticGetter = ldc(MethodHandleDesc.ofField(STATIC_GETTER, ClassDesc.ofDescriptor("LFindStaticGetterTest;"), "staticStrField", ConstantDescs.CR_String));
        check(mhStaticGetter.invoke().toString().equals("class field"));
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_getStatic"})
    void test2() throws Throwable {
        MethodHandle mhStaticGetter = ldc(MethodHandleDesc.ofField(STATIC_GETTER, ClassDesc.ofDescriptor("LFindStaticGetterTest;"), "staticStrField", ConstantDescs.CR_String));
        check(mhStaticGetter.invoke().toString().equals("class field"));
    }
}
