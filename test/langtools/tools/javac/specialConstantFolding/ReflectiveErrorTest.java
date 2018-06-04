/*
 * @test /nodynamiccopyright/
 * @summary testing message for reflective error
 * @compile/fail/ref=ReflectiveErrorTest.out -XDrawDiagnostics ReflectiveErrorTest.java
 */

import java.lang.constant.*;

public class ReflectiveErrorTest {
    // trying to use an erroneous descriptor
    final MethodTypeDesc mt = MethodTypeDesc.ofDescriptor("(Ljava/lang/String;^)D");
}
