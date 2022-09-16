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

package java.lang.template;

import java.util.*;
import java.util.stream.Collectors;

import jdk.internal.javac.PreviewFeature;

/**
 * The Java compiler produces implementations of {@link TemplatedString} to
 * represent string templates and text block templates. Libraries may produce
 * {@link TemplatedString} instances as long as they conform to the requirements
 * of this interface. Like {@link String}, instances of {@link TemplatedString}
 * implementations are considered immutable.
 * <p>
 * Implementations of this interface must minimally implement the methods
 * {@link TemplatedString#fragments()} and {@link TemplatedString#values()}.
 * <p>
 * The {@link TemplatedString#fragments()} method must return an immutable
 * {@code List<String>} consistent with the string template body. The list
 * contains the string of characters preceeding each of the embedded expressions
 * plus the string of characters following the last embedded expression. The order
 * of the strings is left to right as they appear in the string template.
 * For example; {@snippet :
 * TemplatedString ts = "The \{name} and \{address} of the resident.";
 * List<String> fragments = ts.fragments();
 * }
 * {@code fragments} will be equivalent to <code>List.of("The ", " and ", " of the resident.")</code>.
 * <p>
 * The {@link TemplatedString#values()} method returns an immutable {@code
 * List<Object>} of values accumulated by evaluating embedded expressions prior
 * to instantiating the {@link TemplatedString}. The values are accumulated left
 * to right. The first element of the list is the result of the leftmost
 * embedded expression. The last element of the list is the result of the
 * rightmost embedded expression.
 * For example,
 * {@snippet :
 * int x = 10;
 * int y = 20;
 * TemplatedString ts = "\{x} + \{y} = \{x + y}";
 * List<Object> values = ts.values();
 * }
 * {@code values} will be the equivalent of <code>List.of(x, y, x + y)</code>.
 * <p>
 * {@link TemplatedString TemplatedStrings} are primarily used in conjuction
 * with {@link TemplateProcessor} to produce meaningful results. For example, if a
 * user wants string interpolation, then they can use a string template
 * expression with the standard {@link TemplatedString#STR} processor.
 * {@snippet :
 * int x = 10;
 * int y = 20;
 * String result = STR."\{x} + \{y} = \{x + y}";
 * }
 * {@code result} will be equivalent to <code>"10 + 20 = 30"</code>.
 * <p>
 * The {@link TemplatedString#apply} method supplies an alternative to using
 * string template expressions.
 * {@snippet :
 * String result = "\{x} + \{y} = \{x + y}".apply(STR);
 * }
 * In addition to string template expressions, the factory methods
 * {@link TemplatedString#of(String)} and {@link TemplatedString#of(List, List)}
 * can be used to construct {@link TemplatedString TemplatedStrings}. The
 * {@link TemplateBuilder} class can be used to construct
 * {@link TemplatedString TemplatedStrings} from parts; string fragments,
 * values and other {@link TemplatedString TemplatedStrings}.
 * <p>
 * The {@link TemplatedString#interpolate()} method provides a simple way to perform
 * string interpolation of the {@link TemplatedString}.
 * <p>
 * {@link TemplateProcessor Template processors} typically use the following code pattern
 * to perform composition:
 * {@snippet :
 * StringBuilder sb = new StringBuilder();
 * Iterator<String> fragmentsIter = ts.fragments().iterator(); // @highlight substring="fragments()"
 *
 * for (Object value : ts.values()) { // @highlight substring="values()"
 *     sb.append(fragmentsIter.next());
 *     sb.append(value);
 * }
 *
 * sb.append(fragmentsIter.next());
 * String result = sb.toString();
 * }
 *
 * @implSpec An instance of {@link TemplatedString} is immutatble. Also, the
 * fragment list size must be one more than the values list size.
 *
 * @see java.lang.template.TemplateProcessor
 * @see java.lang.template.SimpleProcessor
 * @see java.lang.template.StringProcessor
 * @see java.util.FormatProcessor
 *
 * @since 20
 */
@PreviewFeature(feature=PreviewFeature.Feature.STRING_TEMPLATES)
public interface TemplatedString {
    /**
     * Returns an immutable list of string fragments consisting of the string
     * of characters preceeding each of the embedded expressions plus the
     * string of characters following the last embedded expression. In the
     * example: {@snippet :
     * TemplatedString ts = "The student \{student} is in \{teacher}'s class room.";
     * List<String> fragments = ts.fragments(); // @highlight substring="fragments()"
     * }
     * <code>fragments</code> will be equivalent to
     * <code>List.of("The student ", " is in ", "'s class room.")</code>
     *
     * @return list of string fragments
     *
     * @implSpec The list returned is immutable.
     */
    List<String> fragments();

