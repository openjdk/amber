/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package java.lang.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.List;

/**
 * Runtime object for low-level implementation of <em>pattern matching</em>. A
 * {@linkplain PatternHandle} exposes functionality for determining if a target
 * matches the pattern, and if so, for conditionally extracting the resulting
 * bindings.
 *
 * <p>A {@linkplain PatternHandle} is parameterized by a <em>target type</em>
 * and zero or more <em>binding variable types</em>. The target type (denoted
 * {@code T}) is the type against which the pattern can be applied (often a
 * broad type such as {@link Object}, but need not be), and the binding variable
 * types (denoted {@code B*}) are the types of the binding variables that are
 * produced by a successful match.  These types are combined into a type
 * <em>descriptor</em>, accessed via {@link #descriptor()}, where the return
 * type of the descriptor is the target type, and the parameter types of the
 * descriptor are the binding variable types.
 *
 * <p>The behavior of a {@linkplain PatternHandle} is exposed via method
 * handles.  The method handle returned by {@link #tryMatch()} is applied to the
 * target to be tested, and returns an opaque result of type {@code Object}.  If
 * the result is {@code null}, the match has failed; if is non-null, it has
 * succeeded, and the result can be used as input to the method handles returned
 * by {@link #components()} or {@link #component(int)} to retrieve specific
 * binding variables.
 *
 * <p>The class {@link PatternHandles} contains numerous factories and
 * combinators for {@linkplain PatternHandle}s, including {@link
 * PatternHandles#adaptTarget(PatternHandle, Class)} which can be used to adapt
 * a pattern handle from one target type to another (such as widening the set of
 * types against which it can be applied.)
 *
 * <p>{@linkplain PatternHandle} implementations must be <a
 * href="{@docRoot}/java.base/java/lang/doc-files/ValueBased.html">value-based</a>
 * classes.
 */
public interface PatternHandle {

    /**
     * Returns a method handle that attempts to perform the pattern match
     * described by this pattern handle.  It will have type {@code (T)Object},
     * where {@code T} is the target type of the extractor. It accepts the
     * target to be matched, and returns a non-null opaque carrier of type
     * {@link Object} if the match succeeds, or {@code null} if it fails.
     *
     * @return the {@code tryMatch} method handle
     */
    MethodHandle tryMatch();

    /**
     * Returns a method handle that extracts a component from a successful
     * match.  It will have type {@code (Object)Bi}, where {@code Bi} is the
     * type of the corresponding binding variable, and will take the match
     * carrier and return the corresponding binding variable.
     *
     * @param i the index of the component
     * @return the component method handle
     * @throws IndexOutOfBoundsException if {@code i} does not correspond to the
     *                                   index of a binding variable of this
     *                                   pattern
     */
    MethodHandle component(int i);

    /**
     * Returns all the component method handles for this pattern as a {@link
     * List}.
     *
     * @return the component method handles
     */
    List<MethodHandle> components();

    /**
     * Returns the descriptor for this pattern.  The parameter types of the
     * descriptor are the types of the binding variables, and the return type is
     * the target type.
     *
     * @return the pattern type descriptor
     */
    MethodType descriptor();
}
