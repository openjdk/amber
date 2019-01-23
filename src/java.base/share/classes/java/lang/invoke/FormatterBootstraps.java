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

import java.io.IOException;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.IllegalFormatConversionException;
import java.util.List;
import java.util.Locale;
import java.util.MissingFormatArgumentException;
import java.util.UnknownFormatConversionException;
import java.util.stream.IntStream;

import static java.lang.invoke.MethodHandles.Lookup.IMPL_LOOKUP;
import static java.lang.invoke.MethodHandles.constant;
import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodHandles.filterArgument;
import static java.lang.invoke.MethodHandles.filterReturnValue;
import static java.lang.invoke.MethodHandles.foldArguments;
import static java.lang.invoke.MethodHandles.foldArgumentsWithCombiner;
import static java.lang.invoke.MethodHandles.guardWithTest;
import static java.lang.invoke.MethodHandles.identity;
import static java.lang.invoke.MethodHandles.insertArguments;
import static java.lang.invoke.MethodHandles.throwException;
import static java.lang.invoke.MethodType.methodType;

/**
 * Bootstrapping support for Formatter intrinsics.
 */
public final class FormatterBootstraps {

    private static final MethodHandle APPENDABLE_APPEND =
            findVirtualMethodHandle(Appendable.class, "append", methodType(Appendable.class, CharSequence.class));

    private static final MethodHandle SPECIFIER_PRINT =
            findVirtualMethodHandle(Formatter.Specifier.class, "print",
                    methodType(Formatter.class, Formatter.class, Object.class, Locale.class));
    private static final MethodHandle SPECIFIER_PRINT_STRING =
            findVirtualMethodHandle("java.util.Formatter$FormatSpecifier", "print",
                    methodType(Formatter.class, Formatter.class, String.class, Locale.class));
    private static final MethodHandle SPECIFIER_PRINT_INT =
            findVirtualMethodHandle("java.util.Formatter$FormatSpecifier", "print",
                    methodType(Formatter.class, Formatter.class, int.class, Locale.class));
    private static final MethodHandle SPECIFIER_PRINT_LONG =
            findVirtualMethodHandle("java.util.Formatter$FormatSpecifier", "print",
                    methodType(Formatter.class, Formatter.class, long.class, Locale.class));
    private static final MethodHandle SPECIFIER_PRINT_BYTE =
            findVirtualMethodHandle("java.util.Formatter$FormatSpecifier", "print",
                    methodType(Formatter.class, Formatter.class, byte.class, Locale.class));
    private static final MethodHandle SPECIFIER_PRINT_SHORT =
            findVirtualMethodHandle("java.util.Formatter$FormatSpecifier", "print",
                    methodType(Formatter.class, Formatter.class, short.class, Locale.class));
    private static final MethodHandle SPECIFIER_PRINT_FLOAT =
            findVirtualMethodHandle("java.util.Formatter$FormatSpecifier", "print",
                    methodType(Formatter.class, Formatter.class, float.class, Locale.class));
    private static final MethodHandle SPECIFIER_PRINT_DOUBLE =
            findVirtualMethodHandle("java.util.Formatter$FormatSpecifier", "print",
                    methodType(Formatter.class, Formatter.class, double.class, Locale.class));
    private static final MethodHandle SPECIFIER_PRINT_HASHCODE =
            findVirtualMethodHandle("java.util.Formatter$FormatSpecifier", "printHashCode",
                    methodType(Formatter.class, Formatter.class, Object.class, Locale.class));

    private static final MethodHandle INT_TO_STRING =
            findStaticMethodHandle(Integer.class, "toString", methodType(String.class, int.class));
    private static final MethodHandle CONSTRUCT_FORMATTER =
            findConstructorMethodHandle(Formatter.class, methodType(void.class, Appendable.class))
                    .asType(methodType(Formatter.class, Appendable.class));
    private static final MethodHandle CONSTRUCT_FORMATTER_EMPTY =
            findConstructorMethodHandle(Formatter.class, methodType(void.class))
                    .asType(methodType(Formatter.class));
    private static final MethodHandle CONSTRUCT_FORMATTER_LOCALE =
            findConstructorMethodHandle(Formatter.class, methodType(void.class, Locale.class))
                    .asType(methodType(Formatter.class, Locale.class));

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

