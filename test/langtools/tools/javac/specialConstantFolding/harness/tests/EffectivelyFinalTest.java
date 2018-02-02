/* /nodynamiccopyright/ */

import java.lang.invoke.*;
import java.lang.sym.ClassRef;
import java.lang.sym.MethodHandleRef;
import java.lang.sym.MethodTypeRef;

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
        ClassRef c1 = ClassRef.ofDescriptor("Ljava/lang/String;");
        MethodType mt = ldc(MethodTypeRef.of(c1));
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodType_info", "(Ljava/lang/Integer;)Ljava/lang/String;"})
    void test2() throws Throwable {
        ClassRef c1 = ClassRef.ofDescriptor("Ljava/lang/String;");
        ClassRef c2 = ClassRef.ofDescriptor("Ljava/lang/Integer;");
        MethodType mt = ldc(MethodTypeRef.of(c1, c2));
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_invokeVirtual"})
    void test3(EffectivelyFinalTest f) throws Throwable {
        ClassRef c = ClassRef.ofDescriptor("Ljava/lang/String;");
        MethodTypeRef mt = MethodTypeRef.of(c);
        MethodHandle mh = ldc(MethodHandleRef.of(MethodHandleRef.Kind.VIRTUAL, ClassRef.ofDescriptor("LEffectivelyFinalTest;"), "foo", mt));
        check(mh.invoke(f).toString().equals("invoking EffectivelyFinalTest.foo()"));
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_invokeVirtual"})
    void test4(EffectivelyFinalTest f) throws Throwable {
        ClassRef c = ClassRef.ofDescriptor("Ljava/lang/String;");
        MethodTypeRef mt = MethodTypeRef.of(c);
        final MethodHandle mh = ldc(MethodHandleRef.of(MethodHandleRef.Kind.VIRTUAL, ClassRef.ofDescriptor("LEffectivelyFinalTest;"), "foo", mt));
        check(mh.invoke(f).toString().equals("invoking EffectivelyFinalTest.foo()"));
    }

    final ClassRef cField = ClassRef.ofDescriptor("LEffectivelyFinalTest;");

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_invokeVirtual"})
    void test5(EffectivelyFinalTest f) throws Throwable {
        ClassRef c = ClassRef.ofDescriptor("Ljava/lang/String;");;
        MethodTypeRef mt = MethodTypeRef.of(c);
        MethodHandle mh = ldc(MethodHandleRef.of(MethodHandleRef.Kind.VIRTUAL, cField, "foo", mt));
        check(mh.invoke(f).toString().equals("invoking EffectivelyFinalTest.foo()"));
    }
}
