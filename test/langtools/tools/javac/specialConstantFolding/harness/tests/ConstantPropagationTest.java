/* /nodynamiccopyright/ */

import java.lang.invoke.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;

import static java.lang.invoke.Intrinsics.*;

@SkipExecution
public class ConstantPropagationTest extends ConstantFoldingTest {
    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodType_info", "()LConstantPropagationTest;"})
    void test() throws Throwable {
        ClassDesc c = ClassDesc.ofDescriptor("LConstantPropagationTest;");
        ClassDesc d = c;  // constant!
        MethodType mt1 = ldc(MethodTypeDesc.of(d)); // constant!
    }
}
