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

import java.lang.Class;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.sym.ClassRef;
import java.lang.sym.Constable;
import java.lang.sym.ConstantRef;
import java.lang.sym.SymbolicRef;
import java.lang.sym.SymbolicRefs;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;

/**
 * Base class for XxxRef tests
 */
public abstract class SymbolicRefTest {

    public static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    static List<String> someRefs = List.of("Ljava/lang/String;", "Ljava/util/List;");
    static String[] basicDescs = Stream.concat(Stream.of(Primitives.values())
                                                     .filter(p -> p != Primitives.VOID)
                                                     .map(p -> p.descriptor),
                                               someRefs.stream())
                                       .toArray(String[]::new);
    static String[] paramDescs = Stream.of(basicDescs)
                                       .flatMap(d -> Stream.of(d, "[" + d))
                                       .toArray(String[]::new);
    static String[] returnDescs = Stream.concat(Stream.of(paramDescs), Stream.of("V")).toArray(String[]::new);

    enum Primitives {
        INT("I", "int", int.class, int[].class, SymbolicRefs.CR_int),
        LONG("J", "long", long.class, long[].class, SymbolicRefs.CR_long),
        SHORT("S", "short", short.class, short[].class, SymbolicRefs.CR_short),
        BYTE("B", "byte", byte.class, byte[].class, SymbolicRefs.CR_byte),
        CHAR("C", "char", char.class, char[].class, SymbolicRefs.CR_char),
        FLOAT("F", "float", float.class, float[].class, SymbolicRefs.CR_float),
        DOUBLE("D", "double", double.class, double[].class, SymbolicRefs.CR_double),
        BOOLEAN("Z", "boolean", boolean.class, boolean[].class, SymbolicRefs.CR_boolean),
        VOID("V", "void", void.class, null, SymbolicRefs.CR_void);

        public final String descriptor;
        public final String name;
        public final Class<?> clazz;
        public final Class<?> arrayClass;
        public final ClassRef classRef;

        Primitives(String descriptor, String name, Class<?> clazz, Class<?> arrayClass, ClassRef ref) {
            this.descriptor = descriptor;
            this.name = name;
            this.clazz = clazz;
            this.arrayClass = arrayClass;
            classRef = ref;
        }
    }

    static String classToDescriptor(Class<?> clazz) {
        return MethodType.methodType(clazz).toMethodDescriptorString().substring(2);
    }

    static ClassRef classToRef(Class<?> c) {
        return ClassRef.ofDescriptor(c.toDescriptorString());
    }

    static<T> void testSymbolicRef(ConstantRef<T> ref) throws ReflectiveOperationException {
        // Round trip sym -> resolve -> toSymbolicRef
        ConstantRef<? super ConstantRef<T>> s = ((Constable<ConstantRef<T>>) ref.resolveRef(LOOKUP)).toSymbolicRef(LOOKUP).orElseThrow();
        assertEquals(ref, s);

        // Round trip sym -> quoted sym -> resolve
        Method m = ref.getClass().getMethod("toSymbolicRef", MethodHandles.Lookup.class);
        Optional<ConstantRef<ConstantRef<T>>> opt = (Optional<ConstantRef<ConstantRef<T>>>) m.invoke(ref, LOOKUP);
        ConstantRef<T> sr = opt.orElseThrow().resolveRef(LOOKUP);
        assertEquals(sr, ref);
    }
}
