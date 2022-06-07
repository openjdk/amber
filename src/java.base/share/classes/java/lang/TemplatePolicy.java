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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import jdk.internal.javac.PreviewFeature;

/**
 * This interface describes the methods provided by a generalized string template policy. The
 * primary method {@link TemplatePolicy#apply} is used to validate and compose a result using
 * a {@link TemplatedString TemplatedString's} stencil (or fragments) and values list. For example:
 *
 * {@snippet :
 * class MyPolicy implements TemplatePolicy<String, IllegalArgumentException> {
 *     @Override
 *     public String apply(TemplatedString templatedString) throws IllegalArgumentException {
 *          StringBuilder sb = new StringBuilder();
 *          Iterator<String> fragmentsIter = templatedString.fragments().iterator();
 *
 *          for (Object value : templatedString.values()) {
 *              sb.append(fragmentsIter.next());
 *
 *              if (value instanceof Boolean) {
 *                  throw new IllegalArgumentException("I don't like Booleans");
 *              }
 *
 *              sb.append(value);
 *          }
 *
 *          sb.append(fragmentsIter.next());
 *
 *          return sb.toString();
 *     }
 * }
 *
 * MyPolicy myPolicy = new MyPolicy();
 * try {
 *     int x = 10;
 *     int y = 20;
 *     String result = myPolicy."\{x} + \{y} = \{x + y}";
 *     ...
 * } catch (IllegalArgumentException ex) {
 *     ...
 * }
 * }
 * Implementations of this interface may provide, but are not limited to, validating
 * inputs, composing inputs into a result, and transforming an intermediate string
 * result to a non-string value before delivering the final result.
 * <p>
 * The user has the option of validating inputs used in composition. For example an SQL
 * policy could prevent injection vulnerabilities by sanitizing inputs or throwing an
 * exception of type {@code E} if an SQL statement is a potential vulnerability.
 * <p>
 * Composing allows user control over how the result is assembled. Most often, a
 * user will construct a new string from the template string, with placeholders
 * replaced by stringified objects from the values list.
 * <p>
 * Transforming allows the policy to return something other than a string. For
 * instance, a JSON policy could return a JSON object, by parsing the string created
 * by composition, instead of the composed string.
 * <p>
 * {@link TemplatePolicy} is a {@link FunctionalInterface}. This permits declaration of a
 * policy using lambda expressions;
 * {@snippet :
 * TemplatePolicy<String, RuntimeException> concatPolicy = ts -> {
 *             StringBuilder sb = new StringBuilder();
 *             Iterator<String> fragmentsIter = ts.fragments().iterator();
 *             for (Object value : ts.values()) {
 *                 sb.append(fragmentsIter.next());
 *                 sb.append(value);
 *             }
 *             sb.append(fragmentsIter.next());
 *            return sb.toString();
 *         };
 * }
 * The {@link FunctionalInterface} {@link TemplatePolicy.SimplePolicy} is supplied to avoid
 * declaring checked exceptions;
 * {@snippet :
 * SimplePolicy<String> concatPolicy = ts -> {
 *             StringBuilder sb = new StringBuilder();
 *             Iterator<String> fragmentsIter = ts.fragments().iterator();
 *             for (Object value : ts.values()) {
 *                 sb.append(fragmentsIter.next());
 *                 sb.append(value);
 *             }
 *             sb.append(fragmentsIter.next());
 *            return sb.toString();
 *         };
 * }
 * The {@link FunctionalInterface} {@link StringPolicy} is supplied if
 * the policy returns {@link String};
 * {@snippet :
 * StringPolicy concatPolicy = ts -> {
 *             StringBuilder sb = new StringBuilder();
 *             Iterator<String> fragmentsIter = ts.fragments().iterator();
 *             for (Object value : ts.values()) {
 *                 sb.append(fragmentsIter.next());
 *                 sb.append(value);
 *             }
 *             sb.append(fragmentsIter.next());
 *            return sb.toString();
 *         };
 * }
 * The {@link TemplatedString#concat()} method is available for those policies that just need
 * to work with the concatenation;
 * {@snippet :
 * StringPolicy concatPolicy = TemplateString::concat;
 * }
 * or simply transform the string concatenation into something other than
 * {@link String};
 * {@snippet :
 * SimplePolicy<JSONObject> jsonPolicy = ts -> new JSONObject(ts.concat());
 * }
 * @implNote The Java compiler automatically imports {@link STR},
 * {@link java.util.FormatterPolicy#FMTR}, {@link SimplePolicy} and
 * {@link StringPolicy}.
 *
 * @param <R>  Policy's apply result type.
 * @param <E>  Exception thrown type.
 *
 * @see java.util.FormatterPolicy
 */
