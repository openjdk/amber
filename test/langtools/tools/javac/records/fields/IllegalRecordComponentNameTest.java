/*
 * @test /nodynamiccopyright/
 * @summary dont accept illegal record component names
 * @compile/fail/ref=IllegalRecordComponentNameTest.out -XDrawDiagnostics IllegalRecordComponentNameTest.java
 */

public class IllegalRecordComponentNameTest {
    record R1(String toString) {}
    record R2(String hashCode) {}
    record R3(String getClass) {}
    record R4(String readObjectNoData) {}
    record R5(String readResolve) {}
    record R6(String writeReplace) {}
    record R7(String serialPersistentFields) {}
}
