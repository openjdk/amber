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
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.ConstantUtils;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @test
 * @compile -XDfolding=false ClassRefTest.java
 * @run testng ClassRefTest
 * @summary unit tests for java.lang.constant.ClassDesc
 */
@Test
public class ClassRefTest extends SymbolicRefTest {

    private void testClassRef(ClassDesc r) throws ReflectiveOperationException {
        testSymbolicRef(r);

        // Test descriptor accessor, factory, equals
        assertEquals(r, ClassDesc.ofDescriptor(r.descriptorString()));

        if (!r.descriptorString().equals("V")) {
            assertEquals(r, r.arrayType().componentType());
            // Commutativity: array -> resolve -> componentType -> toSymbolic
            assertEquals(r, r.arrayType().resolveConstantDesc(LOOKUP).getComponentType().describeConstable().orElseThrow());
            // Commutativity: resolve -> array -> toSymbolic -> component type
            assertEquals(r, Array.newInstance(r.resolveConstantDesc(LOOKUP), 0).getClass().describeConstable().orElseThrow().componentType());
        }

        if (r.isArray()) {
            assertEquals(r, r.componentType().arrayType());
            assertEquals(r, r.resolveConstantDesc(LOOKUP).getComponentType().describeConstable().orElseThrow().arrayType());
            assertEquals(r, Array.newInstance(r.componentType().resolveConstantDesc(LOOKUP), 0).getClass().describeConstable().orElseThrow());
        }
    }

    private void testClassRef(ClassDesc r, Class<?> c) throws ReflectiveOperationException {
        testClassRef(r);

        assertEquals(r.resolveConstantDesc(LOOKUP), c);
        assertEquals(c.describeConstable().orElseThrow(), r);
        assertEquals(ClassDesc.ofDescriptor(c.descriptorString()), r);
    }

    public void testSymbolicRefsConstants() throws ReflectiveOperationException {
        int tested = 0;
        Field[] fields = ConstantDescs.class.getDeclaredFields();
        for (Field f : fields) {
            try {
                if (f.getType().equals(ClassDesc.class)
                    && ((f.getModifiers() & Modifier.STATIC) != 0)
                    && ((f.getModifiers() & Modifier.PUBLIC) != 0)) {
                    ClassDesc cr = (ClassDesc) f.get(null);
                    Class c = cr.resolveConstantDesc(MethodHandles.lookup());
                    testClassRef(cr, c);
                    ++tested;
                }
            }
            catch (Throwable e) {
                System.out.println(e.getMessage());
                fail("Error testing field " + f.getName(), e);
            }
        }

        assertTrue(tested > 0);
    }

    public void testPrimitiveClassRef() throws ReflectiveOperationException {
        for (Primitives p : Primitives.values()) {
            List<ClassDesc> refs = List.of(ClassDesc.ofDescriptor(p.descriptor),
                                           p.classRef,
                                           (ClassDesc) p.clazz.describeConstable().orElseThrow());
            for (ClassDesc c : refs) {
                testClassRef(c, p.clazz);
                assertTrue(c.isPrimitive());
                assertEquals(p.descriptor, c.descriptorString());
                assertEquals(p.name, c.displayName());
                refs.forEach(cc -> assertEquals(c, cc));
                if (p != Primitives.VOID) {
                    testClassRef(c.arrayType(), p.arrayClass);
                    assertEquals(c, ((ClassDesc) p.arrayClass.describeConstable().orElseThrow()).componentType());
                    assertEquals(c, p.classRef.arrayType().componentType());
                }
            }

            for (Primitives other : Primitives.values()) {
                ClassDesc otherDescr = ClassDesc.ofDescriptor(other.descriptor);
                if (p != other)
                    refs.forEach(c -> assertNotEquals(c, otherDescr));
                else
                    refs.forEach(c -> assertEquals(c, otherDescr));
            }
        }
    }

