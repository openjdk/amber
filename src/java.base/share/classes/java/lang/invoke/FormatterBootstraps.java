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

package java.lang.invoke;

import sun.security.action.GetPropertyAction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.IllegalFormatConversionException;
import java.util.List;
import java.util.Locale;
import java.util.MissingFormatArgumentException;
import java.util.Properties;
import java.util.UnknownFormatConversionException;
import java.util.stream.IntStream;
import jdk.internal.util.FormatString;
import jdk.internal.util.FormatString.FormatSpecifier;
import jdk.internal.util.FormatString.FormatToken;

import static java.lang.invoke.MethodHandles.Lookup.IMPL_LOOKUP;
import static java.lang.invoke.MethodHandles.constant;
import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodHandles.filterArguments;
import static java.lang.invoke.MethodHandles.filterReturnValue;
import static java.lang.invoke.MethodHandles.foldArguments;
import static java.lang.invoke.MethodHandles.guardWithTest;
import static java.lang.invoke.MethodHandles.identity;
import static java.lang.invoke.MethodHandles.insertArguments;
import static java.lang.invoke.MethodHandles.permuteArguments;
import static java.lang.invoke.MethodHandles.throwException;
import static java.lang.invoke.MethodType.methodType;

/**
 * Bootstrapping support for <code>invokedynamic</code>-based implementations of the following
 * {@link Formatter} related methods:
 *
 * <ul>
 *     <li>{@link Formatter#format(String, Object...)}</li>
 *     <li>{@link Formatter#format(Locale, String, Object...)}</li>
 *     <li>{@link java.io.PrintStream#format(String, Object...)}</li>
 *     <li>{@link java.io.PrintStream#format(Locale, String, Object...)}</li>
 *     <li>{@link java.io.PrintStream#printf(String, Object...)}</li>
 *     <li>{@link java.io.PrintStream#printf(Locale, String, Object...)}</li>
 *     <li>{@link java.io.PrintWriter#format(String, Object...)}</li>
 *     <li>{@link java.io.PrintWriter#format(Locale, String, Object...)}</li>
 *     <li>{@link java.io.PrintWriter#printf(String, Object...)}</li>
 *     <li>{@link java.io.PrintWriter#printf(Locale, String, Object...)}</li>
 *     <li>{@link String#format(String, Object...)}</li>
 *     <li>{@link String#format(Locale, String, Object...)}</li>
 * </ul>
 *
 * <p>This currently provides three implementations which can be selected via system properties:</p>
 *
 * <dl>
 *     <dt>fallback mode</dt>
 *     <dd>Generates a method handle that invokes the exact method specified in the Java source code.
 *     This implementation is activated by setting the {@code formatter.fallback} system property to {@code "true"}.
 *     </dd>
 *
 *     <dt>Formatter/StringBuilder based</dt>
 *     <dd>Generates a method handle composed mostly from methods in {@link Formatter} and {@link StringBuilder}.
 *     This implementation is activated by setting the {@code formatter.stringconcat} system property to {@code "false"}.
 *     </dd>
 *
 *     <dt>StringConcatFactory based</dt>
 *     <dd>Generates a method handle based on {@link StringConcatFactory}.
 *     This implementation is activated by default.
 *     </dd>
 * </dl>
 *
 * <p>In addition , the {@code formatter.specialize} and {@code formatter.directconcat} system properties can be set to
 * {@code "false"} to disable specific optimizations that are enabled by default.</p>
 */
public final class FormatterBootstraps {

    /**
     * Generate method handles that directly correspond to the invoked method.
     */
    private static final boolean USE_FALLBACK_HANDLE;
    /**
     * Use java.lang.invoke.StringConcatFactory to build the target handle
     */
    private static final boolean USE_STRING_CONCAT_FACTORY;
    /**
     * Use specialized method handles for specific conversion/argument type combinations.
     */
    private static final boolean USE_SPECIALIZED_CONVERSION;
    /**
     * Try to avoid conversions where it isn't needed, such as for strings or ints under certain circumstances.
     */
    private static final boolean USE_DIRECT_CONCAT;

    static {
        Properties props = GetPropertyAction.privilegedGetProperties();
        USE_FALLBACK_HANDLE = Boolean.parseBoolean(props.getProperty("formatter.fallback", "false"));
        USE_STRING_CONCAT_FACTORY = Boolean.parseBoolean(props.getProperty("formatter.stringconcat", "true"));
        USE_SPECIALIZED_CONVERSION = Boolean.parseBoolean(props.getProperty("formatter.specialize", "true"));
        USE_DIRECT_CONCAT = Boolean.parseBoolean(props.getProperty("formatter.directconcat", "true"));
    }

    private static final MethodHandle STRINGBUILDER_APPEND_BOOLEAN =
            findVirtualMethodHandle(StringBuilder.class, "append", methodType(StringBuilder.class, boolean.class));
    private static final MethodHandle STRINGBUILDER_APPEND_CHAR =
            findVirtualMethodHandle(StringBuilder.class, "append", methodType(StringBuilder.class, char.class));
    private static final MethodHandle STRINGBUILDER_APPEND_INT =
            findVirtualMethodHandle(StringBuilder.class, "append", methodType(StringBuilder.class, int.class));
    private static final MethodHandle STRINGBUILDER_APPEND_LONG =
            findVirtualMethodHandle(StringBuilder.class, "append", methodType(StringBuilder.class, long.class));
    private static final MethodHandle STRINGBUILDER_APPEND_STRING =
            findVirtualMethodHandle(StringBuilder.class, "append", methodType(StringBuilder.class, String.class));
    private static final MethodHandle STRINGBUILDER_TOSTRING =
            findVirtualMethodHandle(StringBuilder.class, "toString", methodType(String.class));

