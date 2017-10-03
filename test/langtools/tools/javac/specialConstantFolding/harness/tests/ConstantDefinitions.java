/* /nodynamiccopyright/ */

import java.lang.invoke.*;

import static java.lang.invoke.Intrinsics.*;

@IgnoreTest
public class ConstantDefinitions {
    public static final MethodType mtStatic = ldc(MethodTypeRef.of(ClassRef.CR_String));
    public final MethodType mtInstance = ldc(MethodTypeRef.of(ClassRef.CR_String));
}
