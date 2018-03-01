/*
 * @test /nodynamiccopyright/
 * @summary the VM is failing with linkage error if invocationName is an empty string
 * @compile/fail/ref=IndyLinkageErrorTest.out -XDdoConstantFold -XDrawDiagnostics IndyLinkageErrorTest.java
 */

import java.lang.sym.*;
import static java.lang.invoke.Intrinsics.*;

public class IndyLinkageErrorTest {
    String test(String x, String y) throws Throwable {
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
        DynamicCallSiteRef indyDescr = DynamicCallSiteRef.of(mh, "", methodTypeForIndy, param);
        return (String)invokedynamic(indyDescr, x, y);
    }
}
