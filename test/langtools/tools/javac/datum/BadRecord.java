/*
 * @test /nodynamiccopyright/
 * @summary Verifying error recovery for broken record classes
 * @compile/fail/ref=BadRecord.out -XDrawDiagnostics BadRecord.java
 */
record BadRecordb1 {}

record BadRecordb2( {}

record BadRecordb3(int {}

record BadRecordb4(int) {}

record BadRecordb5(int i {}

record BadRecordb6(int i, {}

record BadRecordb7(int i,) {}

record BadRecordb8(int i, int {}

record BadRecordb9(int i, int) {}

record BadRecordba(int i, int j {}

record BadRecordbb(int i, int j, {}

record BadRecordbc(int i, int j,) {}

record BadRecords1;

record BadRecords2(;

record BadRecords3(int;

record BadRecords4(int);

record BadRecords5(int i;

record BadRecords6(int i,;

record BadRecords7(int i,);

record BadRecords8(int i, int;

record BadRecords9(int i, int);

record BadRecordsa(int i, int j;

record BadRecordsb(int i, int j,;

record BadRecordsc(int i, int j,);
