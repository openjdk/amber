/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.invoke.MethodHandles.Lookup;
import java.util.*;

/**
 * A {@link TemplatedString} object represents the information captured from a templated string
 * expression. This information is comprised of a template string and the values from the
 * evaluation of embedded expressions. The template string is a {@link String} with placeholders
 * where expressions existed in the original templated string expression.
 * <p>
 * {@link TemplatedString TemplatedStrings} are primarily used in conjuction with
 * {@link TemplatePolicy TemplatePolicies} to produce useful results. For example, if a user
 * needs to produce a {@link String} by replacing placeholders with values then they might use
 * supplied {@link java.lang.TemplatePolicy#CONCAT} policy.
 * {@snippet :
 * import static java.lang.TemplatePolicy.CONCAT;
 * ...
 * int x = 10;
 * int y = 20;
 * String result = CONCAT."\{x} + \{y} = \{x + y}";
 * System.out.println(result);
 * }
 * Outputs: <code>10 + 20 = 30</code>
 * <p>
 * Some existing APIs have been adapted to accept {@link  TemplatedString TemplatedStrings}
 * as arguments. For example, the above example can be rewritten to use
 * {@link java.io.PrintStream#format(TemplatedString)}.
 * {@snippet :
 * int x = 10;
 * int y = 20;
 * System.out.format("%5d\{x} + %5d\{y} = %5d\{x + y}%n");
 * }
 * Outputs: <code>&nbsp;&nbsp;&nbsp;10&nbsp;+&nbsp;&nbsp;&nbsp;20&nbsp;=&nbsp;&nbsp;&nbsp;&nbsp;30</code>
 * @implSpec An instance of {@link TemplatedString} is immutatble. The number of placeholders in
 * the template string must equal the number of values captured.
 *
 * @see java.lang.TemplatePolicy
 * @see java.util.FormatterPolicy
 */
public interface TemplatedString {

    /**
     * Placeholder character marking insertion points in a templated string. The value used is
     * the unicode OBJECT REPLACEMENT CHARACTER.
     */
    public static final char OBJECT_REPLACEMENT_CHARACTER = '\uFFFC';

    /**
     * String equivalent of {@link OBJECT_REPLACEMENT_CHARACTER}.
     */
    public static final String OBJECT_REPLACEMENT_CHARACTER_STRING =
            Character.toString(OBJECT_REPLACEMENT_CHARACTER);

    /**
     * Returns the template string with placeholders. In the example:
     * {@snippet :
     * TemplatedString templatedString = "\{a} + \{b} = \{a + b}";
     * String template = templatedString.template(); // @highlight substring="template " @highlight substring="template("
     * }
     * <code>template</code> will be equivalent to <code>"&#92;uFFFC + &#92;uFFFC = &#92;uFFFC"</code>.
     *
     * @return template string with placeholders.
     */
    String template();

    /**
     * Returns the list of expression values. In the example:
     * {@snippet :
     * TemplatedString templatedString = "\{a} + \{b} = \{a + b}";
     * List<Object> values = templatedString.values(); // @highlight substring="values"
     * }
     * <code>values</code> will be equivalent to <code>List.of(a, b, a + b)</code>
     *
     * @return list of expression values
     */
    List<Object> values();

    /**
     * Returns an immutable list of string segments created by splitting the template string
     * at placeholders using {@link TemplatedString#split(String)}. In the example:
     * {@snippet :
     * TemplatedString templatedString = "The student \{student} is in \{teacher}'s class room.";
     * List<String> segments = templatedString.segments(); // @highlight substring="segments"
     * }
     * <code>segments</code> will be equivalent to
     * <code>List.of("The student ", " is in ", "'s class room.")</code>
     * <p>
     * {@link segments} is a convenience method for {@link TemplatePolicy TemplatePolicies} that
     * construct results using {@link StringBuilder}. Typically using the following pattern:
     * {@snippet :
     * StringBuilder sb = new StringBuilder();
     * Iterator<Object> values = templatedString.values().iterator();
     *
     * for (String segment : templatedString.segments()) { // @highlight substring="segments"
     *     sb.append(segment);
     *
     *     if (values.hasNext()) {
     *         sb.append(values.next());
     *     }
     * }
     *
     * String result = sb.toString();
     * }
     * @implSpec The list returned is immutable.
     *
     * @implNote The {@link TemplatedString} implementation generated by the compiler for a
     * templated string expression guarantees efficiency by only computing the segments list once.
     * Other implementations should make an effort to do the same.
     *
     * @return list of string segments
     */
    default List<String> segments() {
        return TemplatedString.split(template());
    }