    private static final MethodHandle APPENDABLE_APPEND =
            findVirtualMethodHandle(Appendable.class, "append", methodType(Appendable.class, CharSequence.class));

    private static final MethodHandle SPECIFIER_PRINT =
            findVirtualMethodHandle(Formatter.class, "print",
                    methodType(Formatter.class, FormatSpecifier.class, Object.class, Locale.class));
    private static final MethodHandle SPECIFIER_PRINT_STRING =
            findVirtualMethodHandle(Formatter.class, "print",
                    methodType(Formatter.class, FormatSpecifier.class, String.class, Locale.class));
    private static final MethodHandle SPECIFIER_PRINT_INT =
            findVirtualMethodHandle(Formatter.class, "print",
                    methodType(Formatter.class, FormatSpecifier.class, int.class, Locale.class));
    private static final MethodHandle SPECIFIER_PRINT_LONG =
            findVirtualMethodHandle(Formatter.class, "print",
                    methodType(Formatter.class, FormatSpecifier.class, long.class, Locale.class));
    private static final MethodHandle SPECIFIER_PRINT_BYTE =
            findVirtualMethodHandle(Formatter.class, "print",
                    methodType(Formatter.class, FormatSpecifier.class, byte.class, Locale.class));
    private static final MethodHandle SPECIFIER_PRINT_SHORT =
            findVirtualMethodHandle(Formatter.class, "print",
                    methodType(Formatter.class, FormatSpecifier.class, short.class, Locale.class));
    private static final MethodHandle SPECIFIER_PRINT_FLOAT =
            findVirtualMethodHandle(Formatter.class, "print",
                    methodType(Formatter.class, FormatSpecifier.class, float.class, Locale.class));
    private static final MethodHandle SPECIFIER_PRINT_DOUBLE =
            findVirtualMethodHandle(Formatter.class, "print",
                    methodType(Formatter.class, FormatSpecifier.class, double.class, Locale.class));
    private static final MethodHandle SPECIFIER_PRINT_HASHCODE =
            findVirtualMethodHandle(Formatter.class, "printHashCode",
                    methodType(Formatter.class, FormatSpecifier.class, Object.class, Locale.class));
    private static final MethodHandle FIXED_STRING_PRINT =
            findVirtualMethodHandle(FormatString.FixedString.class, "print",
                    methodType(Formatter.class, Formatter.class));

    private static final MethodHandle CONSTRUCT_FORMATTER =
            findConstructorMethodHandle(Formatter.class, methodType(void.class, Appendable.class))
                    .asType(methodType(Formatter.class, Appendable.class));
    private static final MethodHandle CONSTRUCT_FORMATTER_EMPTY =
            findConstructorMethodHandle(Formatter.class, methodType(void.class))
                    .asType(methodType(Formatter.class));
    private static final MethodHandle CONSTRUCT_FORMATTER_LOCALE =
            findConstructorMethodHandle(Formatter.class, methodType(void.class, Locale.class))
                    .asType(methodType(Formatter.class, Locale.class));
    private static final MethodHandle CONSTRUCT_STRINGBUILDER =
            findConstructorMethodHandle(StringBuilder.class, methodType(void.class))
                    .asType(methodType(StringBuilder.class));

    private static final MethodHandle CONSTRUCT_MISSING_FORMAT_ARGUMENT_EXCEPTION =
            findConstructorMethodHandle(MissingFormatArgumentException.class, methodType(void.class, String.class));
    private static final MethodHandle CONSTRUCT_ILLEGAL_FORMAT_CONVERSION_EXCEPTION =
            findConstructorMethodHandle(IllegalFormatConversionException.class, methodType(void.class, char.class, Class.class));
    private static final MethodHandle CONSTRUCT_UNKNOWN_FORMAT_CONVERSION_EXCEPTION =
            findConstructorMethodHandle(UnknownFormatConversionException.class, methodType(void.class, String.class));
    private static final MethodHandle OBJECT_TO_STRING =
            findVirtualMethodHandle(Object.class, "toString", methodType(String.class));
    private static final MethodHandle APPENDABLE_TO_STRING =
            OBJECT_TO_STRING.asType(methodType(String.class, Appendable.class));
    private static final MethodHandle FORMATTER_OUT =
            findVirtualMethodHandle(Formatter.class, "out", methodType(Appendable.class));
    private static final MethodHandle LOCALE_GETDEFAULT =
            insertArguments(findStaticMethodHandle(Locale.class, "getDefault",
                    methodType(Locale.class, Locale.Category.class)),0, Locale.Category.FORMAT);
    private static final MethodHandle FORMATTER_LOCALE =
            findVirtualMethodHandle(Formatter.class, "locale", methodType(Locale.class));

