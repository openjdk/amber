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

import java.lang.TemplatedString.Builder;
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
 * {@link TemplatedString#stencil()} and {@link TemplatedString#values()}.
 * <p>
 * The {@link TemplatedString#stencil()} method must return a string
 * consistent with the string template content, with placeholders standing in for
 * embedded expressions. The {@linkplain PLACEHOLDER placeholder} character used
 * is the Unicode <code>OBJECT-REPLACEMENT-CHARACTER (&#92;ufffc)</code>.
 * For example; {@snippet :
 * TemplatedString templatedString = "\{x} + \{y} = \{x + y}";
 * String stencil = templatedString.stencil();
 * }
 * {@code stencil} will be equivalent to <code>"&#92;ufffc + &#92;ufffc = &#92;ufffc"</code>.
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
 * TemplatedString templatedString = "\{x} + \{y} = \{x + y}";
 * List<Object> values = templatedString.values();
 * }
 * {@code values} will be the equivalent of <code>List.of(x, y, x + y)</code>.
 * <p>
 * {@link TemplatedString TemplatedStrings} are primarily used in conjuction
 * with {@link TemplatePolicy} to produce meaningful results. For example, if a
 * user wants string concatenation, replacing placeholders in the stencil with
 * values, then they can use a string template expression with the standard
 * {@link TemplatePolicy#STR} policy.
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
 * {@link TemplatedString#of(String)} and {@link TemplatedString#of(String, List)}
 * can be used to construct {@link TemplatedString TemplatedStrings}. The
 * {@link Builder} class can be used to construct {@link TemplatedString TemplatedStrings}
 * from parts; strings, values and other {@link TemplatedString TemplatedStrings}.
 * <p>
 * The remaining methods are primarily used by {@linkplain TemplatePolicy policies}
 * to construct results.
 * <p>
 * The {@link TemplatedString#split(String)} and {@link TemplatedString#fragments()}
 * methods can be used to work with the portions the stencil around the
 * placeholders, especially in junction with {@link StringBuilder}.
 * <p>
 * The {@link TemplatedString#concat()} method provides a easy way to get a
 * simple string concatenation by combining the stencil and values.
 *
 * @implSpec An instance of {@link TemplatedString} is immutatble. Also, the
 * placeholder count in the stencil must equal the values list size.
 *
 * @see java.lang.TemplatePolicy
 * @see java.lang.TemplatePolicy.SimplePolicy
 * @see java.lang.TemplatePolicy.StringPolicy
 * @see java.util.FormatterPolicy
 */
@PreviewFeature(feature=PreviewFeature.Feature.TEMPLATED_STRINGS)
public interface TemplatedString {

    /**
     * Placeholder character usied to replace embedded expressions in the
     * stencil. The value used is the unicode
     * <code>OBJECT-REPLACEMENT-CHARACTER (&#92;ufffc)</code>.
     */
    public static final char PLACEHOLDER = '\ufffc';

    /**
     * String equivalent of {@link PLACEHOLDER}.
     */
    public static final String PLACEHOLDER_STRING = Character.toString(PLACEHOLDER);

    /**
     * Returns the stencil string with placeholders. In the example:
     * {@snippet :
     * TemplatedString templatedString = "\{x} + \{y} = \{x + y}";
     * String stencil = templatedString.stencil(); // @highlight substring="stencil()"
     * }
     * {@code stencil} will be equivalent to <code>"&#92;ufffc + &#92;ufffc = &#92;ufffc"</code>.
     *
     * @return the stencil string with placeholder
     */
    String stencil();

    /**
     * Returns an immutable list of embedded expression results. In the example:
     * {@snippet :
     * TemplatedString templatedString = "\{x} + \{y} = \{x + y}";
     * List<Object> values = templatedString.values(); // @highlight substring="values()"
     * }
     * <code>values</code> will be equivalent to <code>List.of(x, y, x + y)</code>
     *
     * @return list of expression values
     */
    List<Object> values();

    /**
     * Returns an immutable list of string fragments created by splitting the
     * stencil at placeholders using {@link TemplatedString#split(String)}. In
     * the example: {@snippet :
     * TemplatedString templatedString = "The student \{student} is in \{teacher}'s class room.";
     * List<String> fragments = templatedString.fragments(); // @highlight substring="fragments()"
     * }
     * <code>fragments</code> will be equivalent to
     * <code>List.of("The student ", " is in ", "'s class room.")</code>
     * <p>
     * {@link fragments} is a convenience method for {@link TemplatePolicy
     * template policies} that construct results using {@link StringBuilder}.
     * Typically using the following pattern:
     * {@snippet :
     * StringBuilder sb = new StringBuilder();
     * Iterator<String> fragmentsIter = templatedString.fragments().iterator(); // @highlight substring="fragments()"
     *
     * for (Object value : templatedString.values()) {
     *     sb.append(fragmentsIter.next());
     *     sb.append(value);
     * }
     *
     * sb.append(fragmentsIter.next());
     * String result = sb.toString();
     * }
     * @implSpec The list returned is immutable.
     *
     * @implNote The {@link TemplatedString} implementation generated by the
     * compiler for a string template guarantees efficiency by only computing the
     * fragments list once. Other implementations should make an effort to do the
     * same. The default implementation applies the {@link TemplatedString#split}
     * method to the stencil, each time the method is invoked.
     *
     * @return list of string fragments
     */
    default List<String> fragments() {
        return TemplatedString.split(stencil());
    }

    /**
     * {@return the stencil with the values injected at placeholders, i.e., the equivalent
     * of string concatenation}
     */
    default String concat() {
        return TemplatedString.concat(this);
    }

    /**
     * Returns the result of applying the specified policy to this {@link java.lang.TemplatedString}.
     * This method can be used as an alternative to string template expressions. For example,
     * {@snippet :
     * String result1 = STR."\{x} + \{y} = \{x + y}";
     * String result2 = "\{x} + \{y} = \{x + y}".apply(STR);
     * }
     * produces an equivalent result for both {@code result1} and {@code result2}.
     *
     * @param policy the {@link TemplatePolicy} instance to apply
     *
     * @param <R>  Policy's apply result type.
     * @param <E>  Exception thrown type.
     *
     * @return constructed object of type R
     *
     * @throws E exception thrown by the template policy when validation fails
     * @throws NullPointerException if templatedString is null
     *
     * @implNote The default implementation simply invokes the policy's apply
     * method {@code policy.apply(this)}.
     */
    default <R, E extends Throwable> R apply(TemplatePolicy<R, E> policy) throws E {
        Objects.requireNonNull(policy, "policy should not be null");

        return policy.apply(this);
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

        String stencil = templatedString.stencil()
                .replace("\"", "\\\"")
                .replace(PLACEHOLDER_STRING, "\\{}");

        String prefix = "\"" + stencil + "\"(";
        String delimiter = ", ";
        String suffix = ")";

        return templatedString.values()
                .stream()
                .map(v -> String.valueOf(v))
                .collect(Collectors.joining(delimiter, prefix, suffix));
    }

    /**
     * Splits a stencil at placeholders, returning an immutable list of strings.
     *
     * @implSpec Unlike {@link String#split String.split} this method returns an
     * immutable list and retains empty edge cases (empty string and string
     * ending in placeholder) so that fragments prefix and suffix all placeholders
     * (expression values). That is, <code>fragments().size() == values().size()
     * + 1</code>.
     *
     * @param stencil  a {@link String} with placeholders
     *
     * @return list of string fragments
     *
     * @throws NullPointerException if string is null
     */
    public static List<String> split(String stencil) {
        Objects.requireNonNull(stencil, "stencil must not be null");

        List<String> fragments = new ArrayList<>(8);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < stencil.length(); i++) {
            char ch = stencil.charAt(i);

            if (ch == PLACEHOLDER) {
                fragments.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(ch);
            }
        }

        fragments.add(sb.toString());

        return Collections.unmodifiableList(fragments);
    }

    /**
     * {@return the stencil with the values inserted at placeholders}
     *
     * @param templatedString  the {@link TemplatedString} to process
     *
     * @throws NullPointerException if templatedString is null
     */
    private static String concat(TemplatedString templatedString) {
        Objects.requireNonNull(templatedString, "templatedString must not be null");

        String stencil = templatedString.stencil();
        List<Object> values = templatedString.values();

        if (values.size() == 0) {
            return stencil;
        }

        List<String> fragments = templatedString.fragments();

        Iterator<String> fragmentsIter = fragments.iterator();
        StringBuilder sb = new StringBuilder(stencil.length() + 16 * values.size());

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
     * @implSpec The stencil string can not contain expressions or placeholders.
     *
     * @param stencil  stencil string with no placeholders
     *
     * @return TemplatedString composed from stencil
     *
     * @throws IllegalArgumentException if stencil contains a {@link PLACEHOLDER}
     * @throws NullPointerException if string is null
     */
    public static TemplatedString of(String stencil) {
        Objects.requireNonNull(stencil, "stencil must not be null");

        if (stencil.indexOf(PLACEHOLDER) != -1) {
            throw new IllegalArgumentException("stencil contains a placeholder");
        }

        return new SimpleTemplatedString(stencil, List.of(), List.of(stencil));
    }

    /**
     * Returns a TemplatedString composed from a stencil string and values.
     *
     * @implSpec The number of placeholders in the stencil must match the length
     * of the values list.
     *
     * @param stencil  stencil string with placeholders
     * @param values   immutable list of expression values
     *
     * @return TemplatedString composed from string
     *
     * @throws IllegalArgumentException if the number of placeholders in the
     *         stencil doesn't matching the length of the values list
     * @throws NullPointerException if stencil or values is null
     */
    public static TemplatedString of(String stencil, List<Object> values) {
        Objects.requireNonNull(stencil, "stencil must not be null");
        Objects.requireNonNull(values, "values must not be null");
        List<String> fragments = split(stencil);

        if (values.size() + 1 != fragments.size()) {
            throw new IllegalArgumentException(
                    "stencil placeholder count doesn't matching the values list size");
        }

        return new SimpleTemplatedString(stencil, values, fragments);
    }

    /**
     * Factory for creating a new {@link TemplatedString.Builder} instance.
     *
     * @return a new {@link TemplatedString.Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * This class can be used to construct a new TemplatedString from string
     * fragments, values and other {@link TemplatedString TemplatedStrings}.
     * <p>
     * To use, construct a new {@link Builder} using {@link TemplatedString#builder},
     * then chain invokes of {@link Builder#fragment} or {@link Builder#value} to build up the
     * stencil and values.
     * {@link Builder#template(TemplatedString)} can be used to add the
     * stencil and values from another {@link TemplatedString}.
     * {@link Builder#build()} can be invoked at the end of the chain to produce a new
     * TemplatedString using the current state of the builder.
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
     * <p>
     * The {@link Builder} itself implements {@link TemplatedString}. When
     * applied to a policy will use the current state of stencil and values to
     * produce a result.
     */
    @PreviewFeature(feature=PreviewFeature.Feature.TEMPLATED_STRINGS)
    public static class Builder implements TemplatedString {
        /**
         * {@link StringBuilder} used to construct the final stencil.
         */
        private final StringBuilder stencilBuilder;

        /**
         * {@link ArrayList} used to gather values.
         */
        private final List<Object> values;

        /**
         * package private Constructor.
         */
        Builder() {
            this.stencilBuilder = new StringBuilder();
            this.values = new ArrayList<>();
        }

        @Override
        public String stencil() {
            return stencilBuilder.toString();
        }

        @Override
        public List<Object> values() {
            return Collections.unmodifiableList(values);
        }

        /**
         * Add the supplied {@link TemplatedString TemplatedString's} stencil and
         * values to the builder.
         *
         * @param templatedString existing TemplatedString
         *
         * @return this Builder
         *
         * @throws NullPointerException if templatedString is null
         */
        public Builder template(TemplatedString templatedString) {
            Objects.requireNonNull(templatedString, "templatedString must not be null");

            stencilBuilder.append(templatedString.stencil());
            values.addAll(templatedString.values());

            return this;
        }

        /**
         * Add a string fragment to the {@link Builder Builder's} stencil.
         *
         * @param fragment  string fragment to be added
         *
         * @return this Builder
         *
         * @throws IllegalArgumentException if fragment contains a {@link PLACEHOLDER}
         * @throws NullPointerException if string is null
         */
        public Builder fragment(String fragment) {
            Objects.requireNonNull(fragment, "string must not be null");

            if (fragment.indexOf(PLACEHOLDER) != -1) {
                throw new IllegalArgumentException("string fragment contains a placeholder");
            }

            stencilBuilder.append(fragment);

            return this;
        }

        /**
         * Add a value to the {@link Builder}. This method will also insert a placeholder
         * in the {@link Builder Builder's} stencil.
         *
         * @param value value to be added
         *
         * @return this Builder
         */
        public Builder value(Object value) {
            stencilBuilder.append(PLACEHOLDER);
            values.add(value);

            return this;
        }

        /**
         * Returns a {@link TemplatedString} based on the current state of the
         * {@link Builder Builder's} stencil and values.
         *
         * @return a new TemplatedString
         */
        public TemplatedString build() {
            final String stencil = this.stencil();
            final List<Object> values = Collections.unmodifiableList(this.values());
            final List<String> fragments = TemplatedString.split(stencil);

            return new SimpleTemplatedString(stencil, values, fragments);
        }


        /**
         * Resets the builder to the initial state; empty stencil, empty values.
         */
        public void clear() {
            stencilBuilder.setLength(0);
            values.clear();
        }
    }
}
