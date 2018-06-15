/* /nodynamiccopyright/ */

import java.lang.invoke.*;
import java.lang.constant.*;
import java.util.List;
import static java.lang.invoke.Intrinsics.*;

@SkipExecution
class FindMethodWithGenericArgumentsTest extends ConstantFoldingTest {
    void bar(List<String> l) {}

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_invokeVirtual"})
    void test() {
        MethodHandle mh2 = ldc(MethodHandleDesc.of(DirectMethodHandleDesc.Kind.VIRTUAL, ClassDesc.ofDescriptor("LFindMethodWithGenericArgumentsTest;"), "bar",
                                                   MethodTypeDesc.of(ConstantDescs.CR_void, ConstantDescs.CR_List)));
    }
}
