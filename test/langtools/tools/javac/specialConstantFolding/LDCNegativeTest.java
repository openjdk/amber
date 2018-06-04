/*
 * @test /nodynamiccopyright/
 * @summary adding several ldc negative tests
 * @compile/fail/ref=LDCNegativeTest.out -XDrawDiagnostics LDCNegativeTest.java
 */

import java.lang.constant.*;

import static java.lang.invoke.Intrinsics.*;

public class LDCNegativeTest {
    public String y = "I";

    public String x() { return ""; }
    public ClassDesc c() { return null; }
    public ClassDesc d = ConstantDescs.CR_int;

    void foo() {
        // all these fail
        String z = "I";
        ldc(c());
        ldc(d);
        ldc(ClassDesc.ofDescriptor(y));
        ldc(ClassDesc.ofDescriptor(z));
        ldc(ClassDesc.ofDescriptor(x()));
        ldc(MethodTypeDesc.ofDescriptor("()V").changeReturnType(ClassDesc.ofDescriptor(y)));
        z = "Z";
    }
}
