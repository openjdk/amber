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

import java.lang.template.StringProcessor;
import java.lang.template.TemplatedString;
import java.util.*;

/**
 * This class provides automatic access to commonly used static methods and
 * constant static fields.
 *
 * @since  20
 */
public final class StaticImports {
    /**
     * Private constructor.
     */
    private StaticImports() {
        throw new AssertionError("private constructor");
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

}
