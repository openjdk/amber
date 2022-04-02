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

package java.lang;

import java.lang.invoke.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import jdk.internal.javac.PreviewFeature;

/**
 * This interface describes the methods provided by a generalized string template policy. The
 * primary method {@link TemplatePolicy#apply} is used to validate and compose a result using
 * the template string and list of values, from a {@link TemplatedString}. For example:
 *
 * {@snippet :
 * class MyPolicy implements TemplatePolicy<String, IllegalArgumentException> {
 *       @Override
 *       public String apply(TemplatedString templatedString) throws IllegalArgumentException {
 *            StringBuilder sb = new StringBuilder();
 *            Iterator<String> segmentsIter = templatedString.segments().iterator();
 *
 *            for (Object value : templatedString.values()) {
 *                sb.append(segmentsIter.next());
 *
 *                if (value instanceof Boolean) {
 *                    throw new IllegalArgumentException("I don't like Booleans");
 *                }
 *
 *                sb.append(value);
 *            }
 *
 *            sb.append(segmentsIter.next());
 *
 *            return sb.toString();
 *       }
 *    }
 * }
 * Usage:
 * {@snippet :
 * MyPolicy policy = new MyPolicy();
 * try {
 *     int x = 10;
 *     int y = 20;
 *     String result = policy."\{x} + \{y} = \{x + y}";
 *     ...
 * } catch (IllegalArgumentException ex) {
 *     ...
 * }
 * }
 * Implementations of this interface may provide, but are not limited to,
 * validating inputs, composing inputs into a result, and transforming a
 * result to a non-string type before delivering the final result.
 * <p>
 * The user has the option of validating inputs used in composition. For
 * example an SQL policy could prevent injection vulnerabilities by
 * sanitizing inputs or throwing an exception of type {@code E} if an SQL
 * statement is a potential vulnerability.
 * <p>
 * Composing allows the users to control how the result is assembled. Most
 * often, a user will construct a new string from the template string, with
 * placeholders replaced by stringified objects from the values list.
 * <p>
 * A static factory method is provided to simplify constructing {@link
 * TemplatePolicy template policies} using lambdas. A {@link BiFunction
 * BiFunction} lambda passed to {@link TemplatePolicy#ofComposed
 * ofComposed} is supplied a list of string segments and a list of
 * expression values. The result type of the lambda determines the result
 * type {@code R} of the generated {@link TemplatePolicy}.
 * {@snippet :
 * TemplatePolicy<String, RuntimeException> policy =
 *         TemplatePolicy.ofComposed((segments, values) -> {
 *         StringBuilder sb = new StringBuilder();
 *         Iterator<String> segmentsIter = templatedString.segments().iterator();
 *
 *         for (Object value : templatedString.values()) {
 *             sb.append(segmentsIter.next());
 *             sb.append(value);
 *         }
 *
 *         sb.append(segmentsIter.next());
 *
 *         return sb.toString();
 *
 *     });
 * }
 * Transformation allows the user to the construct a result into something
 * other than a string. For example, a JSON policy may transform a {@link
 * TemplatedString} into a JSON object. The {@code R} parameter type allows
 * the user to specify the final result type of the policy's {@link
 * TemplatePolicy#apply apply} method.
 * <p>
 * A static factory method is provided to simplify constructing simple
 * transforming policies using lambdas. The {@link Function Function}
 * lambda passed to {@link  TemplatePolicy#ofTransformed ofTransformed} is
 * supplied the simple concatenation from the {@link TemplatedString} using
 * {@link TemplatedString#concat}. This allows the user to transform that
 * concatenation into some other form. The result type of the lambda
 * determines the result type {@code R} of the generated {@link TemplatePolicy}.
 * {@snippet :
 * TemplatePolicy<JSONObject, RuntimeException> JSON = TemplatePolicy.ofTransformed(JSONObject::new);
 *
 * JSONObject data = JSON."""
 *    {
 *        "name": "\{name}",
 *        "phone": "\{phone}",
 *        "address": "\{address}"
 *    };
 *    """;
 * }
 *
 * @param <R>  Policy's apply result type.
 * @param <E>  Exception thrown type.
 *
 * @see java.util.FormatterPolicy
 */