    /**
     * Returns the template string with the values inserted at placeholders.
     *
     * @implNote The {@link TemplatedString} implementation generated by the compiler for a
     * templated string expression guarantees efficiency by using Java string concatenation.
     * Other implementations can use {@link TemplatedString#concat(TemplatedString)} which
     * is correct but may not be the most efficient method.
     *
     * @return the template string with the values inserted at placeholders
     */
    default String concat() {
        return TemplatedString.concat(this);
    }

    /**
     * Returns an ordered list of MethodHandle value getters. The order matches the order of
     * expressions, left to right. The method is primarily used by
     * {@link TemplatePolicy TemplatePolicies} to produce optimized applications for
     * {@link TemplatePolicy#applyMethodHandle}.
     *
     * @implNote The implementations of TemplatedString generated by the compiler will guarantee
     * the presence of this method, but it is not a requirement for other TemplatedString
     * implementations to supply this method.
     *
     * @return ordered list of MethodHandle value getters
     *
     * @throws ReflectiveOperationException  if the getters are not accessable
     * @throws UnsupportedOperationException if TemplatedString implementation doesn't
     *                                       implement method
     */
    default List<MethodHandle> vars() throws ReflectiveOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the result of applying the specified policy to this TemplatedString.
     *
     * @param policy the {@link TemplatePolicy} instance to apply
     *
     * @param <R>  Policy's apply result type.
     * @param <E>  Exception thrown type.
     *
     * @return constructed object of type R
     *
     * @throws E exception thrown by the template policy when validation fails
     */
    default <R, E extends Throwable> R apply(TemplatePolicy<R, E> policy) throws E {
        return policy.apply(this);
    }

    /**
     * Produces a diagnostic string representing the supplied {@link TemplatedString}.
     *
     * @param templatedString  the {@link TemplatedString} to represent
     *
     * @return diagnostic string representing the supplied templated string
     */
    public static String toString(TemplatedString templatedString) {
        StringBuilder sb = new StringBuilder();
        String template = templatedString.template()
                .replace("\"", "\\\"")
                .replace(OBJECT_REPLACEMENT_CHARACTER_STRING, "\\{}");

        sb.append('"');
        sb.append(template);
        sb.append('"');
        sb.append('(');

        boolean first = true;
        for (Object value : templatedString.values()) {
            if (!first) {
                sb.append(", ");
            } else {
                first = false;
            }

            sb.append(value);
        }

        sb.append(')');

        return sb.toString();
    }

    /**
     * Splits a string at placeholders, returning an immutable list of strings.
     *
     * @implSpec Unlike {@link String#split} this method returns an immutable list and retains empty
     * edge cases (empty string and string ending in placeholder) so that segments prefix and
     * suffix all placeholders (expression values). That is,
     * <code>segments().size() == values().size() + 1</code>.
     *
     * @param string  a {@link String} with placeholders
     *
     * @return list of string segments
     */
    public static List<String> split(String string) {
        List<String> strings = new ArrayList<>(8);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < string.length(); i++) {
            char ch = string.charAt(i);

            if (ch == OBJECT_REPLACEMENT_CHARACTER) {
                strings.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(ch);
            }
        }

        strings.add(sb.toString());

