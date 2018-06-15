/* /nodynamiccopyright/ */

import java.lang.invoke.*;
import java.lang.constant.*;
import static java.lang.invoke.Intrinsics.*;

public class FindVirtualTest02 extends ConstantFoldingTest {
    class Foo {
        String foo() {
            return "invoking method Foo.foo()";
        }
    }

    public static void main(String[] args) throws Throwable {
        new FindVirtualTest02().run();
    }

    void run() throws Throwable {
        Foo f = new Foo();
        test1(f);
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_invokeVirtual"})
    void test1(Foo f) throws Throwable {
        final MethodTypeDesc mt = MethodTypeDesc.ofDescriptor("()Ljava/lang/String;");
        MethodHandle mh2 = ldc(MethodHandleDesc.of(DirectMethodHandleDesc.Kind.VIRTUAL, ClassDesc.ofDescriptor("LFindVirtualTest02$Foo;"), "foo", mt));
        check(mh2.invoke(f).toString().equals("invoking method Foo.foo()"));
    }
}
