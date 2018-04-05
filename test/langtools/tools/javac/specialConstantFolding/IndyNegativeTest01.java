/*
 * @test /nodynamiccopyright/
 * @summary adding indy negative test
 * @compile/fail/ref=IndyNegativeTest01.out -XDdoConstantFold -XDrawDiagnostics IndyNegativeTest01.java
 */

import java.lang.invoke.constant.*;

import static java.lang.invoke.Intrinsics.*;

public class IndyNegativeTest01 {
    void test(String invokeName, String x, String y) throws Throwable {
        MethodTypeRef methodTypeForMethodHandle = MethodTypeRef.of(
                ConstantRefs.CR_CallSite,
                ConstantRefs.CR_MethodHandles_Lookup,
                ConstantRefs.CR_String,
                ConstantRefs.CR_MethodType,
                ConstantRefs.CR_String,
                ConstantRefs.CR_Object.array()
        );
        MethodHandleRef mh = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, ClassRef.ofDescriptor("Ljava/lang/invoke/StringConcatFactory;"),
                                                "makeConcatWithConstants", methodTypeForMethodHandle);
        MethodTypeRef methodTypeForIndy = MethodTypeRef.of(
                ConstantRefs.CR_String,
                ConstantRefs.CR_String,
                ConstantRefs.CR_String
        );
        final String param = "" + '\u0001' + '\u0001';
        DynamicCallSiteRef indyDescr = DynamicCallSiteRef.of(mh, invokeName, methodTypeForIndy, param);
        // invokeName is not a constant
        String indyRes = (String)invokedynamic(indyDescr, x, y);
        indyDescr = null; // not effectively final
    }
}