        return Collections.unmodifiableList(strings);
    }

    /**
     * Returns the template with the values inserted at placeholders.
     *
     * @param templatedString  the {@link TemplatedString} to concatenate
     *
     * @return the template with the values inserted at placeholders
     */
    public static String concat(TemplatedString templatedString) {
        StringBuilder sb = new StringBuilder();
        Iterator<Object> values = templatedString.values().iterator();

        for (String segment : templatedString.segments()) {
            sb.append(segment);

            if (values.hasNext()) {
                sb.append(values.next());
            }
        }

        return sb.toString();
    }

    /**
     * Returns a TemplatedString composed from a String.
     *
     * @implSpec The string can not contain expressions or OBJECT REPLACEMENT CHARACTER.
     *
     * @param string  a {@link String} to be composed into a {@link TemplatedString}.
     *
     * @return TemplatedString composed from string
     *
     * @throws IllegalArgumentException if string contains OBJECT REPLACEMENT CHARACTER.
     */
    public static TemplatedString of(String string) {
        if (string.indexOf(OBJECT_REPLACEMENT_CHARACTER) != -1) {
            throw new IllegalArgumentException("string contains an OBJECT REPLACEMENT CHARACTER");
        }

        return new TemplatedString() {
            List<String> SEGMENTS = List.of(string);

            @Override
            public String template() {
                return string;
            }

            @Override
            public List<Object> values() {
                return List.of();
            }

            @Override
            public List<String> segments() {
                return SEGMENTS;
            }

            @Override
            public String concat() {
                return string;
            }
        };
    }

    /**
     * This method forces lazy expressions to evaluate and freezes the values in a new
     * instance of {@link TemplatedString}.
     *
     * @param templatedString  templated string to freeze
     *
     * @return new {@link TemplatedString} with all lazy expressions evaluated.
     */
    public static TemplatedString freeze(TemplatedString templatedString) {
        final String template = templatedString.template();
        final List<Object> values = templatedString.values();
        final List<String> segments = templatedString.segments();

        return new TemplatedString() {
            @Override
            public String template() {
                return template;
            }

            @Override
            public List<Object> values() {
                return values;
            }

            @Override
            public List<String> segments() {
                return segments;
            }
        };
    }

    /**
     * Bootstrap method use to construct a call site for {@link TemplatePolicy} plus
     * {@link TemplatedString}. This is the method invoked by the invokeDynamic generated
     * for templated string byte code.
     *
     * @param lookup  method lookup (not used)
     * @param name    method name (not used)
     * @param type    method type
     *
     * @return {@link CallSite} for {@link TemplatePolicy} application.
     *
     * @throws java.lang.NullPointerException if any of the arguments are null
     */
    public static CallSite templatedStringBSM(
            Lookup lookup,
            String name,
            MethodType type) {
        Objects.requireNonNull(lookup);
        Objects.requireNonNull(name);
        Objects.requireNonNull(type);

        return TemplateSupport.Bootstrap.templatedStringBSM(type);
    }

    /**
     * Constructs a new {@link TemplatedString.Builder}.
     *
     * @return a new {@link TemplatedString.Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * This class can be used to construct a new TemplatedString from string
     * segments, values and other {@link TemplatedString TemplatedStrings}.
     * <p>
     * To use, construct a new {@link Builder} using {@link TemplatedString#builder},
     * then chain invokes of {@link Builder#segment(String)} or {@link Builder#value(Object)}
     * to build up the template and values. {@link Builder#templatedString(TemplatedString)} can be
     * used to add the template and values from another {@link TemplatedString}.
     * {@link Builder#build()} can be invoked at the end of the chain to produce a new
     * TemplatedString using the current state of the builder.
     * <p>
     *
     * Example:
     *
     * {@snippet :
     *      int x = 10;
     *      int y = 20;
     *      TemplatedString ts = TemplatedString.builder()
     *          .segment("The result of adding ")
     *          .value(x)
     *          .templatedString(" and \{y} equals \{x + y}")
     *          .build();
     *      String result = CONCAT.apply(ts);
     *  }
     *
     *  Result: "The result of adding 10 and 20 equals 30"
     * <p>
     * The {@link Builder} itself implements {@link TemplatedString}. When
     * applied to a policy will use the current state of template and values to
     * produce a result.
     */
    public static class Builder implements TemplatedString {
        /**
         * {@link StringBuilder} used to construct the final template.
         */
        private final StringBuilder templateBuilder;

        /**
         * {@link ArrayList} used to gather values.
         */
        private final List<Object> values;

        /**
         * protected private Constructor.
         */
        Builder() {
            this.templateBuilder = new StringBuilder();
            this.values = new ArrayList<>();
        }

        @Override
        public String template() {
            return templateBuilder.toString();
        }

        @Override
        public List<Object> values() {
            return Collections.unmodifiableList(values);
        }

        /**
         * Add the supplied {@link TemplatedString TemplatedString's} template and
         * values to the builder.
         *
         * @param templatedString existing TemplatedString
         *
         * @return this Builder
         */
        public Builder templatedString(TemplatedString templatedString) {
            templateBuilder.append(templatedString.template());
            values.addAll(templatedString.values());

            return this;
        }

        /**
         * Add a string segment to the {@link Builder Builder's} template.
         *
         * @param string  string segment to be added
         *
         * @return this Builder
         *
         * @throws IllegalArgumentException if segment contains OBJECT REPLACEMENT CHARACTER
         */
        public Builder segment(String string) {
            if (string.indexOf(OBJECT_REPLACEMENT_CHARACTER) != -1) {
                throw new IllegalArgumentException("string contains an OBJECT REPLACEMENT CHARACTER");
            }

            templateBuilder.append(string);

            return this;
        }

        /**
         * Add a value to the {@link Builder}. This method will also insert a placeholder
         * in the {@link Builder Builder's} template.
         *
         * @param value value to be added
         *
         * @return this Builder
         */
        public Builder value(Object value) {
            templateBuilder.append(OBJECT_REPLACEMENT_CHARACTER);
            values.add(value);

            return this;
        }

        /**
         * Returns a {@link TemplatedString} based on the current state of the
         * {@link Builder Builder's} template and values.
         *
         * @return a new TemplatedString
         */
        public TemplatedString build() {
            final String template = this.template();
            final List<Object> values = Collections.unmodifiableList(this.values());
            final List<String> segments = TemplatedString.split(template);

            return new TemplatedString() {
                @Override
                public String template() {
                    return template;
                }

                @Override
                public List<Object> values() {
                    return values;
                }

                @Override
                public List<String> segments() {
                    return segments;
                }
            };
        }
    }
}
