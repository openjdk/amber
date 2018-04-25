/*
 * @test /nodynamiccopyright/
 * @summary testing message for reflective error
 * @compile/fail/ref=ReflectiveErrorTest.out -XDrawDiagnostics ReflectiveErrorTest.java
 */

import java.lang.invoke.constant.*;

public class ReflectiveErrorTest {
    // trying to use an erroneous descriptor
    final MethodTypeDesc mt = MethodTypeDesc.ofDescriptor("(Ljava/lang/String;^)D");
}
