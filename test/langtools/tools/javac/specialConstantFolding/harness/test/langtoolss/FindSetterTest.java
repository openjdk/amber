/* /nodynamiccopyright/ */

import java.lang.invoke.*;

import static java.lang.invoke.Intrinsics.*;

public class FindSetterTest extends ConstantFoldingTest {
    String strField = "instance field";

    public static void main(String[] args) throws Throwable {
        new FindSetterTest().run();
    }

    void run() throws Throwable {
        test1(this);
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_putField"})
    void test1(FindSetterTest f) throws Throwable {
        final MethodHandle mhSetter = ldc(MethodHandleRef.ofSetter(ClassRef.ofDescriptor("LFindSetterTest;"), "strField", ClassRef.CR_String));
        mhSetter.invoke(f, "new instance field value");
        check(f.strField.equals("new instance field value"));
    }
}
