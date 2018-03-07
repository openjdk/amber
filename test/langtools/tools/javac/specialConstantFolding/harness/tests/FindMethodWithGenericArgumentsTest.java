/* /nodynamiccopyright/ */

import java.lang.invoke.*;
import java.lang.invoke.constant.ClassRef;
import java.lang.invoke.constant.MethodHandleRef;
import java.lang.invoke.constant.MethodTypeRef;
import java.lang.invoke.constant.ConstantRefs;
import java.util.List;
import static java.lang.invoke.Intrinsics.*;

@SkipExecution
class FindMethodWithGenericArgumentsTest extends ConstantFoldingTest {
    void bar(List<String> l) {}

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_invokeVirtual"})
    void test() {
        MethodHandle mh2 = ldc(MethodHandleRef.of(MethodHandleRef.Kind.VIRTUAL, ClassRef.ofDescriptor("LFindMethodWithGenericArgumentsTest;"), "bar",
                                                  MethodTypeRef.of(ConstantRefs.CR_void, ConstantRefs.CR_List)));
    }
}
