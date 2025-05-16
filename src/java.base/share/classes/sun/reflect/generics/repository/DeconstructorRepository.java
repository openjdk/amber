/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package sun.reflect.generics.repository;

import sun.reflect.generics.factory.GenericsFactory;
import sun.reflect.generics.parser.SignatureParser;
import sun.reflect.generics.tree.FieldTypeSignature;
import sun.reflect.generics.tree.MethodTypeSignature;
import sun.reflect.generics.tree.TypeSignature;
import sun.reflect.generics.visitor.Reifier;

import java.lang.reflect.Type;


/**
 * This class represents the generic type information for a deconstructor.
 * The code is not dependent on a particular reflective implementation.
 * It is designed to be used unchanged by at least core reflection and JDI.
 */
public class DeconstructorRepository
    extends ExecutableRepository {

    /** The generic binding types.  Lazily initialized. */
    private volatile Type[] bindingTypes;

    // protected, to enforce use of static factory yet allow subclassing
    protected DeconstructorRepository(String rawSig, GenericsFactory f) {
      super(rawSig, f);
    }

    protected MethodTypeSignature parse(String s) {
        return SignatureParser.make().parseMethodSig(s);
    }

    /**
     * Static factory method.
     * @param rawSig - the generic signature of the reflective object
     * that this repository is servicing
     * @param f - a factory that will provide instances of reflective
     * objects when this repository converts its AST
     * @return a {@code ConstructorRepository} that manages the generic type
     * information represented in the signature {@code rawSig}
     */
    public static DeconstructorRepository make(String rawSig, GenericsFactory f) {
        return new DeconstructorRepository(rawSig, f);
    }

    public Type[] getBindingTypes() {
        Type[] value = bindingTypes;
        if (value == null) {
            value = computeBindingTypes();
            bindingTypes = value;
        }
        return value.clone();
    }

    private Type[] computeBindingTypes() {
        // first, extract parameter type subtree(s) from AST
        // todo: introduce getBindingTypes
        TypeSignature[] pts = getTree().getParameterTypes();
        // create array to store reified subtree(s)
        int length = pts.length;
        Type[] bindingTypes = new Type[length];
        // reify all subtrees
        for (int i = 0; i < length; i++) {
            Reifier r = getReifier(); // obtain visitor
            pts[i].accept(r); // reify subtree
            // extract result from visitor and store it
            bindingTypes[i] = r.getResult();
        }
        return bindingTypes;
    }
}
