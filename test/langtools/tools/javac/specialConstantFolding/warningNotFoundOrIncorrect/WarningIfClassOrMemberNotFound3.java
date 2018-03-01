/*
 * @test /nodynamiccopyright/
 * @summary warn if a class or member is not found at compile time
 * @compile/fail/ref=WarningIfClassOrMemberNotFound3.out -XDdoConstantFold -Werror -XDrawDiagnostics WarningIfClassOrMemberNotFound3.java
 */

import java.lang.invoke.*;
import java.lang.sym.*;
import static java.lang.invoke.Intrinsics.*;

public class WarningIfClassOrMemberNotFound3 {
    interface I {}

    private static final ClassRef THE_INTERFACE = ClassRef.of("WarningIfClassOrMemberNotFound3$I");

    public void test1() {
        MethodHandleRef negIMethodRef = MethodHandleRef.of(MethodHandleRef.Kind.CONSTRUCTOR, THE_INTERFACE, "", MethodTypeRef.ofDescriptor("()V"));
        Intrinsics.ldc(negIMethodRef);
    }

    public void test2() {
        MethodHandleRef negIMethodRef = MethodHandleRef.ofField(MethodHandleRef.Kind.GETTER, THE_INTERFACE, "strField", ConstantRefs.CR_String);
        Intrinsics.ldc(negIMethodRef);
    }

    public void test3() {
        MethodHandleRef negIMethodRef = MethodHandleRef.ofField(MethodHandleRef.Kind.SETTER, THE_INTERFACE, "strField", ConstantRefs.CR_String);
        Intrinsics.ldc(negIMethodRef);
    }
}
