/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package java.lang.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import sun.invoke.util.BytecodeName;
import sun.invoke.util.Wrapper;

import static java.lang.invoke.MethodHandleInfo.REF_invokeInterface;
import static java.lang.invoke.MethodHandleInfo.REF_invokeStatic;
import static java.lang.invoke.MethodHandleInfo.REF_invokeVirtual;
import static java.lang.invoke.MethodHandleInfo.REF_newInvokeSpecial;
import static java.util.Objects.requireNonNull;

/**
 * Factories and combinators for {@link PatternHandle}s.
 */
public final class PatternHandles {
    private static final MethodHandle[] EMPTY_MH_ARRAY = new MethodHandle[0];
    private static final Object NULL_SENTINEL = new Object();

    private PatternHandles() {
    }

    // Factories

    /**
     * Returns a {@linkplain PatternHandle} for a <em>type pattern</em>, which
     * matches all non-null instances of the match type, with a single binding
     * variable which is the target cast to the match type.  The target type of
     * the resulting pattern is the match type; if a broader target type is
     * desired, use {@link #ofType(Class, Class)} or adapt the resulting pattern
     * handle with {@link #adaptTarget(PatternHandle, Class)}.
     *
     * @param matchType the type to match against
     * @return a pattern handle for a type pattern
     */
    public static PatternHandle ofType(Class<?> matchType) {
        requireNonNull(matchType);
        MethodType descriptor = MethodType.methodType(matchType, matchType);
        MethodHandle component = MethodHandles.identity(matchType);
        MethodHandle tryMatch
                = matchType.isPrimitive()
                  ? MethodHandles.identity(matchType)
                  : MH_OF_TYPE_TRY_MATCH.bindTo(matchType).asType(descriptor);

        return new PatternHandleImpl(descriptor, tryMatch, List.of(component));
    }

    /**
     * Returns a {@linkplain PatternHandle} for a <em>type pattern</em>, which
     * matches all non-null instances of the match type, with a single binding
     * variable which is the target cast to the match type.  The target type of
     * the resulting pattern is the {@code targetType}.
     *
     * @param matchType  the type to match against
     * @param targetType the desired target type for the resulting pattern
     *                   handle
     * @return a pattern handle for a type pattern
     * @throws IllegalArgumentException if the provided match type and target
     *                                  type are not compatible
     */
    public static PatternHandle ofType(Class<?> matchType, Class<?> targetType) {
        return adaptTarget(ofType(matchType), targetType);
    }

    /**
     * Returns a {@linkplain PatternHandle} for a <em>nullable type
     * pattern</em>, which matches all instances of the match type, plus {@code
     * null}, with a single binding variable which is the target cast to the
     * match type.  The target type of the resulting pattern is the match type;
     * if a broader target type is desired, use {@link #ofType(Class, Class)} or
     * adapt the resulting pattern handle with {@link #adaptTarget(PatternHandle,
     * Class)}.
     *
     * @param matchType the type to match against
     * @return a pattern handle for a nullable type pattern
     */
    public static PatternHandle ofTypeNullable(Class<?> matchType) {
        requireNonNull(matchType);
        MethodType descriptor = MethodType.methodType(matchType, matchType);
        MethodHandle component = MH_OF_TYPE_NULLABLE_COMPONENT
                .asType(MethodType.methodType(matchType, Object.class));
        MethodHandle tryMatch
                = matchType.isPrimitive()
                  ? MethodHandles.identity(matchType)
                  : MH_OF_TYPE_NULLABLE_TRY_MATCH.bindTo(matchType)
                                                 .asType(MethodType.methodType(Object.class, matchType));

        return new PatternHandleImpl(descriptor, tryMatch, List.of(component));
    }

    /**
     * Returns a {@linkplain PatternHandle} for a <em>nullable type
     * pattern</em>, which matches all instances of the match type, plus {@code
     * null}, with a single binding variable which is the target cast to the
     * match type.  The target type of the resulting pattern is the {@code
     * targetType}.
     *
     * @param matchType  the type to match against
     * @param targetType the desired target type for the resulting pattern
     *                   handle
     * @return a pattern handle for a nullable type pattern
     * @throws IllegalArgumentException if the provided match type and target
     *                                  type are not compatible
     */
    public static PatternHandle ofTypeNullable(Class<?> matchType, Class<?> targetType) {
        return adaptTarget(ofTypeNullable(matchType), targetType);
    }

