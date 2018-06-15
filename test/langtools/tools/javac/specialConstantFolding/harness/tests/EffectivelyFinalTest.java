/* /nodynamiccopyright/ */

import java.lang.invoke.*;
import java.lang.constant.*;
import static java.lang.invoke.Intrinsics.*;

public class EffectivelyFinalTest extends ConstantFoldingTest {
    String foo() {
        return "invoking EffectivelyFinalTest.foo()";
    }

    public static void main(String[] args) throws Throwable {
        new EffectivelyFinalTest().run();
    }

    void run() throws Throwable {
        test3(this);
        test4(this);
        test5(this);
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodType_info", "()Ljava/lang/String;"})
    void test1() throws Throwable {
        ClassDesc c1 = ClassDesc.ofDescriptor("Ljava/lang/String;");
        MethodType mt = ldc(MethodTypeDesc.of(c1));
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodType_info", "(Ljava/lang/Integer;)Ljava/lang/String;"})
    void test2() throws Throwable {
        ClassDesc c1 = ClassDesc.ofDescriptor("Ljava/lang/String;");
        ClassDesc c2 = ClassDesc.ofDescriptor("Ljava/lang/Integer;");
        MethodType mt = ldc(MethodTypeDesc.of(c1, c2));
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_invokeVirtual"})
    void test3(EffectivelyFinalTest f) throws Throwable {
        ClassDesc c = ClassDesc.ofDescriptor("Ljava/lang/String;");
        MethodTypeDesc mt = MethodTypeDesc.of(c);
        MethodHandle mh = ldc(MethodHandleDesc.of(DirectMethodHandleDesc.Kind.VIRTUAL, ClassDesc.ofDescriptor("LEffectivelyFinalTest;"), "foo", mt));
        check(mh.invoke(f).toString().equals("invoking EffectivelyFinalTest.foo()"));
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_invokeVirtual"})
    void test4(EffectivelyFinalTest f) throws Throwable {
        ClassDesc c = ClassDesc.ofDescriptor("Ljava/lang/String;");
        MethodTypeDesc mt = MethodTypeDesc.of(c);
        final MethodHandle mh = ldc(MethodHandleDesc.of(DirectMethodHandleDesc.Kind.VIRTUAL, ClassDesc.ofDescriptor("LEffectivelyFinalTest;"), "foo", mt));
        check(mh.invoke(f).toString().equals("invoking EffectivelyFinalTest.foo()"));
    }

    final ClassDesc cField = ClassDesc.ofDescriptor("LEffectivelyFinalTest;");

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_invokeVirtual"})
    void test5(EffectivelyFinalTest f) throws Throwable {
        ClassDesc c = ClassDesc.ofDescriptor("Ljava/lang/String;");;
        MethodTypeDesc mt = MethodTypeDesc.of(c);
        MethodHandle mh = ldc(MethodHandleDesc.of(DirectMethodHandleDesc.Kind.VIRTUAL, cField, "foo", mt));
        check(mh.invoke(f).toString().equals("invoking EffectivelyFinalTest.foo()"));
    }
}
