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

package javax.lang.model.element;

/**
 * Represents a record component.
 *
 * @since 14
 * @deprecated This interface is part of a preview feature and may be removed
 * if the preview feature is removed.
 */
@Deprecated(forRemoval=true, since="14")
@SuppressWarnings("removal")
public interface RecordComponentElement extends Element {
    /**
     * Returns the enclosing element of this record component.
     *
     * The enclosing element of a record component is the type
     * declaring the record component.
     *
     * @return the enclosing element of this record component
     */
    @Override
    Element getEnclosingElement();

    /**
     * Returns the simple name of this record component.
     *
     * <p>The name of each record component must be distinct from the
     * names of all other record components.
     *
     * @return the simple name of this record component
     *
     * @jls 6.2 Names and Identifiers
     */
    @Override
    Name getSimpleName();

    /**
     * Returns the executable element for the accessor associated with the
     * given record component.
     *
     * @return the record component accessor.
     */
    ExecutableElement getAccessor();
}
