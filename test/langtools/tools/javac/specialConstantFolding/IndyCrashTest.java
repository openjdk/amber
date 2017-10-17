/*
 * @test /nodynamiccopyright/
 * @summary javac is crashing for incorrect indy
 * @compile/fail/ref=IndyCrashTest.out -XDdoConstantFold -XDrawDiagnostics IndyCrashTest.java
 */

import java.lang.invoke.*;

public class IndyCrashTest {
    static final ClassRef HELPER_CLASS = ClassRef.ofDescriptor("LIndyCrashTest$IntrinsicTestHelper;");

    static class IntrinsicTestHelper {
        public static int sf;
        public int f;

        public static CallSite simpleBSM(MethodHandles.Lookup lookup,
                                         String invocationName,
                                         MethodType invocationType) {
            return new ConstantCallSite(MethodHandles.constant(String.class, invocationName));
        }
    }

    public void testSimpleIndy() throws Throwable {
        MethodHandleRef bsmMH = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, HELPER_CLASS, "foo", "()Ljava/lang/invoke/CallSite;");
        BootstrapSpecifier bsm = BootstrapSpecifier.of(bsmMH);
        String result = (String)Intrinsics.invokedynamic(bsm, "foo", MethodTypeRef.ofDescriptor("()Ljava/lang/String;"));
    }
}
