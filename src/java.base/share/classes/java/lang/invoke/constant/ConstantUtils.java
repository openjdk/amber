/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package java.lang.invoke.constant;

import java.lang.invoke.MethodHandles;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

/**
 * ConstantUtils
 *
 * @author Brian Goetz
 */
class ConstantUtils {
    static final ConstantRef<?>[] EMPTY_CONSTANTREF = new ConstantRef<?>[0];
    static final Constable<?>[] EMPTY_CONSTABLE = new Constable<?>[0];

    private static final Set<String> pointyNames = Set.of("<init>", "<clinit>");

    static String validateBinaryClassName(String name) {
        for (int i=0; i<name.length(); i++) {
            char ch = name.charAt(i);
            if (ch == ';' || ch == '[' || ch == '/')
                throw new IllegalArgumentException("Invalid class name: " + name);
        }
        return name;
    }

    static String validateMemberName(String name) {
        if (name.length() == 0)
            throw new IllegalArgumentException("zero-length member name");
        for (int i=0; i<name.length(); i++) {
            char ch = name.charAt(i);
            if (ch == '.' || ch == ';' || ch == '[' || ch == '/')
                throw new IllegalArgumentException("Invalid member name: " + name);
            if (ch == '<' || ch == '>') {
                if (!pointyNames.contains(name))
                    throw new IllegalArgumentException("Invalid member name: " + name);
            }
        }
        return name;
    }

    static void validateClassOrInterface(ClassRef clazz) {
        if (!clazz.isClassOrInterface())
            throw new IllegalArgumentException("not a class or interface type: " + clazz);
    }

    static int arrayDepth(String descriptorString) {
        int depth = 0;
        while (descriptorString.charAt(depth) == '[')
            depth++;
        return depth;
    }

    static String binaryToInternal(String name) {
        return name.replace('.', '/');
    }

    static String internalToBinary(String name) {
        return name.replace('/', '.');
    }

    static String dropLastChar(String s) {
        return s.substring(0, s.length() - 1);
    }

    static String dropFirstAndLastChar(String s) {
        return s.substring(1, s.length() - 1);
    }

    /**
     * Produce an {@code Optional<DynamicConstantRef<T>>} describing the invocation
     * of the specified bootstrap with the specified arguments.  The arguments will
     * be converted to nominal descriptors using the provided lookup.  Helper
     * method for implementing {@link DynamicConstantRef#toConstantRef(MethodHandles.Lookup)}.
     *
     * @param lookup A {@link MethodHandles.Lookup} to be used to perform
     *               access control determinations
     * @param bootstrap nominal descriptor for the bootstrap method
     * @param type nominal descriptor for the type of the resulting constant
     * @param args nominal descriptors for the bootstrap arguments
     * @param <T> the type of the resulting constant
     * @return the nominal descriptor for the dynamic constant
     */
    static<T> Optional<DynamicConstantRef<T>> symbolizeHelper(MethodHandles.Lookup lookup,
                                                              MethodHandleRef bootstrap,
                                                              ClassRef type,
                                                              Constable<?>... args) {
        try {
            ConstantRef<?>[] quotedArgs = new ConstantRef<?>[args.length + 1];
            quotedArgs[0] = bootstrap;
            for (int i=0; i<args.length; i++)
                quotedArgs[i+1] = args[i].toConstantRef(lookup).orElseThrow();
            return Optional.of(DynamicConstantRef.of(ConstantRefs.BSM_INVOKE, ConstantRefs.DEFAULT_NAME,
                                                     type, quotedArgs));
        }
        catch (NoSuchElementException e) {
            return Optional.empty();
        }
    }
}
