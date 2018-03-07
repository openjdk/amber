/*
 * @test /nodynamiccopyright/
 * @summary adding several ldc negative tests
 * @compile/fail/ref=LDCNegativeTest.out -XDdoConstantFold -XDrawDiagnostics LDCNegativeTest.java
 */

import java.lang.invoke.constant.*;

import static java.lang.invoke.Intrinsics.*;

public class LDCNegativeTest {
    public String y = "I";

    public String x() { return ""; }
    public ClassRef c() { return null; }
    public ClassRef d = ConstantRefs.CR_int;

    void foo() {
        // all these fail
        String z = "I";
        ldc(c());
        ldc(d);
        ldc(ClassRef.ofDescriptor(y));
        ldc(ClassRef.ofDescriptor(z));
        ldc(ClassRef.ofDescriptor(x()));
        ldc(MethodTypeRef.ofDescriptor("()V").changeReturnType(ClassRef.ofDescriptor(y)));
        z = "Z";
    }
}
