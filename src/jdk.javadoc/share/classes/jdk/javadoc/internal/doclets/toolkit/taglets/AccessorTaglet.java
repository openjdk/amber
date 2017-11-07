/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclets.toolkit.taglets;

import com.sun.source.doctree.DocTree;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.util.Utils;

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import java.util.List;

/**
 * A taglet that represents the @param tag.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 *
 * @author Jamie Ho
 */
public class AccessorTaglet extends BaseTaglet {

    DocTree.Kind kind;

    /**
     * Construct a ParamTaglet.
     */
    public AccessorTaglet(DocTree.Kind kind) {
        name = kind.tagName;
        this.kind = kind;
    }

    /**
     * {@inheritDoc}
     */
    public boolean inField() {
        return true;
    }

        /**
     * {@inheritDoc}
     */
    public boolean inMethod() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean inOverview() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean inModule() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean inPackage() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean inType() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isInlineTag() {
        return false;
    }

    /**
     * Given an array of <code>ParamTag</code>s,return its string representation.
     * @param holder the member that holds the param tags.
     * @param writer the TagletWriter that will write this tag.
     * @return the TagletOutput representation of these <code>ParamTag</code>s.
     */
    public Content getTagletOutput(Element holder, TagletWriter writer) {
        Utils utils = writer.configuration().utils;
        return writer.accessorTagOutput(holder, utils.getBlockTags(holder, kind));
    }
}
