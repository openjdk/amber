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
                SymbolicRefs.CR_CallSite,
                SymbolicRefs.CR_MethodHandles_Lookup,
                SymbolicRefs.CR_String,
                SymbolicRefs.CR_MethodType,
                SymbolicRefs.CR_String,
                SymbolicRefs.CR_Object.array()
        );
        MethodHandleRef mh = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, ClassRef.ofDescriptor("Ljava/lang/invoke/StringConcatFactory;"),
                                                "makeConcatWithConstants", methodTypeForMethodHandle);
        MethodTypeRef methodTypeForIndy = MethodTypeRef.of(
                SymbolicRefs.CR_String,
                SymbolicRefs.CR_String,
                SymbolicRefs.CR_String
        );
        final String param = "" + '\u0001' + '\u0001';
        DynamicCallSiteRef indyDescr = DynamicCallSiteRef.of(mh, "", methodTypeForIndy, param);
        return (String)invokedynamic(indyDescr, x, y);
    }
}
