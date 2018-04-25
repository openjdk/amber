/*
 * @test /nodynamiccopyright/
 * @summary warn if a class or member is not found at compile time
 * @compile/fail/ref=WarningIfClassOrMemberNotFound3.out -Werror -XDrawDiagnostics WarningIfClassOrMemberNotFound3.java
 */

import java.lang.invoke.*;
import java.lang.invoke.constant.*;

public class WarningIfClassOrMemberNotFound3 {
    interface I {}

    private static final ClassDesc THE_INTERFACE = ClassDesc.of("WarningIfClassOrMemberNotFound3$I");

    public void test1() {
        MethodHandleDesc negIMethodRef = MethodHandleDesc.of(MethodHandleDesc.Kind.CONSTRUCTOR, THE_INTERFACE, "", MethodTypeDesc.ofDescriptor("()V"));
        Intrinsics.ldc(negIMethodRef);
    }

    public void test2() {
        MethodHandleDesc negIMethodRef = MethodHandleDesc.ofField(MethodHandleDesc.Kind.GETTER, THE_INTERFACE, "strField", ConstantDescs.CR_String);
        Intrinsics.ldc(negIMethodRef);
    }

    public void test3() {
        MethodHandleDesc negIMethodRef = MethodHandleDesc.ofField(MethodHandleDesc.Kind.SETTER, THE_INTERFACE, "strField", ConstantDescs.CR_String);
        Intrinsics.ldc(negIMethodRef);
    }
}
