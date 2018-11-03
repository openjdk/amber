/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package jdk.internal.org.objectweb.asm;

import java.lang.reflect.InaccessibleObjectException;

public class ClassWriterExt extends ClassWriter {

    public ClassWriterExt(ClassReader cr, int flags) {
        super(cr, flags);
    }

    public ClassWriterExt(int flags) {
        super(flags);
    }

    public int getBytecodeLength(MethodVisitor mv) {
        ByteVector code;
        try {
            java.lang.reflect.Field field = mv.getClass().getDeclaredField("code");
            field.setAccessible(true);
            code = (ByteVector) field.get(mv);
        } catch (InaccessibleObjectException | SecurityException | ReflectiveOperationException e) {
            throw new Error("can not read field 'code' from class " + mv.getClass(), e);
        }
        try {
            java.lang.reflect.Field field = code.getClass().getDeclaredField("length");
            field.setAccessible(true);
            return field.getInt(code);
        } catch (InaccessibleObjectException | SecurityException | ReflectiveOperationException e) {
            throw new Error("can not read field 'length' from class " + code.getClass(), e);
        }
    }
}

