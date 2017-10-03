/* /nodynamiccopyright/ */

import java.lang.invoke.*;

@SkipExecution
@InstructionInfo(bytecodePosition=0, instructionCode=ConstantFoldingTest.GETSTATICOpCode)
@InstructionInfo(bytecodePosition=3, instructionCode=ConstantFoldingTest.PUTSTATICOpCode)
public class ConstantsUser extends ConstantFoldingTest {
    @InstructionInfo(bytecodePosition=1, instructionCode=GETFIELDOpCode)
    @InstructionInfo(bytecodePosition=4, instructionCode=ASTORE_2OpCode)
    void test(ConstantDefinitions cd) {
        final MethodType local1 = cd.mtInstance;
    }

    static final MethodType mtcc = ConstantDefinitions.mtStatic;
}