    private static final MethodHandle BOOLEAN_TO_STRING =
            findStaticMethodHandle(Boolean.class, "toString", methodType(String.class, boolean.class));
    private static final MethodHandle OBJECT_HASHCODE =
            findVirtualMethodHandle(Object.class, "hashCode", methodType(int.class));
    private static final MethodHandle INTEGER_TO_HEX_STRING =
            findStaticMethodHandle(Integer.class, "toHexString", methodType(String.class, int.class));
    private static final MethodHandle INTEGER_TO_OCTAL_STRING =
            findStaticMethodHandle(Integer.class, "toOctalString", methodType(String.class, int.class));
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
     * @param lookup         MethodHandles lookup
     * @param name           Name of method
     * @param methodType     Method signature
     * @param format         Formatter format string
     * @param isStringMethod Called from String method
     * @param hasLocaleArg   Has a Locale argument
     * @throws StringConcatException string concat error
     * @return Callsite for intrinsic method
     */
    public static CallSite formatterBootstrap(MethodHandles.Lookup lookup,
                                       String name,
                                       MethodType methodType,
                                       String format,
                                       boolean isStringMethod,
                                       boolean hasLocaleArg)
            throws StringConcatException {
        Formatter.CompiledFormat compiledFormat;

        try {
            compiledFormat = Formatter.compile(format);
        } catch (UnknownFormatConversionException unknownConversion) {
            return new ConstantCallSite(unknownFormatConversionThrower(unknownConversion, methodType));
        }

        List<Formatter.Specifier> specs = compiledFormat.specifiers();

        if (specs.isEmpty()) {
            return new ConstantCallSite(isStringMethod ?
                    constant(String.class, "").asType(methodType) :
                    identity(methodType.parameterType(0)).asType(methodType));
        }

        // Array of formatter args excluding target and locale
        Class<?>[] argTypes = methodType.dropParameterTypes(0, firstFormatterArg(isStringMethod, hasLocaleArg)).ptypes();
        // index array is needed because arg indexes are calculated forward but our method handle is composed backwards
        int[] argIndexes = calculateArgumentIndexes(specs, argTypes.length);
        boolean isFormatterMethod = methodType.parameterCount() > 0 && methodType.parameterType(0) == Formatter.class;

        if ("true".equals(System.getProperty("formatter.stringconcat", "true"))) {
            return makeStringConcatCallSite(lookup, name, methodType, specs, argTypes, argIndexes, hasLocaleArg, isStringMethod, isFormatterMethod);
        }

        return makeFormatterCallSite(methodType, specs, argTypes, argIndexes, hasLocaleArg, isStringMethod, isFormatterMethod);
    }

    private static CallSite makeStringConcatCallSite(MethodHandles.Lookup lookup, String name, MethodType methodType,
                                                     List<Formatter.Specifier> specs, Class<?>[] argTypes, int[] argIndexes,
                                                     boolean hasLocaleArg, boolean isStringMethod, boolean isFormatterMethod)
                                        throws StringConcatException {
        if (isStringMethod) {
            return new ConstantCallSite(
                    makeStringConcatHandle(lookup, name, methodType, specs, argTypes, argIndexes, hasLocaleArg, true));
        }

        if (!isFormatterMethod) {
            Class<?> type = methodType.returnType();
            MethodType formatterType = methodType.dropParameterTypes(0, 1).changeReturnType(String.class);
            MethodHandle formatterHandle = makeStringConcatHandle(lookup, name,
                    methodType(String.class, formatterType.ptypes()), specs, argTypes,
                    argIndexes, hasLocaleArg, true);

            MethodHandle identityHandle = dropArguments(identity(type), 1, formatterType.ptypes());
            MethodHandle appenderHandle = dropArguments(APPENDABLE_APPEND.asType(methodType(void.class, type, String.class)), 2, formatterType.ptypes());
            appenderHandle = foldArguments(appenderHandle, 1, formatterHandle);
            identityHandle = foldArguments(identityHandle, 0, appenderHandle);
            return new ConstantCallSite((identityHandle.asType(methodType)));
        }

        MethodType formatterType = methodType.dropParameterTypes(0, 1).changeReturnType(String.class);
        if (!hasLocaleArg) {
            formatterType = formatterType.insertParameterTypes(0, Locale.class);
        }
        MethodHandle formatterHandle = makeStringConcatHandle(lookup, name,
                methodType(String.class, formatterType.ptypes()), specs, argTypes,
                argIndexes, true, true);

        MethodHandle identityHandle = dropArguments(identity(Formatter.class), 1, formatterType.ptypes());
        MethodHandle appenderHandle = dropArguments(filterArgument(APPENDABLE_APPEND, 0, FORMATTER_OUT).asType(methodType(void.class, Formatter.class, String.class)), 2, formatterType.ptypes());
        appenderHandle = foldArguments(appenderHandle, 1, formatterHandle);
        identityHandle = foldArguments(identityHandle, 0, appenderHandle);
        if (!hasLocaleArg) {
            identityHandle = foldArgumentsWithCombiner(identityHandle, 1, FORMATTER_LOCALE, 0);
        }
        return new ConstantCallSite((identityHandle.asType(methodType)));
    }

