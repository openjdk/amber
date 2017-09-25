/* /nodynamiccopyright/ */

import java.io.Serializable;
import java.lang.invoke.*;

import static java.lang.invoke.Intrinsics.*;

/** This test checks that for all public static factories, F, of class
 *  java.lang.invoke.MethodType with MethodType as its return type:
 *
 *  if fi is one of this factories and all of its arguments are constants,
 *  the result is a constant which is loaded using an LDC instruction in the
 *  obtained bytecode.
 */
@SkipExecution
public class ConstantFoldingOfMethodTypeDiffContextsTest extends ConstantFoldingTest {
    void foo(MethodType m) {}

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodType_info", "()Ljava/lang/String;"})
    @InstructionInfo(bytecodePosition=3, values={"CONSTANT_MethodType_info", "(I)Ljava/lang/String;"})
    @InstructionInfo(bytecodePosition=6, values={"CONSTANT_MethodType_info", "(Ljava/lang/String;I)V"})
//    @InstructionInfo(bytecodePosition=9, values={"CONSTANT_MethodType_info", "(I)V"})
    @InstructionInfo(bytecodePosition=9, values={"CONSTANT_MethodType_info", "()Ljava/lang/String;"})
    void assignmentContext() {
        MethodType mt1 = ldc(MethodTypeRef.of(ClassRef.CR_String));
        MethodType mt2 = ldc(MethodTypeRef.of(ClassRef.CR_String, ClassRef.CR_int));
        MethodType mt3 = ldc(MethodTypeRef.of(ClassRef.ofVoid(), ClassRef.CR_String, ClassRef.CR_int));
//        MethodType mt4 = ldc(MethodTypeRef.of(VOID, MethodTypeRef.of(ClassRef.of("Ljava/lang/String;"), INT)));
        /*  if last argument of the method below is not null no effort is done to try to constant fold
         *  the method type
         */
        MethodType mt5 = ldc(MethodTypeRef.ofDescriptor("()Ljava/lang/String;"));
    }