    /**
     * Returns a {@linkplain PatternHandle} for a <em>constant pattern</em>,
     * which matches all instances that are {@link Object#equals(Object)} to
     * the specified constant.  The resulting pattern has no binding variables.
     * If the constant is {@code null}, the target type of the pattern is
     * {@link Object}, otherwise it is the result of {@code Object::getClass}
     * on the constant.
     *
     * <p>TODO: restrict type of constant to String, boxes, and enums?
     *
     * @param o the constant
     * @return a pattern handle for a constant pattern
     */
    public static PatternHandle ofConstant(Object o) {
        Class<?> type = o == null ? Object.class : o.getClass();
        MethodHandle match = partialize(MethodHandles.dropArguments(MethodHandles.constant(Object.class, Boolean.TRUE), 0, type),
                                        MethodHandles.insertArguments(MH_OBJECTS_EQUAL, 0, o)
                                                     .asType(MethodType.methodType(boolean.class, type)));
        return new PatternHandleImpl(MethodType.methodType(type), match, List.of());
    }

    /**
     * Returns a {@linkplain PatternHandle} for a <em>constant pattern</em>,
     * which matches all instances that are {@link Object#equals(Object)} to
     * the specified constant.  The resulting pattern has no binding variables.
     * The target type of the pattern is {@code targetType}.
     *
     * @param o the constant
     * @param targetType the target type for the pattern
     * @return a pattern handle for a constant pattern
     * @throws IllegalArgumentException if the type of the constant and the
     * target type are not compatible
     */
    public static PatternHandle ofConstant(Object o, Class<?> targetType) {
        return adaptTarget(ofConstant(0), targetType);
    }

    // @@@ Primitive constant patterns

    /**
     * Returns a {@linkplain PatternHandle} for decomposing a target into its
     * components.  It matches all non-null instances of the specified target
     * type, and extracts one binding variable for each component specified in
     * the {@code components} argument.  The method handles in {@code components}
     * must be of type {@code (T)Bi} where T is the target type of the pattern
     * and Bi is the i'th binding variable.  The components are extracted
     * <em>lazily</em> -- when the component method handle is invoked by the
     * client -- rather than when the {@code tryMatch} method handle is invoked.
     *
     * @param targetType The type of the match target
     * @param components The component method handles
     * @return a pattern handle for a decomposition pattern
     */
    public static PatternHandle ofLazyProjection(Class<?> targetType,
                                                 MethodHandle... components) {
        requireNonNull(targetType);
        requireNonNull(components);
        return new PatternHandleImpl(descriptor(targetType, components),
                                     MethodHandles.identity(targetType),
                                     List.of(components));
    }

    /**
     * Returns a {@linkplain PatternHandle} for decomposing a target into its
     * components.  It matches all non-null instances of the specified target
     * type, and extracts one binding variable for each component specified in
     * the {@code components} argument.  The method handles in {@code components}
     * must be of type {@code (T)Bi} where T is the target type of the pattern
     * and Bi is the i'th binding variable.  The components are extracted
     * <em>eagerly</em> -- at the time the {@code tryMatch} method handle is
     * invoked.
     *
     * @param targetType The type of the match target
     * @param components The component method handles
     * @return a pattern handle for a decomposition pattern
     */
    public static PatternHandle ofEagerProjection(Class<?> targetType,
                                                  MethodHandle... components) {
        requireNonNull(targetType);
        requireNonNull(components);
        MethodType descriptor = descriptor(targetType, components);
        return new PatternHandleImpl(descriptor,
                                     carrierTryExtract(descriptor, components),
                                     PatternCarriers.carrierComponents(descriptor));
    }