    /**
     * Returns an immutable list of embedded expression results. In the example:
     * {@snippet :
     * TemplatedString templatedString = "\{x} + \{y} = \{x + y}";
     * List<Object> values = templatedString.values(); // @highlight substring="values()"
     * }
     * <code>values</code> will be equivalent to <code>List.of(x, y, x + y)</code>
     *
     * @return list of expression values
     *
     * @implSpec The list returned is immutable.
     */
    List<Object> values();


    /**
     * {@return the interpolation of the TemplatedString}
     */
    default String interpolate() {
        return TemplatedString.interpolate(this);
    }

    /**
     * Returns the result of applying the specified processor to this {@link TemplatedString}.
     * This method can be used as an alternative to string template expressions. For example,
     * {@snippet :
     * String result1 = STR."\{x} + \{y} = \{x + y}";
     * String result2 = "\{x} + \{y} = \{x + y}".apply(STR); // @highlight substring="apply"
     * }
     * produces an equivalent result for both {@code result1} and {@code result2}.
     *
     * @param processor the {@link TemplateProcessor} instance to apply
     *
     * @param <R>  Processor's apply result type.
     * @param <E>  Exception thrown type.
     *
     * @return constructed object of type R
     *
     * @throws E exception thrown by the template processor when validation fails
     * @throws NullPointerException if templatedString is null
     *
     * @implNote The default implementation simply invokes the processor's apply
     * method {@code processor.apply(this)}.
     */
    default <R, E extends Throwable> R apply(TemplateProcessor<R, E> processor) throws E {
        Objects.requireNonNull(processor, "processor should not be null");

        return processor.apply(this);
    }

    /**
     * Produces a diagnostic string representing the supplied
     * {@link TemplatedString}.
     *
     * @param templatedString  the {@link TemplatedString} to represent
     *
     * @return diagnostic string representing the supplied templated string
     *
     * @throws NullPointerException if templatedString is null
     */
    public static String toString(TemplatedString templatedString) {
        Objects.requireNonNull(templatedString, "templatedString should not be null");
        String fragments = "[\"" + String.join("\", \"", templatedString.fragments()) + "\"](";

        return templatedString.values()
                .stream()
                .map(v -> String.valueOf(v))
                .collect(Collectors.joining(", ", fragments, ")"));
    }

    /**
     * {@return an interpolatation of fragments and values}
     *
     * @param templatedString  the {@link TemplatedString} to process
     *
     * @throws NullPointerException if templatedString is null
     */
    private static String interpolate(TemplatedString templatedString) {
        Objects.requireNonNull(templatedString, "templatedString must not be null");

        List<String> fragments = templatedString.fragments();
        List<Object> values = templatedString.values();

        if (fragments.size() == 1) {
            return fragments.get(0);
        }

        Iterator<String> fragmentsIter = fragments.iterator();
        StringBuilder sb = new StringBuilder();

        for (Object value : values) {
            sb.append(fragmentsIter.next());
            sb.append(value);
        }

        sb.append(fragmentsIter.next());

        return sb.toString();
    }

    /**
     * Returns a TemplatedString composed from a string.
     *
     * @param string  single string fragment
     *
     * @return TemplatedString composed from string
     *
     * @throws NullPointerException if string is null
     */
    public static TemplatedString of(String string) {
        Objects.requireNonNull(string, "string must not be null");

        return new SimpleTemplatedString(List.of(string), List.of());
    }

    /**
     * Returns a TemplatedString composed from fragments and values.
     *
     * @implSpec The {@code fragments} list size must be one more that the
     * {@code values} list size.
     *
     * @param fragments list of string fragments
     * @param values    list of expression values
     *
     * @return TemplatedString composed from string
     *
     * @throws IllegalArgumentException if fragments list size is not one more
     *         than values list size
     * @throws NullPointerException if fragments or values is null
     *
     * @implNote Contents of both lists are copied to construct immutable lists.
     */
    public static TemplatedString of(List<String> fragments, List<Object> values) {
        Objects.requireNonNull(fragments, "fragments must not be null");
        Objects.requireNonNull(values, "values must not be null");

        if (values.size() + 1 != fragments.size()) {
            throw new IllegalArgumentException(
                    "fragments list size is not one more than values list size");
        }

        fragments = Collections.unmodifiableList(new ArrayList<>(fragments));
        values = Collections.unmodifiableList(new ArrayList<>(values));

        return new SimpleTemplatedString(fragments, values);
    }

    /**
     * Interpolation template processor instance.
     * Example: {@snippet :
     * int x = 10;
     * int y = 20;
     * String result = STR."\{x} + \{y} = \{x + y}"; // @highlight substring="STR"
     * }
     * @implNote The result of interpolation is not interned.
     */
    public static final StringProcessor STR = new StringProcessor() {
        @Override
        public String apply(TemplatedString templatedString) {
            Objects.requireNonNull(templatedString);

            return templatedString.interpolate();
        }
    };

