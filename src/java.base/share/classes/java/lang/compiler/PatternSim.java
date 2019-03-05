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
package java.lang.compiler;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Temporary scaffolding to allow matching / switching on constrained patterns
 * without language support.
 */
public class PatternSim {
    private static String DEFAULT_LABEL = "";

    /**
     * Simulator for statement switch
     * @param label the switch label
     * @param target the switch target
     * @return the switch simulator
     */
    public static StatementSwitch _switch(String label, Object target) {
        return new SwitchImpl(target, label);
    }

    /**
     * Simulator for statement switch
     * @param target the switch target
     * @return the switch simulator
     */
    public static StatementSwitch _switch(Object target) {
        return new SwitchImpl(target, DEFAULT_LABEL);
    }

    /**
     * Simulator for expression switch
     * @param label the switch label
     * @param target the switch target
     * @param <T> the the return type
     * @return the switch simulator
     */
    public static<T> ExpressionSwitch<T> _expswitch(String label, Object target) {
        return new ExprSwitchImpl<>(target, label);
    }

    /**
     * Simulator for expression switch
     * @param target the switch target
     * @param <T> the the return type
     * @return the switch simulator
     */
    public static<T> ExpressionSwitch<T> _expswitch(Object target) {
        return new ExprSwitchImpl<>(target, DEFAULT_LABEL);
    }

    /**
     * Simulator for continuing out of a switch
     */
    public static void _continue() {
        throw new ContinueSignal(DEFAULT_LABEL);
    }

    /**
     * Simulator for continuing out of the labeled switch
     *
     * @param label the label of the switch to continue at
     */
    public static void _continue(String label) {
        throw new ContinueSignal(label);
    }

    /**
     * Simulator type for statement switch
     */
    public interface StatementSwitch {
        /**
         * Simulate a case of a statement switch
         * @param pattern the pattern to match against
         * @param action the success action
         * @param <B> the type of the binding variable
         * @return the switch
         */
        <B> StatementSwitch _case(Supplier<_pattern<B>> pattern, Consumer<B> action);

        /**
         * Simulate a case of a statement switch
         * @param pattern the pattern to match against
         * @param action the success action
         * @param <B> the type of the binding variable
         * @return the switch
         */
        <B> StatementSwitch _case(Supplier<_pattern<B>> pattern, Runnable action);

        /**
         * Simulate the default clause of a statement switch
         * @param r the action
         * @return the switch
         */
        StatementSwitch _default(Runnable r);
    }

    /**
     * Simulator type for expression switch
     * @param <T> the switch type
     */
    public interface ExpressionSwitch<T> {
        /**
         * Simulate a case of an expression switch
         * @param pattern the pattern to match against
         * @param action the success action
         * @param <B> the type of the binding variable
         * @return the switch
         */
        <B> ExpressionSwitch<T> _case(Supplier<_pattern<B>> pattern, Function<B, T> action);

        /**
         * Simulate a case of an expression switch
         * @param pattern the pattern to match against
         * @param action the success action
         * @param <B> the type of the binding variable
         * @return the switch
         */
        <B> ExpressionSwitch<T> _case(Supplier<_pattern<B>> pattern, Supplier<T> action);

        /**
         * Simulate the default clause of an expression switch
         * @param r the action
         * @return the switch
         */
        ExpressionSwitch<T> _default(Supplier<T> r);

        /**
         * Get the result of an expression switch
         * @return the result
         */
        T result();
    }

    /**
     * Helper method for nested pattern in a statement switch
     * @param target the nested target
     * @param pattern the nested pattern
     * @param action the success action
     * @param <B> the type of the nested target
     */
    public static<B> void _nest(Object target, Supplier<_pattern<B>> pattern, Consumer<B> action) {
        Optional<B> match = pattern.get().match(target);
        if (match.isPresent())
            action.accept(match.get());
        else
            throw new ContinueSignal("<$nested$>");
    }

    /**
     * Helper method for nested pattern in a statement switch
     * @param target the nested target
     * @param pattern the nested pattern
     * @param action the success action
     * @param <B> the type of the nested target
     */
    public static<B> void _nest(Object target, Supplier<_pattern<B>> pattern, Runnable action) {
        Optional<B> match = pattern.get().match(target);
        if (match.isPresent())
            action.run();
        else
            throw new ContinueSignal("<$nested$>");
    }

