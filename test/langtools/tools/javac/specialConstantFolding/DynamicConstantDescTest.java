/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @summary testing that DynamicConstantDesc::describeConstable calls describeConstable on the static arguments
 * @modules jdk.compiler/com.sun.tools.javac.util
 * @compile DynamicConstantDescTest.java
 * @run main DynamicConstantDescTest
 */

import java.lang.constant.*;
import com.sun.tools.javac.util.Assert;
import static java.lang.constant.ConstantDescs.*;

public class DynamicConstantDescTest {
    static final DirectMethodHandleDesc MHR_CONCAT = MethodHandleDesc.of(
        DirectMethodHandleDesc.Kind.VIRTUAL,
        CR_String,
        "concat",
        CR_String,
        CR_String
    );

    public static void main(String[] args) throws Throwable {
        ConstantDesc<String> d = DynamicConstantDesc.<String>of(BSM_INVOKE).withArgs(MHR_CONCAT, "Hello, ", "world!");
        Assert.check(d.toString().equals("DynamicConstantDesc[ConstantBootstraps::invoke(MethodHandleDesc[VIRTUAL/String::concat(String)String],Hello, ,world!)Object]"));
    }
}
