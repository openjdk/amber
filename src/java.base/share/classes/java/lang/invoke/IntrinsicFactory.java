/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.invoke;

import java.io.IOException;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.MissingFormatArgumentException;
import java.util.stream.IntStream;

/**
 * Bootstrapping support for Intrinsics.
 */
public final class IntrinsicFactory {
    /**
     * formatterFormatBootstrap bootstrap.
     * @param lookup               MethodHandles lookup
     * @param name                 Name of method
     * @param methodType           Method signature
     * @param format               Formatter format string
     * @param isStringMethod       Method's owner is String or not
     * @param hasLocale            has Locale
     * @throws NoSuchMethodException no such method
     * @throws IllegalAccessException illegal access
     * @throws StringConcatException string concat error
     * @return Callsite for intrinsic method
     */
    public static CallSite formatterBootstrap(MethodHandles.Lookup lookup,
                                       String name,
                                       MethodType methodType,
                                       String format,
                                       int isStringMethod,
                                       int hasLocale)
            throws NoSuchMethodException, IllegalAccessException, StringConcatException {
        return FormatterBootstraps.formatterBootstrap(lookup, name, methodType, format,
                isStringMethod == 1, hasLocale == 1);
    }
}