    private static final MethodHandle INT_TO_STRING =
            findStaticMethodHandle(Integer.class, "toString", methodType(String.class, int.class));
    private static final MethodHandle BOOLEAN_TO_STRING =
            findStaticMethodHandle(Boolean.class, "toString", methodType(String.class, boolean.class));
    private static final MethodHandle OBJECT_HASHCODE =
            findVirtualMethodHandle(Object.class, "hashCode", methodType(int.class));
    private static final MethodHandle INTEGER_TO_HEX_STRING =
            findStaticMethodHandle(Integer.class, "toHexString", methodType(String.class, int.class));
    private static final MethodHandle INTEGER_TO_OCTAL_STRING =
            findStaticMethodHandle(Integer.class, "toOctalString", methodType(String.class, int.class));
    private static final MethodHandle LONG_TO_STRING =
            findStaticMethodHandle(Long.class, "toString", methodType(String.class, long.class));
    private static final MethodHandle LONG_TO_HEX_STRING =
            findStaticMethodHandle(Long.class, "toHexString", methodType(String.class, long.class));
    private static final MethodHandle LONG_TO_OCTAL_STRING =
            findStaticMethodHandle(Long.class, "toOctalString", methodType(String.class, long.class));
    private static final MethodHandle STRING_TO_UPPER_CASE =
            findVirtualMethodHandle(String.class, "toUpperCase", methodType(String.class));

    private static final MethodHandle TOSTRING_RESET = findStaticMethodHandle(FormatterBootstraps.class, "toStringReset",
            methodType(String.class, Formatter.class));
    private static final MethodHandle LOCALE_GUARD = findStaticMethodHandle(FormatterBootstraps.class, "localeGuard",
            methodType(boolean.class, Locale.class, Locale.class));
    private static final MethodHandle BOOLEAN_OBJECT_FILTER = findStaticMethodHandle(FormatterBootstraps.class, "booleanObjectFilter",
            methodType(boolean.class, Object.class));
    private static final MethodHandle NOT_NULL_TEST = findStaticMethodHandle(FormatterBootstraps.class, "notNullTest",
            methodType(boolean.class, Object.class));

    private static final int MISSING_ARGUMENT_INDEX = Integer.MIN_VALUE;



    /**
     * Bootstrap for Formatter intrinsics.
     *
     * @param lookup         MethodHandles lookup
     * @param name           Name of method
     * @param methodType     Method signature
     * @param format         Formatter format string
     * @param isStringMethod Called from String method
     * @param hasLocaleArg   Has a Locale argument
     * @return Callsite for intrinsic method
     */
    public static CallSite formatterBootstrap(MethodHandles.Lookup lookup,
                                              String name,
                                              MethodType methodType,
                                              String format,
                                              boolean isStringMethod,
                                              boolean hasLocaleArg) {
        boolean isVarArgs = isVarArgsType(methodType, isStringMethod, hasLocaleArg);
        if (USE_FALLBACK_HANDLE || isVarArgs) {
            return new ConstantCallSite(fallbackMethodHandle(
                    lookup, name, methodType, format,
                    isStringMethod, hasLocaleArg, isVarArgs));
        }

        List<FormatToken> specs;

        try {
            specs = FormatString.parse(format);
        } catch (UnknownFormatConversionException unknownConversion) {
            return new ConstantCallSite(unknownFormatConversionThrower(unknownConversion, methodType));
        }

        if (specs.isEmpty()) {
            return new ConstantCallSite(isStringMethod ?
                    constant(String.class, "").asType(methodType) :
                    identity(methodType.parameterType(0)).asType(methodType));
        }

        return makeFormatterCallSite(lookup, methodType, specs, hasLocaleArg, isStringMethod);
    }


    private static CallSite makeFormatterCallSite(MethodHandles.Lookup lookup, MethodType methodType, List<FormatToken> specs,
                                                  boolean hasLocaleArg, boolean isStringMethod) {

        boolean isFormatterMethod = methodType.parameterCount() > 0 && methodType.parameterType(0) == Formatter.class;
        // Array of formatter args excluding target and locale
        Class<?>[] argTypes = methodType.dropParameterTypes(0, firstFormatterArg(isStringMethod, hasLocaleArg)).parameterArray();
        // index array is needed for argument analysis
        int[] argIndexes = calculateArgumentIndexes(specs, argTypes.length);

        FormatHandleBuilder builder;

        if (USE_STRING_CONCAT_FACTORY) {
            builder = new StringConcatHandleBuilder(specs, argTypes, argIndexes, true);
        } else {
            builder = new FormatterFormatHandleBuilder(specs, argTypes, argIndexes, isStringMethod, hasLocaleArg);
        }

        return new ConstantCallSite(builder.getHandle(lookup, methodType, hasLocaleArg, isStringMethod, isFormatterMethod));
    }

    private static int[] calculateArgumentIndexes(List<FormatToken> specs, int argCount) {
        int[] argIndexes = new int[specs.size()];
        int last = -1;
        int lasto = -1;

        // Calculate indices and throw exceptions for missing arguments
        for (int i = 0; i < specs.size(); i++) {
            FormatToken ft = specs.get(i);

            int index = ft.index();
            switch (index) {
                case -2:  // fixed string, "%n", or "%%"
                    argIndexes[i] = -1;
                    break;
                case -1:  // relative index
                    argIndexes[i] = (last < 0 || last >= argCount) ? MISSING_ARGUMENT_INDEX : last;
                    break;
                case 0:  // ordinary index
                    lasto++;
                    last = lasto;
                    argIndexes[i] = (last < 0 || last >= argCount) ? MISSING_ARGUMENT_INDEX : last;
                    break;
                default:  // explicit index
                    last = index - 1;
                    argIndexes[i] = (last < 0 || last >= argCount) ? MISSING_ARGUMENT_INDEX : last;
                    break;
            }
        }

        return argIndexes;
    }