    /**
     * Returns a {@linkplain PatternHandle} that delegates matching and
     * extraction to another method handle.  The target type of the pattern is
     * the return type of the {@code descriptor}, and the binding variable types
     * are the parameter types of the {@code descriptor}.  The {@code tryMatch}
     * method handle will invoke the specified {@code digester} method handle
     * with the target, as well as a method handle whose parameter types are
     * the binding variable types and whose return type is some type {@code C}.
     * For a successful match, the digester method should invoke this method
     * handle with the extracted bindings, and return the result; for an
     * unsuccessful match, it should return {@code null}.
     *
     * @param descriptor the type descriptor of the pattern
     * @param digester   the digester method handle
     * @return a pattern handle implementing the pattern
     */
    public static PatternHandle ofImperative(MethodType descriptor,
                                             MethodHandle digester) {
        Class<?> targetType = descriptor.returnType();
        return new PatternHandleImpl(descriptor,
                                     partialize(MethodHandles.insertArguments(digester,
                                                                              1, PatternCarriers.carrierFactory(descriptor)),
                                                MH_OBJECTS_NONNULL.asType(MH_OBJECTS_NONNULL.type().changeParameterType(0, targetType))),
                                     PatternCarriers.carrierComponents(descriptor));
    }

    /**
     * Compose a pattern handle with a method handle that receives the bindings. The
     * argument types of the target method must match those of the binding
     * types.  The resulting method handle accepts an argument which is the
     * target type of the pattern, and which returns either {@code null}
     * if the match fails or the result of the target method handle
     * if the match succeeds.
     *
     * @param patternHandle the pattern handle
     * @param target        a method handle that receives the bindings and
     *                      produces a result
     * @return the composed method handle
     */
    public static MethodHandle compose(PatternHandle patternHandle, MethodHandle target) {
        int count = patternHandle.descriptor().parameterCount();
        MethodHandle[] components = patternHandle.components().toArray(EMPTY_MH_ARRAY);
        Class<?> carrierType = patternHandle.tryMatch().type().returnType();
        Class<?> resultType = target.type().returnType();

        MethodHandle mh = MethodHandles.filterArguments(target, 0, components);
        mh = MethodHandles.permuteArguments(mh, MethodType.methodType(resultType, carrierType), new int[count]);
        mh = MethodHandles.guardWithTest(MH_OBJECTS_NONNULL.asType(MethodType.methodType(boolean.class, carrierType)),
                                         mh,
                                         MethodHandles.dropArguments(MethodHandles.constant(resultType, null), 0, carrierType));
        mh = MethodHandles.filterArguments(mh, 0, patternHandle.tryMatch());
        return mh;
    }

    // Combinators

    /**
     * Adapts a {@linkplain PatternHandle} to a new target type.  If the
     * pattern is of primitive type, it may be adapted to a supertype of its
     * corresponding box type; if it is of reference type, it may be widened
     * or narrowed to another reference type.
     *
     * @param pattern the pattern
     * @param newTarget the new target type
     * @return the adapted pattern
     * @throws IllegalArgumentException if the new target type is not compatible
     * with the target type of the pattern
     */
    public static PatternHandle adaptTarget(PatternHandle pattern, Class<?> newTarget) {
        Class<?> oldTarget = pattern.descriptor().returnType();
        if (oldTarget == newTarget)
            return pattern;

        Class<?> oldWrapperType = oldTarget.isPrimitive() ? Wrapper.forPrimitiveType(oldTarget).wrapperType() : null;
        MethodType guardType = MethodType.methodType(boolean.class, newTarget);
        MethodHandle guard;
        if (oldWrapperType != null && newTarget.isAssignableFrom(oldWrapperType)) {
            // Primitive boxing (with optional widening)
            guard = MH_PRIMITIVE_ADAPT_HELPER.bindTo(oldWrapperType).asType(guardType);
        }
        else if (newTarget.isAssignableFrom(oldTarget) || oldTarget.isAssignableFrom(newTarget)) {
            // reference narrowing or widening
            guard = MH_REFERENCE_ADAPT_HELPER.bindTo(oldTarget).asType(guardType);
        }
        else {
            throw new IllegalArgumentException(String.format("New target type %s not compatible with old target type %s",
                                                             newTarget, oldTarget));
        }

        MethodType tryMatchType = pattern.tryMatch().type().changeParameterType(0, newTarget);
        return new PatternHandleImpl(pattern.descriptor().changeReturnType(newTarget),
                                     partialize(pattern.tryMatch().asType(tryMatchType),
                                                guard),
                                     pattern.components());
    }

