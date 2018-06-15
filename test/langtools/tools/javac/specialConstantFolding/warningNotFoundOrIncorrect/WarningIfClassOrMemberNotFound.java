/*
 * @test /nodynamiccopyright/
 * @summary warn if a class or member is not found at compile time
 * @compile/fail/ref=WarningIfClassOrMemberNotFound.out -Werror -XDrawDiagnostics WarningIfClassOrMemberNotFound.java
 */

import java.lang.invoke.*; import java.lang.constant.*;
import static java.lang.invoke.Intrinsics.*;

class WarningIfClassOrMemberNotFound {
    void m() {
        final MethodTypeDesc mt1 = MethodTypeDesc.of(ClassDesc.ofDescriptor("Ljava/lang/String;"));
        MethodHandle mh1 = ldc(MethodHandleDesc.of(DirectMethodHandleDesc.Kind.STATIC, ClassDesc.ofDescriptor("LNonExistentClass1;"), "foo", mt1));

        ClassDesc cr = ClassDesc.ofDescriptor("LNonExistentClass2;");
        Class<?> c = ldc(cr);

        ClassDesc crArr = ClassDesc.ofDescriptor("[[[LNonExistentClass3;");
        Class<?> cArr = ldc(crArr);

        final MethodTypeDesc mt2 = MethodTypeDesc.of(ClassDesc.ofDescriptor("Ljava/lang/String;"));
        // now the class exists but the method doesn't
        MethodHandle mh2 = ldc(MethodHandleDesc.of(DirectMethodHandleDesc.Kind.STATIC, ClassDesc.ofDescriptor("LWarningIfClassOrMemberNotFound;"), "bar", mt2));

        final MethodTypeDesc mt3 = MethodTypeDesc.of(ClassDesc.ofDescriptor("Ljava/lang/String;"));
        // now the class exists and so is the method but the arguments are incorrect
        MethodHandle mh3 = ldc(MethodHandleDesc.of(DirectMethodHandleDesc.Kind.STATIC, ClassDesc.ofDescriptor("LWarningIfClassOrMemberNotFound;"), "foo", mt3));
    }

    String foo(String s) { return s; }
}