    private static int firstFormatterArg(boolean isStringMethod, boolean hasLocaleArg) {
        int index = isStringMethod ? 0 : 1;
        return hasLocaleArg ? index + 1 : index;
    }

    private static MethodHandle findVirtualMethodHandle(Class<?> type, String name, MethodType methodType) {
        try {
            return IMPL_LOOKUP.findVirtual(type, name, methodType);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static MethodHandle findStaticMethodHandle(Class<?> type, String name, MethodType methodType) {
        try {
            return IMPL_LOOKUP.findStatic(type, name, methodType);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static MethodHandle findConstructorMethodHandle(Class<?> type, MethodType methodType) {
        try {
            return IMPL_LOOKUP.findConstructor(type, methodType);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    static abstract class FormatHandleBuilder {
        List<FormatToken> specs;
        Class<?>[] argTypes;
        int[] argIndexes;

        FormatHandleBuilder(List<FormatToken> specs, Class<?>[] argTypes, int[] argIndexes) {
            this.specs = specs;
            this.argTypes = argTypes;
            this.argIndexes = argIndexes;
        }

        void buildHandles() {
            for (int i = 0; i < specs.size(); i++) {
                if (argIndexes[i] == -1) {
                    addConstantMethodHandle(argTypes, specs.get(i));
                } else if (argIndexes[i] == MISSING_ARGUMENT_INDEX) {
                    addMissingArgumentMethodHandle(argTypes, (FormatSpecifier) specs.get(i));
                } else {
                    addArgumentMethodHandle(argTypes, (FormatSpecifier) specs.get(i), argIndexes[i]);
                }
            }
        }

        abstract void addArgumentMethodHandle(Class<?>[] argTypes, FormatSpecifier spec, int argIndex);
        abstract void addConstantMethodHandle(Class<?>[] argTypes, FormatToken spec);
        abstract void addMissingArgumentMethodHandle(Class<?>[] argTypes, FormatSpecifier spec);
        abstract MethodHandle getHandle(MethodHandles.Lookup lookup, MethodType methodType, boolean hasLocaleArg,
                                        boolean isStringMethod, boolean isFormatterMethod);
    }


    static class FormatterFormatHandleBuilder extends FormatHandleBuilder {

        private MethodHandle handle = null;
        private boolean useDirectConcat;

        FormatterFormatHandleBuilder(List<FormatToken> specs, Class<?>[] argTypes, int[] argIndexes,
                                     boolean isStringMethod, boolean hasLocaleArg) {
            super(specs, argTypes, argIndexes);
            this.useDirectConcat = USE_DIRECT_CONCAT && isStringMethod && !hasLocaleArg && useDirectConcat(specs, argTypes, argIndexes);
        }

        private boolean useDirectConcat(List<FormatToken> specs, Class<?>[] argTypes, int[] argIndexes) {
            for (int i = 0; i < specs.size(); i++) {
                if (argIndexes[i] >= 0
                        && !canUseDirectConcat((FormatSpecifier) specs.get(i), argTypes[argIndexes[i]])
                        && !canUseSpecialConverter((FormatSpecifier) specs.get(i), argTypes[argIndexes[i]])) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public void addArgumentMethodHandle(Class<?>[] argTypes, FormatSpecifier spec, int argIndex) {
            MethodHandle appender;

            if (useDirectConcat) {
                appender = canUseDirectConcat(spec, argTypes[argIndex]) ?
                        getAppenderHandle(argTypes[argIndex]) :
                        filterArguments(STRINGBUILDER_APPEND_STRING, 1, getSpecializedConverter(spec, argTypes[argIndex]));
            } else if (USE_SPECIALIZED_CONVERSION && canUseSpecialConverter(spec, argTypes[argIndex])) {
                MethodHandle conversionFilter = getSpecializedConverter(spec, argTypes[argIndex]);
                appender = filterArguments(SPECIFIER_PRINT_STRING, 2, conversionFilter);
                appender = insertArguments(appender, 0, spec);
            } else {
                appender = getPrintHandle(argTypes[argIndex], spec);
                appender = insertArguments(appender, 1, spec);
            }

            appender = appender.asType(appender.type().changeParameterType(1, argTypes[argIndex]));

            if (argIndex > 0) {
                appender = dropArguments(appender, 1, Arrays.copyOfRange(argTypes, 0, argIndex));
            }
            if (argIndex < argTypes.length - 1) {
                appender = dropArguments(appender, argIndex + 2, Arrays.copyOfRange(argTypes, argIndex + 1, argTypes.length));
            }

            if (handle == null) {
                handle = appender;
            } else {
                handle = foldArguments(appender, handle.asType(handle.type().changeReturnType(void.class)));
            }
        }

        @Override
        public void addConstantMethodHandle(Class<?>[] argTypes, FormatToken spec) {
            MethodHandle appender;
            if (useDirectConcat) {
                appender = insertArguments(STRINGBUILDER_APPEND_STRING, 1, getConstantSpecValue(spec));
            } else if (spec instanceof FormatString.FixedString) {
                appender = dropArguments(insertArguments(FIXED_STRING_PRINT, 0, spec), 1, Locale.class);
            } else {
                appender = insertArguments(SPECIFIER_PRINT, 1, spec);
                appender = insertArguments(appender, 1, (Object) null);
            }
            appender = dropArguments(appender, 1, Arrays.copyOfRange(argTypes, 0, argTypes.length));

            if (handle == null) {
                handle = appender;
            } else {
                handle = foldArguments(appender, handle.asType(handle.type().changeReturnType(void.class)));
            }
        }

        @Override
        public void addMissingArgumentMethodHandle(Class<?>[] argTypes, FormatSpecifier spec) {
            MethodHandle thrower;
            if (useDirectConcat) {
                thrower = missingFormatArgumentThrower(spec.toString(), StringBuilder.class);
                thrower = dropArguments(thrower, 0, StringBuilder.class);
            } else {
                thrower = missingFormatArgumentThrower(spec.toString(), Formatter.class);
                thrower = dropArguments(thrower, 0, Formatter.class);
                thrower = dropArguments(thrower, 1, argTypes);
                thrower = dropArguments(thrower, thrower.type().parameterCount(), Locale.class);
            }

            if (handle == null) {
                handle = thrower;
            } else {
                handle = foldArguments(thrower, handle.asType(handle.type().changeReturnType(void.class)));
            }
        }

        @Override
        public MethodHandle getHandle(MethodHandles.Lookup lookup, MethodType methodType, boolean hasLocaleArg,
                                      boolean isStringMethod, boolean isFormatterMethod) {

            buildHandles();

            MethodHandle wrapper;

            if (useDirectConcat) {
                wrapper = foldArguments(handle, 0, CONSTRUCT_STRINGBUILDER);
            } else if (isFormatterMethod) {
                wrapper = handle;
            } else {
                if (isStringMethod) {
                    wrapper = foldArguments(handle, 0, CONSTRUCT_FORMATTER_EMPTY);
                } else {
                    wrapper = filterArguments(handle, 0, CONSTRUCT_FORMATTER);
                }
                wrapper = filterReturnValue(wrapper, FORMATTER_OUT);
            }

            if (hasLocaleArg) {
                int[] argmap = new int[methodType.parameterCount()];
                if (!isStringMethod) {
                    argmap[0] = 0;
                    argmap[argmap.length - 1] = 1;
                    for (int i = 1; i < argmap.length - 1; i++) {
                        argmap[i] = i + 1;
                    }
                } else {
                    argmap[argmap.length - 1] = 0;
                    for (int i = 0; i < argmap.length - 1; i++) {
                        argmap[i] = i + 1;
                    }
                }
                MethodType newType = methodType.changeReturnType(wrapper.type().returnType());
                if (!isStringMethod) {
                    newType = newType.changeParameterType(0, wrapper.type().parameterType(0));
                }
                wrapper = MethodHandles.permuteArguments(wrapper, newType, argmap);
            } else {
                if (isFormatterMethod) {
                    wrapper = foldLocaleFromFormatter(wrapper, methodType.parameterCount());
                } else if (!useDirectConcat) {
                    wrapper = foldArguments(wrapper, methodType.parameterCount(), LOCALE_GETDEFAULT);
                }
            }
            if (isStringMethod) {
                wrapper = filterReturnValue(wrapper, useDirectConcat ? STRINGBUILDER_TOSTRING : APPENDABLE_TO_STRING);
            }
            return wrapper.asType(methodType);
        }
    }

    static class StringConcatHandleBuilder extends FormatHandleBuilder {

        MethodType concatType = methodType(String.class);
        StringBuilder recipe = new StringBuilder();

        List<Object> constants = new ArrayList<>();
        List<Integer> reorder = new ArrayList<>();
        List<MethodHandle> argumentFormatters = new ArrayList<>();

        boolean isPrimaryHandle;;
        boolean needsFormatter = false;
        boolean needsLocaleGuard = false;
        boolean useFastConcat = false;

        StringConcatHandleBuilder(List<FormatToken> specs, Class<?>[] argTypes, int[] argIndexes, boolean isPrimaryHandle) {
            super(specs, argTypes, argIndexes);
            this.isPrimaryHandle = isPrimaryHandle;
            if (USE_DIRECT_CONCAT && isPrimaryHandle) {
                for (int i = 0; i < specs.size(); i++) {
                    if (argIndexes[i] >= 0) {
                        useFastConcat = useFastConcat || canUseDirectConcat((FormatSpecifier) specs.get(i), argTypes[argIndexes[i]]);
                    }
                }
            }
        }

        @Override
        public void addArgumentMethodHandle(Class<?>[] argTypes, FormatSpecifier spec, int argIndex) {

            // Add argument token to recipe
            recipe.append('\1');

            Class<?> argType = argTypes[argIndex];
            boolean isConcat = useFastConcat && canUseDirectConcat(spec, argType);
            concatType = concatType.appendParameterTypes(isConcat ? argType : String.class);
            reorder.add(argIndex);

            if (!useFastConcat || !canUseDirectConcat(spec, argType)) {

                if (USE_SPECIALIZED_CONVERSION && canUseSpecialConverter(spec, argType)) {
                    // Direct handle requiring no formatter or localization
                    MethodHandle conversionFilter = getSpecializedConverter(spec, argType);
                    argumentFormatters.add(conversionFilter);

                } else {
                    // Use handle from java.util.Formatter with full localization support
                    MethodHandle combiner = getPrintHandle(argType, spec);
                    combiner = combiner.asType(combiner.type().changeParameterType(2, argType));
                    combiner = insertArguments(combiner, 1, spec);
                    combiner = filterReturnValue(combiner, TOSTRING_RESET);
                    argumentFormatters.add(combiner);
                    needsFormatter = true;
                }

            } else {
                if (spec.conversion() == FormatString.Conversion.DECIMAL_INTEGER) {
                    // Direct string concat, but we need to guard against locales requiring Unicode symbols
                    needsLocaleGuard = true;
                }
                argumentFormatters.add(null);
            }
        }

        @Override
        public void addConstantMethodHandle(Class<?>[] argTypes, FormatToken spec) {
            String value = getConstantSpecValue(spec);
            // '\1' and '\2' denote argument or constant to StringConcatFactory
            if (value.indexOf('\1') == -1 && value.indexOf('\2') == -1) {
                recipe.append(value);
            } else {
                recipe.append('\2');
                constants.add(value);
            }
        }

        @Override
        public void addMissingArgumentMethodHandle(Class<?>[] argTypes, FormatSpecifier spec) {
            MethodHandle thrower = throwException(void.class, MissingFormatArgumentException.class);
            thrower = foldArguments(thrower, insertArguments(CONSTRUCT_MISSING_FORMAT_ARGUMENT_EXCEPTION, 0, spec.toString()));
            argumentFormatters.add(thrower);
        }

        @Override
        public MethodHandle getHandle(MethodHandles.Lookup lookup, MethodType methodType, boolean hasLocaleArg,
                                      boolean isStringMethod, boolean isFormatterMethod) {

            buildHandles();

            CallSite cs;

            try {
                cs = StringConcatFactory.makeConcatWithConstants(lookup, "formatterBootstrap", concatType, recipe.toString(), constants.toArray());
            } catch (StringConcatException sce) {
                throw new RuntimeException(sce);
            }

            MethodHandle handle = dropArguments(cs.getTarget(), concatType.parameterCount(), Formatter.class, Locale.class);

            int paramIndex = 0;

            for (MethodHandle formatter : argumentFormatters) {

                if (formatter != null) {
                    int paramCount = formatter.type().parameterCount();
                    if (paramCount == 0) {
                        handle = foldArguments(handle, 0, formatter);
                    } else if (paramCount == 1) {
                        handle = filterArguments(handle, paramIndex, formatter);
                    } else {
                        Class<?> argType = formatter.type().parameterType(1);
                        MethodType newType = methodType(String.class, argType, Arrays.copyOfRange(handle.type().parameterArray(), paramIndex + 1, handle.type().parameterCount()));

                        formatter = permuteArguments(formatter, newType, newType.parameterCount() - 2, 0, newType.parameterCount() -1);
                        handle = dropArguments(handle, paramIndex + 1, argType);
                        handle = foldArguments(handle, paramIndex, formatter);
                    }
                }

                paramIndex++;
            }

            if (needsFormatter) {
                handle = foldArguments(handle, handle.type().parameterCount() - 2, CONSTRUCT_FORMATTER_LOCALE);
            } else {
                handle = insertArguments(handle, handle.type().parameterCount() - 2, (Object) null);
            }

            // move Locale argument from last to first position
            handle = moveArgToFront(handle, handle.type().parameterCount() - 1);

            if (!isPrimaryHandle) {
                return handle;
            }

            /* if (needsLocaleGuard) {
                // We have a decimal int without formatter - this doesn't work for
                // locales using unicode decimal symbols, so add a guard and fallback handle for that case
                Locale defaultLocale = Locale.getDefault(Locale.Category.FORMAT);
                if (defaultLocale == null || DecimalFormatSymbols.getInstance(defaultLocale).getZeroDigit() != '0') {
                    defaultLocale = Locale.US;
                }
                handle = MethodHandles.guardWithTest(
                        insertArguments(LOCALE_GUARD, 0, defaultLocale),
                        handle,
                        new StringConcatHandleBuilder(specs, argTypes, argIndexes, false)
                                .getHandle(methodType, hasLocaleArg, isStringMethod, isFormatterMethod));
            } */

            if (!hasLocaleArg && !isFormatterMethod) {
                handle = foldArguments(handle, 0, LOCALE_GETDEFAULT);
            }

            int[] reorderArray = hasLocaleArg || isFormatterMethod ?
                    // Leading Locale arg - add initial element to keep it in place and increase other values by 1
                    IntStream.concat(IntStream.of(0), reorder.stream().mapToInt(i -> i + 1)).toArray() :
                    reorder.stream().mapToInt(i -> i).toArray();

            MethodType mt = isStringMethod ? methodType : methodType.dropParameterTypes(0, 1).changeReturnType(String.class);
            if (!hasLocaleArg && isFormatterMethod) {
                mt = mt.insertParameterTypes(0, Locale.class);
            }
            handle =  MethodHandles.permuteArguments(handle, mt, reorderArray);

            if (!isStringMethod) {
                Class<?> type = methodType.returnType();

                MethodHandle identityHandle = dropArguments(identity(type), 1, handle.type().parameterArray());
                MethodHandle appenderHandle = isFormatterMethod ?
                        filterArguments(APPENDABLE_APPEND, 0, FORMATTER_OUT) : APPENDABLE_APPEND;
                appenderHandle = dropArguments(appenderHandle
                        .asType(methodType(void.class, type, String.class)), 2, handle.type().parameterArray());

                appenderHandle = foldArguments(appenderHandle, 1, handle);
                handle = foldArguments(identityHandle, 0, appenderHandle);
                if (isFormatterMethod && !hasLocaleArg) {
                    handle = foldLocaleFromFormatter(handle, 1);
                }
            }

            return handle;
        }
    }

    private static MethodHandle moveArgToFront(MethodHandle handle, int argIndex) {
        Class<?>[] paramTypes = handle.type().parameterArray();

        MethodType methodType = methodType(handle.type().returnType(), paramTypes[argIndex]);
        int[] reorder = new int[paramTypes.length];
        reorder[argIndex] = 0;

        for (int i = 0, j = 1; i < paramTypes.length; i++) {
            if (i != argIndex) {
                methodType = methodType.appendParameterTypes(paramTypes[i]);
                reorder[i] = j++;
            }
        }
        return permuteArguments(handle, methodType, reorder);
    }

    private static MethodHandle foldLocaleFromFormatter(MethodHandle handle, int localeArgIndex) {
        return foldArguments(moveArgToFront(handle, localeArgIndex), FORMATTER_LOCALE);
    }

    private static String getConstantSpecValue(Object o) {
        if (o instanceof FormatSpecifier) {
            FormatSpecifier spec = (FormatSpecifier) o;
            assert spec.index() == -2;
            if (spec.conversion() == FormatString.Conversion.LINE_SEPARATOR) {
                return System.lineSeparator();
            } else if (spec.conversion() == FormatString.Conversion.PERCENT_SIGN) {
                return String.format(spec.toString());
            }
        }
        return o.toString();
    }

    private static MethodHandle missingFormatArgumentThrower(String message, Class<?> returnType) {
        MethodHandle thrower = throwException(returnType, MissingFormatArgumentException.class);
        return foldArguments(thrower, insertArguments(CONSTRUCT_MISSING_FORMAT_ARGUMENT_EXCEPTION, 0, message));
    }

    private static MethodHandle unknownFormatConversionThrower(UnknownFormatConversionException unknownFormat, MethodType methodType) {
        MethodHandle thrower = throwException(methodType.returnType(), UnknownFormatConversionException.class);
        thrower = foldArguments(thrower, insertArguments(CONSTRUCT_UNKNOWN_FORMAT_CONVERSION_EXCEPTION, 0, unknownFormat.getConversion()));
        return dropArguments(thrower, 0, methodType.parameterArray());
    }

    private static MethodHandle illegalFormatConversionThrower(char conversion, Class<?> type, MethodType methodType) {
        MethodHandle thrower = throwException(methodType.returnType(), IllegalFormatConversionException.class);
        thrower = foldArguments(thrower, insertArguments(CONSTRUCT_ILLEGAL_FORMAT_CONVERSION_EXCEPTION, 0, conversion, type));
        thrower = dropArguments(thrower, 0, methodType.parameterArray());
        return thrower;
    }

    private static Appendable append(Appendable appendable, CharSequence cs) throws IOException {
        return appendable.append(cs);
    }

    private static String toStringReset(Formatter formatter) {
        String str = formatter.out().toString();
        ((StringBuilder) formatter.out()).setLength(0);
        return str;
    }

    private static boolean localeGuard(Locale locale1, Locale locale2) {
        return locale1 == locale2;
    }

    private static boolean booleanObjectFilter(Object arg) {
        return arg != null && (! (arg instanceof Boolean) || ((Boolean) arg));
    }

    private static boolean notNullTest(Object arg) {
        return arg != null;
    }


    private static MethodHandle getSpecializedConverter(FormatSpecifier spec, Class<?> argType) {
        MethodHandle conversionFilter;

        switch (spec.conversion()) {
            case HASHCODE:
                conversionFilter = filterArguments(INTEGER_TO_HEX_STRING, 0, OBJECT_HASHCODE);
                break;
            case DECIMAL_INTEGER:
                conversionFilter = argType == long.class ? LONG_TO_STRING : INT_TO_STRING;
                break;
            case HEXADECIMAL_INTEGER:
                conversionFilter =  argType == long.class ? LONG_TO_HEX_STRING : INTEGER_TO_HEX_STRING;
                break;
            case OCTAL_INTEGER:
                conversionFilter =  argType == long.class ? LONG_TO_OCTAL_STRING : INTEGER_TO_OCTAL_STRING;
                break;
            case BOOLEAN:
                conversionFilter = BOOLEAN_TO_STRING;
                break;
            default:
                throw new IllegalStateException("Unexpected conversion: " + spec.conversion());
        }

        if (conversionFilter.type().parameterType(0) != argType) {
            if (spec.conversion() == FormatString.Conversion.BOOLEAN)
                conversionFilter = filterArguments(conversionFilter, 0, BOOLEAN_OBJECT_FILTER);
            else if (! argType.isPrimitive())
                conversionFilter = guardWithTest(NOT_NULL_TEST,
                        conversionFilter.asType(methodType(String.class, Object.class)),
                        dropArguments(constant(String.class, "null"), 0, Object.class));
            conversionFilter = conversionFilter.asType(conversionFilter.type().changeParameterType(0, argType));
        }

        if (spec.flags() == FormatString.Flags.UPPERCASE) {
            conversionFilter = filterArguments(STRING_TO_UPPER_CASE,0, conversionFilter);
        }

        return conversionFilter;
    }

    private static boolean canUseSpecialConverter(FormatSpecifier spec, Class<?> argType) {
        return (spec.flags() == FormatString.Flags.NONE || spec.flags() == FormatString.Flags.UPPERCASE)
                && spec.width() == -1
                && !requiresLocalization(spec)
                && isSafeArgumentType(spec.conversion(), argType);
    }

    private static boolean canUseDirectConcat(FormatSpecifier spec, Class<?> argType) {
        if (spec.flags() == FormatString.Flags.NONE
                && spec.width() == -1
                && isSafeArgumentType(spec.conversion(), argType)) {
            switch (spec.conversion()) {
                case STRING:
                case BOOLEAN:
                case CHARACTER:
                case DECIMAL_INTEGER:
                case LINE_SEPARATOR:
                case PERCENT_SIGN:
                    return true;
            }
        }
        return false;
    }

    private static boolean isSafeArgumentType(FormatString.Conversion conversion, Class<?> type) {
        if (conversion == FormatString.Conversion.BOOLEAN) {
            return type == boolean.class || type == Boolean.class;
        }
        if (conversion == FormatString.Conversion.CHARACTER) {
            return type == char.class || type == Character.class;
        }
        if (conversion == FormatString.Conversion.DECIMAL_INTEGER
                || conversion == FormatString.Conversion.HEXADECIMAL_INTEGER
                || conversion == FormatString.Conversion.OCTAL_INTEGER) {
            return type == int.class || type == long.class || type == Integer.class;
        }
        if (conversion == FormatString.Conversion.HASHCODE) {
            return true;
        }
        // Limit to String to prevent us from doing toString() on a java.util.Formattable
        return conversion == FormatString.Conversion.STRING && type == String.class;
    }

    private static MethodHandle getAppenderHandle(Class<?> type) {
        if (type == boolean.class || type == Boolean.class) {
            return STRINGBUILDER_APPEND_BOOLEAN;
        } else if (type == char.class || type == Character.class) {
            return STRINGBUILDER_APPEND_CHAR;
        } else if (type == long.class || type == Long.class) {
            return STRINGBUILDER_APPEND_LONG;
        } else if (type == int.class || type == Integer.class) {
            return STRINGBUILDER_APPEND_INT;
        } else {
            return STRINGBUILDER_APPEND_STRING;
        }
    }


    private static boolean requiresLocalization(FormatSpecifier spec) {
        switch (spec.conversion()) {
            case BOOLEAN:
            case HEXADECIMAL_INTEGER:
            case OCTAL_INTEGER:
            case HASHCODE:
            case LINE_SEPARATOR:
            case PERCENT_SIGN:
                return false;
            default:
                return true;
        }
    }

    private static MethodHandle getPrintHandle(Class<?> argType, FormatSpecifier spec) {
        if (spec.conversion() == FormatString.Conversion.HASHCODE) {
            return SPECIFIER_PRINT_HASHCODE;
        } else if (argType == int.class) {
            return SPECIFIER_PRINT_INT;
        } else if (argType == long.class) {
            return SPECIFIER_PRINT_LONG;
        } else if (argType == byte.class) {
            return SPECIFIER_PRINT_BYTE;
        } else if (argType == short.class) {
            return SPECIFIER_PRINT_SHORT;
        } else if (argType == float.class) {
            return SPECIFIER_PRINT_FLOAT;
        } else if (argType == double.class) {
            return SPECIFIER_PRINT_DOUBLE;
        } else {
            return SPECIFIER_PRINT;
        }
    }



    private static MethodHandle fallbackMethodHandle(MethodHandles.Lookup lookup, String name,
                                                     MethodType methodType, String format, boolean isStringMethod,
                                                     boolean hasLocaleArg, boolean isVarArgs) {
        if (isStringMethod) {
            MethodHandle handle = findStaticMethodHandle(lookup, String.class, name,
                    hasLocaleArg ? methodType(String.class, Locale.class, String.class, Object[].class)
                                 : methodType(String.class, String.class, Object[].class));
            return wrapHandle(handle, hasLocaleArg ? 1 : 0, format, methodType, isVarArgs);
        }
        Class<?> type = methodType.parameterType(0);
        MethodHandle handle = findVirtualMethodHandle(lookup, type, name,
                hasLocaleArg ? methodType(type, Locale.class, String.class, Object[].class)
                             : methodType(type, String.class, Object[].class));
        return wrapHandle(handle, hasLocaleArg ? 2 : 1, format, methodType, isVarArgs);
    }

    private static MethodHandle wrapHandle(MethodHandle handle, int formatArgIndex, String format, MethodType methodType, boolean isVarArg) {
        MethodHandle h = MethodHandles.insertArguments(handle, formatArgIndex, format);
        if (!isVarArg) {
            h = h.asCollector(Object[].class, methodType.parameterCount() - formatArgIndex);
        }
        return h.asType(methodType);
    }

    private static boolean isVarArgsType(MethodType methodType, boolean isStringMethod, boolean hasLocaleArg) {
        int expectedArrayArgument = (isStringMethod ? 0 : 1) + (hasLocaleArg ? 1 : 0);
        return methodType.parameterCount() == expectedArrayArgument + 1
                && methodType.parameterType(expectedArrayArgument) == Object[].class;
    }

    private static MethodHandle findVirtualMethodHandle(MethodHandles.Lookup lookup, Class<?> type, String name, MethodType methodType) {
        try {
            return lookup.findVirtual(type, name, methodType);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static MethodHandle findStaticMethodHandle(MethodHandles.Lookup lookup, Class<?> type, String name, MethodType methodType) {
        try {
            return lookup.findStatic(type, name, methodType);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
