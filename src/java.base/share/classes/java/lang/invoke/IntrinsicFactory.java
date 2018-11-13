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
     * @param lookup      MethodHandles lookup
     * @param name        Name of method
     * @param methodType  Method signature
     * @param format      Formatter format string
     * @throws NoSuchMethodException no such method
     * @throws IllegalAccessException illegal access
     * @throws StringConcatException string concat error
     * @return Callsite for intrinsic method
     */
    public static CallSite formatterFormatBootstrap(MethodHandles.Lookup lookup,
                                                    String name,
                                                    MethodType methodType,
                                                    String format)
            throws NoSuchMethodException, IllegalAccessException, StringConcatException {
        return FormatterBootstraps.formatterBootstrap(lookup, name, methodType, format, false, false);
    }

    /**
     * formatterLocaleFormatBootstrap bootstrap.
     * @param lookup      MethodHandles lookup
     * @param name        Name of method
     * @param methodType  Method signature
     * @param format      Formatter format string
     * @throws NoSuchMethodException no such method
     * @throws IllegalAccessException illegal access
     * @throws StringConcatException string concat error
     * @return Callsite for intrinsic method
     */
    public static CallSite formatterLocaleFormatBootstrap(MethodHandles.Lookup lookup,
                                                          String name,
                                                          MethodType methodType,
                                                          String format)
            throws NoSuchMethodException, IllegalAccessException, StringConcatException {
        return FormatterBootstraps.formatterBootstrap(lookup, name, methodType, format, false, true);
    }

    /**
     * printStreamFormatBootstrap bootstrap.
     * @param lookup      MethodHandles lookup
     * @param name        Name of method
     * @param methodType  Method signature
     * @param format      Formatter format string
     * @throws NoSuchMethodException no such method
     * @throws IllegalAccessException illegal access
     * @throws StringConcatException string concat error
     * @return Callsite for intrinsic method
     */
    public static CallSite printStreamFormatBootstrap(MethodHandles.Lookup lookup,
                                                      String name,
                                                      MethodType methodType,
                                                      String format)
            throws NoSuchMethodException, IllegalAccessException, StringConcatException {
        return FormatterBootstraps.formatterBootstrap(lookup, name, methodType, format, false, false);
    }

    /**
     * printStreamLocaleFormatBootstrap bootstrap.
     * @param lookup      MethodHandles lookup
     * @param name        Name of method
     * @param methodType  Method signature
     * @param format      Formatter format string
     * @throws NoSuchMethodException no such method
     * @throws IllegalAccessException illegal access
     * @throws StringConcatException string concat error
     * @return Callsite for intrinsic method
     */
    public static CallSite printStreamLocaleFormatBootstrap(MethodHandles.Lookup lookup,
                                                            String name,
                                                            MethodType methodType,
                                                            String format)
            throws NoSuchMethodException, IllegalAccessException, StringConcatException {
        return FormatterBootstraps.formatterBootstrap(lookup, name, methodType, format, false, true);
    }

    /**
     * printStreamPrintfBootstrap bootstrap.
     * @param lookup      MethodHandles lookup
     * @param name        Name of method
     * @param methodType  Method signature
     * @param format      Formatter format string
     * @throws NoSuchMethodException no such method
     * @throws IllegalAccessException illegal access
     * @throws StringConcatException string concat error
     * @return Callsite for intrinsic method
     */
    public static CallSite printStreamPrintfBootstrap(MethodHandles.Lookup lookup,
                                                      String name,
                                                      MethodType methodType,
                                                      String format)
            throws NoSuchMethodException, IllegalAccessException, StringConcatException {
        return FormatterBootstraps.formatterBootstrap(lookup, name, methodType, format, false, false);
    }

    /**
     * printStreamLocalePrintfBootstrap bootstrap.
     * @param lookup      MethodHandles lookup
     * @param name        Name of method
     * @param methodType  Method signature
     * @param format      Formatter format string
     * @throws NoSuchMethodException no such method
     * @throws IllegalAccessException illegal access
     * @throws StringConcatException string concat error
     * @return Callsite for intrinsic method
     */
    public static CallSite printStreamLocalePrintfBootstrap(MethodHandles.Lookup lookup,
                                                            String name,
                                                            MethodType methodType,
                                                            String format)
            throws NoSuchMethodException, IllegalAccessException, StringConcatException {
        return FormatterBootstraps.formatterBootstrap(lookup, name, methodType, format, false, true);
    }

    /**
     * printWriterFormatBootstrap bootstrap.
     * @param lookup      MethodHandles lookup
     * @param name        Name of method
     * @param methodType  Method signature
     * @param format      Formatter format string
     * @throws NoSuchMethodException no such method
     * @throws IllegalAccessException illegal access
     * @throws StringConcatException string concat error
     * @return Callsite for intrinsic method
     */
    public static CallSite printWriterFormatBootstrap(MethodHandles.Lookup lookup,
                                                      String name,
                                                      MethodType methodType,
                                                      String format)
            throws NoSuchMethodException, IllegalAccessException, StringConcatException {
        return FormatterBootstraps.formatterBootstrap(lookup, name, methodType, format, false, false);
    }

    /**
     * printWriterLocaleFormatBootstrap bootstrap.
     * @param lookup      MethodHandles lookup
     * @param name        Name of method
     * @param methodType  Method signature
     * @param format      Formatter format string
     * @throws NoSuchMethodException no such method
     * @throws IllegalAccessException illegal access
     * @throws StringConcatException string concat error
     * @return Callsite for intrinsic method
     */
    public static CallSite printWriterLocaleFormatBootstrap(MethodHandles.Lookup lookup,
                                                            String name,
                                                            MethodType methodType,
                                                            String format)
            throws NoSuchMethodException, IllegalAccessException, StringConcatException {
        return FormatterBootstraps.formatterBootstrap(lookup, name, methodType, format, false, true);
    }

    /**
     * printWriterPrintfBootstrap bootstrap.
     * @param lookup      MethodHandles lookup
     * @param name        Name of method
     * @param methodType  Method signature
     * @param format      Formatter format string
     * @throws NoSuchMethodException no such method
     * @throws IllegalAccessException illegal access
     * @throws StringConcatException string concat error
     * @return Callsite for intrinsic method
     */
    public static CallSite printWriterPrintfBootstrap(MethodHandles.Lookup lookup,
                                                      String name,
                                                      MethodType methodType,
                                                      String format)
            throws NoSuchMethodException, IllegalAccessException, StringConcatException {
        return FormatterBootstraps.formatterBootstrap(lookup, name, methodType, format, false, false);
    }

    /**
     * printWriterLocalePrintfBootstrap bootstrap.
     * @param lookup      MethodHandles lookup
     * @param name        Name of method
     * @param methodType  Method signature
     * @param format      Formatter format string
     * @throws NoSuchMethodException no such method
     * @throws IllegalAccessException illegal access
     * @throws StringConcatException string concat error
     * @return Callsite for intrinsic method
     */
    public static CallSite printWriterLocalePrintfBootstrap(MethodHandles.Lookup lookup,
                                                            String name,
                                                            MethodType methodType,
                                                            String format)
            throws NoSuchMethodException, IllegalAccessException, StringConcatException {
        return FormatterBootstraps.formatterBootstrap(lookup, name, methodType, format, false, true);
    }

    /**
     * staticStringFormatBootstrap bootstrap.
     * @param lookup      MethodHandles lookup
     * @param name        Name of method
     * @param methodType  Method signature
     * @param format      Formatter format string
     * @throws NoSuchMethodException no such method
     * @throws IllegalAccessException illegal access
     * @throws StringConcatException string concat error
     * @return Callsite for intrinsic method
     */
    public static CallSite staticStringFormatBootstrap(MethodHandles.Lookup lookup,
                                                       String name,
                                                       MethodType methodType,
                                                       String format)
            throws NoSuchMethodException, IllegalAccessException, StringConcatException {
        return FormatterBootstraps.formatterBootstrap(lookup, name, methodType, format, true, false);
    }

    /**
     * staticStringLocaleFormatBootstrap bootstrap.
     * @param lookup      MethodHandles lookup
     * @param name        Name of method
     * @param methodType  Method signature
     * @param format      Formatter format string
     * @throws NoSuchMethodException no such method
     * @throws IllegalAccessException illegal access
     * @throws StringConcatException string concat error
     * @return Callsite for intrinsic method
     */
    public static CallSite staticStringLocaleFormatBootstrap(MethodHandles.Lookup lookup,
                                                             String name,
                                                             MethodType methodType,
                                                             String format)
            throws NoSuchMethodException, IllegalAccessException, StringConcatException {
        return FormatterBootstraps.formatterBootstrap(lookup, name, methodType, format, true, true);
    }

    /**
     * stringFormatBootstrap bootstrap.
     * @param lookup      MethodHandles lookup
     * @param name        Name of method
     * @param methodType  Method signature
     * @param format      Formatter format string
     * @throws NoSuchMethodException no such method
     * @throws IllegalAccessException illegal access
     * @throws StringConcatException string concat error
     * @return Callsite for intrinsic method
     */
    public static CallSite stringFormatBootstrap(MethodHandles.Lookup lookup,
                                                 String name,
                                                 MethodType methodType,
                                                 String format)
            throws NoSuchMethodException, IllegalAccessException, StringConcatException {
        return FormatterBootstraps.formatterBootstrap(lookup, name, methodType, format, true, false);
    }

    /**
     * stringLocaleFormatBootstrap bootstrap.
     * @param lookup      MethodHandles lookup
     * @param name        Name of method
     * @param methodType  Method signature
     * @param format      Formatter format string
     * @throws NoSuchMethodException no such method
     * @throws IllegalAccessException illegal access
     * @throws StringConcatException string concat error
     * @return Callsite for intrinsic method
     */
    public static CallSite stringLocaleFormatBootstrap(MethodHandles.Lookup lookup,
                                                       String name,
                                                       MethodType methodType,
                                                       String format)
            throws NoSuchMethodException, IllegalAccessException, StringConcatException {
        return FormatterBootstraps.formatterBootstrap(lookup, name, methodType, format, true, true);
    }

    /**
     * objectsHashBootstrap bootstrap.
     * @param lookup      MethodHandles lookup
     * @param name        Name of method
     * @param methodType  Method signature
     * @throws NoSuchMethodException no such method
     * @throws IllegalAccessException illegal access
     * @throws StringConcatException string concat error
     * @return Callsite for intrinsic method
     */
    public static CallSite objectsHashBootstrap(MethodHandles.Lookup lookup,
                                                String name,
                                                MethodType methodType)
            throws NoSuchMethodException, IllegalAccessException {
        return ObjectsBootstraps.hashBootstrap(lookup, name, methodType);
    }

    /**
     * stringMatchesBootstrap bootstrap.
     * @param lookup      MethodHandles lookup
     * @param name        Name of method
     * @param methodType  Method signature
     * @param pattern     Pattern string
     * @throws NoSuchMethodException no such method
     * @throws IllegalAccessException illegal access
     * @throws StringConcatException string concat error
     * @return Callsite for intrinsic method
     */
    public static CallSite stringMatchesBootstrap(MethodHandles.Lookup lookup,
                                                  String name,
                                                  MethodType methodType,
                                                  String pattern)
            throws NoSuchMethodException, IllegalAccessException {
        return StringBootstraps.patternMatchBootstrap(lookup, name, methodType, pattern);
    }

}
