/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package java.lang.compiler;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Temporary scaffolding to allow declaration of constrained patterns
 * without language support.
 */
public interface _pattern<B> {
    /**
     * Attempt to match
     * @param o the target
     * @return the result, or an empty optional
     */
    Optional<B> match(Object o);

    /**
     * Construct a PatternDecl for a partial pattern
     * @param predicate the applicability test
     * @param extract the extraction logic
     * @param <T> the type of a successful target
     * @param <B> the type of the binding
     * @return the PatternDecl
     */
    @SuppressWarnings("unchecked")
    static<T, B> _pattern<B> of(Predicate<T> predicate, Function<T, B> extract) {
        return (Object o) ->
                (predicate.test((T) o))
                ? Optional.of(extract.apply((T) o))
                : Optional.empty();
    }

    /**
     * Construct a PatternDecl for a total pattern on Object
     * @param extract the extraction logic
     * @param <B> the type of the binding
     * @return the PatternDecl
     */
    static<B> _pattern<B> of(Function<?, B> extract) {
        return of(o -> true, extract);
    }

    /**
     * Construct a PatternDecl for a type test pattern
     * @param clazz The type to test against
     * @param extract the extraction logic
     * @param <T> the type of a successful target
     * @param <B> the type of the binding
     * @return the PatternDecl
     */
    static<T, B> _pattern<B> ofType(Class<T> clazz, Function<T, B> extract) {
        return of(o -> clazz.isAssignableFrom(o.getClass()),
                  o -> extract.apply(clazz.cast(o)));
    }

    /**
     * Construct a PatternDecl for a type test pattern
     * @param clazz The type to test against
     * @param <T> the type of a successful target
     * @return the PatternDecl
     */
    static<T> _pattern<T> ofType(Class<T> clazz) {
        return of(o -> clazz.isAssignableFrom(o.getClass()), clazz::cast);
    }

    /**
     * Construct a PatternDecl for a constant
     * @param constant the constant
     * @param <T> the type of the constant
     * @return the PatternDecl
     */
    static<T> _pattern<T> ofConstant(T constant) {
        return of(constant::equals, o -> constant);
    }
}
