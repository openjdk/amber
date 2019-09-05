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
package java.lang;

/**
 * This is the common base class of all Java language record classes.
 *
 * <p>More information about records, including descriptions of the
 * implicitly declared methods synthesized by the compiler, can be
 * found in section 8.10 of
 * <cite>The Java&trade; Language Specification</cite>.
 *
 * <p>A <em>record class</em> is a shallowly immutable, transparent carrier for
 * a fixed set of values, called the <em>record components</em>.  The Java&trade;
 * language provides concise syntax for declaring record classes, whereby the
 * record components are declared in the record header.  The list of record
 * components declared in the record header form the <em>record descriptor</em>.
 *
 * <p>A record class has the following mandated members: a public <em>canonical
 * constructor</em>, whose descriptor is the same as the record descriptor;
 * a private final field corresponding to each component, whose name and
 * type are the same as that of the component; a public accessor method
 * corresponding to each component, whose name and return type are the same as
 * that of the component.  If not explicitly declared in the body of the record,
 * implicit implementations for these members are provided.
 *
 * <p>The implicit declaration of the canonical constructor initializes the
 * component fields from the corresponding constructor arguments.  The implicit
 * declaration of the accessor methods returns the value of the corresponding
 * component field.  The implicit declaration of the {@link Object#equals(Object)},
 * {@link Object#hashCode()}, and {@link Object#toString()} methods are derived
 * from all of the component fields.
 *
 * <p>The primary reasons to provide an explicit declaration for the
 * canonical constructor or accessor methods are to validate constructor
 * arguments, perform defensive copies on mutable components, or normalize groups
 * of components (such as reducing a rational number to lowest terms.)  If any
 * of these are provided explicitly.
 *
 * <p>For all record classes, the following invariant must hold: if a record R's
 * components are {@code c1, c2, ... cn}, then if a record instance is copied
 * as follows:
 * <pre>
 *     R copy = new R(r.c1(), r.c2(), ..., r.cn());
 * </pre>
 * then it must be the case that {@code r.equals(copy)}.
 *
 * @jls 8.10
 * @since 14
 */
public abstract class Record {
    /**
     * Indicates whether some other object is "equal to" this one.  In addition
     * to the general contract of {@link Object#equals(Object)},
     * record classes must further participate in the invariant that when
     * a record instance is "copied" by passing the result of the record component
     * accessor methods to the canonical constructor, the resulting copy is
     * equal to the original instance.
     *
     * @implNote
     * The implicitly provided implementation returns {@code true} if and
     * only if the argument is an instance of the same record type as this object,
     * and each component of this record is equal to the corresponding component
     * of the argument, according to {@link Object#equals(Object)} for components
     * whose types are reference types, and the primitive wrapper equality
     * comparison for components whose types are primitive types.
     *
     * @see Object#equals(Object)
     *
     * @param   obj   the reference object with which to compare.
     * @return  {@code true} if this object is the same as the obj
     *          argument; {@code false} otherwise.
     */
    @Override
    public abstract boolean equals(Object obj);

    /**
     * {@inheritDoc}
     *
     * @implNote
     * The implicitly provided implementation returns a hash code value derived
     * by combining the hash code value for all the components, according to
     * {@link Object#hashCode()} for components whose types are reference types,
     * or the primitive wrapper hash code for components whose types are primitive
     * types.
     *
     * @see     Object#hashCode()
     *
     * @return  a hash code value for this object.
     */
    @Override
    public abstract int hashCode();

    /**
     * {@inheritDoc}
     *
     * @implNote
     * The implicitly provided implementation returns a string that is derived
     * from the name of the record class and the names and string representations
     * of all the components, according to {@link Object#toString()} for components
     * whose types are reference types, and the primitive wrapper {@code toString}
     * method for components whose types are primitive types.
     *
     * @see     Object#toString() ()
     *
     * @return  a string representation of the object.
     */
    @Override
    public abstract String toString();
}