    public void testSimpleClassRef() throws ReflectiveOperationException {

        List<ClassDesc> stringClassRefs = Arrays.asList(ClassDesc.ofDescriptor("Ljava/lang/String;"),
                                                        ClassDesc.of("java.lang", "String"),
                                                        ClassDesc.of("java.lang.String"),
                                                        ClassDesc.of("java.lang.String").arrayType().componentType(),
                                                        String.class.describeConstable().orElseThrow());
        for (ClassDesc r : stringClassRefs) {
            testClassRef(r, String.class);
            assertFalse(r.isPrimitive());
            assertEquals("Ljava/lang/String;", r.descriptorString());
            assertEquals("String", r.displayName());
            assertEquals(r.arrayType().resolveConstantDesc(LOOKUP), String[].class);
            stringClassRefs.forEach(rr -> assertEquals(r, rr));
        }

        testClassRef(ClassDesc.of("java.lang.String").arrayType(), String[].class);
        testClassRef(ClassDesc.of("java.util.Map").inner("Entry"), Map.Entry.class);

        ClassDesc thisClassRef = ClassDesc.ofDescriptor("LClassRefTest;");
        assertEquals(thisClassRef, ClassDesc.of("", "ClassRefTest"));
        assertEquals(thisClassRef, ClassDesc.of("ClassRefTest"));
        assertEquals(thisClassRef.displayName(), "ClassRefTest");
        testClassRef(thisClassRef, ClassRefTest.class);
    }

    private void testBadPackageName(ClassDesc cr) {
        try {
            cr.packageName();
            fail("");
        } catch (IllegalStateException e) {
            // good
        }
    }

    public void testPackageName() {
        assertEquals("com.foo", ClassDesc.of("com.foo.Bar").packageName());
        assertEquals("com.foo", ClassDesc.of("com.foo.Bar").inner("Baz").packageName());
        assertEquals("", ClassDesc.of("Bar").packageName());
        assertEquals("", ClassDesc.of("Bar").inner("Baz").packageName());

        testBadPackageName(ConstantDescs.CR_int);
        testBadPackageName(ConstantDescs.CR_int.arrayType());
        testBadPackageName(ConstantDescs.CR_String.arrayType());
        testBadPackageName(ClassDesc.of("Bar").arrayType());
    }

    private void testBadArrayRank(ClassDesc cr) {
        try {
            cr.arrayType(-1);
            fail("");
        } catch (IllegalArgumentException e) {
            // good
        }
    }

    public void testArrayClassRef() throws ReflectiveOperationException {
        for (String d : basicDescs) {
            ClassDesc a0 = ClassDesc.ofDescriptor(d);
            ClassDesc a1 = a0.arrayType();
            ClassDesc a2 = a1.arrayType();

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

            assertNull(a0.componentType());
            assertEquals(a0, a1.componentType());
            assertEquals(a1, a2.componentType());

            assertNotEquals(a0, a1);
            assertNotEquals(a1, a2);

            assertEquals(a1, ClassDesc.ofDescriptor("[" + d));
            assertEquals(a2, ClassDesc.ofDescriptor("[[" + d));
            assertEquals(classToDescriptor(a0.resolveConstantDesc(LOOKUP)), a0.descriptorString());
            assertEquals(classToDescriptor(a1.resolveConstantDesc(LOOKUP)), a1.descriptorString());
            assertEquals(classToDescriptor(a2.resolveConstantDesc(LOOKUP)), a2.descriptorString());

            testBadArrayRank(ConstantDescs.CR_int);
            testBadArrayRank(ConstantDescs.CR_String);
            testBadArrayRank(ClassDesc.of("Bar"));
        }
    }

    public void testBadClassRefs() {
        List<String> badDescriptors = List.of("II", "I;", "Q", "L",
                                              "java.lang.String", "[]", "Ljava/lang/String",
                                              "Ljava.lang.String;", "java/lang/String");

        for (String d : badDescriptors) {
            try {
                ClassDesc constant = ClassDesc.ofDescriptor(d);
                fail(d);
            }
            catch (IllegalArgumentException e) {
                // good
            }
        }

        List<String> badBinaryNames = List.of("I;", "[]", "Ljava/lang/String",
                "Ljava.lang.String;", "java/lang/String");
        for (String d : badBinaryNames) {
            try {
                ClassDesc constant = ClassDesc.of(d);
                fail(d);
            } catch (IllegalArgumentException e) {
                // good
            }
        }

        for (Primitives p : Primitives.values()) {
            testBadInnerClasses(ClassDesc.ofDescriptor(p.descriptor), "any");
            testBadInnerClasses(ClassDesc.ofDescriptor(p.descriptor), "any", "other");
        }
    }

    private void testBadInnerClasses(ClassDesc cr, String firstInnerName, String... moreInnerNames) {
        try {
            cr.inner(firstInnerName, moreInnerNames);
            fail("");
        } catch (IllegalStateException e) {
            // good
        }
    }
}
