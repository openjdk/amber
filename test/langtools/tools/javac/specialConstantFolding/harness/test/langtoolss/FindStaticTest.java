/* /nodynamiccopyright/ */

import java.lang.invoke.*;

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

    @InstructionInfo(bytecodePosition=13, values={"CONSTANT_MethodHandle_info", "REF_invokeStatic"})
    void test1() throws Throwable {
        final MethodTypeRef mt = MethodTypeRef.of(ClassRef.ofDescriptor("Ljava/lang/String;"));
        MethodHandle mh2 = ldc(MethodHandleRef.ofStatic(ClassRef.ofDescriptor("LFindStaticTest;"), "foo", mt));
        check(mh2.invoke().toString().equals("invoking static method FindStaticTest.foo()"));
    }

    @InstructionInfo(bytecodePosition=13, values={"CONSTANT_MethodHandle_info", "REF_invokeStatic"})
    void test2() throws Throwable {
        MethodTypeRef mt = MethodTypeRef.of(ClassRef.ofDescriptor("Ljava/lang/String;"));
        MethodHandle mh2 = ldc(MethodHandleRef.ofStatic(ClassRef.ofDescriptor("LFindStaticTest;"), "foo", mt));
        check(mh2.invoke().toString().equals("invoking static method FindStaticTest.foo()"));
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_invokeStatic"})
    void test3() throws Throwable {
        MethodHandle mh2 = ldc(MethodHandleRef.ofStatic(ClassRef.ofDescriptor("LFindStaticTest;"), "foo", MethodTypeRef.of(ClassRef.ofDescriptor("Ljava/lang/String;"))));
        check(mh2.invoke().toString().equals("invoking static method FindStaticTest.foo()"));
    }
}
