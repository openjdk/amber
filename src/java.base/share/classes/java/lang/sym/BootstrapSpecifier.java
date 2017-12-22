/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package java.lang.sym;

import java.lang.annotation.TrackableConstant;
import java.util.Arrays;
import java.util.Objects;

/**
 * A descriptor for an {@code invokedynamic} invocation
 */
public final class BootstrapSpecifier {
    private final MethodHandleRef bootstrapMethod;
    private final SymbolicRef<?>[] bootstrapArgs;

    private BootstrapSpecifier(MethodHandleRef bootstrapMethod, SymbolicRef<?>... bootstrapArgs) {
        this.bootstrapMethod = Objects.requireNonNull(bootstrapMethod);
        this.bootstrapArgs = Objects.requireNonNull(bootstrapArgs.clone());
    }

    /**
     * Create a descriptor for an {@code invokedynamic} invocation.
     * @param bootstrapMethod the bootstrap method for the {@code invokedynamic}
     * @param bootstrapArgs the bootstrap arguments for the {@code invokedynamic}
     * @return the descriptor
     */
    @TrackableConstant
    public static BootstrapSpecifier of(MethodHandleRef bootstrapMethod, SymbolicRef<?>... bootstrapArgs) {
        return new BootstrapSpecifier(bootstrapMethod, bootstrapArgs);
    }

    /**
     * Returns the bootstrap method for the {@code invokedynamic}
     * @return the bootstrap method for the {@code invokedynamic}
     */
    @TrackableConstant
    public MethodHandleRef method() { return bootstrapMethod; }

    /**
     * Returns the bootstrap arguments for the {@code invokedynamic}
     * @return the bootstrap arguments for the {@code invokedynamic}
     */
    public SymbolicRef<?>[] arguments() { return bootstrapArgs.clone(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BootstrapSpecifier specifier = (BootstrapSpecifier) o;
        return Objects.equals(bootstrapMethod, specifier.bootstrapMethod) &&
               Arrays.equals(bootstrapArgs, specifier.bootstrapArgs);
    }

    @Override
    public int hashCode() {

        int result = Objects.hash(bootstrapMethod);
        result = 31 * result + Arrays.hashCode(bootstrapArgs);
        return result;
    }

    @Override
    public String toString() {
        return String.format("BootstrapSpecifier[%s,%s]", bootstrapMethod, Arrays.toString(bootstrapArgs));
    }
}
