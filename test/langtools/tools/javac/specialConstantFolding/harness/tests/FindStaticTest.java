/* /nodynamiccopyright/ */

import java.lang.invoke.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;

import static java.lang.invoke.Intrinsics.*;

public class FindStaticTest extends ConstantFoldingTest {
    static String foo() {
        return "invoking static method FindStaticTest.foo()";
    }

    public static void main(String[] args) throws Throwable {
        new FindStaticTest().run();
    }

    void run() throws Throwable {
        test1();
        test2();
        test3();
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_invokeStatic"})
    void test1() throws Throwable {
        final MethodTypeDesc mt = MethodTypeDesc.of(ClassDesc.ofDescriptor("Ljava/lang/String;"));
        MethodHandle mh2 = ldc(MethodHandleDesc.of(MethodHandleDesc.Kind.STATIC, ClassDesc.ofDescriptor("LFindStaticTest;"), "foo", mt));
        check(mh2.invoke().toString().equals("invoking static method FindStaticTest.foo()"));
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_invokeStatic"})
    void test2() throws Throwable {
        MethodTypeDesc mt = MethodTypeDesc.of(ClassDesc.ofDescriptor("Ljava/lang/String;"));
        MethodHandle mh2 = ldc(MethodHandleDesc.of(MethodHandleDesc.Kind.STATIC, ClassDesc.ofDescriptor("LFindStaticTest;"), "foo", mt));
        check(mh2.invoke().toString().equals("invoking static method FindStaticTest.foo()"));
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_invokeStatic"})
    void test3() throws Throwable {
        MethodHandle mh2 = ldc(MethodHandleDesc.of(MethodHandleDesc.Kind.STATIC, ClassDesc.ofDescriptor("LFindStaticTest;"), "foo", MethodTypeDesc.of(ClassDesc.ofDescriptor("Ljava/lang/String;"))));
        check(mh2.invoke().toString().equals("invoking static method FindStaticTest.foo()"));
    }
}
