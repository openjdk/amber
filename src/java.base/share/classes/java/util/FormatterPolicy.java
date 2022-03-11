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

package java.util;

import java.lang.invoke.*;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.TemplatePolicy.Linkage;

import jdk.internal.javac.PreviewFeature;

/**
 * This {@link TemplatePolicy} constructs a String result using {@link
 * Formatter}. Unlike {@link Formatter}, FormatterPolicy locates values in
 * the expressions that come immediately after the format specifier.
 * TemplatedString expressions without a preceeding specifier, use "%s" by
 * default. Example:
 * {@snippet :
 * import static java.util.FormatterPolicy.FMTR;
 * ...
 * int x = 10;
 * int y = 20;
 * String result = FMTR."%5d\{x} + %5d\{y} = %5d\{x + y}"
 * }
 * result is: <code>&nbsp;&nbsp;&nbsp;10&nbsp;+&nbsp;&nbsp;&nbsp;20&nbsp;=&nbsp;&nbsp;&nbsp;&nbsp;30</code>
 * <p>
 * When used in conjunction with a compiler generated {@link
 * TemplatedString} this {@link TemplatePolicy} will use the format
 * specifiers in the template and types of the values to produce a more
 * performant formatter.
 *
 * @since 19
 */
@PreviewFeature(feature=PreviewFeature.Feature.TEMPLATED_STRINGS)
public final class FormatterPolicy implements Linkage<String, RuntimeException> {

    /**
     * Predefined FormatterPolicy instance that uses default locale.
     */
    public static final FormatterPolicy FMTR = new FormatterPolicy();

    /**
     * Locale used by this FormatterPolicy.
     */
    private final Locale locale;

    /**
     * Constructor.
     */
    public FormatterPolicy() {
        this.locale = Locale.US;
    }

    /**
     * Constructor.
     *
     * @param locale   formatting locale
     *
     * @throws NullPointerException if locale is null
     */
    public FormatterPolicy(Locale locale) {
        Objects.requireNonNull(locale, "must be a non-null locale");

        this.locale = locale;
    }

    /**
     * {@return the FormatterPolicy instance locale}
     */
    public Locale locale() {
        return locale;
    }

    @Override
    public final String apply(TemplatedString templatedString) {
        Objects.requireNonNull(templatedString);
        String format = Formatter.templatedStringFormat(templatedString.template());
        Object[] values = templatedString.values().toArray(new Object[0]);

        return new Formatter(locale).format(format, values).toString();
    }

    @Override
    public MethodHandle applier(MethodHandles.Lookup lookup,
                                MethodType type, String template) {
        Objects.requireNonNull(lookup);
        Objects.requireNonNull(type);
        Objects.requireNonNull(template);
        String format = Formatter.templatedStringFormat(template);
        MethodHandle mh = Formatter.formatFactory(format, locale,
                type.dropParameterTypes(0,1).parameterArray());
        mh = MethodHandles.dropArguments(mh, 0, type.parameterType(0));

        return mh;
    }
}
