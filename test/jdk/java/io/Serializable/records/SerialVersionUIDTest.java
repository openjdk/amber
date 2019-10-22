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
 * @summary Basic tests for SUID in the serial stream
 * @compile --enable-preview -source 14 SerialVersionUIDTest.java
 * @run testng/othervm --enable-preview SerialVersionUIDTest
 * @run testng/othervm/java.security.policy=empty_security.policy --enable-preview SerialVersionUIDTest
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.io.ObjectStreamConstants.STREAM_MAGIC;
import static java.io.ObjectStreamConstants.STREAM_VERSION;
import static java.io.ObjectStreamConstants.TC_CLASSDESC;
import static java.io.ObjectStreamConstants.TC_OBJECT;
import static java.lang.System.out;
import static org.testng.Assert.assertEquals;

public class SerialVersionUIDTest {

    record R1 () implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    record R2 (int x, int y) implements Serializable {
        private static final long serialVersionUID = 0L;
    }

    record R3 () implements Serializable { }

    record R4 (String s) implements Serializable { }

    record R5 (long l) implements Serializable {
        private static final long serialVersionUID = 5678L;
    }

    @DataProvider(name = "recordObjects")
    public Object[][] recordObjects() {
        return new Object[][] {
            new Object[] { new R1(),        1L    },
            new Object[] { new R2(1, 2),    0L    },
            new Object[] { new R3(),        0L    },
            new Object[] { new R4("s"),     0L    },
            new Object[] { new R5(7L),      5678L },
        };
    }

    @Test(dataProvider = "recordObjects")
    public void testSerialize(Object objectToSerialize, long expectedUID)
        throws Exception
    {
        out.println("\n---");
        out.println("serializing : " + objectToSerialize);
        byte[] bytes = serialize(objectToSerialize);

        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        DataInputStream dis = new DataInputStream(bais);

        // sanity
        assertEquals(dis.readShort(), STREAM_MAGIC);
        assertEquals(dis.readShort(), STREAM_VERSION);
        assertEquals(dis.readByte(), TC_OBJECT);
        assertEquals(dis.readByte(), TC_CLASSDESC);
        assertEquals(dis.readUTF(), objectToSerialize.getClass().getName());

        // verify that the UID is as expected
        assertEquals(dis.readLong(), expectedUID);
    }

    // --- infra

    static <T> byte[] serialize(T obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.close();
        return baos.toByteArray();
    }
}
