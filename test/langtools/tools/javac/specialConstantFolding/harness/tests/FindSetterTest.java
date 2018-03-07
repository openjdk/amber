/* /nodynamiccopyright/ */

import java.lang.invoke.*;
import java.lang.invoke.constant.ClassRef;
import java.lang.invoke.constant.MethodHandleRef;
import java.lang.invoke.constant.ConstantRefs;

import static java.lang.invoke.Intrinsics.*;
import static java.lang.invoke.constant.MethodHandleRef.Kind.SETTER;

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
        final MethodHandle mhSetter = ldc(MethodHandleRef.ofField(SETTER, ClassRef.ofDescriptor("LFindSetterTest;"), "strField", ConstantRefs.CR_String));
        mhSetter.invoke(f, "new instance field value");
        check(f.strField.equals("new instance field value"));
    }
}
