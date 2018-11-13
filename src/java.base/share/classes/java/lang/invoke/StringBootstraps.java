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

import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bootstrapping support for String intrinsics.
 */
public final class StringBootstraps {
    /**
     * Bootstrap for String intrinsics.
     * @param lookup         MethodHandles lookup
     * @param name           Name of method
     * @param methodType     Method signature
     * @param pattern        Pattern string
     * @return Callsite for intrinsic method
     * @throws IllegalAccessException illegal access
     * @throws NoSuchMethodException no such method
     */
    public static CallSite patternMatchBootstrap(MethodHandles.Lookup lookup,
                                                 String name,
                                                 MethodType methodType,
                                                 String pattern)
        throws NoSuchMethodException, IllegalAccessException {
        assert methodType.returnType() == boolean.class;

        MethodType matcherMethodType = MethodType.methodType(Matcher.class, CharSequence.class);
        MethodType matcherMethodTypeCast = MethodType.methodType(Matcher.class, Pattern.class, String.class);
        MethodType matchesMethodType = MethodType.methodType(boolean.class);

        MethodHandle matcher = lookup.findVirtual(Pattern.class, "matcher", matcherMethodType).
                asType(matcherMethodTypeCast);
        MethodHandle matches = lookup.findVirtual(Matcher.class, "matches", matchesMethodType);

        return new ConstantCallSite(
                MethodHandles.filterArguments(matches, 0, matcher.bindTo(Pattern.compile(pattern))));
    }
}