@PreviewFeature(feature=PreviewFeature.Feature.TEMPLATED_STRINGS)
@FunctionalInterface
public interface TemplatePolicy<R, E extends Throwable> {

    /**
     * Constructs a result based on the template string and values in the
     * supplied {@link TemplatedString templatedString} object.
     *
     * @param templatedString  a {@link TemplatedString} instance
     *
     * @return constructed object of type R
     *
     * @throws E exception thrown by the template policy when validation fails
     */
    R apply(TemplatedString templatedString) throws E;

    /**
     * This interface describes the methods provided by string template policy specialized
     * to not throw exceptions. The primary method {@link SimplePolicy#apply} is used
     * to compose a result using the template string and list of values, from a
     * {@link TemplatedString}. For example:
     * {@snippet :
     * class MyPolicy implements SimplePolicy<String> {
     *       @Override
     *       public String apply(TemplatedString templatedString) {
     *            StringBuilder sb = new StringBuilder();
     *            Iterator<String> segmentsIter = templatedString.segments().iterator();
     *
     *            for (Object value : templatedString.values()) {
     *                sb.append(segmentsIter.next());
     *                sb.append(value);
     *            }
     *
     *            sb.append(segmentsIter.next());
     *
     *            return sb.toString();
     *       }
     *    }
     * }
     * Usage:
     * {@snippet :
     * MyPolicy policy = new MyPolicy();
     * int x = 10;
     * int y = 20;
     * String result = policy."\{x} + \{y} = \{x + y}";
     * }
     */
    @PreviewFeature(feature=PreviewFeature.Feature.TEMPLATED_STRINGS)
    @FunctionalInterface
    interface SimplePolicy<R> extends TemplatePolicy<R, RuntimeException> {
        /**
         * Constructs a  result based on the template string and values in the
         * supplied {@link TemplatedString templatedString} object.
         *
         * @param templatedString  a {@link TemplatedString} instance
         *
         * @return constructed object of type R
         */
        R apply(TemplatedString templatedString);
    }

    /**
     * This interface describes the methods provided by string template policy specialized for
     * strings. The primary method {@link StringPolicy#apply} is used to validate and
     * compose a result using the template string and list of values, from a
     * {@link TemplatedString}. For example:
     * {@snippet :
     * class MyPolicy implements StringPolicy {
     *       @Override
     *       public String apply(TemplatedString templatedString) {
     *            StringBuilder sb = new StringBuilder();
     *            Iterator<String> segmentsIter = templatedString.segments().iterator();
     *
     *            for (Object value : templatedString.values()) {
     *                sb.append(segmentsIter.next());
     *                sb.append(value);
     *            }
     *
     *            sb.append(segmentsIter.next());
     *
     *            return sb.toString();
     *       }
     *    }
     * }
     * Usage:
     * {@snippet :
     * MyPolicy policy = new MyPolicy();
     * int x = 10;
     * int y = 20;
     * String result = policy."\{x} + \{y} = \{x + y}";
     * }
     */
    @PreviewFeature(feature=PreviewFeature.Feature.TEMPLATED_STRINGS)
    @FunctionalInterface
    interface StringPolicy extends SimplePolicy<String> {
        /**
         * Constructs a String result based on the template string and values in the
         * supplied {@link TemplatedString templatedString} object.
         *
         * @param templatedString  a {@link TemplatedString} instance
         *
         * @return constructed String
         */
        String apply(TemplatedString templatedString);
    }

