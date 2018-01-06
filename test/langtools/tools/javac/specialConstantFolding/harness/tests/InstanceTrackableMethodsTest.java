/* /nodynamiccopyright/ */

import java.lang.sym.ClassRef;

import static java.lang.invoke.Intrinsics.*;

public class InstanceTrackableMethodsTest extends ConstantFoldingTest {
    public static void main(String[] args) throws Throwable {
        new InstanceTrackableMethodsTest().run();
    }

    void run() throws Throwable {
        test1();
    }

    void test1() {
        ClassRef string1 = ClassRef.ofDescriptor("Ljava/lang/String;");
        ClassRef stringArr = string1.array();
        ClassRef string2 = stringArr.componentType();
        ClassRef string3 = ClassRef.ofDescriptor(string2.descriptorString());
        check(string1.descriptorString().equals(string2.descriptorString()));
        check(string1.descriptorString().equals(string3.descriptorString()));
        check(string2.descriptorString().equals(string3.descriptorString()));
    }

    @InstructionInfo(bytecodePosition=6, values={"CONSTANT_Class_info", "[Ljava/lang/String;"})
    @InstructionInfo(bytecodePosition=14, values={"CONSTANT_Class_info", "[Ljava/lang/String;"})
    void test2() {
        ClassRef stringClass = ClassRef.ofDescriptor("Ljava/lang/String;");
        Class<?> stringArrClass = ldc(stringClass.array());
        ClassRef stringArrConst = stringClass.array();
        Class<?> stringArrClass2 = ldc(stringArrConst);
    }
}
