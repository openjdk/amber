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
import java.util.stream.Stream;

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
     * Whether this extractor might fail.
     * @return if this extractor might fail
     */
    boolean isPartial();

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

    private static MethodType descriptor(Class<?> targetType, MethodHandle[] components) {
        Class<?>[] paramTypes = Stream.of(components)
                                      .map(mh -> mh.type().returnType())
                                      .toArray(Class[]::new);
        return MethodType.methodType(targetType, paramTypes);
    }

    private static MethodHandle carrierTryExtract(MethodType descriptor, MethodHandle[] components) {
        MethodHandle carrierFactory = ExtractorCarriers.carrierFactory(descriptor);
        int[] reorder = new int[descriptor.parameterCount()]; // default value is what we want already

        return MethodHandles.permuteArguments(MethodHandles.filterArguments(carrierFactory, 0, components),
                                              MethodType.methodType(carrierFactory.type().returnType(), descriptor.returnType()),
                                              reorder);
    }

    /**
     * Create a total {@linkplain Extractor} with the given descriptor, which
     * operates by feeding results into a factory method handle and returning
     * the result.
     *
     * @param descriptor the descriptor
     * @param digester the digester method handle
     * @return the extractor
     */
    public static Extractor of(MethodType descriptor,
                               MethodHandle digester) {
        return new ExtractorImpl(descriptor, false,
                                 MethodHandles.insertArguments(digester,
                                                               1, ExtractorCarriers.carrierFactory(descriptor)),
                                 ExtractorCarriers.carrierComponents(descriptor));
    }

    /**
     * Create a partial {@linkplain Extractor} with the given descriptor, which
     * operates by feeding results into a factory method handle and returning
     * the result.
     *
     * @param descriptor the descriptor
     * @param digester the digester method handle
     * @return the extractor
     */
    public static Extractor ofPartial(MethodType descriptor,
                                      MethodHandle digester) {
        return new ExtractorImpl(descriptor, true,
                                 MethodHandles.insertArguments(digester,
                                                               1, ExtractorCarriers.carrierFactory(descriptor)),
                                 ExtractorCarriers.carrierComponents(descriptor));
    }

    /**
     * Create a total {@linkplain Extractor} for a target of type {@code targetType}
     * and a given set of component method handles.
     *
     * @param targetType The type of the match target
     * @param components The component method handles
     * @return the extractor
     */
    public static Extractor ofTotal(Class<?> targetType, MethodHandle... components) {
        MethodType descriptor = descriptor(targetType, components);
        return new ExtractorImpl(descriptor, false,
                                 carrierTryExtract(descriptor, components),
                                 ExtractorCarriers.carrierComponents(descriptor));
    }

    /**
     * Create a total {@linkplain Extractor} for a target of type {@code targetType}
     * and a given set of component method handles, using itself as a carrier.
     *
     * @param targetType The type of the match target
     * @param components The component method handles
     * @return the extractor
     */
    public static Extractor ofSelfTotal(Class<?> targetType, MethodHandle... components) {
        return new ExtractorImpl(descriptor(targetType, components), false,
                                 MethodHandles.identity(targetType), components);
    }

    /**
     * Create a partial {@linkplain Extractor} for a given set of component
     * method handles.
     *
     * @param predicate The match predicate
     * @param components The component method handles
     * @return the extractor
     */
    public static Extractor ofPartial(MethodHandle predicate, MethodHandle... components) {
        Class<?> targetType = predicate.type().parameterType(0);
        MethodType descriptor = descriptor(targetType, components);
        MethodHandle carrierTryExtract = carrierTryExtract(descriptor, components);
        MethodHandle tryExtract = MethodHandles.guardWithTest(predicate,
                                                              carrierTryExtract,
                                                              MethodHandles.dropArguments(MethodHandles.constant(carrierTryExtract.type().returnType(), null),
                                                                                          0, targetType));
        return new ExtractorImpl(descriptor, true,
                                 tryExtract, ExtractorCarriers.carrierComponents(descriptor));
    }

    /**
     * Create a partial {@linkplain Extractor} for a given set of component
     * method handles, using itself as a carrier.
     *
     * @param predicate The match predicate
     * @param components The component method handles
     * @return the extractor
     */
    public static Extractor ofSelfPartial(MethodHandle predicate, MethodHandle... components) {
        Class<?> targetType = predicate.type().parameterType(0);
        MethodHandle tryExtract = MethodHandles.guardWithTest(predicate,
                                                              MethodHandles.identity(targetType),
                                                              MethodHandles.dropArguments(MethodHandles.constant(targetType, null),
                                                                                          0, targetType));
        return new ExtractorImpl(descriptor(targetType, components), true, tryExtract, components);
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
     * Bootstrap for creating a lazy, partial, self-carrier {@linkplain Extractor} from components
     *
     * @param lookup ignored
     * @param constantName ignored
     * @param constantType Must be {@code ()Extractor}
     * @param descriptor the descriptor method type
     * @param components the {@code components} method handles
     * @return a callsite
     * @throws Throwable doc
     */
    public static CallSite makeLazyExtractor(MethodHandles.Lookup lookup, String constantName, MethodType constantType,
                                             MethodType descriptor, MethodHandle... components) throws Throwable {
        return new ConstantCallSite(MethodHandles.constant(Extractor.class, ofSelfTotal(descriptor.returnType(), components)));
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
    public static Extractor ofSelfTotal(MethodHandles.Lookup lookup, String constantName, Class<Extractor> constantType,
                                        MethodType descriptor, MethodHandle... components) throws Throwable {
        return ofSelfTotal(descriptor.returnType(), components);
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
