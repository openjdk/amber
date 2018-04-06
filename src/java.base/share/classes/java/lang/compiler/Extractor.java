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
package java.lang.compiler;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import sun.invoke.util.BytecodeName;

import static java.lang.invoke.MethodHandleInfo.REF_invokeInterface;
import static java.lang.invoke.MethodHandleInfo.REF_invokeStatic;
import static java.lang.invoke.MethodHandleInfo.REF_invokeVirtual;
import static java.lang.invoke.MethodHandleInfo.REF_newInvokeSpecial;

/**
 * Supporting type for implementation of pattern matching.  An {@linkplain Extractor}
 * is a constant bundle of method handles that describe a particular pattern, and
 * suitable for storing in the constant pool.
 *
 * <p>An {@linkplain Extractor} is describe by a {@code descriptor} in the form
 * of a {@link MethodType}.  The return value of the descriptor is ignored; the
 * argument types of the descriptor indicate the types of the output binding
 * variables.
 *
 * @author Brian Goetz
 */
public interface Extractor {
    /**
     * A method handle that attempts to perform a match.  It will have type
     * {@code (Object)Object}.  It accepts the target to be matched, and returns
     * an opaque carrier if the match succeeds, or {@code null} if it fails.
     *
     * @return the {@code tryMatch} method handle
     */
    MethodHandle tryMatch();

    /**
     * A method handle that extracts a component from the match carrier.  It
     * will take the match carrier and return the corresponding match binding.
     *
     * @param i the index of the component
     * @return the {@code component} method handle
     */
    MethodHandle component(int i);

    /**
     * The descriptor of the {@linkplain Extractor}.  The parameter types of
     * the descriptor are the types of the binding variables.  The return type
     * is ignored.
     *
     * @return the descriptor
     */
    MethodType descriptor();

    /**
     * Compose an extractor with a method handle that receives the bindings
     *
     * @param target method handle to receive the bindings
     * @return the composed method handle
     */
    default MethodHandle compose(MethodHandle target) {
        int count = descriptor().parameterCount();
        MethodHandle[] components = new MethodHandle[count];
        int[] reorder = new int[count];
        for (int i=0; i<count; i++) {
            components[i] = component(i);
            reorder[i] = 0;
        }

        MethodHandle mh = MethodHandles.filterArguments(target, 0, components);
        mh = MethodHandles.permuteArguments(mh, MethodType.methodType(target.type().returnType(), tryMatch().type().returnType()),
                                            reorder);
        mh = MethodHandles.filterArguments(mh, 0, tryMatch());
        // @@@ What if pattern doesn't match?
        return mh;
    }


    /**
     * Create an {@linkplain Extractor} from its components
     *
     * @param descriptor the descriptor method type
     * @param tryMatch the {@code tryMatch} method handle
     * @param components the {@code components} method handles
     * @return the {@linkplain Extractor}
     */
    public static Extractor of(MethodType descriptor,
                               MethodHandle tryMatch,
                               MethodHandle... components) {
        return new ExtractorImpl(descriptor, tryMatch, components);
    }

    /**
     * Create a lazy, self-carrier {@linkplain Extractor}
     *
     * @param descriptor the descriptor method type
     * @param components the {@code component} method handles
     * @return the {@linkplain Extractor}
     */
    public static Extractor ofLazy(MethodType descriptor,
                                   MethodHandle... components) {
        return of(descriptor, MethodHandles.identity(descriptor.returnType()), components);
    }

    /**
     * Create a lazy, partial, self-carrier {@linkplain Extractor}
     *
     * @param descriptor the descriptor method type
     * @param components the {@code component} method handles
     * @param predicate a {@link MethodHandle} that accepts the target and returns
     *                  boolean, indicating whether the pattern matched
     * @return the {@linkplain Extractor}
     */
    public static Extractor ofLazyPartial(MethodType descriptor,
                                          MethodHandle predicate,
                                          MethodHandle... components) {
        Class<?> targetType = descriptor.returnType();
        MethodHandle tryMatch = MethodHandles.guardWithTest(predicate,
                                                            MethodHandles.identity(targetType),
                                                            MethodHandles.dropArguments(MethodHandles.constant(targetType, null), 0, targetType));
        return of(descriptor, tryMatch, components);
    }

