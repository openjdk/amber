/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.MethodType;
import java.lang.invoke.constant.ClassDesc;
import java.lang.invoke.constant.MethodTypeDesc;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.testng.annotations.Test;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * @test
 * @compile -XDfolding=false MethodTypeRefTest.java
 * @run testng MethodTypeRefTest
 * @summary unit tests for java.lang.invoke.constant.MethodTypeRef
 */
@Test
public class MethodTypeRefTest extends SymbolicRefTest {

    private void testMethodTypeRef(MethodTypeDesc r) throws ReflectiveOperationException {
        testSymbolicRef(r);

        // Tests accessors (rType, pType, pCount, pList, pArray, descriptorString),
        // factories (ofDescriptor, of), equals
        assertEquals(r, MethodTypeDesc.ofDescriptor(r.descriptorString()));
        assertEquals(r, MethodTypeDesc.of(r.returnType(), r.parameterArray()));
        assertEquals(r, MethodTypeDesc.of(r.returnType(), r.parameterList().toArray(new ClassDesc[0])));
        assertEquals(r, MethodTypeDesc.of(r.returnType(), r.parameterList().stream().toArray(ClassDesc[]::new)));
        assertEquals(r, MethodTypeDesc.of(r.returnType(), IntStream.range(0, r.parameterCount())
                                                                   .mapToObj(r::parameterType)
                                                                   .toArray(ClassDesc[]::new)));
    }

    private void testMethodTypeRef(MethodTypeDesc r, MethodType mt) throws ReflectiveOperationException {
        testMethodTypeRef(r);

        assertEquals(r.resolveConstantDesc(LOOKUP), mt);
        assertEquals(mt.describeConstable().get(), r);

        assertEquals(r.descriptorString(), mt.toMethodDescriptorString());
        assertEquals(r.parameterCount(), mt.parameterCount());
        assertEquals(r.parameterList(), mt.parameterList().stream().map(SymbolicRefTest::classToRef).collect(toList()));
        assertEquals(r.parameterArray(), Stream.of(mt.parameterArray()).map(SymbolicRefTest::classToRef).toArray(ClassDesc[]::new));
        for (int i=0; i<r.parameterCount(); i++)
            assertEquals(r.parameterType(i), classToRef(mt.parameterType(i)));
        assertEquals(r.returnType(), classToRef(mt.returnType()));
    }

    private void assertMethodType(ClassDesc returnType,
                                  ClassDesc... paramTypes) throws ReflectiveOperationException {
        String descriptor = Stream.of(paramTypes).map(ClassDesc::descriptorString).collect(joining("", "(", ")"))
                            + returnType.descriptorString();
        MethodTypeDesc mtRef = MethodTypeDesc.of(returnType, paramTypes);

        // MTRef accessors
        assertEquals(descriptor, mtRef.descriptorString());
        assertEquals(returnType, mtRef.returnType());
        assertEquals(paramTypes, mtRef.parameterArray());
        assertEquals(Arrays.asList(paramTypes), mtRef.parameterList());
        assertEquals(paramTypes.length, mtRef.parameterCount());
        for (int i=0; i<paramTypes.length; i++)
            assertEquals(paramTypes[i], mtRef.parameterType(i));

        // Consistency between MT and MTRef
        MethodType mt = MethodType.fromMethodDescriptorString(descriptor, null);
        testMethodTypeRef(mtRef, mt);

        // changeReturnType
        for (String r : returnDescs) {
            ClassDesc rc = ClassDesc.ofDescriptor(r);
            MethodTypeDesc newRef = mtRef.changeReturnType(rc);
            assertEquals(newRef, MethodTypeDesc.of(rc, paramTypes));
            testMethodTypeRef(newRef, mt.changeReturnType(rc.resolveConstantDesc(LOOKUP)));
        }

        // changeParamType
        for (int i=0; i<paramTypes.length; i++) {
            for (String p : paramDescs) {
                ClassDesc pc = ClassDesc.ofDescriptor(p);
                ClassDesc[] ps = paramTypes.clone();
                ps[i] = pc;
                MethodTypeDesc newRef = mtRef.changeParameterType(i, pc);
                assertEquals(newRef, MethodTypeDesc.of(returnType, ps));
                testMethodTypeRef(newRef, mt.changeParameterType(i, pc.resolveConstantDesc(LOOKUP)));
            }
        }

        // dropParamType
        for (int i=0; i<paramTypes.length; i++) {
            int k = i;
            ClassDesc[] ps = IntStream.range(0, paramTypes.length)
                                      .filter(j -> j != k)
                                      .mapToObj(j -> paramTypes[j])
                                      .toArray(ClassDesc[]::new);
            MethodTypeDesc newRef = mtRef.dropParameterTypes(i, i + 1);
            assertEquals(newRef, MethodTypeDesc.of(returnType, ps));
            testMethodTypeRef(newRef, mt.dropParameterTypes(i, i+1));
        }

        // addParam
        for (int i=0; i <= paramTypes.length; i++) {
            for (ClassDesc p : paramTypes) {
                int k = i;
                ClassDesc[] ps = IntStream.range(0, paramTypes.length + 1)
                                          .mapToObj(j -> (j < k) ? paramTypes[j] : (j == k) ? p : paramTypes[j-1])
                                          .toArray(ClassDesc[]::new);
                MethodTypeDesc newRef = mtRef.insertParameterTypes(i, p);
                assertEquals(newRef, MethodTypeDesc.of(returnType, ps));
                testMethodTypeRef(newRef, mt.insertParameterTypes(i, p.resolveConstantDesc(LOOKUP)));
            }
        }
    }

    public void testMethodTypeRef() throws ReflectiveOperationException {
        for (String r : returnDescs) {
            assertMethodType(ClassDesc.ofDescriptor(r));
            for (String p1 : paramDescs) {
                assertMethodType(ClassDesc.ofDescriptor(r), ClassDesc.ofDescriptor(p1));
                for (String p2 : paramDescs) {
                    assertMethodType(ClassDesc.ofDescriptor(r), ClassDesc.ofDescriptor(p1), ClassDesc.ofDescriptor(p2));
                }
            }
        }
    }

    public void testBadMethodTypeRefs() {
        List<String> badDescriptors = List.of("()II", "()I;", "(I;)", "(I)", "()L", "(V)V",
                                              "(java.lang.String)V", "()[]", "(Ljava/lang/String)V",
                                              "(Ljava.lang.String;)V", "(java/lang/String)V");

        for (String d : badDescriptors) {
            try {
                MethodTypeDesc r = MethodTypeDesc.ofDescriptor(d);
                fail(d);
            }
            catch (IllegalArgumentException e) {
                // good
            }
        }
    }
}
