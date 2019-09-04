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
 * @summary reflection test for records
 * @compile --enable-preview -source 14 RecordReflectionTest.java
 * @run testng/othervm --enable-preview RecordReflectionTest
 */

import java.lang.reflect.*;
import java.util.List;

import org.testng.annotations.*;
import static org.testng.Assert.*;

@Test
public class RecordReflectionTest {

    class NoRecord {}

    record R1() {}

    record R2(int i, int j) {}

    record R3(List<String> ls) {}

    public void testIsRecord() {
        assertFalse(NoRecord.class.isRecord());

        for (Class<?> c : List.of(R1.class, R2.class, R3.class))
            assertTrue(c.isRecord());
    }

    public void testGetComponentsNoRecord() throws ReflectiveOperationException {
        assertTrue(NoRecord.class.getRecordAccessors().length == 0);
    }

    public void testRecordAccessors() throws ReflectiveOperationException {
        checkRecordReflection(new R1(), 0, null, null);
        checkRecordReflection(new R2(1, 2), 2, new Object[]{1, 2}, new String[]{"int", "int"});
        checkRecordReflection(new R3(List.of("1")), 1, new Object[]{List.of("1")}, new String[]{"java.util.List<java.lang.String>"});
    }


    private void checkRecordReflection(Object recordOb, int numberOfComponents, Object[] values, String[] signatures) throws ReflectiveOperationException {
        Class<?> recordClass = recordOb.getClass();
        assertTrue(recordClass.isRecord());
        Method[] accessors = recordClass.getRecordAccessors();
        assertEquals(accessors.length, numberOfComponents);
        int i = 0;
        for (Method m : accessors) {
            assertEquals(m.invoke(recordOb), values[i]);
            assertEquals(m.getGenericReturnType().toString(), signatures[i],
                         String.format("signature of method \"%s\" different from expected signature \"%s\"", m.getGenericReturnType(), signatures[i]));
            i++;
        }
    }
}
