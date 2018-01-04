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
import java.lang.sym.ClassRef;
import java.lang.sym.MethodTypeRef;
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
 * @run testng MethodTypeRefTest
 * @summary unit tests for java.lang.sym.MethodTypeRef
 */
@Test
public class MethodTypeRefTest extends SymbolicRefTest {

    private void testMethodTypeRef(MethodTypeRef r) throws ReflectiveOperationException {
        testSymbolicRef(r);

        // Tests accessors (rType, pType, pCount, pList, pArray, descriptorString),
        // factories (ofDescriptor, of), equals
        assertEquals(r, MethodTypeRef.ofDescriptor(r.descriptorString()));
        assertEquals(r, MethodTypeRef.of(r.returnType(), r.parameterArray()));
        assertEquals(r, MethodTypeRef.of(r.returnType(), r.parameterList().toArray(new ClassRef[0])));
        assertEquals(r, MethodTypeRef.of(r.returnType(), r.parameterList().stream().toArray(ClassRef[]::new)));
        assertEquals(r, MethodTypeRef.of(r.returnType(), IntStream.range(0, r.parameterCount())
                                                                  .mapToObj(r::parameterType)
                                                                  .toArray(ClassRef[]::new)));
    }

    private void testMethodTypeRef(MethodTypeRef r, MethodType mt) throws ReflectiveOperationException {
        testMethodTypeRef(r);

        assertEquals(r.resolveRef(LOOKUP), mt);
        assertEquals(mt.toSymbolicRef(LOOKUP).get(), r);

        assertEquals(r.descriptorString(), mt.toMethodDescriptorString());
        assertEquals(r.parameterCount(), mt.parameterCount());
        assertEquals(r.parameterList(), mt.parameterList().stream().map(SymbolicRefTest::classToRef).collect(toList()));
        assertEquals(r.parameterArray(), Stream.of(mt.parameterArray()).map(SymbolicRefTest::classToRef).toArray(ClassRef[]::new));
        for (int i=0; i<r.parameterCount(); i++)
            assertEquals(r.parameterType(i), classToRef(mt.parameterType(i)));
        assertEquals(r.returnType(), classToRef(mt.returnType()));
    }

    private void assertMethodType(ClassRef returnType,
                                  ClassRef... paramTypes) throws ReflectiveOperationException {
        String descriptor = Stream.of(paramTypes).map(ClassRef::descriptorString).collect(joining("", "(", ")"))
                            + returnType.descriptorString();
        MethodTypeRef mtRef = MethodTypeRef.of(returnType, paramTypes);

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
            ClassRef rc = ClassRef.ofDescriptor(r);
            MethodTypeRef newRef = mtRef.changeReturnType(rc);
            assertEquals(newRef, MethodTypeRef.of(rc, paramTypes));
            testMethodTypeRef(newRef, mt.changeReturnType(rc.resolveRef(LOOKUP)));
        }

        // changeParamType
        for (int i=0; i<paramTypes.length; i++) {
            for (String p : paramDescs) {
                ClassRef pc = ClassRef.ofDescriptor(p);
                ClassRef[] ps = paramTypes.clone();
                ps[i] = pc;
                MethodTypeRef newRef = mtRef.changeParameterType(i, pc);
                assertEquals(newRef, MethodTypeRef.of(returnType, ps));
                testMethodTypeRef(newRef, mt.changeParameterType(i, pc.resolveRef(LOOKUP)));
            }
        }

        // dropParamType
        for (int i=0; i<paramTypes.length; i++) {
            int k = i;
            ClassRef[] ps = IntStream.range(0, paramTypes.length)
                                     .filter(j -> j != k)
                                     .mapToObj(j -> paramTypes[j])
                                     .toArray(ClassRef[]::new);
            MethodTypeRef newRef = mtRef.dropParameterTypes(i, i + 1);
            assertEquals(newRef, MethodTypeRef.of(returnType, ps));
            testMethodTypeRef(newRef, mt.dropParameterTypes(i, i+1));
        }

        // addParam
        for (int i=0; i <= paramTypes.length; i++) {
            for (ClassRef p : paramTypes) {
                int k = i;
                ClassRef[] ps = IntStream.range(0, paramTypes.length + 1)
                                         .mapToObj(j -> (j < k) ? paramTypes[j] : (j == k) ? p : paramTypes[j-1])
                                         .toArray(ClassRef[]::new);
                MethodTypeRef newRef = mtRef.insertParameterTypes(i, p);
                assertEquals(newRef, MethodTypeRef.of(returnType, ps));
                testMethodTypeRef(newRef, mt.insertParameterTypes(i, p.resolveRef(LOOKUP)));
            }
        }
    }

    public void testMethodTypeRef() throws ReflectiveOperationException {
        for (String r : returnDescs) {
            assertMethodType(ClassRef.ofDescriptor(r));
            for (String p1 : paramDescs) {
                assertMethodType(ClassRef.ofDescriptor(r), ClassRef.ofDescriptor(p1));
                for (String p2 : paramDescs) {
                    assertMethodType(ClassRef.ofDescriptor(r), ClassRef.ofDescriptor(p1), ClassRef.ofDescriptor(p2));
                }
            }
        }
    }

    public void testBadMethodTypeRefs() {
        List<String> badDescriptors = List.of("()II", "()I;", "(I;)", "(I)", "()L",
                                              "(java.lang.String)V", "()[]", "(Ljava/lang/String)V",
                                              "(Ljava.lang.String;)V", "(java/lang/String)V");

        for (String d : badDescriptors) {
            try {
                MethodTypeRef r = MethodTypeRef.ofDescriptor(d);
                fail(d);
            }
            catch (IllegalArgumentException e) {
                // good
            }
        }
    }
}
