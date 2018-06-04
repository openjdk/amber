/*
 * @test /nodynamiccopyright/
 * @summary report incorrect method handles for indy
 * @ignore
 * @compile/fail/ref=ReportIncorrectMHForIndyTest.out -XDrawDiagnostics ReportIncorrectMHForIndyTest.java
 */

import java.lang.invoke.*;
import java.lang.constant.BootstrapSpecifier;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;

public class ReportIncorrectMHForIndyTest {

    public static CallSite makeConcatWithConstants(String s,
                                                   String name,
                                                   MethodType concatType,
                                                   String recipe,
                                                   Object... constants) { return null; }

    String test(String x, String y) throws Throwable {
        // correct method type
        MethodTypeDesc methodTypeForMethodHandle = MethodTypeDesc.of(
                ConstantDescs.CR_CallSite,
                ConstantDescs.CR_String,  // bad argument must be of type MethodHandles.Lookup
                ConstantDescs.CR_String,
                ConstantDescs.CR_MethodType,
                ConstantDescs.CR_String,
                ConstantDescs.CR_Object.array()
        );
        MethodHandleDesc mh = MethodHandleDesc.of(MethodHandleDesc.Kind.STATIC, ClassDesc.ofDescriptor("LReportIncorrectMHForIndyTest;"),
                                                  "makeConcatWithConstants", methodTypeForMethodHandle);
        // should this call fail if the mh is incorrect?
        BootstrapSpecifier indyDescr = BootstrapSpecifier.of(mh);
        return "";
    }
}
