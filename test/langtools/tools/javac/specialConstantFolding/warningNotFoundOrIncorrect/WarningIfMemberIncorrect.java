/*
 * @test /nodynamiccopyright/
 * @summary warn if a member is incorrect at compile time
 * @compile/fail/ref=WarningIfMemberIncorrect.out -XDdoConstantFold -Werror -XDrawDiagnostics WarningIfMemberIncorrect.java
 */

import java.lang.invoke.*;
import static java.lang.invoke.Intrinsics.*;

class WarningIfMemberIncorrect {
    interface I {
        String foo();
    }

    void m() {
        MethodTypeRef mtReturnString = MethodTypeRef.of(ClassRef.ofDescriptor("Ljava/lang/String;"));

        // warn: the method is not static
        MethodHandle mh1 = ldc(MethodHandleRef.of(MethodHandleRef.Kind.STATIC, ClassRef.ofDescriptor("LWarningIfMemberIncorrect;"), "foo", mtReturnString));

        // warn: referring to a field not to a method plus a warning because the symbol has the wrong staticness
        MethodHandle mh2 = ldc(MethodHandleRef.of(MethodHandleRef.Kind.STATIC, ClassRef.ofDescriptor("LWarningIfMemberIncorrect;"), "bar", mtReturnString));

        // warn: wrong kind to refer to a constructor
        MethodHandle mhConstructor = ldc(MethodHandleRef.of(MethodHandleRef.Kind.VIRTUAL, ClassRef.ofDescriptor("LWarningIfMemberIncorrect;"), "<init>",
                                                                             MethodTypeRef.of(ClassRef.CR_void)));

        MethodHandle mh3 = ldc(MethodHandleRef.of(MethodHandleRef.Kind.VIRTUAL, ClassRef.ofDescriptor("LWarningIfMemberIncorrect$I;"), "foo", mtReturnString));
    }

    String foo() { return null; }
    String bar;
}