@PreviewFeature(feature=PreviewFeature.Feature.STRING_TEMPLATES)
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
     * This interface simplifies declaration of {@link TemplatePolicy TemplatePolicys}
     * that do not throw checked exceptions. For example:
     * {@snippet :
     * SimplePolicy<String> concatPolicy = ts -> {
     *             StringBuilder sb = new StringBuilder();
     *             Iterator<String> fragmentsIter = ts.fragments().iterator();
     *             for (Object value : ts.values()) {
     *                 sb.append(fragmentsIter.next());
     *                 sb.append(value);
     *             }
     *             sb.append(fragmentsIter.next());
     *            return sb.toString();
     *         });
     * }
     *
     * @param <R>  Policy's apply result type.
     */
    @PreviewFeature(feature=PreviewFeature.Feature.STRING_TEMPLATES)
    @FunctionalInterface
    interface SimplePolicy<R> extends TemplatePolicy<R, RuntimeException> {
        /**
         * Chain template policies to produce a new policy that applies the supplied
         * policies from right to left. The {@code head} policy is a {@link SimplePolicy}
         * The {@code tail} policies must return type {@link TemplatedString}.
         *
         * @param head  last {@link SimplePolicy} to be applied, return type {@code R}
         * @param tail  first policies to apply, return type {@code TemplatedString}
         *
         * @return a new {@link SimplePolicy} that applies the supplied policies
         *         from right to left
         *
         * @param <R> return type of the head policy and resulting policy
         *
         * @throws NullPointerException if any of the arguments is null.
         */
        @SuppressWarnings("varargs")
        @SafeVarargs
        public static <R> SimplePolicy<R>
            chain(SimplePolicy<R> head,
                  TemplatePolicy<TemplatedString, RuntimeException>... tail) {
            Objects.requireNonNull(head, "head must not be null");
            Objects.requireNonNull(tail, "tail must not be null");

            if (tail.length == 0) {
                return head;
            }

            TemplatePolicy<TemplatedString, RuntimeException> last =
                    TemplatePolicy.chain(tail[0], Arrays.copyOfRange(tail, 1, tail.length));

            return ts -> head.apply(last.apply(ts));
        }
    }

    /**
     * This interface simplifies declaration of {@link java.lang.TemplatePolicy TemplatePolicys}
     * that do not throw checked exceptions and have a result type of {@link String}. For example:
     * {@snippet :
     * StringPolicy policy = ts -> ts.concat();
     * }
     */
    @PreviewFeature(feature=PreviewFeature.Feature.STRING_TEMPLATES)
    @FunctionalInterface
    interface StringPolicy extends SimplePolicy<String> {
        /**
         * Chain template policies to produce a new policy that applies the supplied
         * policies from right to left. The {@code head} policy is a {@link StringPolicy}
         * The {@code tail} policies must return type {@link TemplatedString}.
         *
         * @param head  last {@link StringPolicy} to be applied, return type {@link String}
         * @param tail  first policies to apply, return type {@code TemplatedString}
         *
         * @return a new {@link StringPolicy} that applies the supplied policies
         *         from right to left
         *
         * @throws NullPointerException if any of the arguments is null.
         */
        @SuppressWarnings("varargs")
        @SafeVarargs
        public static StringPolicy
            chain(StringPolicy head,
                 TemplatePolicy<TemplatedString, RuntimeException>... tail) {
            Objects.requireNonNull(head, "head must not be null");
            Objects.requireNonNull(tail, "tail must not be null");

            if (tail.length == 0) {
                return head;
            }

            TemplatePolicy<TemplatedString, RuntimeException> last =
                    TemplatePolicy.chain(tail[0], Arrays.copyOfRange(tail, 1, tail.length));

            return ts -> head.apply(last.apply(ts));
        }
    }

    /**
     * Chain template policies to produce a new policy that applies the supplied
     * policies from right to left. The {@code tail} policies must return type
     * {@link TemplatedString}.
     *
     * @param head  last {@link TemplatePolicy} to be applied, return type {@code R}
     * @param tail  first policies to apply, return type {@code TemplatedString}
     *
     * @return a new {@link TemplatePolicy} that applies the supplied
     *         policies from right to left
     *
     * @param <R> return type of the head policy and resulting policy
     * @param <E> exception thrown type by head policy and resulting policy
     *
     * @throws NullPointerException if any of the arguments is null.
     */
    @SafeVarargs
    public static <R, E extends Throwable> TemplatePolicy<R, E>
            chain(TemplatePolicy<R, E> head,
              TemplatePolicy<TemplatedString, RuntimeException>... tail) {
        Objects.requireNonNull(head, "head must not be null");
        Objects.requireNonNull(tail, "tail must not be null");

        if (tail.length == 0) {
            return head;
        }

        int index = tail.length;
        TemplatePolicy<TemplatedString, RuntimeException> current = tail[--index];

        while (index != 0) {
            TemplatePolicy<TemplatedString, RuntimeException> second = tail[--index];
            TemplatePolicy<TemplatedString, RuntimeException> first = current;
            current = ts -> second.apply(first.apply(ts));
        }

        TemplatePolicy<TemplatedString, RuntimeException> last = current;

        return ts -> head.apply(last.apply(ts));
    }

    /**
     * Simple concatenation policy instance.
     *
     * @implNote The result of concatenation is not interned.
     */
    public static final StringPolicy STR = new StringPolicy() {
        @Override
        public String apply(TemplatedString templatedString) {
            Objects.requireNonNull(templatedString);

            return templatedString.concat();
        }
    };

    /**
     * Policies using this additional interface have the flexibility to specialize
     * the composition of the templated string by returning a customized
     * {@link CallSite CallSites} from {@link TemplatePolicy.PolicyLinkage#applier applier}.
     * These specializations are typically implemented to improve performance;
     * specializing value types or avoiding boxing and vararg arrays.
     *
     * @implNote This interface is sealed to only allow standard policies.
     */
    @PreviewFeature(feature=PreviewFeature.Feature.STRING_TEMPLATES)
    sealed interface PolicyLinkage permits FormatterPolicy {
        /**
         * Construct a {@link MethodHandle} that constructs a result based on the
         * bootstrap method information.
         *
         * @param stencil     stencil string with placeholders
         * @param type        methiod type
         *
         * @return {@link MethodHandle} for the policy applied to template
         *
         * @throws NullPointerException if any of the arguments are null
         */
        MethodHandle applier(String stencil, MethodType type);
    }

    /**
     * Factory for creating a new {@link PolicyBuilder} instance.
     *
     * @return a new {@link PolicyBuilder} instance.
     */
    public static PolicyBuilder builder() {
        return new PolicyBuilder();
    }

    /**
     * This builder class can be used to simplify the construction of a
     * {@link StringPolicy} or {@link SimplePolicy}.
     * <p>
     * The user starts by creating a new instance of this class
     * using {@link java.lang.TemplatePolicy#builder()}. When the user is finished
     * composing the policy then they should invoke {@link PolicyBuilder#build()}
     * on the instance returning a new instance of {@link StringPolicy}.
     * <p>
     * The {@link PolicyBuilder#fragment} and {@link PolicyBuilder#value}
     * methods can be used to validate and map the
     * {@link TemplatedString TemplatedString's} fragments and values.
     * <p>
     * Most instance methods in {@link PolicyBuilder} return {@code this}
     * {@link PolicyBuilder}, so it is possible to chain the methods.
     * Example: {@snippet :
     *      StringPolicy policy = TemplatePolicy.builder()
     *          .fragment(f -> f.toUpperCase())
     *          .value(v -> v instanceof Integer i ? Math.abs(i) : v)
     *          .build();
     * }
     * The {@link PolicyBuilder#preliminary} method allows the policy to validate
     * and map the source {@link TemplatedString}.
     * <p>
     * The {@link PolicyBuilder#format} method allows the policy to format
     * values. The {@code marker} specifies the string of characters marking the
     * beginning of a format specifier. The specifier is then passed to a
     * {@link BiFunction} along with the value to format.
     * Example: {@snippet :
     *      StringPolicy policy = TemplatePolicy.builder()
     *          .format("%", (specifier, value) ->
     *                  justify(String.valueOf(value), Integer.parseInt(specifier)))
     *          .build();
     *      int x = 10;
     *      int y = 12345;
     *      String result = policy.apply("%4\{x} + %4\{y} = %-5\{x + y}");
     * }
     * Output: {@code "  10 + **** = 12355"}
     * <p>
     * The {@link PolicyBuilder#resolve} method adds a value map that resolves
     * {@link Supplier}, {@link Future} and {@link FutureTask} values before
     * passing resolve values on to the next value map.
     * <p>
     * The alternate form of {@link PolicyBuilder#build(Function)} allows the
     * user to transform the {@link PolicyBuilder} result to something other than
     * a (@link String}.
     * Example: {@snippet :
     *      SimplePolicy<JSONObject> policy = TemplatePolicy.builder()
     *          .build(s -> new JSONObject(s));
     * }
     *
     * @implNote Due to the nature of lambdas, validating functions can only throw
     * unchecked exceptions, ex. {@link RuntimeException}.
     */
    @PreviewFeature(feature=PreviewFeature.Feature.STRING_TEMPLATES)
    public static class PolicyBuilder {
        /**
         * True if the policy is a simple concatenation policy. The default
         * is {@code true}. This field gets set to false if any of the state is
         * changed.
         */
        private boolean isSimple;

        /**
         * Function that can validate and map the source {@link TemplatedString}.
         */
        private Function<TemplatedString, TemplatedString> preliminary;

        /**
         * Function that can validate and map each fragment.
         */
        private Function<String, String> mapFragment;

        /**
         * Function that can validate and map each value.
         */
        private Function<Object, Object> mapValue;

        /**
         * String that marks the beginning of a format specifier. The
         * default is the empty string indicating no formatting.
         */
        private String marker;

        /**
         * Binary function that can format the value using a user defined
         * specifier.
         */
        private BiFunction<String, Object, String> formatValue;

        /**
         * Private constructor.
         */
        private PolicyBuilder() {
            clear();
        }

        /**
         * Resets the builder to the initial state.
         *
         * @return this Builder
         */
        public PolicyBuilder clear() {
            this.isSimple = true;
            this.preliminary = Function.identity();
            this.mapFragment = Function.identity();
            this.mapValue = Function.identity();
            this.marker = "";
            this.formatValue = (specifier, value) -> String.valueOf(value);

            return this;
        }

        /**
         * Augment the function that is used to validate and map
         * the source {@link TemplatedString}. Each successive call adds
         * to the mapping, chaining the result from the previous function. The
         * initial function is the identity function.
         * Example: {@snippet :
         *     StringPolicy policy = TemplatePolicy.builder()
         *          .preliminary(ts -> TemplatedString.of(ts.stencil().toUpperCase(), ts.values()))
         *          .build();
         * }
         *
         * @param preliminary  {@link Function} used to validate and map the source
         *                     {@link TemplatedString}.
         *
         * @return this {@link PolicyBuilder} instance
         */
        public PolicyBuilder preliminary(Function<TemplatedString, TemplatedString> preliminary) {
            Objects.requireNonNull(preliminary, "preliminary must not be null");
            final Function<TemplatedString, TemplatedString> oldPreliminary = this.preliminary;
            this.preliminary = ts -> preliminary.apply(oldPreliminary.apply(ts));
            this.isSimple = false;

            return this;
        }

        /**
         * Augment the function that is used to validate and map
         * {@link TemplatedString} fragments. Each successive call adds
         * to the mapping, chaining the result from the previous function. The
         * initial function is the identity function.
         * Example: {@snippet :
         *     StringPolicy policy = TemplatePolicy.builder()
         *          .fragment(f -> f.toUpperCase())
         *          .build();
         * }
         *
         * @param mapFragment {@link Function} used to validate and map the fragment
         *
         * @return this {@link PolicyBuilder} instance
         */
        public PolicyBuilder fragment(Function<String, String> mapFragment) {
            Objects.requireNonNull(mapFragment, "mapFragment must not be null");
            final Function<String, String> oldMapFragment = this.mapFragment;
            this.mapFragment = fragment ->
                    mapFragment.apply(oldMapFragment.apply(fragment));
            this.isSimple = false;

            return this;
        }

        /**
         * Augment the function that is used to validate and map
         * {@link TemplatedString} values. Each successive call adds
         * to the mapping, chaining the result from the previous function. The
         * initial function is the identity function.
         * Example: {@snippet :
         *     StringPolicy policy = TemplatePolicy.builder()
         *          .value(v -> v instanceof Integer i ? Math.abs(i) : v)
         *          .build();
         * }
         *
         * @param mapValue {@link Function} used to validate and map the fragment
         *
         * @return this {@link PolicyBuilder} instance
         */
        public PolicyBuilder value(Function<Object, Object> mapValue) {
            Objects.requireNonNull(mapValue, "mapValue must not be null");
            final Function<Object, Object> oldMapValue = this.mapValue;
            this.mapValue = v -> mapValue.apply(oldMapValue.apply(v));
            this.isSimple = false;

            return this;
        }

        /**
         * Augment the function that is used to validate and map
         * {@link TemplatedString} values by adding a function that resolves
         * {@link Supplier}, {@link Future} and {@link FutureTask} values before
         * passing on to next map value function.
         * Example: {@snippet :
         *     StringPolicy policy = TemplatePolicy.builder()
         *          .resolve()
         *          .build();
         * }
         *
         * @return this {@link PolicyBuilder} instance
         *
         * @implNote The resolving function will throw a {@link RuntimeException}
         * if a (@link Future} throws during resolution.
         */
        public PolicyBuilder resolve() {
            return value(value -> {
                if (value instanceof Future<?> future) {
                    if (future instanceof FutureTask<?> task) {
                        task.run();
                    }

                    try {
                        return future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                } else if (value instanceof Supplier<?> supplier) {
                    return supplier.get();
                }

                return value;
            });
        }

        /**
         * Add a final formatting step before a value is added to
         * the policy result. The {@code marker} string is used to locate any
         * format specifier in the fragment prior to the value. The specifier
         * is removed from the fragment and passed to the {@code formatValue}
         * binary function along with the value. The {@code marker} is not
         * included in the specifier.
         * Example: {@snippet :
         *      StringPolicy policy = TemplatePolicy.builder()
         *          .format("%", (specifier, value) ->
         *                  justify(String.valueOf(value), Integer.parseInt(specifier)))
         *          .build();
         * }
         *
         * @param marker       String used to mark the beginning of a format specifier
         *                     or {@code ""} to indicate no format processing.
         * @param formatValue  {@link BiFunction} used to format the value
         *
         * @return this {@link PolicyBuilder} instance
         */
        public PolicyBuilder format(String marker, BiFunction<String, Object, String> formatValue) {
            this.marker = Objects.requireNonNull(marker, "marker must not be null");
            this.formatValue = Objects.requireNonNull(formatValue, "doFormat must not be null");
            this.isSimple = false;

            return this;
        }

        /**
         * Construct a new {@link StringPolicy} using elements added to the builder.
         * The policy will initially run the preliminary map function on the source
         * {@link TemplatedString}. The policy will then iterate through fragments
         * and values, applying the fragment map function and value map function
         * on each iteration. If a format function is supplied, the value will be
         * formatted before adding to the result. The result of the policy will
         * be a string composed the mapped fragments and values.
         * Example: {@snippet :
         *      StringPolicy policy = TemplatePolicy.builder()
         *          .build();
         * }
         *
         * @return a new {@link StringPolicy} instance
         */
        public StringPolicy build() {
            final Function<TemplatedString, TemplatedString> preliminary = this.preliminary;
            final Function<String, String> mapFragment = this.mapFragment;
            final Function<Object, Object> mapValue = this.mapValue;
            final String marker = this.marker;
            final BiFunction<String, Object, String> formatValue = this.formatValue;

            return isSimple ? ts -> ts.concat()
                            : ts -> {
                ts = preliminary.apply(ts);
                String stencil = ts.stencil();
                List<String> fragments = ts.fragments();
                List<Object> values = ts.values();
                int estimate = stencil.length() + 16 * values.size();
                StringBuilder sb = new StringBuilder(estimate);
                Iterator<String> fragmentIterator = fragments.iterator();
                Iterator<Object> valuesIterator = values.iterator();

                if (!marker.isEmpty()) {
                    while(valuesIterator.hasNext()) {
                        String fragment = mapFragment.apply(fragmentIterator.next());
                        Object value = mapValue.apply(valuesIterator.next());
                        int index = fragment.indexOf(marker);

                        if (index != -1) {
                            String specifier = fragment.substring(index + marker.length());
                            fragment = fragment.substring(0, index);
                            value = formatValue.apply(specifier, value);
                        }

                        sb.append(fragment);
                        sb.append(value);
                    }
                } else {
                    while(valuesIterator.hasNext()) {
                        sb.append(mapFragment.apply(fragmentIterator.next()));
                        sb.append(mapValue.apply(valuesIterator.next()));
                    }
                }

                sb.append(mapFragment.apply(fragmentIterator.next()));

                return sb.toString();
            };
        }

        /**
         * This version of build performs the same actions as
         * {@link PolicyBuilder#build()} but always for a final transformation
         * step to convert the string result to a non-string result.
         * Example: {@snippet :
         * }
         *
         * @param transform {@link Function} used to map the string result
         *                  to a non-string result.
         *
         * @return a new {@link SimplePolicy} instance
         *
         * @param <R> type of result
         */
        public <R> SimplePolicy<R> build(Function<String, R> transform) {
            Objects.requireNonNull(transform, "transform must not be null");

            return ts -> transform.apply(build().apply(ts));
        }

    }
}
