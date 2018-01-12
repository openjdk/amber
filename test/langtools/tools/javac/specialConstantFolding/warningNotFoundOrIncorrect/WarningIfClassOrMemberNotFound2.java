/*
 * @test /nodynamiccopyright/
 * @summary warn if a class or member is not found at compile time
 * @compile/fail/ref=WarningIfClassOrMemberNotFound2.out -XDdoConstantFold -Werror -XDrawDiagnostics WarningIfClassOrMemberNotFound2.java
 */

import java.lang.invoke.*;
import java.lang.sym.*;
import static java.lang.invoke.Intrinsics.*;

public class WarningIfClassOrMemberNotFound2 {
    private static final ClassRef THIS = ClassRef.of("WarningIfClassOrMemberNotFound2");
    public int m(int i) { return i; }

    public void test() {
        MethodHandleRef negIMethodRef = MethodHandleRef.of(MethodHandleRef.Kind.INTERFACE_VIRTUAL, THIS, "m", MethodTypeRef.ofDescriptor("(I)I"));
        Intrinsics.ldc(negIMethodRef);
    }
}
