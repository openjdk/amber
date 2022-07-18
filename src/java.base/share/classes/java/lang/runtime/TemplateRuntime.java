/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.*;
import java.lang.reflect.Modifier;
import java.lang.template.PolicyLinkage;
import java.lang.template.TemplatePolicy;
import java.util.*;

import jdk.internal.access.JavaLangAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.javac.PreviewFeature;

/**
 * This class provides runtime support for string templates. The methods within
 * are intended for internal use.
 *
 * @since 20
 */
@PreviewFeature(feature=PreviewFeature.Feature.STRING_TEMPLATES)
public final class TemplateRuntime {

    private static final JavaLangAccess JLA = SharedSecrets.getJavaLangAccess();

    /**
     * Private constructor.
     */
    private TemplateRuntime() {
        throw new AssertionError("private constructor");
    }

    /**
     * Templated string bootstrap method.
     *
     * @param lookup       method lookup
     * @param name         method name
     * @param type         method type
     * @param policyGetter {@link MethodHandle} to get static final policy
     * @param fragments    fragments from string template
     * @return {@link CallSite} to handle templated string processing
     * @throws NullPointerException if any of the arguments is null
     * @throws Throwable            if applier fails
     */
    public static CallSite templatedStringBSM(
            MethodHandles.Lookup lookup,
            String name,
            MethodType type,
            MethodHandle policyGetter,
            String... fragments) throws Throwable {
        Objects.requireNonNull(lookup, "lookup is null");
        Objects.requireNonNull(name, "name is null");
        Objects.requireNonNull(type, "type is null");
        Objects.requireNonNull(policyGetter, "policyGetter is null");
        Objects.requireNonNull(fragments, "fragments is null");

        MethodType policyGetterType = MethodType.methodType(TemplatePolicy.class);
        TemplatePolicy<?, ? extends Throwable> policy =
                (TemplatePolicy<?, ? extends Throwable>)policyGetter.asType(policyGetterType).invokeExact();
        TemplateBootstrap bootstrap = new TemplateBootstrap(lookup, name, type, List.of(fragments), policy);

        return bootstrap.applyWithPolicy();
    }

    /**
     * Manages the boostrapping of {@link PolicyLinkage} callsites.
     */
    private static class TemplateBootstrap {
        /**
         * {@link MethodHandle} to {@link TemplateBootstrap#defaultApply}.
         */
        private static final MethodHandle DEFAULT_APPLY_MH;

        /**
         * {@link MethodHandles.Lookup} passed to the bootstrap method.
         */
        private final MethodHandles.Lookup lookup;

        /**
         * Name passed to the bootstrap method ("apply").
         */
        private final String name;

        /**
         * {@link MethodType} passed to the bootstrap method.
         */
        private final MethodType type;

        /**
         * Fragments from string template.
         */
        private final List<String> fragments;

        /**
         * Static final policy.
         */
        private final TemplatePolicy<?, ? extends Throwable> policy;

        /**
         * Initialize {@link MethodHandle MethodHandles}.
         */
        static {
            try {
                MethodHandles.Lookup lookup = MethodHandles.lookup();

                MethodType mt = MethodType.methodType(Object.class,
                        List.class, TemplatePolicy.class, Object[].class);
                DEFAULT_APPLY_MH = lookup.findStatic(TemplateBootstrap.class, "defaultApply", mt);
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError("templated string bootstrap fail", ex);
            }
        }

        /**
         * Constructor.
         *
         * @param lookup    method lookup
         * @param name      method name
         * @param type      method type
         * @param fragments fragments from string template
         * @param policy    static final policy
         */
        private TemplateBootstrap(MethodHandles.Lookup lookup, String name, MethodType type,
                                  List<String> fragments,
                                  TemplatePolicy<?, ? extends Throwable> policy) {
            this.lookup = lookup;
            this.name = name;
            this.type = type;
            this.fragments = fragments;
            this.policy = policy;

        }

        /**
         * Create callsite to invoke specialized policy apply method.
         *
         * @return {@link CallSite} for handling apply policy templated strings.
         * @throws Throwable if applier fails
         */
        private CallSite applyWithPolicy() throws Throwable {
            MethodHandle mh = policy instanceof PolicyLinkage policyLinkage ?
                    policyLinkage.applier(fragments, type) : defaultApplyMethodHandle();

            return new ConstantCallSite(mh);
        }

        /**
         * Creates a simple {@link TemplatedString} and then invokes the policy's apply method.
         *
         * @param fragments fragments from string template
         * @param policy    {@link TemplatePolicy} to apply
         * @param values    array of expression values
         * @return
         */
        private static Object defaultApply(List<String> fragments,
                                           TemplatePolicy<Object, Throwable> policy,
                                           Object[] values) throws Throwable {
            return policy.apply(JLA.newTemplatedString(fragments, List.of(values)));
        }

        /**
         * Generate a {@link MethodHandle} which is effectively invokes
         * {@code policy.apply(new TemplatedString(fragments, values...)}.
         *
         * @return default apply {@link MethodHandle}
         */
        private MethodHandle defaultApplyMethodHandle() {
            MethodHandle mh = MethodHandles.insertArguments(DEFAULT_APPLY_MH, 0, fragments, policy);
            mh = mh.withVarargs(true);
            mh = mh.asType(type);

            return mh;
        }

    }

    /**
     * Collect nullable elements into a unmodifiable list.
     *
     * @param elements  elements to place in list
     *
     * @return unmodifiable list.
     *
     * @param <E>  type of elements
     *
     * @implNote Intended for use by {@link TemplatedString} implementations.
     * Other usage may
     */
    @SuppressWarnings("unchecked")
    public static <E> List<E> toList(E... elements) {
        return Collections.unmodifiableList(Arrays.asList(elements));
    }

}
