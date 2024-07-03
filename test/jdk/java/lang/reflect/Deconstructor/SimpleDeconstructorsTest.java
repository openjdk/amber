/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
 * @summary SimpleDeconstructorsTest
 * @enablePreview
 * @compile --enable-preview --source ${jdk.version} -parameters SimpleDeconstructorsTest.java
 * @run main/othervm --enable-preview SimpleDeconstructorsTest
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Deconstructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SimpleDeconstructorsTest {

    public static void main(String[] args) throws NoSuchPatternException, IllegalAccessException {
        testGetMethods();
        testGetDeconstructors();
        testGetDeclaredDeconstructors();
        testGetDeclaredDeconstructor();
        testDeconstructorElements();
        testGenericDeconstructorElements();
        testInvoke();
        testDeconstructorElementsAnnotations();
        testDeconstructorAnnotations();
        testGenericString();
        testGetDeclaredDeconstructors_bug1();
        testGetDeclaredDeconstructors_bug2();
        testGetDeclaredDeconstructors_bug3();
    }

    public static void testGetMethods() {
        Class<?> class1 = Person1.class;

        Method[] methods = class1.getMethods();

        assertEquals(methods.length, 13);
    }

    public static void testGetDeclaredDeconstructors() {
        Class<?> class1 = Person1.class;

        Deconstructor<?>[] methods = class1.getDeclaredDeconstructors();

        assertEquals(methods.length, 5);
    }

    public static void testGetDeconstructors() {
        Class<?> class1 = Person1.class;

        Deconstructor<?>[] methods = class1.getDeconstructors();

        assertEquals(methods.length, 4);
    }

    public static void testGetDeclaredDeconstructor() throws NoSuchPatternException {
        Class<?> class1 = Person1.class;

        Deconstructor method1 = class1.getDeclaredDeconstructor(String.class, String.class);

        assertEquals(method1.getName(), "SimpleDeconstructorsTest$Person1");
    }

    public static void testDeconstructorElements() throws NoSuchPatternException {
        Class<?> class1 = Person1.class;

        Deconstructor method = class1.getDeclaredDeconstructor(String.class, String.class);

        var elems = method.getPatternBindings();

        assertEquals(elems.length, 2);
        assertEquals(elems[0].getType(), String.class);
        assertEquals(elems[1].getType(), String.class);
        assertEquals(elems[0].getName(), "name");
        assertEquals(elems[1].getName(), "username");
        assertEquals(elems[0].getDeclaringDeconstructor(), method);
        assertEquals(elems[1].getDeclaringDeconstructor(), method);
    }

    public static void testGenericDeconstructorElements() throws NoSuchPatternException {
        Class<?> class1 = Person1.class;

        Deconstructor method = class1.getDeclaredDeconstructor(List.class);

        var elems = method.getPatternBindings();

        assertEquals(elems.length, 1);
        assertEquals(elems[0].getType(), List.class);
        assertEquals(elems[0].getName(), "name");
        assertEquals(elems[0].getGenericSignature(), "Ljava/util/List<Ljava/lang/Character;>;");
        assertEquals(elems[0].getGenericType() instanceof ParameterizedType, true);
    }

    public static void testInvoke() throws IllegalAccessException, NoSuchPatternException {
        Person1 p = new Person1("Name", "Surname", false);

        Class<?> class1 = Person1.class;

        Deconstructor method1 = class1.getDeclaredDeconstructor(List.class);
        Object[] bindings1 = method1.invoke(p);
        List<Character> expected = List.of('N', 'a', 'm', 'e');
        for (int i = 0; i < 4; i++) {
            assertEquals(((List<Character>)bindings1[0]).get(i), expected.get(i));
        }

        Deconstructor method2 = class1.getDeclaredDeconstructor(int.class);
        Object[] bindings2 = method2.invoke(p);
        assertEquals(((int)bindings2[0]), 42);
    }

    public static void testDeconstructorElementsAnnotations() throws NoSuchPatternException {
        Class<?> class1 = Person1.class;

        Deconstructor method = class1.getDeclaredDeconstructor(String.class, String.class);

        var elems = method.getPatternBindings();

        Person1.BindingAnnotation ba = elems[1].getDeclaredAnnotation(Person1.BindingAnnotation.class);

        assertEquals(ba.value(), 1);
    }

    public static void testDeconstructorAnnotations() throws NoSuchPatternException {
        Class<?> class1 = Person1.class;

        Deconstructor method = class1.getDeclaredDeconstructor(String.class, String.class);

        Person1.DeconstructorAnnotation da = method.getDeclaredAnnotation(Person1.DeconstructorAnnotation.class);

        assertEquals(da.value(), 1);
    }

    public static void testGenericString() throws NoSuchPatternException{
        Class<?> class1 = Person1.class;
        Deconstructor method = null;

        method = class1.getDeclaredDeconstructor(String.class, String.class);
        assertEquals(method.toGenericString(), "public pattern SimpleDeconstructorsTest$Person1(java.lang.String,java.lang.String)");

        method = class1.getDeclaredDeconstructor(List.class);
        assertEquals(method.toGenericString(), "public pattern SimpleDeconstructorsTest$Person1(java.util.List<java.lang.Character>)");
    }

    public static void testGetDeclaredDeconstructors_bug1() {
        Deconstructor<?>[] methods = B.class.getDeclaredDeconstructors();

        assertEquals(methods.length, 1);
    }

    public static void testGetDeclaredDeconstructors_bug2() {
        Deconstructor<?>[] methods = null;

        methods = String.class.getDeclaredDeconstructors();
        assertEquals(methods.length, 0);

        methods = int.class.getDeclaredDeconstructors();
        assertEquals(methods.length, 0);
    }

    public static class Bug {
        int i;
        public Bug(int i) {
            this.i = i;
        }
        private pattern Bug(int i) {
            match Bug(this.i);
        }
    }

    public static void testGetDeclaredDeconstructors_bug3() throws IllegalAccessException {
        var b = new Bug(2);
        Deconstructor<?> declaredDeconstructor = Bug.class.getDeclaredDeconstructors()[0];
        declaredDeconstructor.setAccessible(true);
        var x = declaredDeconstructor.invoke(b);
        assertEquals(x[0], 2);
    }

    static void assertEquals(Object actual, Object expected) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected: " + expected + ", but got: " + actual);
        }
    }

    class A {
        String s;
        public A(String s) { this.s = s; }
        public pattern A(String s) { match A(this.s); }
    }

    class B {
        A a;
        public B(A a) { this.a = a; }
        public pattern B(A a) { match B(this.a); }
    }

    public static class Person1 {
        private final String name;
        private final String username;
        private boolean capitalize;

        // 1 declared contructors
        public Person1(String name, String username, boolean capitalize) {
            this.name = name;
            this.username = username;
            this.capitalize = capitalize;
        }

        // 5 declared pattern declarations but 3 public
        @DeconstructorAnnotation(value = 1)
        public pattern Person1(String name, @BindingAnnotation(value = 1) String username) {
            if (capitalize) {
                match Person1(this.name.toUpperCase(), this.username);
            } else {
                match Person1(this.name, this.username);
            }
        }

        public pattern Person1(List<Character> name) {
            match Person1(this.name.chars().mapToObj(e -> (char)e).collect(Collectors.toList()));
        }

        public pattern Person1(int t) {
            match Person1(42);
        }

        public pattern Person1(int[] t) {
            match Person1(new int[]{1, 2, 3});
        }

        private pattern Person1(List<Character> name, List<Character> username) {
            match Person1(this.name.chars().mapToObj(e -> (char)e).collect(Collectors.toList()),
                          this.username.chars().mapToObj(e -> (char)e).collect(Collectors.toList()));
        }

        // 3 methods
        public void test1() {  }

        public int test2(int i) {
            return i++;
        }

        public int test3(int i) {
            return i++;
        }

        public int test4(int i) {
            return i++;
        }

        @Target(ElementType.PATTERN_BINDING)
        @Retention(RetentionPolicy.RUNTIME)
        public @interface BindingAnnotation {
            int value() default 0;
        }

        @Target(ElementType.DECONSTRUCTOR)
        @Retention(RetentionPolicy.RUNTIME)
        public @interface DeconstructorAnnotation {
            int value() default 0;
        }
    }
}



