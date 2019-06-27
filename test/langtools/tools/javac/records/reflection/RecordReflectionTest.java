/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * @modules jdk.compiler/com.sun.tools.javac.util
 */

import java.lang.reflect.*;
import java.util.List;
import com.sun.tools.javac.util.Assert;

public class RecordReflectionTest {

    class NoRecord {}

    record R1();

    record R2(int i, int j);

    record R3(List<String> ls);

    public static void main(String... args) throws Throwable {
        Class<?> noRecordClass = NoRecord.class;
        Assert.check(!noRecordClass.isRecord());
        Assert.check(noRecordClass.getRecordAccessors().length == 0);

        RecordReflectionTest recordReflectionTest = new RecordReflectionTest();
        recordReflectionTest.checkRecordReflection(new R1(), 0, null, null);
        recordReflectionTest.checkRecordReflection(new R2(1, 2), 2, new Object[]{1, 2}, new String[]{"int", "int"});
        recordReflectionTest.checkRecordReflection(new R3(List.of("1")), 1, new Object[]{List.of("1")}, new String[]{"java.util.List<java.lang.String>"});
    }

    void checkRecordReflection(Object recordOb, int numberOfComponents, Object[] values, String[] signatures) throws Throwable {
        Class<?> recordClass = recordOb.getClass();
        Assert.check(recordClass.isRecord());
        Method[] accessors = recordClass.getRecordAccessors();
        Assert.check(accessors.length == numberOfComponents);
        int i = 0;
        for (Method m : accessors) {
            Assert.check(m.invoke(recordOb).equals(values[i]));
            Assert.check(m.getGenericReturnType().toString().equals(signatures[i]), String.format("signature of method \"%s\" different from expected signature \"%s\"", m.getGenericReturnType(), signatures[i]));
            i++;
        }
    }
}
