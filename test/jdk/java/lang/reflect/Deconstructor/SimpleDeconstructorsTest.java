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

import java.lang.reflect.AccessFlag;
import static java.util.Arrays.asList;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Deconstructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.PatternBinding;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class SimpleDeconstructorsTest {

    public static void main(String[] args) throws Exception {
        testNoReflection();
        testOtherMembersUnaffected();
        testCtorForComparison();

        testDtorAttributes();
        testBindingAttributes();
        testPrivateDtor();
        testNontrivialOutParams();

        testInvoke();
        testDeconstructorElementsAnnotations();
        testDeconstructorAnnotations();
        testGenericString();
        testGetDeclaredDeconstructors_bug1();
        testGetDeclaredDeconstructors_bug2();
        testGetDeclaredDeconstructors_bug3();
    }

    public static class BasicDtor {
        int field;
        public BasicDtor(int field) {
            this.field = field;
        }
        public pattern BasicDtor(int field) {
            match BasicDtor(field);
        }
    }

    // Just demonstrating the deconstructor in operation
    public static void testNoReflection() {
        assertEquals(true, new BasicDtor(5) instanceof BasicDtor(var i) && i == 5);
        switch (new BasicDtor(42)) {
            case BasicDtor(var i) -> assertEquals(42, i);
            default -> throw new AssertionError();
        }
    }

    public static void testOtherMembersUnaffected() {
        class NoDtor {
            int field;
            public NoDtor(int field) {
                this.field = field;
            }
        }

        assertEquals(NoDtor.class.getMethods().length,
                  BasicDtor.class.getMethods().length);
        assertEquals(NoDtor.class.getConstructors().length,
                  BasicDtor.class.getConstructors().length);
        assertEquals(NoDtor.class.getDeclaredConstructors().length,
                  BasicDtor.class.getDeclaredConstructors().length);

        // TODO fix bug where an extra method called "BasicDtor:I" is showing through
        assertEquals(NoDtor.class.getDeclaredMethods().length + 1,
                  BasicDtor.class.getDeclaredMethods().length);
    }

    public static void testCtorForComparison() throws NoSuchMethodException {
        Constructor<BasicDtor> ctor = BasicDtor.class.getConstructor(int.class);
        assertEquals(ctor, BasicDtor.class.getDeclaredConstructor(int.class));

        assertEquals(List.of(ctor), asList(BasicDtor.class.getConstructors()));
        assertEquals(List.of(ctor), asList(BasicDtor.class.getDeclaredConstructors()));

        assertEquals(BasicDtor.class, ctor.getDeclaringClass());
        assertEquals(BasicDtor.class.getName(), ctor.getName());

        assertEquals(EnumSet.of(AccessFlag.PUBLIC), ctor.accessFlags());
        assertEquals(Modifier.PUBLIC, ctor.getModifiers());
        assertEquals(false, ctor.isSynthetic());

        assertEquals(0, ctor.getExceptionTypes().length);
        assertEquals(0, ctor.getGenericExceptionTypes().length);

        assertEquals(0, ctor.getTypeParameters().length);

        assertEquals("public SimpleDeconstructorsTest$BasicDtor(int)", ctor.toGenericString());
        assertEquals(false, ctor.isVarArgs());
    }

    public static void testDtorAttributes() throws NoSuchPatternException {
        Deconstructor<BasicDtor>[] dtors = BasicDtor.class.getDeconstructors();
        assertEquals(1, dtors.length);
        Deconstructor<BasicDtor> dtor = dtors[0];

        // TODO implement Dtor.equals()
        // assertEquals(List.of(dtor), asList(BasicDtor.class.getDeclaredDeconstructors()));
        // assertEquals(dtor, BasicDtor.class.getDeconstructor(int.class));
        // assertEquals(dtor, BasicDtor.class.getDeclaredDeconstructor(int.class));

        assertEquals(BasicDtor.class, dtor.getDeclaringClass());
        assertEquals(BasicDtor.class.getName(), dtor.getName());

        // TODO: static and synthetic seem wrong
        assertEquals(EnumSet.of(AccessFlag.PUBLIC, AccessFlag.STATIC, AccessFlag.SYNTHETIC),
            dtor.accessFlags());
        assertEquals(Modifier.PUBLIC | Modifier.STATIC | 0x1000, dtor.getModifiers());
        assertEquals(true, dtor.isSynthetic());

        assertEquals(0, dtor.getExceptionTypes().length);
        assertEquals(0, dtor.getGenericExceptionTypes().length);

        assertEquals(0x7000, dtor.getPatternFlags()); // TODO what is?
        assertEquals(0, dtor.getTypeParameters().length);

        assertEquals("public pattern SimpleDeconstructorsTest$BasicDtor(int)", dtor.toGenericString());
        assertEquals(false, dtor.isVarArgs());
    }

    public static void testBindingAttributes() {
        Deconstructor<BasicDtor> dtor = BasicDtor.class.getDeconstructors()[0];

        // TODO: should expect int.class
        // assertEquals(List.of(), asList(dtor.getParameters())); // TODO broken
        assertEquals(List.of(), asList(dtor.getParameterTypes()));
        assertEquals(List.of(), asList(dtor.getGenericParameterTypes()));

        PatternBinding[] bindings = dtor.getPatternBindings();
        assertEquals(1, bindings.length);
        PatternBinding binding = bindings[0];

        assertEquals(dtor, binding.getDeclaringDeconstructor());
        assertEquals("field", binding.getName());

        assertEquals(int.class, binding.getType());
        assertEquals(int.class, binding.getGenericType());
        assertEquals("I", binding.getGenericSignature());
    }

    public static class PrivateDtor {
        int field;
        public PrivateDtor(int field) {
            this.field = field;
        }
        private pattern PrivateDtor(int field) {
            match PrivateDtor(field);
        }
    }

    public static void testPrivateDtor() throws NoSuchPatternException {
        assertEquals(0, PrivateDtor.class.getDeconstructors().length);

        Deconstructor<PrivateDtor> dtor = PrivateDtor.class.getDeclaredDeconstructor(int.class);

        // TODO implement Dtor.equals()
        // assertEquals(List.of(dtor), asList(PrivateDtor.class.getDeclaredDeconstructors()));

        try {
            PrivateDtor.class.getDeconstructor(int.class);
            throw new AssertionError("no exception was thrown");
        } catch (NoSuchPatternException expected) {
        }

        // TODO: static and synthetic seem wrong?
        assertEquals(EnumSet.of(AccessFlag.PRIVATE, AccessFlag.STATIC, AccessFlag.SYNTHETIC),
            dtor.accessFlags());
        assertEquals(Modifier.PRIVATE | Modifier.STATIC | 0x1000, dtor.getModifiers());

        assertEquals("private pattern SimpleDeconstructorsTest$PrivateDtor(int)", dtor.toGenericString());
    }

    public class NontrivialDtor {
        List<? extends String[]> field1;
        Map<? super Long, Integer>[] field2;

        public NontrivialDtor(List<? extends String[]> field1, Map<? super Long, Integer>[] field2) {
            this.field1 = field1;
            this.field2 = field2;
        }
        public pattern NontrivialDtor(
            List<? extends String[]> field1, Map<? super Long, Integer>[] field2) {
            match NontrivialDtor(field1, field2);
        }
    }

    public static void testNontrivialOutParams() throws NoSuchPatternException {
        Deconstructor<NontrivialDtor> dtor =
            NontrivialDtor.class.getDeconstructor(List.class, Map[].class);

        PatternBinding[] bindings = dtor.getPatternBindings();
        assertEquals(2, bindings.length);

        PatternBinding binding1 = bindings[0];

        assertEquals(dtor, binding1.getDeclaringDeconstructor());
        assertEquals("field1", binding1.getName());

        assertEquals(List.class, binding1.getType());
        assertEquals(List.class, ((ParameterizedType) binding1.getGenericType()).getRawType());

        Type arg = ((ParameterizedType) binding1.getGenericType()).getActualTypeArguments()[0];
        assertEquals(String[].class, ((WildcardType) arg).getUpperBounds()[0]);
        assertEquals("Ljava/util/List<+[Ljava/lang/String;>;", binding1.getGenericSignature());
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

    static void assertEquals(Object expected, Object actual) {
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



