/*
 * @test /nodynamiccopyright/
 * @summary report incorrect method handles for indy
 * @ignore
 * @compile/fail/ref=ReportIncorrectMHForIndyTest.out -XDdoConstantFold -XDrawDiagnostics ReportIncorrectMHForIndyTest.java
 */

import java.lang.invoke.*;
import java.lang.sym.BootstrapSpecifier;
import java.lang.sym.ClassRef;
import java.lang.sym.MethodHandleRef;
import java.lang.sym.MethodTypeRef;
import java.lang.sym.SymbolicRefs;

public class ReportIncorrectMHForIndyTest {

    public static CallSite makeConcatWithConstants(String s,
                                                   String name,
                                                   MethodType concatType,
                                                   String recipe,
                                                   Object... constants) { return null; }

    String test(String x, String y) throws Throwable {
        // correct method type
        MethodTypeRef methodTypeForMethodHandle = MethodTypeRef.of(
                SymbolicRefs.CR_CallSite,
                SymbolicRefs.CR_String,  // bad argument must be of type MethodHandles.Lookup
                SymbolicRefs.CR_String,
                SymbolicRefs.CR_MethodType,
                SymbolicRefs.CR_String,
                SymbolicRefs.CR_Object.array()
        );
        MethodHandleRef mh = MethodHandleRef.of(MethodHandleRef.Kind.STATIC, ClassRef.ofDescriptor("LReportIncorrectMHForIndyTest;"),
                                                "makeConcatWithConstants", methodTypeForMethodHandle);
        // should this call fail if the mh is incorrect?
        BootstrapSpecifier indyDescr = BootstrapSpecifier.of(mh);
        return "";
    }
}
