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
import java.lang.TemplatePolicy.PolicyLinkage;
import java.util.*;

import jdk.internal.access.JavaLangAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.javac.PreviewFeature;

/**
 * This class constructs a {@link CallSite} to handle templated
 * string processing.
 *
 * @since 19
 */
@PreviewFeature(feature=PreviewFeature.Feature.TEMPLATED_STRINGS)
public final class TemplateBootstrap {

    private static final JavaLangAccess JLA = SharedSecrets.getJavaLangAccess();

    /**
     * {@link MethodHandle} to {@link TemplateBootstrap#defaultApply}
     */
    private static final MethodHandle DEFAULT_APPLY_MH;

    /**
     * {@link MethodHandles.Lookup} passed to the bootstrap method
     */
    private final MethodHandles.Lookup lookup;

    /**
     * Name passed to the bootstrap method ("apply")
     */
    private final String name;

    /**
     * {@link MethodType} passed to the bootstrap method
     */
    private final MethodType type;

    /**
     * Template stencil passed to the bootstrap method
     */
    private final String stencil;

    /**
     * {@link MethodHandle} to get static final policy
     */
    private final MethodHandle policyGetter;

    /**
     * Initialize {@link MethodHandle MethodHandles}
     */
    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            MethodType mt = MethodType.methodType(Object.class,
                    String.class, List.class, TemplatePolicy.class, Object[].class);
            DEFAULT_APPLY_MH = lookup.findStatic(TemplateBootstrap.class, "defaultApply", mt);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("templated string bootstrap fail", ex);
        }
    }

    /**
     * Constructor.
     *
     * @param lookup       method lookup
     * @param name         method name
     * @param type         method type
     * @param stencil      stencil string with placeholders
     * @param policyGetter {@link MethodHandle} to get static final policy
     */
    private TemplateBootstrap(MethodHandles.Lookup lookup, String name,
                              MethodType type, String stencil, MethodHandle policyGetter) {
        this.lookup = lookup;
        this.name = name;
        this.type = type;
        this.stencil = stencil;
        this.policyGetter = policyGetter;
    }

     /**
     * Templated string bootstrap method.
     *
     * @param lookup       method lookup
     * @param name         method name
     * @param type         method type
     * @param stencil      stencil string with placeholders
     * @param policyGetter {@link MethodHandle} to get static final policy
     *
     * @return {@link CallSite} to handle templated string processing
     *
     * @throws NullPointerException if any of the arguments is null
     */
    public static CallSite templatedStringBSM(
            MethodHandles.Lookup lookup,
            String name,
            MethodType type,
            String stencil,
            MethodHandle policyGetter) {
        Objects.requireNonNull(lookup, "lookup is null");
        Objects.requireNonNull(name, "name is null");
        Objects.requireNonNull(type, "type is null");
        Objects.requireNonNull(stencil, "stencil is null");
        Objects.requireNonNull(stencil, "policyGetter is null");

        TemplateBootstrap bootstrap = new TemplateBootstrap(lookup, name, type, stencil, policyGetter);

        return bootstrap.applyWithPolicy();
    }

    /**
     * Create callsite to invoke specialized policy apply method.
     *
     * @return {@link CallSite} for handling apply policy templated strings.
     */
    private CallSite applyWithPolicy() {
        try {
            MethodType getterType = MethodType.methodType(TemplatePolicy.class);
            TemplatePolicy<?, ? extends Throwable> policy =
                    (TemplatePolicy<?, ? extends Throwable>)policyGetter.asType(getterType).invokeExact();
            MethodHandle mh = null;

            if (policy instanceof PolicyLinkage policyLinkage) {
                mh = ((PolicyLinkage)policy).applier(stencil, type);
            }

            if (mh == null) {
                mh = defaultApplyMethodHandle();
            }

            return new ConstantCallSite(mh);
        } catch (Throwable ex) {
            throw new RuntimeException("Can not bootstrap policy");
        }
    }

    /**
     * Creates a simple {@link TemplatedString} and then invokes the policy's apply method.
     *
     * @param stencil    stencil string with placeholders
     * @param fragments  immutable list of string fragments created by splitting
     *                   the stencil at placeholders
     * @param policy     {@link TemplatePolicy} to apply
     * @param values     array of expression values
     *
     * @return
     */
    private static Object defaultApply(String stencil,
                                       List<String> fragments,
                                       TemplatePolicy<Object, Throwable> policy,
                                       Object[] values) throws Throwable {
        return policy.apply(JLA.newTemplatedString(stencil, List.of(values), fragments));
    }

    /**
     * Generate a {@link MethodHandle} which is effectively invokes
     * {@code policy.apply(new TemplatedString(stencil, values...)}.
     *
     * @return default apply {@link MethodHandle}
     */
    private MethodHandle defaultApplyMethodHandle() {
        MethodHandle mh = MethodHandles.insertArguments(DEFAULT_APPLY_MH, 0, stencil,
                                                        TemplatedString.split(stencil));
        mh = mh.withVarargs(true);
        mh = mh.asType(type);

        return mh;
    }

}
