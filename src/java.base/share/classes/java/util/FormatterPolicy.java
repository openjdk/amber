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

package java.util;

import java.lang.invoke.*;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.TemplatePolicy.Linkage;

/**
 * This {@link TemplatePolicy} constructs a String result using {@link Formatter}.
 * Unlike {@link Formatter}, FormatterPolicy locates values in the expressions that
 * come immediately after the format specifier. TemplatedString expressions
 * without a preceeding specifier, use "%s" by default.
 * <p>
 * When used in conjuction with a compiler generated {@link TemplatedString} this
 * {@link TemplatePolicy} will use the format specifiers in the template and types of the
 * values to produce a more performant formatter.
 */
public final class FormatterPolicy implements Linkage<String, RuntimeException> {

    /**
     * Predefined FormatterPolicy instance that uses default locale.
     */
    public static final FormatterPolicy FORMAT = new FormatterPolicy();

    /**
     * MethodHandle to FormatterPolicy locale.
     */
    private static final MethodHandle LOCALE_MH;

    /**
     * MethodHandle to TemplatePolicy apply.
     */
    private static final MethodHandle APPLY_MH;

    /**
     * Initialize MethodHandle constants
     */
    static {
        try {
            Lookup lookup = MethodHandles.lookup();
            LOCALE_MH = lookup.findVirtual(FormatterPolicy.class, "locale",
                    MethodType.methodType(Locale.class));
            APPLY_MH = lookup.findVirtual(TemplatePolicy.class, "apply",
                    MethodType.methodType(Object.class, TemplatedString.class));
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Locale used by this FormatterPolicy.
     */
    private final Locale locale;

    /**
     * Constructor.
     */
    public FormatterPolicy() {
        this.locale = null;
    }

    /**
     * Constructor.
     *
     * @param locale   formatting locale
     */
    public FormatterPolicy(Locale locale) {
        this.locale = locale;
    }

    /**
     * Returns the {@link FormatterPolicy} instance locale.
     *
     * @return the {@link FormatterPolicy} instance locale
     */
    public Locale locale() {
        return locale;
    }

    @Override
    public final String apply(TemplatedString templatedString) {
        Objects.requireNonNull(templatedString);
        String format = Formatter.templatedStringFormat(templatedString.template());
        Object[] values = templatedString.values().toArray(new Object[0]);

        return format.formatted(values);
    }

    @Override
    public MethodHandle applier(MethodHandles.Lookup lookup,
                                MethodType type, String template) {
        Objects.requireNonNull(lookup);
        Objects.requireNonNull(type);
        Objects.requireNonNull(template);
        String format = Formatter.templatedStringFormat(template);
        MethodType formatterType = type.changeParameterType(0, Locale.class);
        MethodHandle mh = Formatter.formatFactory(format, formatterType.parameterArray());
        mh = MethodHandles.filterArguments(mh, 0, LOCALE_MH);

        return mh;
    }
}
