/* /nodynamiccopyright/ */

import java.lang.invoke.*;
import java.lang.constant.*;
import static java.lang.invoke.Intrinsics.*;

/** Tests the use of MethodType.MethodTypeRef.of(Class<?> rtype) in combination with
 *  MethodHandle.findVirtual(Class<?> refc, String name, MethodType type)
 */
public class FindVirtualTest01 extends ConstantFoldingTest {
    String foo() {
        return "invoking method FindVirtualTest01.foo()";
    }

    String bar(int i) {
        return "invoking method FindVirtualTest01.bar() with argument " + i;
    }

    public static void main(String[] args) throws Throwable {
        new FindVirtualTest01().run();
    }

    void run() throws Throwable {
        test1(this);
        test2(this);
        test2_1(this);
        test3(this);
        test4(this);
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_invokeVirtual"})
    void test1(FindVirtualTest01 f) throws Throwable {
        final MethodTypeDesc mt = MethodTypeDesc.of(ClassDesc.ofDescriptor("Ljava/lang/String;"));
        MethodHandle mh2 = ldc(MethodHandleDesc.of(DirectMethodHandleDesc.Kind.VIRTUAL, ClassDesc.ofDescriptor("LFindVirtualTest01;"), "foo", mt));
        check(mh2.invoke(f).toString().equals("invoking method FindVirtualTest01.foo()"));
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_invokeVirtual"})
    void test2(FindVirtualTest01 f) throws Throwable {
        final MethodTypeDesc mt = MethodTypeDesc.of(ClassDesc.ofDescriptor("Ljava/lang/String;"));
        final ClassDesc fooClass = ClassDesc.ofDescriptor("LFindVirtualTest01;");
        MethodHandle mh2 = ldc(MethodHandleDesc.of(DirectMethodHandleDesc.Kind.VIRTUAL, fooClass, "foo", mt));
        check(mh2.invoke(f).toString().equals("invoking method FindVirtualTest01.foo()"));
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_invokeVirtual"})
    void test2_1(FindVirtualTest01 f) throws Throwable {
        final MethodTypeDesc mt = MethodTypeDesc.of(ClassDesc.ofDescriptor("Ljava/lang/String;"));
        final ClassDesc fooClass = ClassDesc.ofDescriptor("LFindVirtualTest01;");
        MethodHandle mh2 = ldc(MethodHandleDesc.of(DirectMethodHandleDesc.Kind.VIRTUAL, fooClass, "foo", mt));
        check(mh2.invoke(f).toString().equals("invoking method FindVirtualTest01.foo()"));
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_invokeVirtual"})
    void test3(FindVirtualTest01 f) throws Throwable {
        MethodTypeDesc mt = MethodTypeDesc.of(ClassDesc.ofDescriptor("Ljava/lang/String;"));
        MethodHandle mh2 = ldc(MethodHandleDesc.of(DirectMethodHandleDesc.Kind.VIRTUAL, ClassDesc.ofDescriptor("LFindVirtualTest01;"), "foo", mt));
        check(mh2.invoke(f).toString().equals("invoking method FindVirtualTest01.foo()"));
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_invokeVirtual"})
    void test4(FindVirtualTest01 f) throws Throwable {
        MethodHandle mh2 = ldc(MethodHandleDesc.of(DirectMethodHandleDesc.Kind.VIRTUAL, ClassDesc.ofDescriptor("LFindVirtualTest01;"), "foo",
                                                   MethodTypeDesc.of(ClassDesc.ofDescriptor("Ljava/lang/String;"))));
        check(mh2.invoke(f).toString().equals("invoking method FindVirtualTest01.foo()"));
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_invokeVirtual"})
    void test5(FindVirtualTest01 f) throws Throwable {
        MethodHandle mhBar = ldc(MethodHandleDesc.of(DirectMethodHandleDesc.Kind.VIRTUAL, ClassDesc.ofDescriptor("LFindVirtualTest01;"), "bar",
                                                     MethodTypeDesc.of(ClassDesc.ofDescriptor("Ljava/lang/String;"), ConstantDescs.CR_int)));
        check(mhBar.invoke(f, 3).toString().equals("invoking method FindVirtualTest01.bar() with argument 3"));
    }
}
