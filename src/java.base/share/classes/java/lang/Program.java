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

package java.lang;

import java.lang.compiler.IntrinsicCandidate;

/**
 * Provide access to compile time information.
 *
 * @since 12
 */
public class Program {
    /**
     * Returns the enclosing class.
     *
     * @return enclosing class
     */
    @IntrinsicCandidate
    public static Class<?> getThisClass() {
        throw new UnsupportedOperationException("getThisClass intrinsic not available");
    }

    /**
     * Returns the outermost enclosing class.
     *
     * @return enclosing class
     */
    @IntrinsicCandidate
    public static Class<?> getOuterMostClass() {
        throw new UnsupportedOperationException("getOuterMostClass intrinsic not available");
    }

    /**
     * Returns the current method name.
     *
     * @return current method name
     */
    @IntrinsicCandidate
    public static String getMethodName() {
        throw new UnsupportedOperationException("getMethodName intrinsic not available");
    }

    /**
     * Returns the source file name.
     *
     * @return source file name
     */
    @IntrinsicCandidate
    public static String getSourceName() {
        throw new UnsupportedOperationException("getSourceName intrinsic not available");
    }

    /**
     * Returns the current line number.
     *
     * @return current line number
     */
    @IntrinsicCandidate
    public static int getLineNumber() {
        throw new UnsupportedOperationException("getLineNumber intrinsic not available");
    }
}