    /**
     * This predefined FormatProcessor instance constructs a String result using {@link
     * Formatter}. Unlike {@link Formatter}, FormatProcessor uses the value from
     * the embedded expression that follows immediately after the
     * <a href="../../util/Formatter.html#syntax">format specifier</a>.
     * TemplatedString expressions without a preceeding specifier, use "%s" by

     * Example: {@snippet :
     * int x = 123;
     * int y = 987;
     * String result = FMT."%3d\{x} + %3d\{y} = %4d\{x + y}"; // @highlight substring="FMT"
     * }
     * {@link FMT} uses the Locale.US {@link Locale}.
     */
    public static final FormatProcessor FMT = new FormatProcessor(Locale.US);

    /**
     * Factory for creating a new {@link TemplateBuilder} instance.
     *
     * @return a new {@link TemplateBuilder} instance.
     */
    public static TemplateBuilder builder() {
        return new TemplateBuilder();
    }

    /**
     * Instances of this class can be used to construct a new TemplatedString from
     * string fragments, values and other {@link TemplatedString TemplatedStrings}.
     * <p>
     * To use, construct a new {@link TemplateBuilder} using
     * {@link TemplatedString#builder}, then chain invokes of
     * {@link TemplateBuilder#fragment} or {@link TemplateBuilder#value} to
     * build up the {@link TemplatedString}.
     * {@link TemplateBuilder#template(TemplatedString)} can be used to add the
     * fragments and values from another {@link TemplatedString}.
     * {@link TemplateBuilder#build()} can be invoked at the end of the chain to
     * produce a new {@link TemplatedString} using the current state of the
     * builder.
     * <p>
     * Example: {@snippet :
     *      int x = 10;
     *      int y = 20;
     *      TemplatedString ts = TemplatedString.builder()
     *          .fragment("The result of adding ")
     *          .value(x)
     *          .template(" and \{y} equals \{x + y}")
     *          .build();
     *      String result = STR.apply(ts);
     *  }
     *
     *  Result: "The result of adding 10 and 20 equals 30"
     */
    @PreviewFeature(feature=PreviewFeature.Feature.STRING_TEMPLATES)
    public static class TemplateBuilder {
        /**
         * {@link ArrayList} used to gather fragments.
         */
        private final List<String> fragments;

        /**
         * {@link ArrayList} used to gather values.
         */
        private final List<Object> values;

        /**
         * Private Constructor.
         */
        private TemplateBuilder() {
            this.fragments = new ArrayList<>();
            this.values = new ArrayList<>();

            fragments.add("");
        }

        /**
         * Add the supplied {@link TemplatedString TemplatedString's} fragments and
         * values to the builder.
         *
         * @param templatedString existing TemplatedString
         *
         * @return this Builder
         *
         * @throws NullPointerException if templatedString is null
         */
        public TemplateBuilder template(TemplatedString templatedString) {
            Objects.requireNonNull(templatedString, "templatedString must not be null");
            List<String> otherFragments = templatedString.fragments();
            List<Object> otherValues = templatedString.values();
            fragment(otherFragments.get(0));
            fragments.addAll(otherFragments.subList(1, otherFragments.size()));
            values.addAll(otherValues);

            return this;
        }

        /**
         * Add a string fragment to the {@link TemplateBuilder Builder's} fragments.
         *
         * @param fragment  string fragment to be added
         *
         * @return this Builder
         *
         * @throws NullPointerException if string is null
         */
        public TemplateBuilder fragment(String fragment) {
            Objects.requireNonNull(fragment, "string must not be null");

            int i = fragments.size() - 1;
            fragments.set(i, fragments.get(i) + fragment);

            return this;
        }

        /**
         * Add a value to the {@link TemplateBuilder}. This method will also advance the
         * builder's last fragment.
         *
         * @param value value to be added
         *
         * @return this Builder
         */
        public TemplateBuilder value(Object value) {
            values.add(value);
            fragments.add("");

            return this;
        }

        /**
         * Resets the builder to the initial state; one empty fragment and empty values.
         *
         * @return this Builder
         */
        public TemplateBuilder clear() {
            fragments.clear();
            values.clear();
            fragments.add("");

            return this;
        }

        /**
         * Returns a {@link TemplatedString} based on the current state of the
         * {@link TemplateBuilder Builder's} fragments and values.
         *
         * @return a new TemplatedString
         */
        public TemplatedString build() {
            return new SimpleTemplatedString(Collections.unmodifiableList(fragments),
                                             Collections.unmodifiableList(values));
        }

    }
}
