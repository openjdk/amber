/* /nodynamiccopyright/ */

import java.lang.invoke.*;

import static java.lang.invoke.Intrinsics.*;

@SkipExecution
public class ConstantPropagationTest extends ConstantFoldingTest {
    @InstructionInfo(bytecodePosition=8, values={"CONSTANT_MethodType_info", "()LConstantPropagationTest;"})
    void test() throws Throwable {
        ClassRef c = ClassRef.ofDescriptor("LConstantPropagationTest;");
        ClassRef d = c;  // constant!
        MethodType mt1 = ldc(MethodTypeRef.of(d)); // constant!
    }
}
