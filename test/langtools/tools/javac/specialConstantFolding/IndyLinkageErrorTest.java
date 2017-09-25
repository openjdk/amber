/*
 * @test /nodynamiccopyright/
 * @summary the VM is failing with linkage error if invocationName is an empty string
 * @compile/fail/ref=IndyLinkageErrorTest.out -XDdoConstantFold -XDrawDiagnostics IndyLinkageErrorTest.java
 */

import java.lang.invoke.*;

import static java.lang.invoke.Intrinsics.*;

public class IndyLinkageErrorTest {
    String test(String x, String y) throws Throwable {
        MethodTypeRef methodTypeForMethodHandle = MethodTypeRef.of(
            ClassRef.CR_CallSite,
            ClassRef.CR_Lookup,
            ClassRef.CR_String,
            ClassRef.CR_MethodType,
            ClassRef.CR_String,
            ClassRef.CR_Object.array()
        );
        MethodHandleRef mh = MethodHandleRef.ofStatic(ClassRef.ofDescriptor("Ljava/lang/invoke/StringConcatFactory;"),
                "makeConcatWithConstants", methodTypeForMethodHandle);
        final String param = "" + '\u0001' + '\u0001';
        BootstrapSpecifier indyDescr = BootstrapSpecifier.of(mh, param);
        return (String)invokedynamic(indyDescr, "", x, y);
    }
}