    /**
     * Create a partial, self-carrier {@linkplain Extractor}
     * @param descriptor the descriptor method type
     * @param copier a {@link MethodHandle} that clones the target
     * @param predicate a {@link MethodHandle} that accepts the target and returns
     *                  boolean, indicating whether the pattern matched
     * @param components the {@code component} method handles
     * @return the {@linkplain Extractor}
     */
    public static Extractor ofPartial(MethodType descriptor,
                                      MethodHandle copier,
                                      MethodHandle predicate,
                                      MethodHandle... components) {

        Class<?> targetType = descriptor.returnType();
        MethodHandle guarded = MethodHandles.guardWithTest(predicate,
                                                           MethodHandles.identity(targetType),
                                                           MethodHandles.dropArguments(MethodHandles.constant(targetType, null), 0, targetType));
        MethodHandle tryMatch = MethodHandles.filterArguments(guarded, 0, copier);
        return of(descriptor, tryMatch, components);
    }

    // target digester = (R, MH[CDESC]->Obj) -> Obj, where MH[...] is carrier ctor

    /**
     * Create a {@linkplain Extractor} using a carrier specified by a descriptor.
     *
     * <p>
     *
     * @param descriptor the extractor descriptor
     * @param carrierFactory a method handle to create the carrier from the target
     * @param digester a {@link MethodHandle} that accepts a target and a carrier
     *                 factory method handle, and which calls the factory with the
     *                 values extracted from the target
     * @param components the {@code component} method handles
     * @return the {@linkplain Extractor}
     */
    public static Extractor ofCarrier(MethodType descriptor,
                                      MethodHandle carrierFactory,
                                      MethodHandle digester,
                                      MethodHandle... components) {
        MethodHandle tryMatch = MethodHandles.insertArguments(digester, 1, carrierFactory);
        return of(descriptor, tryMatch, components);
    }

    /**
     * Create a {@linkplain Extractor} using a carrier specified by a descriptor.
     *
     * <p>
     *
     * @param descriptor the extractor descriptor
     * @param carrierFactory a method handle to create the carrier from the target
     * @param digester a {@link MethodHandle} that accepts a target and a carrier
     *                 factory method handle, and which calls the factory with the
     *                 values extracted from the target
     * @param predicate Predicate, applied to the carrier values, determining
     *                  whether there was a match
     * @param components the {@code component} method handles
     * @return the {@linkplain Extractor}
     */
    public static Extractor ofCarrierPartial(MethodType descriptor,
                                             MethodHandle carrierFactory,
                                             MethodHandle digester,
                                             MethodHandle predicate,
                                             MethodHandle... components) {
        MethodHandle nuller = MethodHandles.constant(Object.class, null);
        nuller = MethodHandles.dropArguments(nuller, 0, carrierFactory.type().parameterList());
        MethodHandle guarded = MethodHandles.guardWithTest(predicate, carrierFactory, nuller);
        MethodHandle tryMatch = MethodHandles.insertArguments(digester, 1, guarded);
        return of(descriptor, tryMatch, components);
    }

    /**
     * Create an {@linkplain Extractor} for a type pattern, with a single binding
     * variable
     *
     * @param type the type to match against
     * @return the {@linkplain Extractor}
     */
    public static Extractor ofType(Class<?> type) {
        // tryMatch = (t instanceof type) ? t : null
        // component = (type) o
        return null;
    }

    /**
     * Bootstrap for creating an {@linkplain Extractor} from components
     *
     * @param lookup ignored
     * @param constantName ignored
     * @param constantType Must be {@code Extractor.class}
     * @param descriptor the descriptor method type
     * @param tryMatch the {@code tryMatch} method handle
     * @param components the {@code components} method handles
     * @return the {@linkplain Extractor}
     */
    public static Extractor of(MethodHandles.Lookup lookup, String constantName, Class<Extractor> constantType,
                               MethodType descriptor, MethodHandle tryMatch, MethodHandle... components) {
        return Extractor.of(descriptor, tryMatch, components);
    }

