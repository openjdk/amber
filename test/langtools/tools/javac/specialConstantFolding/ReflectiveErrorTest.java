/*
 * @test /nodynamiccopyright/
 * @summary testing message for reflective error
 * @compile/fail/ref=ReflectiveErrorTest.out -XDdoConstantFold -XDrawDiagnostics ReflectiveErrorTest.java
 */

import java.lang.sym.*;

public class ReflectiveErrorTest {
    // trying to use an erroneous descriptor
    final MethodTypeRef mt = MethodTypeRef.ofDescriptor("(Ljava/lang/String;^)D");
}
