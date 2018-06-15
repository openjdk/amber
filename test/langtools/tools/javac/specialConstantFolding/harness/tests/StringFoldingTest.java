/* /nodynamiccopyright/ */

import java.lang.constant.*;
import static java.lang.invoke.Intrinsics.*;

public class StringFoldingTest extends ConstantFoldingTest {
    public static void main(String[] args) throws Throwable {
        new StringFoldingTest().test();
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_Class_info", "java/lang/String"})
    @InstructionInfo(bytecodePosition=3, values={"CONSTANT_Class_info", "java/lang/String"})
    void test() {
        Class<?> c1 = (Class<?>)ldc(ClassDesc.ofDescriptor("Ljava/lang/String;" + ""));
        ClassDesc c2 = ClassDesc.ofDescriptor("Ljava/lang/String;");
        Class<?> c3 = (Class<?>)ldc(ClassDesc.ofDescriptor("" + c2.descriptorString()));
    }
}