    /**
     * Bootstrap for creating a lazy, partial, self-carrier {@linkplain Extractor} from components
     *
     * @param lookup ignored
     * @param constantName ignored
     * @param constantType Must be {@code Extractor.class}
     * @param descriptor the descriptor method type
     * @param components the {@code components} method handles
     * @return the {@linkplain Extractor}
     */
    public static Extractor ofLazy(MethodHandles.Lookup lookup, String constantName, Class<Extractor> constantType,
                                   MethodType descriptor, MethodHandle... components) {
        return Extractor.ofLazy(descriptor, components);
    }

    /**
     * Bootstrap for creating a lazy, partial, self-carrier {@linkplain Extractor} from components
     *
     * @param lookup ignored
     * @param constantName ignored
     * @param constantType doc
     * @param descriptor the descriptor method type
     * @param components the {@code components} method handles
     * @return a callsite
     * @throws Throwable doc
     */
    public static CallSite makeLazyExtractor(MethodHandles.Lookup lookup, String constantName, MethodType constantType,
                                             MethodType descriptor, MethodHandle... components) throws Throwable {
        return new ConstantCallSite(MethodHandles.constant(Extractor.class, ofLazy(descriptor, components)));
    }

    /**
     * Bootstrap for creating a lazy, partial, self-carrier {@linkplain Extractor} from components
     *
     * @param lookup ignored
     * @param constantName ignored
     * @param constantType Must be {@code Extractor.class}
     * @param descriptor the descriptor method type
     * @param predicate predicate method handle, applied to target
     * @param components the {@code components} method handles
     * @return the {@linkplain Extractor}
     */
    public static Extractor ofLazyPartial(MethodHandles.Lookup lookup, String constantName, Class<Extractor> constantType,
                                          MethodType descriptor, MethodHandle predicate, MethodHandle... components) {
        return ofLazyPartial(descriptor, predicate, components);
    }

    /**
     * Bootstrap for creating a lazy, partial, self-carrier {@linkplain Extractor} from components
     *
     * @param lookup ignored
     * @param constantName ignored
     * @param constantType Must be {@code Extractor.class}
     * @param descriptor the descriptor method type
     * @param copier a {@link MethodHandle} that clones the target
     * @param predicate predicate method handle, applied to target
     * @param components the {@code components} method handles
     * @return the {@linkplain Extractor}
     */
    public static Extractor ofPartial(MethodHandles.Lookup lookup, String constantName, Class<Extractor> constantType,
                                      MethodType descriptor, MethodHandle copier, MethodHandle predicate, MethodHandle... components) {
        return ofPartial(descriptor, copier, predicate, components);
    }


    /**
     * Create a {@linkplain Extractor} using a carrier specified by a descriptor.
     *
     * <p>
     *
     * @param lookup ignored
     * @param constantName ignored
     * @param constantType Must be {@code Extractor.class}
     * @param descriptor the extractor descriptor
     * @param carrierFactory a method handle to create the carrier from the target
     * @param digester a {@link MethodHandle} that accepts a target and a carrier
     *                 factory method handle, and which calls the factory with the
     *                 values extracted from the target
     * @param components the {@code component} method handles
     * @return the {@linkplain Extractor}
     */
    public static Extractor makeCarrierExtractor(MethodHandles.Lookup lookup, String constantName, Class<Extractor> constantType,
                                                 MethodType descriptor,
                                                 MethodHandle carrierFactory,
                                                 MethodHandle digester,
                                                 MethodHandle... components) {
        return ofCarrier(descriptor, carrierFactory, digester, components);
    }

