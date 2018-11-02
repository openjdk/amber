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

package com.sun.tools.javac.intrinsics;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DynamicCallSiteDesc;
import java.lang.constant.MethodTypeDesc;

/**
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public interface IntrinsicProcessor {
    /**
     *  <p><b>This is NOT part of any supported API.
     *  If you write code that depends on this, you do so at your own risk.
     *  This code and its internal interfaces are subject to change or
     *  deletion without notice.</b>
     */
    public interface Result {
        /**
         *  <p><b>This is NOT part of any supported API.
         *  If you write code that depends on this, you do so at your own risk.
         *  This code and its internal interfaces are subject to change or
         *  deletion without notice.</b>
         */
        public class None implements Result {
        }

        /**
         *  <p><b>This is NOT part of any supported API.
         *  If you write code that depends on this, you do so at your own risk.
         *  This code and its internal interfaces are subject to change or
         *  deletion without notice.</b>
         */
        public class Ldc implements Result {
            private final ConstantDesc constant;

            /**
             * <p><b>This is NOT part of any supported API.</b>
             * @param constant  constant value result
             */
            public Ldc(ConstantDesc constant) {
                this.constant = constant;
            }

            /**
             * <p><b>This is NOT part of any supported API.</b>
             * @return constant result descriptor
             */
            public ConstantDesc constant() {
                return constant;
            }
        }

        /**
         *  <p><b>This is NOT part of any supported API.
         *  If you write code that depends on this, you do so at your own risk.
         *  This code and its internal interfaces are subject to change or
         *  deletion without notice.</b>
         */
        public class Indy implements Result {
            private final DynamicCallSiteDesc indy;
            private final int[] args;

            /**
             * <p><b>This is NOT part of any supported API.</b>
             * @param indy call site descriptor
             * @param args indices of call arguments
             */
            public Indy(DynamicCallSiteDesc indy, int[] args) {
                this.indy = indy;
                this.args = args;
            }

            /**
             * <p><b>This is NOT part of any supported API.</b>
             * @param indy  call site descriptor
             * @param nArgs argument range, 0..nArgs
             */
            public Indy(DynamicCallSiteDesc indy, int nArgs) {
                this.indy = indy;
                this.args = new int[nArgs];
                for (int i = 0; i < nArgs; i++)
                    args[i] = i;
            }

            /**
             * <p><b>This is NOT part of any supported API.</b>
             * @return DynamicCallSiteDesc value
             */
            public DynamicCallSiteDesc indy() {
                return indy;
            }

            /**
             * <p><b>This is NOT part of any supported API.</b>
             * @return indices of call arguments
             */
            public int[] args() {
                return args.clone();
            }
        }
    }

    /**
     * <p><b>This is NOT part of any supported API.</b>
     * @param ownerDesc       method owner
     * @param methodName      method name
     * @param methodType      method type descriptor
     * @param isStatic        true if static method call
     * @param argClassDescs   class descriptors for each argument (includes receiver)
     * @param constantArgs    constant value for each argument (includes receiver), null means unknown
     * @return IntrinsicProcessor.Result value
     */
    public Result tryIntrinsify(IntrinsicContext intrinsicContext,
                                ClassDesc ownerDesc,
                                String methodName,
                                MethodTypeDesc methodType,
                                boolean isStatic,
                                ClassDesc[] argClassDescs,
                                ConstantDesc[] constantArgs);
}

