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
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import sun.invoke.util.BytecodeName;

import static java.lang.invoke.MethodHandleInfo.REF_invokeInterface;
import static java.lang.invoke.MethodHandleInfo.REF_invokeStatic;
import static java.lang.invoke.MethodHandleInfo.REF_invokeVirtual;
import static java.lang.invoke.MethodHandleInfo.REF_newInvokeSpecial;
import static java.util.Objects.requireNonNull;

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
 * Notes:
 *  - totality is erased;
 *  - compilers expected to optimize away total type patterns;
 *  - adaptation done in nest() and switch combinators
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
     * Returns the component method handles, as an array
     * @return the component method handles
     */
    MethodHandle[] components();

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
     * @param sentinel value to return when the extractor does not match
     * @return the composed method handle
     */
    default MethodHandle compose(MethodHandle target, Object sentinel) {
        int count = descriptor().parameterCount();
        MethodHandle[] components = components();
        Class<?> carrierType = tryMatch().type().returnType();
        Class<?> resultType = target.type().returnType();

        MethodHandle mh = MethodHandles.filterArguments(target, 0, components);
        mh = MethodHandles.permuteArguments(mh, MethodType.methodType(resultType, carrierType), new int[count]);
        mh = MethodHandles.guardWithTest(ExtractorImpl.MH_OBJECTS_NONNULL.asType(MethodType.methodType(boolean.class, carrierType)),
                                         mh,
                                         MethodHandles.dropArguments(MethodHandles.constant(resultType, sentinel), 0, carrierType));
        mh = MethodHandles.filterArguments(mh, 0, tryMatch());
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
     * Construct a partial method handle that uses the predicate as guardWithTest,
     * which applies the target if the test succeeds, and returns null if the
     * test fails.  The resulting method handle is of the same type as the
     * {@code target} method handle.
     * @param target
     * @param predicate
     * @return
     */
    private static MethodHandle partialize(MethodHandle target, MethodHandle predicate) {
        Class<?> targetType = target.type().parameterType(0);
        Class<?> carrierType = target.type().returnType();
        return MethodHandles.guardWithTest(predicate,
                                           target,
                                           MethodHandles.dropArguments(MethodHandles.constant(carrierType, null),
                                                                       0, targetType));
    }

    /**
     * Construct a method handle that delegates to target, unless the nth argument
     * is null, in which case it returns null
     */
    private static MethodHandle bailIfNthNull(MethodHandle target, int n) {
        MethodHandle test = ExtractorImpl.MH_OBJECTS_ISNULL.asType(ExtractorImpl.MH_OBJECTS_ISNULL.type().changeParameterType(0, target.type().parameterType(n)));
        test = MethodHandles.permuteArguments(test, target.type().changeReturnType(boolean.class), n);
        MethodHandle nullh = MethodHandles.dropArguments(MethodHandles.constant(target.type().returnType(), null), 0, target.type().parameterArray());
        return MethodHandles.guardWithTest(test, nullh, target);
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
        return new ExtractorImpl(descriptor,
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
        return new ExtractorImpl(descriptor,
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
        return new ExtractorImpl(descriptor(targetType, components),
                                 MethodHandles.identity(targetType), components);
    }

    /**
     * Create a partial {@linkplain Extractor} for a given set of component
     * method handles.
     *
     * @param targetType the target type
     * @param predicate The match predicate
     * @param components The component method handles
     * @return the extractor
     */
    public static Extractor ofPartial(Class<?> targetType, MethodHandle predicate, MethodHandle... components) {
        MethodType descriptor = descriptor(targetType, components);
        MethodHandle carrierTryExtract = carrierTryExtract(descriptor, components);
        return new ExtractorImpl(descriptor,
                                 partialize(carrierTryExtract, predicate),
                                 ExtractorCarriers.carrierComponents(descriptor));
    }

    /**
     * Create a partial {@linkplain Extractor} for a given set of component
     * method handles, using itself as a carrier.
     *
     * @param targetType the target type
     * @param predicate The match predicate
     * @param components The component method handles
     * @return the extractor
     */
    public static Extractor ofSelfPartial(Class<?> targetType, MethodHandle predicate, MethodHandle... components) {
        return new ExtractorImpl(descriptor(targetType, components),
                                 partialize(MethodHandles.identity(targetType), predicate),
                                 components);
    }

    /**
     * Create an {@linkplain Extractor} for a type pattern, with a single binding
     * variable, whose target type is {@code Object}
     *
     * @param type the type to match against
     * @return the {@linkplain Extractor}
     */
    public static Extractor ofType(Class<?> type) {
        requireNonNull(type);
        if (type.isPrimitive())
            throw new IllegalArgumentException("Reference type expected, found: " + type);
        return new ExtractorImpl(MethodType.methodType(type, type),
                                 ExtractorImpl.MH_OF_TYPE_HELPER.bindTo(type).asType(MethodType.methodType(type, type)),
                                 MethodHandles.identity(type));
    }

    /**
     * Create an {@linkplain Extractor} for a constant pattern
     *
     * @param o the constant
     * @return the extractor
     */
    public static Extractor ofConstant(Object o) {
        Class<?> type = o == null ? Object.class : o.getClass();
        MethodHandle match = partialize(MethodHandles.dropArguments(MethodHandles.constant(Object.class, Boolean.TRUE), 0, type),
                                        MethodHandles.insertArguments(ExtractorImpl.MH_OBJECTS_EQUAL, 0, o)
                                                     .asType(MethodType.methodType(boolean.class, type)));
        return new ExtractorImpl(MethodType.methodType(type), match);
    }

    /**
     * Create an {@linkplain Extractor} for a nullable type pattern, with a
     * single binding variable, whose target type is {@code Object}
     *
     * @param type the type to match against
     * @return the {@linkplain Extractor}
     */
    public static Extractor ofTypeNullable(Class<?> type) {
        requireNonNull(type);
        if (type.isPrimitive())
            throw new IllegalArgumentException("Reference type expected, found: " + type);
        return new ExtractorImpl(MethodType.methodType(type, type),
                                 ExtractorImpl.MH_OF_TYPE_NULLABLE_HELPER.bindTo(type).asType(MethodType.methodType(type, type)),
                                 MethodHandles.identity(type));
    }

    /**
     * Create an {@linkplain Extractor} that is identical to another {@linkplain Extractor},
     * but without the specified binding variables
     * @param etor the original extractor
     * @param positions which binding variables to drop
     * @return the extractor
     */
    public static Extractor dropBindings(Extractor etor, int... positions) {
        MethodHandle[] mhs = etor.components();
        for (int position : positions)
            mhs[position] = null;
        mhs = Stream.of(mhs).filter(Objects::nonNull).toArray(MethodHandle[]::new);
        return new ExtractorImpl(descriptor(etor.descriptor().returnType(), mhs), etor.tryMatch(), mhs);
    }

    /**
     * Adapt an extractor to a new target type
     *
     * @param e the extractor
     * @param newTarget the new target type
     * @return the new extractor
     */
    public static Extractor adapt(Extractor e, Class<?> newTarget) {
        if (e.descriptor().returnType().isAssignableFrom(newTarget))
            return e;
        MethodHandle tryMatch = partialize(e.tryMatch().asType(e.tryMatch().type().changeParameterType(0, newTarget)),
                                           ExtractorImpl.MH_ADAPT_HELPER.bindTo(e.descriptor().returnType())
        .asType(MethodType.methodType(boolean.class, newTarget)));
        return new ExtractorImpl(e.descriptor().changeReturnType(newTarget),
                                 tryMatch, e.components());
    }

    /**
     * Construct a nested extractor, which first matches the target to the
     * outer extractor, and then matches the resulting bindings to the inner
     * extractors (if not null).  The resulting extractor is partial if any
     * of the input extractors are; its target type is the target type of the
     * outer extractor; and its bindings are the concatenation of the bindings
     * of the outer extractor followed by the bindings of the non-null inner
     * extractors.
     *
     * @param outer The outer extractor
     * @param extractors The inner extractors, or null if no nested extraction
     *                   for this outer binding is desired
     * @return the nested extractor
     */
    public static Extractor ofNested(Extractor outer, Extractor... extractors) {
        int outerCount = outer.descriptor().parameterCount();
        Class<?> outerCarrierType = outer.tryMatch().type().returnType();

        // Adapt inners to types of outer bindings
        for (int i = 0; i < extractors.length; i++) {
            Extractor extractor = extractors[i];
            if (extractor.descriptor().returnType() != outer.descriptor().parameterType(i))
                extractors[i] = adapt(extractor, outer.descriptor().parameterType(i));
        }

        int[] innerPositions = IntStream.range(0, extractors.length)
                                        .filter(i -> extractors[i] != null)
                                        .toArray();
        MethodHandle[] innerComponents = Stream.of(extractors)
                                               .filter(Objects::nonNull)
                                               .map(Extractor::components)
                                               .flatMap(Stream::of)
                                               .toArray(MethodHandle[]::new);
        MethodHandle[] innerTryMatches = Stream.of(extractors)
                                               .filter(Objects::nonNull)
                                               .map(e -> e.tryMatch())
                                               .toArray(MethodHandle[]::new);
        Class<?>[] innerCarriers = Stream.of(extractors)
                                         .filter(Objects::nonNull)
                                         .map(e -> e.tryMatch().type().returnType())
                                         .toArray(Class[]::new);
        Class<?>[] innerTypes = Stream.of(innerComponents)
                                      .map(mh -> mh.type().returnType())
                                      .toArray(Class[]::new);

        MethodType descriptor = outer.descriptor().appendParameterTypes(innerTypes);

        MethodHandle mh = ExtractorCarriers.carrierFactory(descriptor);
        mh = MethodHandles.filterArguments(mh, outerCount, innerComponents);
        int[] spreadInnerCarriers = new int[outerCount + innerComponents.length];
        for (int i=0; i<outerCount; i++)
            spreadInnerCarriers[i] = i;
        int k = outerCount;
        int j = 0;
        for (Extractor e : extractors) {
            if (e == null)
                continue;
            for (int i=0; i<e.descriptor().parameterCount(); i++)
                spreadInnerCarriers[k++] = outerCount + j;
            j++;
        }
        MethodType spreadInnerCarriersMT = outer.descriptor()
                                                .appendParameterTypes(innerCarriers)
                                                .changeReturnType(mh.type().returnType());
        mh = MethodHandles.permuteArguments(mh, spreadInnerCarriersMT, spreadInnerCarriers);
        for (int position : innerPositions)
            mh = bailIfNthNull(mh, outerCount + position);
        mh = MethodHandles.filterArguments(mh, outerCount, innerTryMatches);
        int[] spreadNestedCarrier = new int[outerCount + innerPositions.length];
        for (int i=0; i<outerCount; i++)
            spreadNestedCarrier[i] = i;
        for (int i=0; i<innerPositions.length; i++)
            spreadNestedCarrier[outerCount+i] = innerPositions[i];
        mh = MethodHandles.permuteArguments(mh, outer.descriptor().changeReturnType(mh.type().returnType()),
                                            spreadNestedCarrier);
        mh = MethodHandles.filterArguments(mh, 0, outer.components());
        mh = MethodHandles.permuteArguments(mh, MethodType.methodType(mh.type().returnType(), outerCarrierType),
                                            new int[outerCount]);
        mh = bailIfNthNull(mh, 0);
        mh = MethodHandles.filterArguments(mh, 0, outer.tryMatch());

        MethodHandle tryExtract = mh;

        return new ExtractorImpl(descriptor, tryExtract, ExtractorCarriers.carrierComponents(descriptor));
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
     * Condy bootstrap for creating nested extractors
     *
     * @param lookup ignored
     * @param invocationName ignored
     * @param invocationType must be {@code Class<Extractor>}
     * @param outer the outer extractor
     * @param inners the inner extractors, null if no nesting is needed for this binding
     * @return the nested extractor
     */
    public static Extractor ofNested(MethodHandles.Lookup lookup, String invocationName, Class<Extractor> invocationType,
                                     Extractor outer, Extractor... inners) {
        return ofNested(outer, inners);
    }

    /**
     * Condy bootstrap for creating non-nullable type extractor
     *
     * @param lookup ignored
     * @param invocationName ignored
     * @param invocationType must be {@code Class<Extractor>}
     * @param type the type
     * @return the extractor
     */
    public static Extractor ofType(MethodHandles.Lookup lookup, String invocationName, Class<Extractor> invocationType,
                                   Class<?> type) {
        return ofType(type);
    }

    /**
     * Condy bootstrap for creating nullable type extractor
     *
     * @param lookup ignored
     * @param invocationName ignored
     * @param invocationType must be {@code Class<Extractor>}
     * @param type the type
     * @return the extractor
     */
    public static Extractor ofTypeNullable(MethodHandles.Lookup lookup, String invocationName, Class<Extractor> invocationType,
                                           Class<?> type) {
        return ofTypeNullable(type);
    }

    /**
     * Condy bootstrap for creating constant extractor
     *
     * @param lookup ignored
     * @param invocationName ignored
     * @param invocationType must be {@code Class<Extractor>}
     * @param constant the constant
     * @return the extractor
     */
    public static Extractor ofConstant(MethodHandles.Lookup lookup, String invocationName, Class<Extractor> invocationType,
                                       Object constant) {
        return ofConstant(constant);
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
        String patternMethodName
                = BytecodeName.toBytecodeName(String.format("$pattern$%s$%s",
                                                            (refKind == REF_newInvokeSpecial ? owner.getSimpleName() : name),
                                                            dd.substring(0, dd.indexOf(')') + 1)));
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
