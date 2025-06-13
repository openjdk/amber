/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.access;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

/** An interface which gives privileged packages Java-level access to
    internals of java.lang.runtime. Use as a last resort! */
public interface JavaLangRuntimeAccess {
    /**
     * {@return the combination {@link MethodHandle} of the constructor and initializer
     * for the carrier representing {@code methodType}. The carrier constructor/initializer
     * will always take the component values and a return type of {@link Object} }
     *
     * @param methodType  {@link MethodType} whose parameter types supply the shape of the
     *                    carrier's components
     */
    public MethodHandle initializingConstructor(MethodType methodType);

    /**
     * {@return a {@link MethodHandle MethodHandle} which accepts a carrier object
     * matching the given {@code methodType} which when invoked will return a newly
     * created object array containing the boxed component values of the carrier object.}
     *
     * @param methodType  {@link MethodType} whose parameter types supply the shape of the
     *                    carrier's components
     */
    public MethodHandle boxedComponentValueArray(MethodType methodType);
}
