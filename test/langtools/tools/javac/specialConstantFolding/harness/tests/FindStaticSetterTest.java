/* /nodynamiccopyright/ */

import java.lang.invoke.*;
import java.lang.constant.*;
import static java.lang.invoke.Intrinsics.*;
import static java.lang.constant.DirectMethodHandleDesc.Kind.STATIC_SETTER;

public class FindStaticSetterTest extends ConstantFoldingTest {
    static String staticStrField = "class field";

    public static void main(String[] args) throws Throwable {
        new FindStaticSetterTest().run();
    }

    void run() throws Throwable {
        test1();
        test2();
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_putStatic"})
    void test1() throws Throwable {
        final MethodHandle mhStaticSetter = ldc(MethodHandleDesc.ofField(STATIC_SETTER, ClassDesc.ofDescriptor("LFindStaticSetterTest;"), "staticStrField", ConstantDescs.CR_String));
        mhStaticSetter.invoke("new class field value");
        check(FindStaticSetterTest.staticStrField.equals("new class field value"));
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_putStatic"})
    void test2() throws Throwable {
        MethodHandle mhStaticSetter = ldc(MethodHandleDesc.ofField(STATIC_SETTER, ClassDesc.ofDescriptor("LFindStaticSetterTest;"), "staticStrField", ConstantDescs.CR_String));
        mhStaticSetter.invoke("new class field value");
        check(FindStaticSetterTest.staticStrField.equals("new class field value"));
    }
}
