/*
 * @test /nodynamiccopyright/
 * @summary warn if a class or member is not found at compile time
 * @compile/fail/ref=WarningIfClassOrMemberNotFound.out -XDdoConstantFold -Werror -XDrawDiagnostics WarningIfClassOrMemberNotFound.java
 */

import java.lang.invoke.*; import java.lang.invoke.constant.*;
import static java.lang.invoke.Intrinsics.*;

class WarningIfClassOrMemberNotFound {
    void m() {
        final MethodTypeRef mt1 = MethodTypeRef.of(ClassRef.ofDescriptor("Ljava/lang/String;"));
        MethodHandle mh1 = ldc(MethodHandleRef.of(MethodHandleRef.Kind.STATIC, ClassRef.ofDescriptor("LNonExistentClass1;"), "foo", mt1));

        ClassRef cr = ClassRef.ofDescriptor("LNonExistentClass2;");
        Class<?> c = ldc(cr);

        ClassRef crArr = ClassRef.ofDescriptor("[[[LNonExistentClass3;");
        Class<?> cArr = ldc(crArr);

        final MethodTypeRef mt2 = MethodTypeRef.of(ClassRef.ofDescriptor("Ljava/lang/String;"));
        // now the class exists but the method doesn't
        MethodHandle mh2 = ldc(MethodHandleRef.of(MethodHandleRef.Kind.STATIC, ClassRef.ofDescriptor("LWarningIfClassOrMemberNotFound;"), "bar", mt2));

        final MethodTypeRef mt3 = MethodTypeRef.of(ClassRef.ofDescriptor("Ljava/lang/String;"));
        // now the class exists and so is the method but the arguments are incorrect
        MethodHandle mh3 = ldc(MethodHandleRef.of(MethodHandleRef.Kind.STATIC, ClassRef.ofDescriptor("LWarningIfClassOrMemberNotFound;"), "foo", mt3));
    }

    String foo(String s) { return s; }
}
