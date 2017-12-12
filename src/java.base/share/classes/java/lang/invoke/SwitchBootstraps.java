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

package java.lang.invoke;

import jdk.internal.misc.SwitchBootstrapsImpl;

/**
 * SwitchBootstraps
 *
 * @author Brian Goetz
 */
public class SwitchBootstraps {
    /**Prepare a CallSite implementing switch translation. The value passed to the CallSite will
     * be matched to all case expressions, and the index of the first matching branch will be returned.
     *
     * @param lookup Represents a lookup context with the accessibility
     *               privileges of the caller.  When used with {@code invokedynamic},
     *               this is stacked automatically by the VM.
     * @param invocationName The name of the method to implement.  When used with
     *                    {@code invokedynamic}, this is provided by the
     *                    {@code NameAndType} of the {@code InvokeDynamic}
     *                    structure and is stacked automatically by the VM.
     * @param invocationType The expected signature of the {@code CallSite}.  The
     *                    parameter types represent the types of capture variables;
     *                    the return type is the interface to implement.   When
     *                    used with {@code invokedynamic}, this is provided by
     *                    the {@code NameAndType} of the {@code InvokeDynamic}
     *                    structure and is stacked automatically by the VM.
     *                    In the event that the implementation method is an
     *                    instance method and this signature has any parameters,
     *                    the first parameter in the invocation signature must
     *                    correspond to the receiver.
     * @param intLabels values that should be matched to the value passed to the CallSite.
     * @return an index of a value from branches matching the value passed to the CallSite, {@literal -1}
     *         if the value is null, {@code branches.length} if none is matching.
     * @throws Throwable if something goes wrong
     */
    public static CallSite intSwitch(MethodHandles.Lookup lookup, String invocationName, MethodType invocationType,
                                     int... intLabels) throws Throwable {
        return SwitchBootstrapsImpl.intSwitch(lookup, invocationName, invocationType, intLabels);
    }

    /**Prepare a CallSite implementing switch translation. The value passed to the CallSite will
     * be matched to all case expressions, and the index of the first matching branch will be returned.
     *
     * @param lookup Represents a lookup context with the accessibility
     *               privileges of the caller.  When used with {@code invokedynamic},
     *               this is stacked automatically by the VM.
     * @param invocationName The name of the method to implement.  When used with
     *                    {@code invokedynamic}, this is provided by the
     *                    {@code NameAndType} of the {@code InvokeDynamic}
     *                    structure and is stacked automatically by the VM.
     * @param invocationType The expected signature of the {@code CallSite}.  The
     *                    parameter types represent the types of capture variables;
     *                    the return type is the interface to implement.   When
     *                    used with {@code invokedynamic}, this is provided by
     *                    the {@code NameAndType} of the {@code InvokeDynamic}
     *                    structure and is stacked automatically by the VM.
     *                    In the event that the implementation method is an
     *                    instance method and this signature has any parameters,
     *                    the first parameter in the invocation signature must
     *                    correspond to the receiver.
     * @param stringLabels values that should be matched to the value passed to the CallSite.
     * @return an index of a value from branches matching the value passed to the CallSite, {@literal -1}
     *         if the value is null, {@code branches.length} if none is matching.
     * @throws Throwable if something goes wrong
     */
    public static CallSite stringSwitch(MethodHandles.Lookup lookup, String invocationName, MethodType invocationType,
                                        String... stringLabels) throws Throwable {
        return SwitchBootstrapsImpl.stringSwitch(lookup, invocationName, invocationType, stringLabels);
    }

    /**Prepare a CallSite implementing switch translation. The value passed to the CallSite will
     * be matched to all case expressions, and the index of the first matching branch will be returned.
     *
     * @param lookup Represents a lookup context with the accessibility
     *               privileges of the caller.  When used with {@code invokedynamic},
     *               this is stacked automatically by the VM.
     * @param invocationName The name of the method to implement.  When used with
     *                    {@code invokedynamic}, this is provided by the
     *                    {@code NameAndType} of the {@code InvokeDynamic}
     *                    structure and is stacked automatically by the VM.
     * @param invocationType The expected signature of the {@code CallSite}.  The
     *                    parameter types represent the types of capture variables;
     *                    the return type is the interface to implement.   When
     *                    used with {@code invokedynamic}, this is provided by
     *                    the {@code NameAndType} of the {@code InvokeDynamic}
     *                    structure and is stacked automatically by the VM.
     *                    In the event that the implementation method is an
     *                    instance method and this signature has any parameters,
     *                    the first parameter in the invocation signature must
     *                    correspond to the receiver.
     * @param <E> the enum type
     * @param enumClass the enum class
     * @param caseLabels enum names that should be matched to the value passed to the CallSite.
     * @return an index of a value from branches matching the value passed to the CallSite, {@literal -1}
     *         if the value is null, {@code branches.length} if none is matching.
     * @throws Throwable if something goes wrong
     */
    public static<E extends Enum<E>> CallSite enumSwitch(MethodHandles.Lookup lookup, String invocationName, MethodType invocationType,
                                                         Class<E> enumClass, String... caseLabels) throws Throwable {
        return SwitchBootstrapsImpl.enumSwitch(lookup, invocationName, invocationType, enumClass, caseLabels);
    }

}
