/* /nodynamiccopyright/ */

import java.lang.constant.ClassDesc;

import static java.lang.invoke.Intrinsics.*;

public class InstanceTrackableMethodsTest extends ConstantFoldingTest {
    public static void main(String[] args) throws Throwable {
        new InstanceTrackableMethodsTest().run();
    }

    void run() throws Throwable {
        test1();
    }

    void test1() {
        ClassDesc string1 = ClassDesc.ofDescriptor("Ljava/lang/String;");
        ClassDesc stringArr = string1.arrayType();
        ClassDesc string2 = stringArr.componentType();
        ClassDesc string3 = ClassDesc.ofDescriptor(string2.descriptorString());
        check(string1.descriptorString().equals(string2.descriptorString()));
        check(string1.descriptorString().equals(string3.descriptorString()));
        check(string2.descriptorString().equals(string3.descriptorString()));
    }

    @InstructionInfo(bytecodePosition=0, values={"CONSTANT_Class_info", "[Ljava/lang/String;"})
    @InstructionInfo(bytecodePosition=3, values={"CONSTANT_Class_info", "[Ljava/lang/String;"})
    void test2() {
        ClassDesc stringClass = ClassDesc.ofDescriptor("Ljava/lang/String;");
        Class<?> stringArrClass = ldc(stringClass.arrayType());
        ClassDesc stringArrConst = stringClass.arrayType();
        Class<?> stringArrClass2 = ldc(stringArrConst);
    }
}
