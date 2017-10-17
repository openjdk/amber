/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @test 8168964
 * @summary javac is generating duplicate bootstrap specifiers to the BSM attribute
 * @modules jdk.compiler/com.sun.tools.javac.util
            jdk.jdeps/com.sun.tools.classfile
 * @compile -XDdoConstantFold MultipleBSMEntriesTest.java
 * @run main MultipleBSMEntriesTest
 * @author tvaleev
 * @author vromero
 */

import java.io.File;
import java.lang.invoke.*;
import java.math.BigInteger;

import com.sun.tools.classfile.*;
import com.sun.tools.classfile.ConstantPool.*;
import com.sun.tools.javac.util.Assert;

public class MultipleBSMEntriesTest {
    // library code starts
    static class MultiplyCallSite extends MutableCallSite {
        private static final MethodTypeRef TYPE =
                MethodTypeRef.of(ClassRef.ofDescriptor("Ljava/math/BigInteger;"), ClassRef.CR_long, ClassRef.CR_long);
        private static final ClassRef ME = ClassRef.ofDescriptor("LMultipleBSMEntriesTest$MultiplyCallSite;");

        private static final MethodHandle FAST = Intrinsics.ldc(MethodHandleRef.of(MethodHandleRef.Kind.VIRTUAL, ME, "fast", TYPE));
        private static final MethodHandle SLOW = Intrinsics.ldc(MethodHandleRef.of(MethodHandleRef.Kind.STATIC, ME, "slow", TYPE));

        MultiplyCallSite(MethodType type) {
            super(type);
            setTarget(FAST.bindTo(this).asType(type));
        }

        BigInteger fast(long a, long b) {
            try {
                return BigInteger.valueOf(Math.multiplyExact(a, b));
            } catch (ArithmeticException ex) {
                // switch to slower implementation
                setTarget(SLOW.asType(type()));
                return slow(a, b);
            }
        }

        static BigInteger slow(long a, long b) {
            return BigInteger.valueOf(a).multiply(BigInteger.valueOf(b));
        }
    }

    public static final BootstrapSpecifier MULT = BootstrapSpecifier.of(
            MethodHandleRef.of(MethodHandleRef.Kind.STATIC, ClassRef.ofDescriptor("LMultipleBSMEntriesTest;"), "multiplyFactory",
                               ClassRef.CR_CallSite, ClassRef.CR_Lookup, ClassRef.CR_String, ClassRef.CR_MethodType));

    public static CallSite multiplyFactory(MethodHandles.Lookup lookup, String name, MethodType type) {
        return new MultiplyCallSite(type);
    }
// library code ends

    public static BigInteger multiplyIndy(long bigNum, long smallNum) {
        try {
            return ((BigInteger) Intrinsics.invokedynamic(MULT, " ", bigNum, bigNum))
                .add((BigInteger) Intrinsics.invokedynamic(MULT, " ", smallNum, smallNum));
        } catch (Throwable throwable) {
            throw new InternalError(throwable);
        }
    }

    public static void main(String[] args) throws Throwable {
        // sanity test
        multiplyIndy(5000000000L, 6);

        File testClasses = new File(System.getProperty("test.classes"));
        /* better to use concat to avoid firing the string concat machinery which will generate another
         * bootstrap specifier
         */
        File file = new File(testClasses, MultipleBSMEntriesTest.class.getName().concat(".class"));
        ClassFile classFile = ClassFile.read(file);
        BootstrapMethods_attribute bootstrapMethods = (BootstrapMethods_attribute)classFile.getAttribute("BootstrapMethods");
        Assert.check(bootstrapMethods.bootstrap_method_specifiers.length == 1, "there can only be a bootstrap method specifier");

        // lets check now that the specifier is the one we are expecting
        BootstrapMethods_attribute.BootstrapMethodSpecifier specifier = bootstrapMethods.bootstrap_method_specifiers[0];
        CONSTANT_MethodHandle_info mhInfo = (CONSTANT_MethodHandle_info)classFile.constant_pool.get(specifier.bootstrap_method_ref);
        Assert.check(mhInfo.reference_kind == ConstantPool.RefKind.REF_invokeStatic);
        CONSTANT_Methodref_info mrInfo = (CONSTANT_Methodref_info)classFile.constant_pool.get(mhInfo.reference_index);
        CONSTANT_NameAndType_info nameAndType = (CONSTANT_NameAndType_info)classFile.constant_pool.getNameAndTypeInfo(mrInfo.name_and_type_index);
        Assert.check(nameAndType.getName().equals("multiplyFactory"));
        Assert.check(nameAndType.getType().equals("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"));
    }
}