    @InstructionInfo(bytecodePosition=1, values={"CONSTANT_MethodType_info", "()Ljava/lang/String;"})
    @InstructionInfo(bytecodePosition=10, values={"CONSTANT_MethodType_info", "(I)Ljava/lang/String;"})
    @InstructionInfo(bytecodePosition=19, values={"CONSTANT_MethodType_info", "(Ljava/lang/String;I)V"})
//    @InstructionInfo(bytecodePosition=28, values={"CONSTANT_MethodType_info", "(I)V"})
    @InstructionInfo(bytecodePosition=28, values={"CONSTANT_MethodType_info", "()Ljava/lang/String;"})
    void invocationContext1() {
        foo(ldc(MethodTypeRef.of(ClassRef.CR_String)));
        foo(ldc(MethodTypeRef.of(ClassRef.CR_String, ClassRef.CR_int)));
        foo(ldc(MethodTypeRef.of(ClassRef.ofVoid(), ClassRef.CR_String, ClassRef.CR_int)));
//        foo(ldc(MethodTypeRef.of(VOID, MethodTypeRef.of(ClassRef.of("Ljava/lang/String;"), INT))));
        /*  if last argument of the method below is not null no effort is done to try to constant fold
         *  the method type
         */
        foo(ldc(MethodTypeRef.ofDescriptor("()Ljava/lang/String;")));
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodType_info", "()Ljava/lang/String;"})
    @InstructionInfo(bytecodePosition=8, values={"CONSTANT_MethodType_info", "(I)Ljava/lang/String;"})
    @InstructionInfo(bytecodePosition=16, values={"CONSTANT_MethodType_info", "(Ljava/lang/String;I)V"})
//    @InstructionInfo(bytecodePosition=24, values={"CONSTANT_MethodType_info", "(I)V"})
    @InstructionInfo(bytecodePosition=24, values={"CONSTANT_MethodType_info", "()Ljava/lang/String;"})
    void invocationContext2() {
        MethodType mt1 = ldc(MethodTypeRef.of(ClassRef.CR_String));
        foo(mt1);
        MethodType mt2 = ldc(MethodTypeRef.of(ClassRef.CR_String, ClassRef.CR_int));
        foo(mt2);
        MethodType mt3 = ldc(MethodTypeRef.of(ClassRef.ofVoid(), ClassRef.CR_String, ClassRef.CR_int));
        foo(mt3);
//        MethodType mt4 = ldc(MethodTypeRef.of(VOID, MethodTypeRef.of(ClassRef.of("Ljava/lang/String;"), INT)));
//        foo(mt4);
        /*  if last argument of the method below is not null no effort is done to try to constant fold
         *  the method type
         */
        MethodType mt5 = ldc(MethodTypeRef.ofDescriptor("()Ljava/lang/String;"));
        foo(mt5);
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodType_info", "()Ljava/lang/String;"})
    @InstructionInfo(bytecodePosition=6, values={"CONSTANT_MethodType_info", "(I)Ljava/lang/String;"})
    @InstructionInfo(bytecodePosition=12, values={"CONSTANT_MethodType_info", "(Ljava/lang/String;I)V"})
//    @InstructionInfo(bytecodePosition=18, values={"CONSTANT_MethodType_info", "(I)V"})
    @InstructionInfo(bytecodePosition=18, values={"CONSTANT_MethodType_info", "()Ljava/lang/String;"})
    void castContext1() {
        Serializable s1 = (Serializable)ldc(MethodTypeRef.of(ClassRef.CR_String));
        Serializable s2 = (Serializable)ldc(MethodTypeRef.of(ClassRef.CR_String, ClassRef.CR_int));
        Serializable s3 = (Serializable)ldc(MethodTypeRef.of(ClassRef.ofVoid(), ClassRef.CR_String, ClassRef.CR_int));
//        Serializable s4 = (Serializable)ldc(MethodTypeRef.of(VOID, MethodTypeRef.of(ClassRef.of("Ljava/lang/String;"), INT)));
        Serializable s5 = (Serializable)ldc(MethodTypeRef.ofDescriptor("()Ljava/lang/String;"));
    }


    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_MethodType_info", "()Ljava/lang/String;"})
    @InstructionInfo(bytecodePosition=5, values={"CONSTANT_MethodType_info", "(I)Ljava/lang/String;"})
    @InstructionInfo(bytecodePosition=11, values={"CONSTANT_MethodType_info", "(Ljava/lang/String;I)V"})
//    @InstructionInfo(bytecodePosition=19, values={"CONSTANT_MethodType_info", "(I)V"})
    @InstructionInfo(bytecodePosition=19, values={"CONSTANT_MethodType_info", "()Ljava/lang/String;"})
    void castContext2() {
        MethodType m1 = ldc(MethodTypeRef.of(ClassRef.CR_String));
        Serializable s1 = (Serializable)m1;
        MethodType m2 = ldc(MethodTypeRef.of(ClassRef.CR_String, ClassRef.CR_int));
        Serializable s2 = (Serializable)m2;
        MethodType m3 = ldc(MethodTypeRef.of(ClassRef.ofVoid(), ClassRef.CR_String, ClassRef.CR_int));
        Serializable s3 = (Serializable)m3;
//        MethodType m4 = ldc(MethodTypeRef.of(VOID, MethodTypeRef.of(ClassRef.of("Ljava/lang/String;"), INT)));
//        Serializable s4 = (Serializable)m4;
        MethodType m5 = ldc(MethodTypeRef.ofDescriptor("()Ljava/lang/String;"));
        Serializable s5 = (Serializable)m5;
    }

    @InstructionInfo(bytecodePosition=1, values={"CONSTANT_MethodType_info", "()Ljava/lang/String;"})
    @InstructionInfo(bytecodePosition=10, values={"CONSTANT_MethodType_info", "(I)Ljava/lang/String;"})
    @InstructionInfo(bytecodePosition=19, values={"CONSTANT_MethodType_info", "(Ljava/lang/String;I)V"})
//    @InstructionInfo(bytecodePosition=28, values={"CONSTANT_MethodType_info", "(I)V"})
    @InstructionInfo(bytecodePosition=28, values={"CONSTANT_MethodType_info", "()Ljava/lang/String;"})
    void cast_plus_invocationContext() {
        foo((MethodType)ldc(MethodTypeRef.of(ClassRef.CR_String)));
        foo((MethodType)ldc(MethodTypeRef.of(ClassRef.CR_String, ClassRef.CR_int)));
        foo((MethodType)ldc(MethodTypeRef.of(ClassRef.ofVoid(), ClassRef.CR_String, ClassRef.CR_int)));
//        foo((MethodType)ldc(MethodTypeRef.of(VOID, MethodTypeRef.of(ClassRef.of("Ljava/lang/String;"), INT))));
        /*  if last argument of the method below is not null no effort is done to try to constant fold
         *  the method type
         */
        foo((MethodType)ldc(MethodTypeRef.ofDescriptor("()Ljava/lang/String;")));
    }
}
