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

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.sym.ClassRef;
import java.lang.sym.ConstantRefs;
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
            assertEquals(r, r.array().resolveConstantRef(LOOKUP).getComponentType().toConstantRef(LOOKUP).orElseThrow());
            // Commutativity: resolve -> array -> toSymbolic -> component type
            assertEquals(r, Array.newInstance(r.resolveConstantRef(LOOKUP), 0).getClass().toConstantRef(LOOKUP).orElseThrow().componentType());
        }

        if (r.isArray()) {
            assertEquals(r, r.componentType().array());
            assertEquals(r, r.resolveConstantRef(LOOKUP).getComponentType().toConstantRef(LOOKUP).orElseThrow().array());
            assertEquals(r, Array.newInstance(r.componentType().resolveConstantRef(LOOKUP), 0).getClass().toConstantRef(LOOKUP).orElseThrow());
        }
    }

    private void testClassRef(ClassRef r, Class<?> c) throws ReflectiveOperationException {
        testClassRef(r);

        assertEquals(r.resolveConstantRef(LOOKUP), c);
        assertEquals(c.toConstantRef(LOOKUP).orElseThrow(), r);
        assertEquals(ClassRef.ofDescriptor(c.toDescriptorString()), r);
    }

    public void testSymbolicRefsConstants() throws ReflectiveOperationException {
        int tested = 0;
        Field[] fields = ConstantRefs.class.getDeclaredFields();
        for (Field f : fields) {
            try {
                if (f.getType().equals(ClassRef.class)
                    && ((f.getModifiers() & Modifier.STATIC) != 0)
                    && ((f.getModifiers() & Modifier.PUBLIC) != 0)) {
                    ClassRef cr = (ClassRef) f.get(null);
                    Class c = cr.resolveConstantRef(MethodHandles.lookup());
                    testClassRef(cr, c);
                    ++tested;
                }
            }
            catch (Throwable e) {
                fail("Error testing field " + f.getName(), e);
            }
        }

        assertTrue(tested > 0);
    }

    public void testPrimitiveClassRef() throws ReflectiveOperationException {
        for (Primitives p : Primitives.values()) {
            List<ClassRef> refs = List.of(ClassRef.ofDescriptor(p.descriptor),
                                          p.classRef,
                                          (ClassRef) p.clazz.toConstantRef().orElseThrow());
            for (ClassRef c : refs) {
                testClassRef(c, p.clazz);
                assertTrue(c.isPrimitive());
                assertEquals(p.descriptor, c.descriptorString());
                assertEquals(p.name, c.simpleName());
                refs.forEach(cc -> assertEquals(c, cc));
                if (p != Primitives.VOID) {
                    testClassRef(c.array(), p.arrayClass);
                    assertEquals(c, ((ClassRef) p.arrayClass.toConstantRef().orElseThrow()).componentType());
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
                                                       String.class.toConstantRef(LOOKUP).orElseThrow());
        for (ClassRef r : stringClassRefs) {
            testClassRef(r, String.class);
            assertFalse(r.isPrimitive());
            assertEquals("Ljava/lang/String;", r.descriptorString());
            assertEquals("String", r.simpleName());
            assertEquals(r.array().resolveConstantRef(LOOKUP), String[].class);
            stringClassRefs.forEach(rr -> assertEquals(r, rr));
        }

        testClassRef(ClassRef.of("java.lang.String").array(), String[].class);
        testClassRef(ClassRef.of("java.util.Map").inner("Entry"), Map.Entry.class);

        ClassRef thisClassRef = ClassRef.ofDescriptor("LClassRefTest;");
        assertEquals(thisClassRef, ClassRef.of("", "ClassRefTest"));
        assertEquals(thisClassRef, ClassRef.of("ClassRefTest"));
        assertEquals(thisClassRef.simpleName(), "ClassRefTest");
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
            assertEquals(classToDescriptor(a0.resolveConstantRef(LOOKUP)), a0.descriptorString());
            assertEquals(classToDescriptor(a1.resolveConstantRef(LOOKUP)), a1.descriptorString());
            assertEquals(classToDescriptor(a2.resolveConstantRef(LOOKUP)), a2.descriptorString());
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
