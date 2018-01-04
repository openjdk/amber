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

import java.lang.reflect.Array;
import java.lang.sym.ClassRef;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @test
 * @run testng ClassRefTest
 * @summary unit tests for java.lang.sym.ClassRef
 */
@Test
public class ClassRefTest extends SymbolicRefTest {

    private void testClassRef(ClassRef r) throws ReflectiveOperationException {
        testSymbolicRef(r);

        // Test descriptor accessor, factory, equals
        assertEquals(r, ClassRef.ofDescriptor(r.descriptorString()));

        if (!r.descriptorString().equals("V")) {
            assertEquals(r, r.array().componentType());
            // Commutativity: array -> resolve -> componentType -> toSymbolic
            assertEquals(r, r.array().resolveRef(LOOKUP).getComponentType().toSymbolicRef(LOOKUP).get());
            // Commutativity: resolve -> array -> toSymbolic -> component type
            assertEquals(r, Array.newInstance(r.resolveRef(LOOKUP), 0).getClass().toSymbolicRef(LOOKUP).get().componentType());
        }

        if (r.isArray()) {
            assertEquals(r, r.componentType().array());
            assertEquals(r, r.resolveRef(LOOKUP).getComponentType().toSymbolicRef(LOOKUP).get().array());
            assertEquals(r, Array.newInstance(r.componentType().resolveRef(LOOKUP), 0).getClass().toSymbolicRef(LOOKUP).get());
        }
    }

    private void testClassRef(ClassRef r, Class<?> c) throws ReflectiveOperationException {
        testClassRef(r);

        assertEquals(r.resolveRef(LOOKUP), c);
        assertEquals(c.toSymbolicRef(LOOKUP).get(), r);
        assertEquals(ClassRef.ofDescriptor(c.toDescriptorString()), r);
    }

    public void testPrimitiveClassRef() throws ReflectiveOperationException {
        for (Primitives p : Primitives.values()) {
            List<ClassRef> refs = List.of(ClassRef.ofDescriptor(p.descriptor),
                                          p.classRef,
                                          (ClassRef) p.clazz.toSymbolicRef().get());
            for (ClassRef c : refs) {
                testClassRef(c, p.clazz);
                assertTrue(c.isPrimitive());
                assertEquals(p.descriptor, c.descriptorString());
                assertEquals(p.name, c.canonicalName());
                refs.forEach(cc -> assertEquals(c, cc));
                if (p != Primitives.VOID) {
                    testClassRef(c.array(), p.arrayClass);
                    assertEquals(c, ((ClassRef) p.arrayClass.toSymbolicRef().get()).componentType());
                    assertEquals(c, p.classRef.array().componentType());
                }
            }

            for (Primitives other : Primitives.values()) {
                ClassRef otherDescr = ClassRef.ofDescriptor(other.descriptor);
                if (p != other)
                    refs.forEach(c -> assertNotEquals(c, otherDescr));
                else
                    refs.forEach(c -> assertEquals(c, otherDescr));
            }
        }
    }

    public void testSimpleClassRef() throws ReflectiveOperationException {

        List<ClassRef> stringClassRefs = Arrays.asList(ClassRef.ofDescriptor("Ljava/lang/String;"),
                                                       ClassRef.of("java.lang", "String"),
                                                       ClassRef.of("java.lang.String"),
                                                       ClassRef.of("java.lang.String").array().componentType(),
                                                       String.class.toSymbolicRef(LOOKUP).get());
        for (ClassRef r : stringClassRefs) {
            testClassRef(r, String.class);
            assertFalse(r.isPrimitive());
            assertEquals("Ljava/lang/String;", r.descriptorString());
            assertEquals("java.lang.String", r.canonicalName());
            assertEquals(r.array().resolveRef(LOOKUP), String[].class);
            stringClassRefs.forEach(rr -> assertEquals(r, rr));
        }

        testClassRef(ClassRef.of("java.lang.String").array(), String[].class);
        testClassRef(ClassRef.of("java.util.Map").inner("Entry"), Map.Entry.class);

        ClassRef thisClassRef = ClassRef.ofDescriptor("LClassRefTest;");
        assertEquals(thisClassRef, ClassRef.of("", "ClassRefTest"));
        assertEquals(thisClassRef, ClassRef.of("ClassRefTest"));
        assertEquals(thisClassRef.canonicalName(), "ClassRefTest");
        testClassRef(thisClassRef, ClassRefTest.class);
    }

    public void testArrayClassRef() throws ReflectiveOperationException {
        for (String d : basicDescs) {
            ClassRef a0 = ClassRef.ofDescriptor(d);
            ClassRef a1 = a0.array();
            ClassRef a2 = a1.array();

            testClassRef(a0);
            testClassRef(a1);
            testClassRef(a2);
            assertFalse(a0.isArray());
            assertTrue(a1.isArray());
            assertTrue(a2.isArray());
            assertFalse(a1.isPrimitive());
            assertFalse(a2.isPrimitive());
            assertEquals(a0.descriptorString(), d);
            assertEquals(a1.descriptorString(), "[" + a0.descriptorString());
            assertEquals(a2.descriptorString(), "[[" + a0.descriptorString());

            try {
                assertEquals(a0, a0.componentType());
                fail("Didn't throw ISE");
            }
            catch (IllegalStateException expected) {
                // succeed
            }
            assertEquals(a0, a1.componentType());
            assertEquals(a1, a2.componentType());

            assertNotEquals(a0, a1);
            assertNotEquals(a1, a2);

            assertEquals(a1, ClassRef.ofDescriptor("[" + d));
            assertEquals(a2, ClassRef.ofDescriptor("[[" + d));
            assertEquals(classToDescriptor(a0.resolveRef(LOOKUP)), a0.descriptorString());
            assertEquals(classToDescriptor(a1.resolveRef(LOOKUP)), a1.descriptorString());
            assertEquals(classToDescriptor(a2.resolveRef(LOOKUP)), a2.descriptorString());
        }
    }

    public void testBadClassRefs() {
        List<String> badDescriptors = List.of("II", "I;", "Q", "L",
                                              "java.lang.String", "[]", "Ljava/lang/String",
                                              "Ljava.lang.String;", "java/lang/String");

        for (String d : badDescriptors) {
            try {
                ClassRef constant = ClassRef.ofDescriptor(d);
                fail(d);
            }
            catch (IllegalArgumentException e) {
                // good
            }
        }
    }
}
