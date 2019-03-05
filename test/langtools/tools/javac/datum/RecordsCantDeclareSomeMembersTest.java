/*
 * @test /nodynamiccopyright/
 * @summary records cannot explicitly declare hashCode or equals
 * @compile/fail/ref=RecordsCantDeclareSomeMembersTest.out -XDrawDiagnostics RecordsCantDeclareSomeMembersTest.java
 */

public record RecordsCantDeclareSomeMembersTest(int i) {
    @Override
    public int hashCode() { return 0; }
    @Override
    public boolean equals(Object o) { return true; }
    @Override
    public String toString() { return ""; } // toString() is OK
}
