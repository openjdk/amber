/*
 * @test /nodynamiccopyright/
 * @summary records cannot explicitly declare hashCode or equals
 * @compile RecordsCanReDeclareMembersTest.java
 */

public record RecordsCanReDeclareMembersTest(int i) {
    @Override
    public int hashCode() { return 0; }
    @Override
    public boolean equals(Object o) { return true; }
    @Override
    public String toString() { return ""; } // toString() is OK

    public int i() { return i; }
}