    /**
     * Returns a {@linkplain PatternHandle} that implements the same pattern
     * as another {@linkplain PatternHandle}, but potentially with fewer binding
     * variables.
     *
     * @param pattern the original pattern
     * @param positions the indexes of the binding variables to drop
     * @return the new pattern
     * @throws IndexOutOfBoundsException if any of the indexes are out of range
     * for the bindings of the original pattern
     */
    public static PatternHandle dropBindings(PatternHandle pattern, int... positions) {
        MethodHandle[] mhs = pattern.components().toArray(EMPTY_MH_ARRAY);
        for (int position : positions)
            mhs[position] = null;
        mhs = Stream.of(mhs).filter(Objects::nonNull).toArray(MethodHandle[]::new);
        return new PatternHandleImpl(descriptor(pattern.descriptor().returnType(), mhs), pattern.tryMatch(), List.of(mhs));
    }

    /**
     * Returns a {@linkplain PatternHandle} for a <em>nested</em> pattern.  A
     * nested pattern first matches the target to the outer pattern, and if
     * it matches successfully, then matches the resulting bindings to the inner
     * patterns.  The resulting pattern matches if the outer pattern matches
     * the target, and the bindings match the appropriate inner patterns.  The
     * target type of the nested pattern is the same as the target type of
     * the outer pattern.  The bindings are the bindings for the outer pattern,
     * followed by the concatenation of the bindings for the inner patterns.
     *
     * @param outer  The outer pattern
     * @param inners The inner patterns, which can be null if no nested pattern
     *               for the corresponding binding is desired
     * @return the nested pattern
     */
    public static PatternHandle nested(PatternHandle outer, PatternHandle... inners) {
        PatternHandle[] patternHandles = inners.clone();
        int outerCount = outer.descriptor().parameterCount();
        Class<?> outerCarrierType = outer.tryMatch().type().returnType();

        // Adapt inners to types of outer bindings
        for (int i = 0; i < patternHandles.length; i++) {
            PatternHandle patternHandle = patternHandles[i];
            if (patternHandle.descriptor().returnType() != outer.descriptor().parameterType(i))
                patternHandles[i] = adaptTarget(patternHandle, outer.descriptor().parameterType(i));
        }

        int[] innerPositions = IntStream.range(0, patternHandles.length)
                                        .filter(i -> patternHandles[i] != null)
                                        .toArray();
        MethodHandle[] innerComponents = Stream.of(patternHandles)
                                               .filter(Objects::nonNull)
                                               .map(PatternHandle::components)
                                               .flatMap(List::stream)
                                               .toArray(MethodHandle[]::new);
        MethodHandle[] innerTryMatches = Stream.of(patternHandles)
                                               .filter(Objects::nonNull)
                                               .map(PatternHandle::tryMatch)
                                               .toArray(MethodHandle[]::new);
        Class<?>[] innerCarriers = Stream.of(patternHandles)
                                         .filter(Objects::nonNull)
                                         .map(e -> e.tryMatch().type().returnType())
                                         .toArray(Class[]::new);
        Class<?>[] innerTypes = Stream.of(innerComponents)
                                      .map(mh -> mh.type().returnType())
                                      .toArray(Class[]::new);

        MethodType descriptor = outer.descriptor().appendParameterTypes(innerTypes);

        MethodHandle mh = PatternCarriers.carrierFactory(descriptor);
        mh = MethodHandles.filterArguments(mh, outerCount, innerComponents);
        int[] spreadInnerCarriers = new int[outerCount + innerComponents.length];
        for (int i = 0; i < outerCount; i++)
            spreadInnerCarriers[i] = i;
        int k = outerCount;
        int j = 0;
        for (PatternHandle e : patternHandles) {
            if (e == null)
                continue;
            for (int i = 0; i < e.descriptor().parameterCount(); i++)
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
        for (int i = 0; i < outerCount; i++)
            spreadNestedCarrier[i] = i;
        for (int i = 0; i < innerPositions.length; i++)
            spreadNestedCarrier[outerCount + i] = innerPositions[i];
        mh = MethodHandles.permuteArguments(mh, outer.descriptor().changeReturnType(mh.type().returnType()),
                                            spreadNestedCarrier);
        mh = MethodHandles.filterArguments(mh, 0, outer.components().toArray(EMPTY_MH_ARRAY));
        mh = MethodHandles.permuteArguments(mh, MethodType.methodType(mh.type().returnType(), outerCarrierType),
                                            new int[outerCount]);
        mh = bailIfNthNull(mh, 0);
        mh = MethodHandles.filterArguments(mh, 0, outer.tryMatch());

        MethodHandle tryExtract = mh;

        return new PatternHandleImpl(descriptor, tryExtract, PatternCarriers.carrierComponents(descriptor));
    }

    // @@@ AND combinator
    // @@@ GUARDED combinator

    // Bootstraps

    /**
     * Bootstrap method for creating a lazy projection pattern, as per
     * {@link #ofLazyProjection(Class, MethodHandle...)},
     * suitable for use as a {@code constantdynamic} bootstrap.  Suitable for use
     * by compilers which are generating implementations of patterns whose bindings
     * are independently derived from the target.
     *
     * @apiNote When the "bootstrap consolidation" project completes, this method
     * can go away and {@link #ofLazyProjection(Class, MethodHandle...)}
     * can be used directly as a condy bootstrap.
     *
     * @param lookup       ignored
     * @param constantName ignored
     * @param constantType Must be {@code PatternHandle.class}
     * @param targetType   the target type of the pattern
     * @param components   the pattern components
     * @return a pattern handle
     * @throws Throwable doc
     */
    public static PatternHandle ofLazyProjection(MethodHandles.Lookup lookup,
                                                 String constantName,
                                                 Class<?> constantType,
                                                 Class<?> targetType,
                                                 MethodHandle... components)
            throws Throwable {
        return ofLazyProjection(targetType, components);
    }

    /**
     * Bootstrap method for finding named {@link PatternHandle}s that have been
     * compiled according to the scheme outlined in JLS ?.?.
     *
     * @param lookup       the lookup context
     * @param constantName ignored
     * @param constantType must be {@code PatternHandle.class}
     * @param owner        the class containing the pattern
     * @param descriptor   the extractor descriptor
     * @param name         the extractor name
     * @param refKind      the kind of method
     * @return the extractor
     * @throws Throwable if something went wrong
     */
    public static PatternHandle ofNamed(MethodHandles.Lookup lookup,
                                        String constantName,
                                        Class<PatternHandle> constantType,
                                        Class<?> owner,
                                        MethodType descriptor,
                                        String name,
                                        int refKind) throws Throwable {
        String dd = descriptor.toMethodDescriptorString();
        String memberName = String.format("$pattern$%s$%s",
                                      (refKind == REF_newInvokeSpecial ? owner.getSimpleName() : name),
                                      dd.substring(0, dd.indexOf(')') + 1));
        String patternMethodName = BytecodeName.toBytecodeName(memberName);
        MethodType factoryDesc = MethodType.methodType(PatternHandle.class);
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

        return (PatternHandle) mh.invoke();
    }

    /**
     * Bootstrap method for extracting the {@code tryMatch} method handle from a
     * {@linkplain PatternHandle}.
     *
     * @apiNote When the "bootstrap consolidation" project completes, this method
     * can go away and {@link PatternHandle#tryMatch()} can be used directly as
     * a condy bootstrap.
     *
     * @param lookup        ignored
     * @param constantName  ignored
     * @param constantType  Must be {@code MethodHandle.class}
     * @param patternHandle the pattern handle
     * @return the {@code tryMatch} method handle
     */
    public static MethodHandle tryMatch(MethodHandles.Lookup lookup, String constantName, Class<MethodHandle> constantType,
                                        PatternHandle patternHandle) {
        return patternHandle.tryMatch();
    }

    /**
     * Bootstrap method for extracting a {@code component} method handle from a
     * {@linkplain PatternHandle}.
     *
     * @apiNote When the "bootstrap consolidation" project completes, this method
     * can go away and {@link PatternHandle#component(int)} ()} can be used directly as
     * a condy bootstrap.
     *
     * @param lookup        ignored
     * @param constantName  ignored
     * @param constantType  Must be {@code MethodHandle.class}
     * @param patternHandle the pattern
     * @param i the index of the desired component
     * @return the component method handle
     */
    public static MethodHandle component(MethodHandles.Lookup lookup,
                                         String constantName,
                                         Class<MethodHandle> constantType,
                                         PatternHandle patternHandle, int i) {
        return patternHandle.component(i);
    }

    // Helpers

    /**
     * Construct a partial method handle that uses the predicate as
     * guardWithTest, which applies the target if the test succeeds, and returns
     * null if the test fails.  The resulting method handle is of the same type
     * as the {@code target} method handle.
     *
     * @param target
     * @param predicate
     * @return
     */
    private static MethodHandle partialize(MethodHandle target,
                                           MethodHandle predicate) {
        Class<?> targetType = target.type().parameterType(0);
        Class<?> carrierType = target.type().returnType();
        return MethodHandles.guardWithTest(predicate,
                                           target,
                                           MethodHandles.dropArguments(MethodHandles.constant(carrierType, null),
                                                                       0, targetType));
    }

    /**
     * Construct a method handle that delegates to target, unless the nth
     * argument is null, in which case it returns null
     */
    private static MethodHandle bailIfNthNull(MethodHandle target, int n) {
        MethodHandle test = MH_OBJECTS_ISNULL
                .asType(MH_OBJECTS_ISNULL.type()
                                         .changeParameterType(0, target.type().parameterType(n)));
        test = MethodHandles.permuteArguments(test, target.type().changeReturnType(boolean.class), n);
        MethodHandle nullh = MethodHandles.dropArguments(MethodHandles.constant(target.type().returnType(), null),
                                                         0, target.type().parameterArray());
        return MethodHandles.guardWithTest(test, nullh, target);
    }

    private static MethodType descriptor(Class<?> targetType, MethodHandle[] components) {
        Class<?>[] paramTypes = Stream.of(components)
                                      .map(mh -> mh.type().returnType())
                                      .toArray(Class[]::new);
        return MethodType.methodType(targetType, paramTypes);
    }

    private static MethodHandle carrierTryExtract(MethodType descriptor, MethodHandle[] components) {
        MethodHandle carrierFactory = PatternCarriers.carrierFactory(descriptor);
        int[] reorder = new int[descriptor.parameterCount()]; // default value is what we want already

        Class<?> targetType = descriptor.returnType();
        return partialize(MethodHandles.permuteArguments(MethodHandles.filterArguments(carrierFactory, 0, components),
                                                         MethodType.methodType(carrierFactory.type().returnType(), targetType),
                                                         reorder),
                          MH_OBJECTS_NONNULL.asType(MH_OBJECTS_NONNULL.type().changeParameterType(0, targetType)));
    }

    private static MethodHandle lookupStatic(Class<?> clazz,
                                             String name,
                                             Class<?> returnType,
                                             Class<?>... paramTypes)
            throws ExceptionInInitializerError {
        try {
            return MethodHandles.lookup().findStatic(clazz, name, MethodType.methodType(returnType, paramTypes));
        }
        catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final MethodHandle MH_OF_TYPE_TRY_MATCH
            = lookupStatic(PatternHandles.class, "ofTypeTryMatch",
                           Object.class, Class.class, Object.class);
    private static final MethodHandle MH_OF_TYPE_NULLABLE_TRY_MATCH
            = lookupStatic(PatternHandles.class, "ofTypeNullableTryMatch",
                           Object.class, Class.class, Object.class);
    private static final MethodHandle MH_OF_TYPE_NULLABLE_COMPONENT
            = lookupStatic(PatternHandles.class, "ofTypeNullableComponent",
                           Object.class, Object.class);
    private static final MethodHandle MH_PRIMITIVE_ADAPT_HELPER
            = lookupStatic(PatternHandles.class, "primitiveAdaptHelper",
                           boolean.class, Class.class, Object.class);
    private static final MethodHandle MH_REFERENCE_ADAPT_HELPER
            = lookupStatic(PatternHandles.class, "referenceAdaptHelper",
                           boolean.class, Class.class, Object.class);
    private static final MethodHandle MH_OBJECTS_ISNULL
            = lookupStatic(Objects.class, "isNull",
                           boolean.class, Object.class);
    private static final MethodHandle MH_OBJECTS_NONNULL
            = lookupStatic(Objects.class, "nonNull",
                           boolean.class, Object.class);
    private static final MethodHandle MH_OBJECTS_EQUAL
            = lookupStatic(Objects.class, "equals",
                           boolean.class, Object.class, Object.class);

    private static Object ofTypeTryMatch(Class<?> type, Object o) {
        return o != null && type.isAssignableFrom(o.getClass())
               ? o
               : null;
    }

    private static Object ofTypeNullableTryMatch(Class<?> type, Object o) {
        if (o == null)
            return NULL_SENTINEL;
        else if (type.isAssignableFrom(o.getClass()))
            return o;
        else
            return null;
    }

    private static Object ofTypeNullableComponent(Object o) {
        return o == NULL_SENTINEL ? null : o;
    }

    private static boolean primitiveAdaptHelper(Class<?> type, Object o) {
        return o != null && type.isAssignableFrom(o.getClass());
    }

    private static boolean referenceAdaptHelper(Class<?> type, Object o) {
        return o == null || type.isAssignableFrom(o.getClass());
    }

    /**
     * Non-public implementation of {@link PatternHandle}
     */
    private static class PatternHandleImpl implements PatternHandle {

        private final MethodType descriptor;
        private final MethodHandle tryMatch;
        private final List<MethodHandle> components;


        /**
         * Construct an {@link PatternHandle} from components Constraints: -
         * output of tryMatch must match input of components - input of tryMatch
         * must match descriptor - output of components must match descriptor
         *
         * @param descriptor The {@code descriptor} method type
         * @param tryMatch   The {@code tryMatch} method handle
         * @param components The {@code component} method handles
         */
        PatternHandleImpl(MethodType descriptor, MethodHandle tryMatch,
                          List<MethodHandle> components) {
            MethodHandle[] componentsArray = components.toArray(new MethodHandle[0]);
            Class<?> carrierType = tryMatch.type().returnType();
            if (descriptor.parameterCount() != componentsArray.length)
                throw new IllegalArgumentException(String.format("MethodType %s arity should match component count %d",
                                                                 descriptor, componentsArray.length));
            if (!descriptor.returnType().equals(tryMatch.type().parameterType(0)))
                throw new IllegalArgumentException(String.format("Descriptor %s should match tryMatch input %s",
                                                                 descriptor, tryMatch.type()));
            for (int i = 0; i < componentsArray.length; i++) {
                MethodType componentType = componentsArray[i].type();
                if (componentType.parameterCount() != 1
                    || componentType.returnType().equals(void.class)
                    || !componentType.parameterType(0).equals(carrierType))
                    throw new IllegalArgumentException("Invalid component descriptor " + componentType);
                if (!componentType.returnType().equals(descriptor.parameterType(i)))
                    throw new IllegalArgumentException(String.format("Descriptor %s should match %d'th component %s",
                                                                     descriptor, i, componentsArray[i]));
            }

            if (!carrierType.equals(Object.class)) {
                tryMatch = tryMatch.asType(tryMatch.type().changeReturnType(Object.class));
                for (int i = 0; i < componentsArray.length; i++) {
                    MethodHandle component = componentsArray[i];
                    componentsArray[i] = component.asType(component.type().changeParameterType(0, Object.class));
                }
            }

            this.descriptor = descriptor;
            this.tryMatch = tryMatch;
            this.components = List.of(componentsArray);
        }

        @Override
        public MethodHandle tryMatch() {
            return tryMatch;
        }

        @Override
        public MethodHandle component(int i) {
            return components.get(i);
        }

        @Override
        public List<MethodHandle> components() {
            return components;
        }

        @Override
        public MethodType descriptor() {
            return descriptor;
        }

    }
}
