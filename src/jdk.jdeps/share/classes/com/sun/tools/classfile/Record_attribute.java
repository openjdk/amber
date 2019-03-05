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

package com.sun.tools.classfile;

import java.io.IOException;
import com.sun.tools.classfile.Attribute.Visitor;

/**
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Record_attribute extends Attribute {
    Record_attribute(ClassReader cr, int name_index, int length) throws IOException {
        super(name_index, length);
        num_params = cr.readUnsignedShort();
        params = new Param_data[num_params];
        for (int i = 0; i < num_params; i++) {
            params[i] = new Param_data(cr);
        }
        attributes = new Attributes(cr);
    }

    @Override
    public <R, D> R accept(Visitor<R, D> visitor, D data) {
        return visitor.visitRecord(this, data);
    }

    public final int num_params;
    public final Param_data[] params;
    public final Attributes attributes;

    public static class Param_data {
        Param_data(ClassReader cr) throws IOException {
            param_name_index = cr.readUnsignedShort();
            param_flags = cr.readUnsignedShort();
            param_descriptor = cr.readUnsignedShort();
            param_signature = cr.readUnsignedShort();
        }

        public String getName(ConstantPool constant_pool) throws ConstantPoolException {
            return constant_pool.getUTF8Value(param_name_index);
        }

        public final int param_name_index;
        public final int param_flags;
        public final int param_descriptor;
        public final int param_signature;
    }
}
