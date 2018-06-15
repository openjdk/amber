/*
 * @test /nodynamiccopyright/
 * @summary warn if a member is incorrect at compile time
 * @compile/fail/ref=WarningIfMemberIncorrect.out -Werror -XDrawDiagnostics WarningIfMemberIncorrect.java
 */

import java.lang.invoke.*; import java.lang.constant.*;
import static java.lang.invoke.Intrinsics.*;

class WarningIfMemberIncorrect {
    interface I {
        String foo();
    }

    void m() {
        MethodTypeDesc mtReturnString = MethodTypeDesc.of(ClassDesc.ofDescriptor("Ljava/lang/String;"));

        // warn: the method is not static
        MethodHandle mh1 = ldc(MethodHandleDesc.of(DirectMethodHandleDesc.Kind.STATIC, ClassDesc.ofDescriptor("LWarningIfMemberIncorrect;"), "foo", mtReturnString));

        // warn: referring to a field not to a method plus a warning because the symbol has the wrong staticness
        MethodHandle mh2 = ldc(MethodHandleDesc.of(DirectMethodHandleDesc.Kind.STATIC, ClassDesc.ofDescriptor("LWarningIfMemberIncorrect;"), "bar", mtReturnString));

        // warn: wrong kind to refer to a constructor
        MethodHandle mhConstructor = ldc(MethodHandleDesc.of(DirectMethodHandleDesc.Kind.VIRTUAL, ClassDesc.ofDescriptor("LWarningIfMemberIncorrect;"), "<init>",
                                                             MethodTypeDesc.of(ConstantDescs.CR_void)));

        MethodHandle mh3 = ldc(MethodHandleDesc.of(DirectMethodHandleDesc.Kind.VIRTUAL, ClassDesc.ofDescriptor("LWarningIfMemberIncorrect$I;"), "foo", mtReturnString));
    }

    String foo() { return null; }
    String bar;
}
