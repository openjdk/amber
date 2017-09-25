/*
 * @test /nodynamiccopyright/
 * @summary report incorrect method handles for indy
 * @ignore
 * @compile/fail/ref=ReportIncorrectMHForIndyTest.out -XDdoConstantFold -XDrawDiagnostics ReportIncorrectMHForIndyTest.java
 */

import java.lang.invoke.*;

public class ReportIncorrectMHForIndyTest {

    public static CallSite makeConcatWithConstants(String s,
                                                   String name,
                                                   MethodType concatType,
                                                   String recipe,
                                                   Object... constants) { return null; }

    String test(String x, String y) throws Throwable {
        // correct method type
        MethodTypeRef methodTypeForMethodHandle = MethodTypeRef.of(
                ClassRef.CR_CallSite,
                ClassRef.CR_String,  // bad argument must be of type MethodHandles.Lookup
                ClassRef.CR_String,
                ClassRef.CR_MethodType,
                ClassRef.CR_String,
                ClassRef.CR_Object.array()
        );
        MethodHandleRef mh = MethodHandleRef.ofStatic(ClassRef.ofDescriptor("LReportIncorrectMHForIndyTest;"),
                "makeConcatWithConstants", methodTypeForMethodHandle);
        // should this call fail if the mh is incorrect?
        BootstrapSpecifier indyDescr = BootstrapSpecifier.of(mh);
        return "";
    }
}
