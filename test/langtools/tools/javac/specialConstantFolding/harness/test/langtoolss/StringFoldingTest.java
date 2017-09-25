/* /nodynamiccopyright/ */

import java.lang.invoke.*;

import static java.lang.invoke.Intrinsics.*;

public class StringFoldingTest extends ConstantFoldingTest {
    public static void main(String[] args) throws Throwable {
        new StringFoldingTest().test();
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_Class_info", "java/lang/String"})
    @InstructionInfo(bytecodePosition=9, values={"CONSTANT_Class_info", "java/lang/String"})
    void test() {
        Class<?> c1 = ldc(ClassRef.ofDescriptor("Ljava/lang/String;" + ""));
        ClassRef c2 = ClassRef.ofDescriptor("Ljava/lang/String;");
        Class<?> c3 = ldc(ClassRef.ofDescriptor("" + c2.descriptorString()));
    }
}
