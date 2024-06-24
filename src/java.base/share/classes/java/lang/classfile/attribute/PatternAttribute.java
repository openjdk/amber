/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.AttributedElement;
import java.lang.classfile.MethodElement;
import java.lang.classfile.constantpool.Utf8Entry;
import jdk.internal.classfile.impl.BoundAttribute;
import jdk.internal.classfile.impl.TemporaryConstantPool;
import jdk.internal.classfile.impl.UnboundAttribute;

import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;
import java.util.List;
import java.util.Set;

/**
 * Models the {@code pattern} attribute {@jvms X.X.XX}, which can
 * appear on patterns, and records additional information about the
 * nature of this pattern represented as a method.
 *
 * TODO
 * Delivered as a {@link MethodElement} when
 * traversing the elements of a {@link java.lang.classfile.MethodModel}.
 */
public sealed interface PatternAttribute
        extends Attribute<PatternAttribute>, MethodElement, AttributedElement
        permits BoundAttribute.BoundPatternAttribute,
                UnboundAttribute.UnboundPatternAttribute {

    /**
     * {@return the the module flags of the module, as a bit mask}
     */
    int patternFlagsMask();

    /**
     * {@return the flags of the module, as a set of enum constants}
     */
    default Set<AccessFlag> patternFlags() {
        return AccessFlag.maskToAccessFlags(patternFlagsMask(), AccessFlag.Location.PATTERN);
    }

    /** {@return the name of this method} */
    Utf8Entry patternName();

    /** {@return the method descriptor of this method} */
    Utf8Entry patternMethodType();

    /** {@return the method descriptor of this method, as a symbolic descriptor} */
    default MethodTypeDesc patternTypeSymbol() {
        return MethodTypeDesc.ofDescriptor(patternMethodType().stringValue());
    }

    /**
     * {@return a {@code patternAttribute} attribute}
     * @param patternName the name of the pattern
     * @param patternFlags the flags of the pattern
     * @param patternDescriptor the descriptor of the pattern
     * @param patternAttributes the list of attributes of the pattern
     */
    static PatternAttribute of(String patternName,
                               int patternFlags,
                               MethodTypeDesc patternDescriptor,
                               List<Attribute<?>> patternAttributes) {
        return new UnboundAttribute.UnboundPatternAttribute(
                TemporaryConstantPool.INSTANCE.utf8Entry(patternName),
                patternFlags,
                TemporaryConstantPool.INSTANCE.utf8Entry(patternDescriptor.descriptorString()),
                patternAttributes);
    }

    /**
     * {@return a {@code patternAttribute} attribute}
     * @param patternName the name of the pattern
     * @param patternFlags the flags of the pattern
     * @param patternDescriptor the descriptor of the pattern
     * @param patternAttributes the list of attributes of the pattern
     */
    static PatternAttribute of(Utf8Entry patternName,
                               int patternFlags,
                               Utf8Entry patternDescriptor,
                               List<Attribute<?>> patternAttributes) {
        return new UnboundAttribute.UnboundPatternAttribute(patternName, patternFlags, patternDescriptor, patternAttributes);
    }
}