    /**
     * Factory method that produces a template policy based on a supplied lambda
     * function. The function's input will be a {@link TemplatedString} object.
     * The result type of the function will be the result type of the generated
     * policy.
     *
     * @param policy  function for applying template policy
     *
     * @param <R>  Type of the function's result.
     *
     * @return a {@link TemplatePolicy} that uses the lambda as the policy's
     *         apply method.
     */
    public static <R> SimplePolicy<R>
            of(Function<TemplatedString, R> policy) {
        return new SimplePolicy<>() {
            @Override
            public final R apply(TemplatedString templatedString) {
                Objects.requireNonNull(templatedString);

                return policy.apply(templatedString);
            }
        };
    }

    /**
     * Factory method that produces a template policy based on a supplied lambda
     * function. The function's inputs will be a the list of segments and a list
     * of values from the {@link TemplatedString} object. The result type from the
     * function will be the result type of the generated policy.
     *
     * @param policy  function for applying template policy
     *
     * @param <R>  Type of the function's result.
     *
     * @return a {@link TemplatePolicy} that uses the lambda as the policy's
     *         apply method.
     */
    public static <R> SimplePolicy<R>
            ofComposed(BiFunction<List<String>, List<Object>, R> policy) {
        return new SimplePolicy<>() {
            @Override
            public final R apply(TemplatedString templatedString) {
                Objects.requireNonNull(templatedString);

                return policy.apply(templatedString.segments(),
                                    templatedString.values());
            }
        };
    }

    /**
     * Factory method that produces a template policy based on a supplied lambda
     * function. The function's input will be the basic concatenation,
     * {TemplatedString#concat}, from the {@link TemplatedString} object. The
     * result type from the function will be the result type of the generated
     * policy.
     *
     * @param policy  function for applying template policy
     *
     * @param <R>  Type of the function's result.
     *
     * @return a {@link TemplatePolicy} that uses the lambda as the policy's
     *         apply method.
     */
    public static <R> SimplePolicy<R>
            ofTransformed(Function<String, R> policy) {
        return new SimplePolicy<>() {
            @Override
            public final R apply(TemplatedString templatedString) {
                Objects.requireNonNull(templatedString);

                return policy.apply(templatedString.concat());
            }
       };
    }

    /**
     * Simple concatenation policy instance.
     */
    public static final TemplatePolicy<String, RuntimeException>
            STR = new ConcatinationPolicy();

    /**
     * Policies using this interface have the flexibility to specialize the
     * composition of the templated string by returning a customized from
     * {@link CallSite CallSites} from {@link TemplatePolicy.Linkage#applier
     * applier}. These specializations are typically implemented to improve
     * performance; specializing value types or avoiding boxing and vararg
     * arrays.
     *
     * @implNote {@link TemplatePolicy} implemented using this interface outside
     * java.base will default to using the {@link TemplatePolicy#apply} method.
     *
     * @param <R>  Policy's apply result type.
     * @param <E>  Exception thrown type.
     */
    sealed interface Linkage<R, E extends Throwable> extends TemplatePolicy<R, E>
        permits ConcatinationPolicy, FormatterPolicy
    {
        /**
         * Return a boolean guard to assure that only specific policies should
         * use the applier.
         *
         * @param lookup      method lookup
         * @param type        methiod type
         * @param template    template string with placeholders
         *
         * @return guarding {@link MethodHandle}
         *
         * @throws NullPointerException if any of the arguments are null
         */
        default MethodHandle guard(MethodHandles.Lookup lookup,
                                   MethodType type, String template) {
            Objects.requireNonNull(lookup);
            Objects.requireNonNull(type);
            Objects.requireNonNull(template);

            return null;
        }

        /**
         * Construct a {@link CallSite} that constructs a result based on the
         * bootstrap method information.
         *
         * @param lookup      method lookup
         * @param type        methiod type
         * @param template    template string with placeholders
         *
         * @return {@link MethodHandle} for the policy applied to template
         *
         * @throws NullPointerException if any of the arguments are null
         */
        default MethodHandle applier(MethodHandles.Lookup lookup,
                                     MethodType type, String template) {
            Objects.requireNonNull(lookup);
            Objects.requireNonNull(type);
            Objects.requireNonNull(template);

            return null;
        }
    }

}
