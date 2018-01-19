/*
 * @test /nodynamiccopyright/
 * @summary adding indy negative test
 * @compile/fail/ref=IndyNegativeTest01.out -XDdoConstantFold -XDrawDiagnostics IndyNegativeTest01.java
 */

import java.lang.sym.*;

import static java.lang.invoke.Intrinsics.*;

public class IndyNegativeTest01 {
    void test(String invokeName, String x, String y) throws Throwable {
        MethodTypeRef methodTypeForMethodHandle = MethodTypeRef.of(
                SymbolicRefs.CR_CallSite,
                SymbolicRefs.CR_Lookup,
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
        IndyRef indyDescr = IndyRef.of(mh, invokeName, methodTypeForIndy, param);
        // invokeName is not a constant
        String indyRes = (String)invokedynamic(indyDescr, x, y);
        indyDescr = null; // not effectively final
    }
}
