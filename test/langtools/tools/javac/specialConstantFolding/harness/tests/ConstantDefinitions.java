/* /nodynamiccopyright/ */

import java.lang.invoke.*;
import java.lang.sym.MethodTypeRef;
import java.lang.sym.ConstantRefs;

import static java.lang.invoke.Intrinsics.*;

@IgnoreTest
public class ConstantDefinitions {
    public static final MethodType mtStatic = ldc(MethodTypeRef.of(ConstantRefs.CR_String));
    public final MethodType mtInstance = ldc(MethodTypeRef.of(ConstantRefs.CR_String));
}