    private static CallSite makeFormatterCallSite(MethodType methodType, List<Formatter.Specifier> specs,
                                                  Class<?>[] argTypes, int[] argIndexes, boolean hasLocaleArg,
                                                  boolean isStringMethod, boolean isFormatterMethod) {
        MethodHandle handle = null;

        // Reverse loop to compose method handle
        for (int i = specs.size() - 1; i >= 0; i--) {
            if (argIndexes[i] == -1) {
                handle = addConstantMethodHandle(handle, argTypes, specs.get(i));
            } else if (argIndexes[i] == MISSING_ARGUMENT_INDEX) {
                handle = addMissingArgumentMethodHandle(handle, argTypes, specs.get(i));
            } else {
                handle = addMethodHandle(handle, argTypes, specs.get(i), argIndexes[i]);
            }
        }

        handle = wrapFormatter(handle, methodType, hasLocaleArg, isStringMethod, isFormatterMethod);

        return new ConstantCallSite(handle.asType(methodType));
    }

    private static int firstFormatterArg(boolean isStringMethod, boolean hasLocaleArg) {
        int index = isStringMethod ? 0 : 1;
        return hasLocaleArg ? index + 1 : index;
    }

    private static int[] calculateArgumentIndexes(List<Formatter.Specifier> specs, int argCount) {
        int[] argIndexes = new int[specs.size()];
        int last = -1;
        int lasto = -1;

        // Forward loop to calculate indices and throw exceptions for missing arguments
        for (int i = 0; i < specs.size(); i++) {
            Formatter.Specifier spec = specs.get(i);
            int index = spec.index();
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


    private static MethodHandle findVirtualMethodHandle(String className, String methodName, MethodType methodType) {
        try {
            return findVirtualMethodHandle(Class.forName(className), methodName, methodType);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
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

    private static MethodHandle addMethodHandle(MethodHandle handle, Class<?>[] argTypes, Formatter.Specifier spec, int index) {

        MethodHandle appender;

        if (spec.hasEmptyFlags() && !requiresLocalization(spec) && argTypes[index].isPrimitive()) {
            MethodHandle conversionFilter = getDirectConversionFilter(spec, argTypes[index]);
            appender = filterArgument(SPECIFIER_PRINT_STRING, 2, conversionFilter);

        } else {
            appender = getPrintHandle(argTypes[index], spec);
        }

        appender = insertArguments(appender, 0, spec);
        appender = appender.asType(appender.type().changeParameterType(1, argTypes[index]));

        if (handle == null) {
            if (index > 0) {
                appender = dropArguments(appender, 1, Arrays.copyOfRange(argTypes, 0, index));
            }
            if (index < argTypes.length - 1) {
                appender = dropArguments(appender, index + 2, Arrays.copyOfRange(argTypes, index + 1, argTypes.length));
            }
            return appender;
        }

        appender = appender.asType(appender.type().changeReturnType(void.class));
        return foldArgumentsWithCombiner(handle, 0, appender,
                0, index + 1, argTypes.length + 1);
    }


    private static MethodHandle addMissingArgumentMethodHandle(MethodHandle handle, Class<?>[] argTypes, Formatter.Specifier spec) {
        MethodHandle thrower = missingFormatArgumentThrower(spec.toString());
        if (handle == null) {
            thrower = dropArguments(thrower, 0, Formatter.class);
            thrower = dropArguments(thrower, 1, argTypes);
            thrower = dropArguments(thrower, thrower.type().parameterCount(), Locale.class);
            return thrower;
        }

        thrower = thrower.asType(thrower.type().changeReturnType(void.class));

        return foldArguments(handle, 0, thrower);
    }


    private static MethodHandle addConstantMethodHandle(MethodHandle handle, Class<?>[] argTypes, Formatter.Specifier spec) {
        MethodHandle appender = insertArguments(SPECIFIER_PRINT, 0, spec);
        appender = insertArguments(appender, 1, (Object) null);

        if (handle == null) {
            return dropArguments(appender, 1, Arrays.copyOfRange(argTypes, 0, argTypes.length));
        }
        return foldArgumentsWithCombiner(handle, 0, appender.asType(appender.type().changeReturnType(void.class)),
                0, argTypes.length + 1);
    }


    private static MethodHandle wrapFormatter(MethodHandle handle, MethodType methodType, boolean hasLocaleArg,
                                              boolean isStringMethod, boolean isFormatterMethod) {
        MethodHandle wrapper;
        if (isFormatterMethod) {
            wrapper = handle;
        } else {
            if (isStringMethod) {
                wrapper = foldArguments(handle, 0, CONSTRUCT_FORMATTER_EMPTY);
            } else {
                wrapper = filterArgument(handle, 0, CONSTRUCT_FORMATTER);
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
                wrapper = foldArgumentsWithCombiner(wrapper, methodType.parameterCount(), FORMATTER_LOCALE, 0);
            } else {
                wrapper = foldArguments(wrapper, methodType.parameterCount(), LOCALE_GETDEFAULT);
            }
        }
        return isStringMethod ? filterReturnValue(wrapper, APPENDABLE_TO_STRING) : wrapper;
    }

    private static MethodHandle missingFormatArgumentThrower(String message) {
        MethodHandle thrower = throwException(Appendable.class, MissingFormatArgumentException.class);
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

    private static MethodHandle makeStringConcatHandle(MethodHandles.Lookup lookup, String name, MethodType callsiteType,
                                                     List<Formatter.Specifier> specs, Class<?>[] argTypes, int[] argIndexes,
                                                     boolean hasLocaleArg, boolean useConcat) throws StringConcatException {
        List<Object> constants = new ArrayList<>();
        List<Integer> reorder = new ArrayList<>();
        MethodType concatType = methodType(String.class);
        StringBuilder recipe = new StringBuilder();
        boolean needsFormatter = false;
        boolean needsLocaleGuard = false;

        for (int i = 0; i < argIndexes.length; i++) {
            Formatter.Specifier spec = specs.get(i);
            if (argIndexes[i] == -1) {
                char conversion = spec.conversion();
                if (conversion == 'n') {
                    recipe.append(System.lineSeparator());
                } else if (conversion == '%') {
                    recipe.append(String.format(spec.toString()));
                } else {
                    String str = spec.toString();
                    if (str.length() == 1 && str.charAt(0) > 1) {
                        recipe.append(str);
                    } else if (str.length() > 1) {
                        recipe.append('\2');
                        constants.add(spec.toString());
                    }
                }
            } else if (argIndexes[i] != MISSING_ARGUMENT_INDEX) {
                recipe.append('\1');
                int index = argIndexes[i];
                boolean isConcat = useConcat && useStringConcat(spec, argTypes[index]);
                concatType = concatType.appendParameterTypes(isConcat ? argTypes[index] : String.class);
                reorder.add(index);
            }
        }


        CallSite cs = StringConcatFactory.makeConcatWithConstants(lookup, name, concatType, recipe.toString(), constants.toArray());

        MethodHandle handle = dropArguments(cs.getTarget(), 0, Formatter.class, Locale.class);
        int paramIndex = 2;

        for (int i = 0; i < argIndexes.length; i++) {
            if (argIndexes[i] == -1) {
                continue;
            }

            if (argIndexes[i] == MISSING_ARGUMENT_INDEX) {
                MethodHandle thrower = throwException(void.class, MissingFormatArgumentException.class);
                thrower = foldArguments(thrower, insertArguments(CONSTRUCT_MISSING_FORMAT_ARGUMENT_EXCEPTION, 0, specs.get(i).toString()));
                handle = foldArguments(handle, 3, thrower);
            } else {
                Formatter.Specifier spec = specs.get(i);
                Class<?> argType = argTypes[argIndexes[i]];

                if (!useConcat || !useStringConcat(spec, argType)) {

                    if (spec.hasEmptyFlags() && !requiresLocalization(spec) && argType.isPrimitive()) {
                        // Direct handle requiring no formatter or localization
                        MethodHandle conversionFilter = getDirectConversionFilter(spec, argType);
                        handle = filterArgument(handle, paramIndex, conversionFilter);

                    } else {
                        // Use handle from java.util.Formatter with full localization support
                        MethodHandle combiner = getPrintHandle(argType, spec);
                        combiner = combiner.asType(combiner.type().changeParameterType(2, argType));
                        combiner = insertArguments(combiner, 0, specs.get(i));
                        combiner = filterReturnValue(combiner, TOSTRING_RESET);
                        handle = dropArguments(handle, paramIndex, argType);
                        handle = foldArgumentsWithCombiner(handle, paramIndex + 1, combiner, 0, paramIndex, 1);
                        needsFormatter = true;
                    }

                } else if (spec.conversion() == 'd') {
                    // Direct string concat, but we need to guard against locales requiring Unicode symbols
                    needsLocaleGuard = true;
                }
            }
            paramIndex++;

        }

        if (needsFormatter) {
            handle = foldArguments(handle, 0, CONSTRUCT_FORMATTER_LOCALE);
        } else {
            handle = insertArguments(handle, 0, (Object) null);
        }

        if (!useConcat) {
            return handle;
        }

        if (!needsFormatter && needsLocaleGuard) {
            // We have a decimal int without formatter - this doesn't work for
            // locales using unicode decimal symbols, so add a guard and fallback handle for that case
            Locale defaultLocale = Locale.getDefault(Locale.Category.FORMAT);
            if (defaultLocale == null || DecimalFormatSymbols.getInstance(defaultLocale).getZeroDigit() != '0') {
                defaultLocale = Locale.US;
            }
            handle = MethodHandles.guardWithTest(
                    insertArguments(LOCALE_GUARD, 0, defaultLocale),
                    handle,
                    makeStringConcatHandle(lookup, name, callsiteType, specs, argTypes, argIndexes, hasLocaleArg, false));
        }

        if (!hasLocaleArg) {
            handle = foldArguments(handle, 0, LOCALE_GETDEFAULT);
        }

        int[] reorderArray = hasLocaleArg ?
                // Leading Locale arg - add initial element to keep it in place and increase other values by 1
                IntStream.concat(IntStream.of(0), reorder.stream().mapToInt(i -> i + 1)).toArray() :
                reorder.stream().mapToInt(i -> i).toArray();


        return MethodHandles.permuteArguments(handle, callsiteType, reorderArray);
    }

    private static MethodHandle getDirectConversionFilter(Formatter.Specifier spec, Class<?> argType) {
        MethodHandle conversionFilter;

        switch (spec.conversion()) {
            case 'h':
                conversionFilter = filterArgument(INTEGER_TO_HEX_STRING, 0, OBJECT_HASHCODE);
                break;
            case 'd':
                conversionFilter = INT_TO_STRING;
                break;
            case 'x':
                conversionFilter = INTEGER_TO_HEX_STRING;
                break;
            case 'o':
                conversionFilter = INTEGER_TO_OCTAL_STRING;
                break;
            case 'b':
                conversionFilter = BOOLEAN_TO_STRING;
                break;
            default:
                throw new IllegalStateException("Unexpected conversion: " + spec.conversion());
        }

        if (conversionFilter.type().parameterType(0) != argType) {
            if (spec.conversion() == 'b')
                conversionFilter = filterArgument(conversionFilter, 0, BOOLEAN_OBJECT_FILTER);
            else if (! argType.isPrimitive())
                conversionFilter = guardWithTest(NOT_NULL_TEST,
                        conversionFilter.asType(methodType(String.class, Object.class)),
                        dropArguments(constant(String.class, "null"), 0, Object.class));
            conversionFilter = conversionFilter.asType(conversionFilter.type().changeParameterType(0, argType));
        }

        if (spec.isUpperCase()) {
            conversionFilter = filterArgument(STRING_TO_UPPER_CASE,0, conversionFilter);
        }

        return conversionFilter;
    }

    private static boolean isSafeArgumentType(char conversion, Class<?> type) {
        if (conversion == 'd') {
            return type == int.class || type == long.class || type == short.class || type == byte.class ||
                    type == Integer.class || type == Long.class || type == Short.class || type == Byte.class;
        }
        return true;
    }

    private static boolean useStringConcat(Formatter.Specifier spec, Class<?> argType) {
        return spec.hasEmptyFlags()
                && !spec.isUpperCase()
                && !spec.isDateTime()
                && "sdn%".indexOf(spec.conversion()) >= 0
                && isSafeArgumentType(spec.conversion(), argType);
    }

    private static boolean requiresLocalization(Formatter.Specifier spec) {
        return spec.isDateTime() || "bxon%".indexOf(spec.conversion()) < 0;
    }

    private static MethodHandle getPrintHandle(Class<?> argType, Formatter.Specifier spec) {
        if (spec.conversion() == 'h' && !spec.isDateTime()) {
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

}
