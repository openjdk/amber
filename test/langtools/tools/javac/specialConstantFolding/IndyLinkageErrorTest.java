/*
 * @test /nodynamiccopyright/
 * @summary the VM is failing with linkage error if invocationName is an empty string
 * @compile/fail/ref=IndyLinkageErrorTest.out -XDdoConstantFold -XDrawDiagnostics IndyLinkageErrorTest.java
 */

import java.lang.invoke.constant.*;
import static java.lang.invoke.Intrinsics.*;

public class IndyLinkageErrorTest {
    String test(String x, String y) throws Throwable {
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
        DynamicCallSiteDesc indyDescr = DynamicCallSiteDesc.of(mh, "", methodTypeForIndy, param);
        return (String)invokedynamic(indyDescr, x, y);
    }
}
