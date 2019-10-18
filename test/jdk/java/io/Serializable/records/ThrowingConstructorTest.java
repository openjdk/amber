/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @summary Tests constructor invocation exceptions are handled appropriately
 * @compile --enable-preview -source 14 ThrowingConstructorTest.java
 * @run testng/othervm --enable-preview ThrowingConstructorTest
 * @run testng/othervm/java.security.policy=empty_security.policy --enable-preview ThrowingConstructorTest
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.System.out;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

/**
 * If the constructor invocation throws an exception, an
 * `InvalidObjectException` is thrown with that exception as its cause.
 */
public class ThrowingConstructorTest {

    record R1 () implements Serializable {
        public R1() { throw new NullPointerException("thrown from R1"); } // canonical
        public R1(String s) { }  // for test construction
    }

    record R2 (int x) implements Serializable {
        public R2(int x) { throw new IllegalArgumentException("thrown from R2"); } // canonical
        public R2(String s) { x = 1;}  // for test construction
    }

    record R3 (int x, int y) implements Serializable {
        public R3(int x, int y) { throw new NumberFormatException("thrown from R3"); } // canonical
        public R3(String s) { x = 2; y = 3; }  // for test construction
    }

    static class C implements Serializable {
        final Object obj ;
        C(Object obj) { this.obj= obj; }
        @Override public String toString() { return "C[" + obj + "]"; }
    }

    static final Class<InvalidObjectException> IOE = InvalidObjectException.class;

    @DataProvider(name = "exceptionInstances")
    public Object[][] exceptionInstances() {
        return new Object[][] {
            new Object[] { new R1("s"),        NullPointerException.class,     "thrown from R1" },
            new Object[] { new R2("s"),        IllegalArgumentException.class, "thrown from R2" },
            new Object[] { new R3("s"),        NumberFormatException .class,   "thrown from R3" },
            new Object[] { new C(new R1("s")), NullPointerException.class,     "thrown from R1" },
            new Object[] { new C(new R2("s")), IllegalArgumentException.class, "thrown from R2" },
            new Object[] { new C(new R3("s")), NumberFormatException .class,   "thrown from R3" },
        };
    }

    @Test(dataProvider = "exceptionInstances")
    public void testExceptions(Object objectToSerialize,
                               Class<? extends Throwable> expectedExType,
                               String expectedExMessage)
        throws Exception
    {
        out.println("\n---");
        out.println("serializing: " + objectToSerialize);
        byte[] bytes = serialize(objectToSerialize);
        InvalidObjectException ioe = expectThrows(IOE, () -> deserialize(bytes));
        out.println("caught expected IOE: " + ioe);
        Throwable t = ioe.getCause();
        assertTrue(t.getClass().equals(expectedExType),
                   "Expected:" + expectedExType + ", got:" + t);
        out.println("expected cause " + expectedExType +" : " + t);
        assertEquals(t.getMessage(), expectedExMessage);
    }

    //  -- errors ( pass through unwrapped )

    record R4 () implements Serializable {
        public R4() { throw new OutOfMemoryError("thrown from R4"); } // canonical
        public R4(String s) { }  // for test construction
    }

    record R5 (int x) implements Serializable {
        public R5(int x) { throw new StackOverflowError("thrown from R5"); } // canonical
        public R5(String s) { x = 1;}  // for test construction
    }

    record R6 (int x, int y) implements Serializable {
        public R6(int x, int y) { throw new AssertionError("thrown from R6"); } // canonical
        public R6(String s) { x = 2; y = 3; }  // for test construction
    }

    @DataProvider(name = "errorInstances")
    public Object[][] errorInstances() {
        return new Object[][] {
            new Object[] { new R4("s"),        OutOfMemoryError.class,   "thrown from R4" },
            new Object[] { new R5("s"),        StackOverflowError.class, "thrown from R5" },
            new Object[] { new R6("s"),        AssertionError .class,    "thrown from R6" },
            new Object[] { new C(new R4("s")), OutOfMemoryError.class,   "thrown from R4" },
            new Object[] { new C(new R5("s")), StackOverflowError.class, "thrown from R5" },
            new Object[] { new C(new R6("s")), AssertionError .class,    "thrown from R6" },
        };
    }

    @Test(dataProvider = "errorInstances")
    public void testErrors(Object objectToSerialize,
                           Class<? extends Throwable> expectedExType,
                           String expectedExMessage)
        throws Exception
    {
        out.println("\n---");
        out.println("serializing: " + objectToSerialize);
        byte[] bytes = serialize(objectToSerialize);
        Throwable t = expectThrows(expectedExType, () -> deserialize(bytes));
        assertTrue(t.getClass().equals(expectedExType),
                   "Expected:" + expectedExType + ", got:" + t);
        out.println("caught expected " + expectedExType +" : " + t);
        assertEquals(t.getMessage(), expectedExMessage);
    }

    // --- infra

    static <T> byte[] serialize(T obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.close();
        return baos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    static <T> T deserialize(byte[] streamBytes)
        throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(streamBytes);
        ObjectInputStream ois  = new ObjectInputStream(bais);
        return (T) ois.readObject();
    }

    static <T> T serializeDeserialize(T obj)
        throws IOException, ClassNotFoundException
    {
        return deserialize(serialize(obj));
    }
}
