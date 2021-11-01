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
public final class FormatterPolicy implements TemplatePolicy<String, RuntimeException> {

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
        MethodHandle localeMH = null;
        MethodHandle applyMH = null;

        try {
            Lookup lookup = MethodHandles.lookup();
            localeMH = lookup.findVirtual(FormatterPolicy.class, "locale",
                    MethodType.methodType(Locale.class));
            applyMH = lookup.findVirtual(TemplatePolicy.class, "apply",
                    MethodType.methodType(Object.class, TemplatedString.class));
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }

        LOCALE_MH = localeMH;
        APPLY_MH = applyMH;
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
        String format = Formatter.templatedStringFormat(templatedString);
        Object[] values = templatedString.values().toArray(new Object[0]);

        return format.formatted(values);
    }

    @Override
    public MethodHandle applyMethodHandle(TemplatedString templatedString) {
        Objects.requireNonNull(templatedString);

        if (!templatedString.getClass().isSynthetic()) {
            return APPLY_MH;
        }

        List<MethodHandle> vars;

        try {
            vars = templatedString.vars();
        } catch (ReflectiveOperationException | UnsupportedOperationException ex) {
            return APPLY_MH;
        }

        String format = Formatter.templatedStringFormat(templatedString);
        List<Class<?>> types = new ArrayList<>();
        types.add(Locale.class);
        int count = vars.size();
        vars.stream().forEach(mh -> types.add(mh.type().returnType()));
        MethodHandle mh = Formatter.formatFactory(format, types.toArray(new Class<?>[0]));
        MethodHandle[] filters = new MethodHandle[count + 1];
        filters[0] = LOCALE_MH;

        int i = 1;
        for (MethodHandle var : vars) {
            MethodType varMethodType = var.type();
            varMethodType = varMethodType.changeParameterType(0, TemplatedString.class);
            filters[i++] = var.asType(varMethodType);
        }

        mh = MethodHandles.filterArguments(mh, 0, filters);
        int[] permute = new int[count + 1];
        Arrays.fill(permute, 1, permute.length, 1);
        MethodType methodType = MethodType.methodType(String.class, FormatterPolicy.class, TemplatedString.class);
        mh = MethodHandles.permuteArguments(mh, methodType, permute);
        mh = mh.asType(MethodType.methodType(Object.class, TemplatePolicy.class, TemplatedString.class));

        return mh;
    }
}
