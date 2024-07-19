/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Testing parsing of a pattern declaration.
 * @enablePreview
 * @build testdata.*
 * @run junit PatternTest
 */

import java.lang.classfile.*;

import java.io.IOException;
import java.lang.classfile.attribute.PatternAttribute;
import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.List;

import static helpers.ClassRecord.assertEqualsDeep;
import static helpers.TestUtil.assertEmpty;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.classfile.components.ClassPrinter;
import org.junit.jupiter.api.Test;

class PatternTest {

    @Test
    void testReadDeconstructor() throws Exception {
        List<String> extractedInfo = new ArrayList<>();
        ClassFile cc = ClassFile.of();
        ClassTransform xform = (clb, cle) -> {
            if (cle instanceof MethodModel mm) {
                clb.transformMethod(mm, (mb, me) -> {
                    if (me instanceof PatternAttribute ma) {
                        extractedInfo.add(ma.patternName().toString());
                        extractedInfo.add(String.valueOf(ma.patternFlags().contains(AccessFlag.DECONSTRUCTOR)));
                        extractedInfo.add(ma.patternTypeSymbol().toString());
                        extractedInfo.add(ma.attributes().toString());
                        mb.with(me);
                    } else {
                        mb.with(me);
                    }
                });
            }
            else
                clb.with(cle);
        };
        cc.transformClass(cc.parse(PatternTest.class.getResourceAsStream("/testdata/Points.class").readAllBytes()), xform);
        assertEquals("[Points, true, MethodTypeDesc[(Collection,Collection)void], [Attribute[name=MethodParameters], Attribute[name=Signature], Attribute[name=RuntimeVisibleParameterAnnotations]]]", extractedInfo.toString());
    }

    @Test
    void testReadAndVerifyDeconstructor() throws IOException {
        byte[] bytes = PatternTest.class.getResourceAsStream("/testdata/Points.class").readAllBytes();
        var cc = ClassFile.of().verify(bytes);

        ClassModel classModel = ClassFile.of().parse(bytes);

        if (!cc.isEmpty()) {
            ClassPrinter.toYaml(classModel, ClassPrinter.Verbosity.TRACE_ALL, System.out::print);
            assertEmpty(cc);
        }
    }

    private static void assertOut(StringBuilder out, String expected) {
        assertArrayEquals(out.toString().trim().split(" *\r?\n"), expected.trim().split("\n"));
    }
}
