/* /nodynamiccopyright/ */

import java.lang.invoke.*;
import java.lang.sym.ClassRef;
import java.lang.sym.MethodHandleRef;
import java.lang.sym.ConstantRefs;

import static java.lang.invoke.Intrinsics.*;
import static java.lang.sym.MethodHandleRef.Kind.STATIC_GETTER;

public class FindStaticGetterTest extends ConstantFoldingTest {
    static String staticStrField = "class field";

    public static void main(String[] args) throws Throwable {
        new FindStaticGetterTest().run();
    }

    void run() throws Throwable {
        test1();
        test2();
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_getStatic"})
    void test1() throws Throwable {
        final MethodHandle mhStaticGetter = ldc(MethodHandleRef.ofField(STATIC_GETTER, ClassRef.ofDescriptor("LFindStaticGetterTest;"), "staticStrField", ConstantRefs.CR_String));
        check(mhStaticGetter.invoke().toString().equals("class field"));
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodHandle_info", "REF_getStatic"})
    void test2() throws Throwable {
        MethodHandle mhStaticGetter = ldc(MethodHandleRef.ofField(STATIC_GETTER, ClassRef.ofDescriptor("LFindStaticGetterTest;"), "staticStrField", ConstantRefs.CR_String));
        check(mhStaticGetter.invoke().toString().equals("class field"));
    }
}
