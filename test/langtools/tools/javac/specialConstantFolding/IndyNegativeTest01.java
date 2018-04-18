/*
 * @test /nodynamiccopyright/
 * @summary adding indy negative test
 * @compile/fail/ref=IndyNegativeTest01.out -XDdoConstantFold -XDrawDiagnostics IndyNegativeTest01.java
 */

import java.lang.invoke.constant.*;

import static java.lang.invoke.Intrinsics.*;

public class IndyNegativeTest01 {
    void test(String invokeName, String x, String y) throws Throwable {
        MethodTypeDesc methodTypeForMethodHandle = MethodTypeDesc.of(
                ConstantDescs.CR_CallSite,
                ConstantDescs.CR_MethodHandles_Lookup,
                ConstantDescs.CR_String,
                ConstantDescs.CR_MethodType,
                ConstantDescs.CR_String,
                ConstantDescs.CR_Object.array()
        );
        ConstantMethodHandleDesc mh = MethodHandleDesc.of(MethodHandleDesc.Kind.STATIC, ClassDesc.ofDescriptor("Ljava/lang/invoke/StringConcatFactory;"),
                                                          "makeConcatWithConstants", methodTypeForMethodHandle);
        MethodTypeDesc methodTypeForIndy = MethodTypeDesc.of(
                ConstantDescs.CR_String,
                ConstantDescs.CR_String,
                ConstantDescs.CR_String
        );
        final String param = "" + '\u0001' + '\u0001';
        DynamicCallSiteDesc indyDescr = DynamicCallSiteDesc.of(mh, invokeName, methodTypeForIndy, param);
        // invokeName is not a constant
        String indyRes = (String)invokedynamic(indyDescr, x, y);
        indyDescr = null; // not effectively final
    }
}
