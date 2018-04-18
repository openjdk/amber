/* /nodynamiccopyright/ */

import java.lang.invoke.*;
import java.lang.invoke.constant.ClassDesc;
import java.lang.invoke.constant.ConstantDescs;
import java.lang.invoke.constant.MethodHandleDesc;
import java.lang.invoke.constant.MethodTypeDesc;
import java.util.List;
import static java.lang.invoke.Intrinsics.*;

@SkipExecution
class FindMethodWithGenericArgumentsTest extends ConstantFoldingTest {
    void bar(List<String> l) {}

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_invokeVirtual"})
    void test() {
        MethodHandle mh2 = ldc(MethodHandleDesc.of(MethodHandleDesc.Kind.VIRTUAL, ClassDesc.ofDescriptor("LFindMethodWithGenericArgumentsTest;"), "bar",
                                                   MethodTypeDesc.of(ConstantDescs.CR_void, ConstantDescs.CR_List)));
    }
}
