/* /nodynamiccopyright/ */

import java.lang.invoke.*;
import java.lang.sym.ClassRef;
import java.lang.sym.MethodHandleRef;
import java.lang.sym.MethodTypeRef;

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

    @InstructionInfo(bytecodePosition=6, values={"CONSTANT_MethodHandle_info", "REF_invokeVirtual"})
    void test1(Foo f) throws Throwable {
        final MethodTypeRef mt = MethodTypeRef.ofDescriptor("()Ljava/lang/String;");
        MethodHandle mh2 = ldc(MethodHandleRef.of(MethodHandleRef.Kind.VIRTUAL, ClassRef.ofDescriptor("LFindVirtualTest02$Foo;"), "foo", mt));
        check(mh2.invoke(f).toString().equals("invoking method Foo.foo()"));
    }
}