    /**
     * Create a {@linkplain Extractor} using a carrier specified by a descriptor.
     *
     * <p>
     *
     * @param lookup ignored
     * @param constantName ignored
     * @param constantType Must be {@code Extractor.class}
     * @param descriptor the extractor descriptor
     * @param carrierFactory a method handle to create the carrier from the target
     * @param digester a {@link MethodHandle} that accepts a target and a carrier
     *                 factory method handle, and which calls the factory with the
     *                 values extracted from the target
     * @param predicate Predicate, applied to the carrier values, determining
     *                  whether there was a match
     * @param components the {@code component} method handles
     * @return the {@linkplain Extractor}
     */
    public static Extractor makeCarrierPartialExtractor(MethodHandles.Lookup lookup, String constantName, Class<Extractor> constantType,
                                                        MethodType descriptor,
                                                        MethodHandle carrierFactory,
                                                        MethodHandle digester,
                                                        MethodHandle predicate,
                                                        MethodHandle... components) {
        return ofCarrierPartial(descriptor, carrierFactory, digester, predicate, components);
    }

    /**
     * Condy bootstrap for creating lazy extractors
     *
     * @param lookup ignored
     * @param constantName ignored
     * @param constantType ignored
     * @param descriptor the extractor descriptor
     * @param components the extractor components
     * @return the extractor factory
     * @throws Throwable if something went wrong
     */

    public static Extractor makeLazyExtractor(MethodHandles.Lookup lookup, String constantName, Class<Extractor> constantType,
                                              MethodType descriptor, MethodHandle... components) throws Throwable {
        return ofLazy(descriptor, components);
    }


    /**
     * Condy bootstrap for finding extractors
     *
     * @param lookup the lookup context
     * @param constantName ignored
     * @param constantType ignored
     * @param owner the class containing the extractor
     * @param descriptor the extractor descriptor
     * @param name the extractor name
     * @param refKind the kind of method
     * @return the extractor
     * @throws Throwable if something went wrong
     */
    public static Extractor findExtractor(MethodHandles.Lookup lookup, String constantName, Class<Extractor> constantType,
                                          Class<?> owner, MethodType descriptor, String name, int refKind) throws Throwable {
        String dd = descriptor.toMethodDescriptorString();
        dd = dd.substring(0, dd.indexOf(')') + 1);
        String patternMethodName
                = BytecodeName.toBytecodeName(String.format("$pattern$%s$%s",
                                                            (refKind == REF_newInvokeSpecial ? owner.getSimpleName() : name),
                                                            dd));
        MethodType factoryDesc = MethodType.methodType(Extractor.class);
        MethodHandle mh;
        switch (refKind) {
            case REF_invokeStatic:
            case REF_newInvokeSpecial:
                mh = lookup.findStatic(owner, patternMethodName, factoryDesc);
                break;
            case REF_invokeVirtual:
            case REF_invokeInterface:
                mh = lookup.findVirtual(owner, patternMethodName, factoryDesc);
                break;
            default:
                throw new IllegalAccessException(Integer.toString(refKind));
        }

        return (Extractor) mh.invoke();
    }

    /**
     * Bootstrap for extracting the {@code tryMatch} method handle from a {@linkplain Extractor}
     *
     * @param lookup ignored
     * @param constantName ignored
     * @param constantType Must be {@code MethodHandle.class}
     * @param extractor the {@linkplain Extractor}
     * @return the {@code tryMatch} method handle
     */


    public static MethodHandle extractorTryMatch(MethodHandles.Lookup lookup, String constantName, Class<MethodHandle> constantType,
                                                 Extractor extractor) {
        return extractor.tryMatch();
    }

    /**
     * Bootstrap for extracting a {@code component} method handle from a {@linkplain Extractor}
     *
     * @param lookup ignored
     * @param constantName ignored
     * @param constantType Must be {@code MethodHandle.class}
     * @param extractor the {@linkplain Extractor}
     * @param i the component index
     * @return the {@code component} method handle
     */
    public static MethodHandle extractorComponent(MethodHandles.Lookup lookup, String constantName, Class<MethodHandle> constantType,
                                                  Extractor extractor, int i) {
        return extractor.component(i);
    }

}
