/* /nodynamiccopyright/ */

import java.lang.invoke.*;
import java.util.List;
import static java.lang.invoke.Intrinsics.*;

@SkipExecution
class FindMethodWithGenericArgumentsTest extends ConstantFoldingTest {
    void bar(List<String> l) {}

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_invokeVirtual"})
    void test() {
        MethodHandle mh2 = ldc(MethodHandleRef.of(MethodHandleRef.Kind.VIRTUAL, ClassRef.ofDescriptor("LFindMethodWithGenericArgumentsTest;"), "bar",
                                                  MethodTypeRef.of(ClassRef.CR_void, ClassRef.CR_List)));
    }
}
