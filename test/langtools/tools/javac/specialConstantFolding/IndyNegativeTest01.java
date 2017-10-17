/*
 * @test /nodynamiccopyright/
 * @summary adding indy negative test
 * @compile/fail/ref=IndyNegativeTest01.out -XDdoConstantFold -XDrawDiagnostics IndyNegativeTest01.java
 */

import java.lang.invoke.*;

import static java.lang.invoke.Intrinsics.*;

public class IndyNegativeTest01 {
    void test(String invokeName, String x, String y) throws Throwable {
        MethodTypeRef methodTypeForMethodHandle = MethodTypeRef.of(
            ClassRef.CR_CallSite,
            ClassRef.CR_Lookup,
            ClassRef.CR_String,
            ClassRef.CR_MethodType,
            ClassRef.CR_String,
            ClassRef.CR_Object.array()
        );
        MethodHandleRef mh = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, ClassRef.ofDescriptor("Ljava/lang/invoke/StringConcatFactory;"),
                                                "makeConcatWithConstants", methodTypeForMethodHandle);
        final String param = "" + '\u0001' + '\u0001';
        BootstrapSpecifier indyDescr = BootstrapSpecifier.of(mh, param);
        // invokeName is not a constant
        String indyRes = (String)invokedynamic(indyDescr, invokeName, x, y);
        indyDescr = null; // not effectively final
    }
}
