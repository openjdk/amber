/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.Test;

import java.io.Serializable;
import java.lang.Enum.EnumDesc;
import java.lang.classfile.ClassFile;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.AccessFlag;
import java.lang.runtime.Carriers;
import java.lang.runtime.PatternBootstraps;
import java.lang.runtime.SwitchBootstraps;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.Assert.*;

/**
 * @test
 * @bug 8333725
 * @enablePreview
 * @modules java.base/jdk.internal.classfile
 * @compile PatternBootstrapsTest.java
 * @run testng/othervm PatternBootstrapsTest
 */
@Test
public class PatternBootstrapsTest {

    public static final MethodHandle INVK_PATTERN;
    static {
        try {
            INVK_PATTERN = MethodHandles.lookup().findStatic(PatternBootstraps.class, "invokePattern",
                    MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class));
        }
        catch (ReflectiveOperationException e) {
            throw new AssertionError("Should not happen", e);
        }
    }

    record R(int i, int j) {}

    class R2 {
        int x;
        int y;
        public R2(int x, int y) {
            this.x = x;
            this.y = y;
        }
        public pattern R2(int x, int y) {
            match R2(this.x, this.y);
        }
    }

    private void testPatternInvocation(Object target, Class<?> targetType, String mangledName, int componentNo, int result) throws Throwable {
        MethodType dtorType = MethodType.methodType(Object.class, targetType);
        MethodHandle indy = ((CallSite) INVK_PATTERN.invoke(MethodHandles.lookup(), "", dtorType, mangledName)).dynamicInvoker();
        List<MethodHandle> components = Carriers.components(MethodType.methodType(Object.class, int.class, int.class));
        assertEquals((int) components.get(componentNo).invokeExact(indy.invoke(target)), result);
    }

    public void testPatternInvocations() throws Throwable {
        testPatternInvocation(new R(1, 2), R.class, "R:I:I", 0, 1);
        testPatternInvocation(new R2(1, 2), R2.class, "R2:I:I", 0, 1);
    }
}