    /**
     * Helper method for nested pattern in an expression switch
     * @param target the nested target
     * @param pattern the nested pattern
     * @param action the success action
     * @param <B> the type of the nested target
     * @param <T> the return type of the success action
     * @return the return value of the success action
     */
    public static<B, T> T _expnest(Object target, Supplier<_pattern<B>> pattern, Function<B, T> action) {
        Optional<B> match = pattern.get().match(target);
        if (match.isPresent())
            return action.apply(match.get());
        else
            throw new ContinueSignal("<$nested$>");
    }

    /**
     * Helper method for nested pattern in an expression switch
     * @param target the nested target
     * @param pattern the nested pattern
     * @param action the success action
     * @param <B> the type of the nested target
     * @param <T> the return type of the success action
     * @return the return value of the success action
     */
    public static<B, T> T _expnest(Object target, Supplier<_pattern<B>> pattern, Supplier<T> action) {
        Optional<B> match = pattern.get().match(target);
        if (match.isPresent())
            return action.get();
        else
            throw new ContinueSignal("<$nested$>");
    }

    @SuppressWarnings("serial")
    static class ContinueSignal extends RuntimeException {
        String label;

        ContinueSignal(String label) {
            super();
            this.label = label;
        }

        void maybeRethrow(String label) {
            if (!this.label.equals(label))
                throw this;
        }
    }
}

class SwitchImpl implements PatternSim.StatementSwitch {
    private final Object target;
    private final String label;
    private boolean done = false;

    SwitchImpl(Object target,
               String label) {
        this.target = target;
        this.label = label;
    }

    public <B> PatternSim.StatementSwitch _case(Supplier<_pattern<B>> pattern, Consumer<B> action) {
        if (!done) {
            Optional<B> match = pattern.get().match(target);
            if (match.isPresent()) {
                try {
                    action.accept(match.get());
                    done = true;
                }
                catch (PatternSim.ContinueSignal signal) {
                    signal.maybeRethrow(label);
                }
            }
        }
        return this;
    }

    public <B> PatternSim.StatementSwitch _case(Supplier<_pattern<B>> pattern, Runnable action) {
        if (!done) {
            Optional<B> match = pattern.get().match(target);
            if (match.isPresent()) {
                try {
                    action.run();
                    done = true;
                }
                catch (PatternSim.ContinueSignal signal) {
                    signal.maybeRethrow(label);
                }
            }
        }
        return this;
    }

    public PatternSim.StatementSwitch _default(Runnable r) {
        if (!done) {
            try {
                r.run();
                done = true;
            }
            catch (PatternSim.ContinueSignal signal) {
                signal.maybeRethrow(label);
            }
        }
        return this;
    }

}

class ExprSwitchImpl<T> implements PatternSim.ExpressionSwitch<T> {
    private final Object target;
    private final String label;
    private boolean done = false;
    private T result = null;

    ExprSwitchImpl(Object target,
                   String label) {
        this.target = target;
        this.label = label;
    }

    public<B> PatternSim.ExpressionSwitch<T> _case(Supplier<_pattern<B>> pattern, Function<B, T> action) {
        if (!done) {
            Optional<B> match = pattern.get().match(target);
            if (match.isPresent()) {
                try {
                    result = action.apply(match.get());
                    done = true;
                }
                catch (PatternSim.ContinueSignal signal) {
                    signal.maybeRethrow(label);
                }
            }
        }
        return this;
    }

    @Override
    public <B> PatternSim.ExpressionSwitch<T> _case(Supplier<_pattern<B>> pattern, Supplier<T> action) {
        if (!done) {
            Optional<B> match = pattern.get().match(target);
            if (match.isPresent()) {
                try {
                    result = action.get();
                    done = true;
                }
                catch (PatternSim.ContinueSignal signal) {
                    signal.maybeRethrow(label);
                }
            }
        }
        return this;
    }

    public PatternSim.ExpressionSwitch<T> _default(Supplier<T> r) {
        if (!done) {
            try {
                result = r.get();
                done = true;
            }
            catch (PatternSim.ContinueSignal signal) {
                signal.maybeRethrow(label);
            }
        }
        return this;
    }

    public T result() {
        return result;
    }

}
