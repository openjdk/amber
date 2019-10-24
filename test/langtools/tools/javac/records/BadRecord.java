/*
 * @test /nodynamiccopyright/
 * @summary Verifying error recovery for broken record classes
 * @compile/fail/ref=BadRecord.out --enable-preview -source ${jdk.version} -XDrawDiagnostics BadRecord.java
 */
record BadRecord001 {}

record BadRecord0022( {}

record BadRecord0033(int {}

record BadRecord0044(int) {}

record BadRecord005(int i {}

record BadRecord006(int i, {}

record BadRecord007(int i,) {}

record BadRecord008(int i, int {}

record BadRecord009(int i, int) {}

record BadRecord010(int i, int j {}

record BadRecord011(int i, int j, {}

record BadRecord012(int i, int j,) {}

record BadRecord013;

record BadRecord014(;

record BadRecord015(int;

record BadRecord016(int);

record BadRecord017(int i;

record BadRecord018(int i,;

record BadRecord019(int i,);

record BadRecord020(int i, int;

record BadRecord021(int i, int);

record BadRecord022(int i, int j;

record BadRecord023(int i, int j,;

record BadRecord024(int i, int j,);

record BadRecord025(int x)

record BadRecord026 {}

record BadRecord027(final int x) { }

record BadRecord028(private int x) { }

record BadRecord029(public int x) { }

record BadRecord030(volatile int x) { }

record BadRecord030(int x) {
    private int x() { return x; }
}

record R(int x) {
    public int x;
}
