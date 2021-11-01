/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.util;

import java.io.IOException;
import java.lang.invoke.*;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.Formatter.*;

/**
 * This package private class supports the construction of the {@link MethodHandle}
 * returned by {@Link Formatter#formatFactory}.
 */
final class FormatBuilder {
    private static final Lookup LOOKUP = MethodHandles.lookup();
    private static final Class<?> BUILDER = FormatBuilder.Builder.class;

    private final String format;
    private final Class<?>[] ptypes;

    FormatBuilder(String format, Class<?>[] ptypes) {
        this.format = format;
        this.ptypes = ptypes;
    }

    private static Class<?> mapType(Class<?> type) {
        return type.isPrimitive() || type == String.class ? type : Object.class;
    }

    private static MethodHandle findMethod(Class<?> cls, String name,
                                           Class<?> rType, Class<?>... ptypes) {
        MethodType methodType = MethodType.methodType(rType, Arrays.asList(ptypes));

        try {
            return LOOKUP.findVirtual(cls, name, methodType);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Missing method in " +
                    cls + ": " + name + " " + methodType);
        }
    }

    private static MethodHandle findStaticMethod(Class<?> cls, String name,
                                                 Class<?> rType, Class<?>... ptypes) {
        MethodType methodType = MethodType.methodType(rType, Arrays.asList(ptypes));

        try {
            return LOOKUP.findStatic(cls, name, methodType);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Missing method in " +
                    cls + ": " + name + " " + methodType);
        }
    }

    static final class Builder {
        final private StringBuilder sb;
        final private Formatter fmt;

        private Builder(Locale locale, int size) {
            this.sb = new StringBuilder(size);
            this.fmt = new Formatter(sb, locale);
        }

        static Builder create(Locale locale, int size) {
            Builder builder = new Builder(locale, size);
            return builder;
        }

        Builder print(FormatSpecifier fs, boolean arg) throws IOException {
            fs.print(fmt, (Object)arg, fmt.locale());
            return this;
        }

        Builder print(FormatSpecifier fs, byte arg) throws IOException {
            fs.print(fmt, (Object)arg, fmt.locale());
            return this;
        }

        Builder print(FormatSpecifier fs, short arg) throws IOException {
            fs.print(fmt, (Object)arg, fmt.locale());
            return this;
        }

        Builder print(FormatSpecifier fs, char arg) throws IOException {
            fs.print(fmt, (Object)arg, fmt.locale());
            return this;
        }

        Builder print(FormatSpecifier fs, int arg) throws IOException {
            fs.print(fmt, (Object)arg, fmt.locale());
            return this;
        }

        Builder print(FormatSpecifier fs, long arg) throws IOException {
            fs.print(fmt, (Object)arg, fmt.locale());
            return this;
        }

        Builder print(FormatSpecifier fs, float arg) throws IOException {
            fs.print(fmt, (Object)arg, fmt.locale());
            return this;
        }

        Builder print(FormatSpecifier fs, double arg) throws IOException {
            fs.print(fmt, (Object)arg, fmt.locale());
            return this;
        }

        Builder print(FormatSpecifier fs, String arg) throws IOException {
            fs.print(fmt, (Object)arg, fmt.locale());
            return this;
        }

        Builder print(FormatSpecifier fs, Object arg) throws IOException {
            fs.print(fmt, arg, fmt.locale());
            return this;
        }

        Builder printBoolean(FormatSpecifier fs, boolean arg) throws IOException {
            fs.print(fmt, arg, fmt.locale());
            return this;
        }

        Builder printInteger(FormatSpecifier fs, byte arg) throws IOException {
            fs.print(fmt, arg, fmt.locale());
            return this;
        }

        Builder printInteger(FormatSpecifier fs, short arg) throws IOException {
            fs.print(fmt, arg, fmt.locale());
            return this;
        }

        Builder printCharacter(FormatSpecifier fs, char arg) throws IOException {
            fs.print(fmt, arg, fmt.locale());
            return this;
        }

        Builder printInteger(FormatSpecifier fs, int arg) throws IOException {
            fs.print(fmt, arg, fmt.locale());
            return this;
        }

        Builder printInteger(FormatSpecifier fs, long arg) throws IOException {
            fs.print(fmt, arg, fmt.locale());
            return this;
        }

        Builder printFloat(FormatSpecifier fs, float arg) throws IOException {
            fs.print(fmt, arg, fmt.locale());
            return this;
        }

        Builder printFloat(FormatSpecifier fs, double arg) throws IOException {
            fs.print(fmt, arg, fmt.locale());
            return this;
        }


        Builder printIntegerSimple(FormatSpecifier fs, byte arg) throws IOException {
            if (Formatter.getZero(fmt.locale()) == '0') {
                sb.append(arg);
            } else {
                fs.print(fmt, arg, fmt.locale());
            }
            return this;
        }

        Builder printIntegerSimple(FormatSpecifier fs, short arg) throws IOException {
            if (Formatter.getZero(fmt.locale()) == '0') {
                sb.append(arg);
            } else {
                fs.print(fmt, arg, fmt.locale());
            }
            return this;
        }

        Builder printIntegerSimple(FormatSpecifier fs, int arg) throws IOException {
            if (Formatter.getZero(fmt.locale()) == '0') {
                sb.append(arg);
            } else {
                fs.print(fmt, arg, fmt.locale());
            }
            return this;
        }

        Builder printIntegerSimple(FormatSpecifier fs, long arg) throws IOException {
            if (Formatter.getZero(fmt.locale()) == '0') {
                sb.append(arg);
            } else {
                fs.print(fmt, arg, fmt.locale());
            }
            return this;
        }

        Builder printFloatSimple(FormatSpecifier fs, float arg) throws IOException {
            if (Formatter.getZero(fmt.locale()) == '0') {
                sb.append(arg);
            } else {
                fs.print(fmt, arg, fmt.locale());
            }
            return this;
        }

        Builder printFloatSimple(FormatSpecifier fs, double arg) throws IOException {
            if (Formatter.getZero(fmt.locale()) == '0') {
                sb.append(arg);
            } else {
                fs.print(fmt, arg, fmt.locale());
            }
            return this;
        }

        Builder percent() {
            sb.append('%');
            return this;
        }

        Builder lineSeparator() {
            sb.append(System.lineSeparator());
            return this;
        }

        Builder append(boolean arg) {
            sb.append(arg);
            return this;
        }

        Builder append(byte arg) {
            sb.append(arg);
            return this;
        }

        Builder append(short arg) {
            sb.append(arg);
            return this;
        }

        Builder append(int arg) {
            sb.append(arg);
            return this;
        }

        Builder append(long arg) {
            sb.append(arg);
            return this;
        }

        Builder append(float arg) {
            sb.append(arg);
            return this;
        }

        Builder append(double arg) {
            sb.append(arg);
            return this;
        }

        Builder append(String arg) {
            sb.append(arg);
            return this;
        }

        Builder append(Object arg) {
            sb.append(arg);
            return this;
        }

        @Override
        public String toString() {
            return sb.toString();
        }
    }

    private MethodHandle formatString(FormatString fs) {
        if (fs instanceof FormatSpecifier specifier) {
            switch (specifier.c) {
                case Conversion.LINE_SEPARATOR:
                    return findMethod(BUILDER, "lineSeparator", BUILDER);
                case Conversion.PERCENT_SIGN:
                    return findMethod(BUILDER, "percent", BUILDER);
                default:
                    throw new MissingFormatArgumentException(fs.toString());
            }
        } else {
            MethodHandle mh = findMethod(BUILDER, "append", BUILDER, String.class);

            return MethodHandles.insertArguments(mh, 1, fs.toString());
        }
    }

    private MethodHandle formatSpecifier(FormatSpecifier fs, Class<?> type) {
        MethodHandle mh = null;
        boolean isSimple = fs.isSimple();

        if (!fs.dt) {
            switch (fs.c) {
                case Conversion.DECIMAL_INTEGER:
                    if (type == int.class | type == long.class |
                            type == short.class | type == byte.class) {
                        mh = findMethod(BUILDER, isSimple ? "printIntegerSimple" : "printInteger",
                                BUILDER, FormatSpecifier.class, type);
                        mh = MethodHandles.insertArguments(mh, 1, fs);
                    }
                    break;
                case Conversion.OCTAL_INTEGER:
                case Conversion.HEXADECIMAL_INTEGER:
                    if (type == int.class | type == long.class |
                            type == short.class | type == byte.class) {
                        mh = findMethod(BUILDER, "printInteger", BUILDER, FormatSpecifier.class, type);
                        mh = MethodHandles.insertArguments(mh, 1, fs);
                    }
                    break;
                case Conversion.GENERAL:
                    if (type == double.class | type == float.class) {
                        mh = findMethod(BUILDER, isSimple ? "printFloat" : "printFloat",
                                BUILDER, FormatSpecifier.class, type);
                        mh = MethodHandles.insertArguments(mh, 1, fs);
                    }
                    break;
                case Conversion.SCIENTIFIC:
                case Conversion.DECIMAL_FLOAT:
                case Conversion.HEXADECIMAL_FLOAT:
                    if (type == double.class | type == float.class) {
                        mh = findMethod(BUILDER, "printFloat", BUILDER, FormatSpecifier.class, type);
                        mh = MethodHandles.insertArguments(mh, 1, fs);
                    }
                    break;
                case Conversion.CHARACTER:
                    if (type == char.class) {
                        if (isSimple) {
                            mh = findMethod(BUILDER, "append", BUILDER, mapType(type));
                        } else {
                            mh = findMethod(BUILDER, "printCharacter", BUILDER, FormatSpecifier.class, type);
                            mh = MethodHandles.insertArguments(mh, 1, fs);
                        }
                    }
                    break;
                case Conversion.BOOLEAN:
                    if (type == boolean.class) {
                        if (isSimple) {
                            mh = findMethod(BUILDER, "append", BUILDER, mapType(type));
                        } else {
                            mh = findMethod(BUILDER, "printBoolean", BUILDER, FormatSpecifier.class, type);
                            mh = MethodHandles.insertArguments(mh, 1, fs);
                        }
                    }
                    break;
                case Conversion.STRING:
                    if (isSimple) {
                        mh = findMethod(BUILDER, "append", BUILDER, mapType(type));
                    }
                    break;
                default:
                    break;
            }
        }

        if (mh == null) {
            mh = findMethod(BUILDER, "print", BUILDER, FormatSpecifier.class, mapType(type));
            mh = MethodHandles.insertArguments(mh, 1, fs);
        }

        return mh.asType(MethodType.methodType(BUILDER, BUILDER, type));
    }

    private boolean isSimple(FormatString f) {
        if (f instanceof FormatSpecifier fs) {
            if ((fs.c != Conversion.STRING &&
                    fs.c != Conversion.LINE_SEPARATOR &&
                    fs.c != Conversion.PERCENT_SIGN) ||
                    !fs.isSimple()) {
                return false;
            }
        }
        return true;
    }

    private boolean isSimple(Class<?> ptype) {
        return ptype.isPrimitive() || ptype == String.class;
    }

    private boolean isSimple(List<FormatString> fsa) {
        for (FormatString f : fsa) {
            if (!isSimple(f)) {
                return false;
            }
        }

        boolean isFirst = true;
        for (Class<?> ptype : ptypes) {
            if (isFirst) {
                isFirst = false;
                if (ptype == Locale.class) {
                    continue;
                }
            }

            if (!isSimple(ptype)) {
                return false;
            }
        }

        return true;
    }

    private int resultSize() {
        return format.length() * 4;
    }

    private MethodHandle buildConcat(boolean hasLocale, List<FormatString> fsa) {
        StringBuilder recipe = new StringBuilder();
        List<String> constants = new ArrayList<>();
        int nParam = ptypes.length;
        int iParam = hasLocale ? 1 : 0;

        for (FormatString fs : fsa) {
            int index = fs.index();

            switch (index) {
                case -2:  // fixed string, "%n", or "%%"
                    recipe.append('\2');
                    if (fs instanceof FormatSpecifier specifier) {
                        switch (specifier.c) {
                            case Conversion.LINE_SEPARATOR:
                                constants.add(System.lineSeparator());
                                break;
                            case Conversion.PERCENT_SIGN:
                                constants.add("%");
                                break;
                            default:
                                throw new MissingFormatArgumentException(fs.toString());
                        }
                    } else {
                        constants.add(fs.toString());
                    }
                    break;
                case 0:  // ordinary index
                    recipe.append('\1');
                    if (iParam < nParam) {
                        iParam++;
                    } else {
                        throw new MissingFormatArgumentException(fs.toString());
                    }
                    break;
                case -1:  // relative index
                default:  // explicit index
                    // TODO - Support indexing by permutating the arguments
                    throw new MissingFormatArgumentException(fs.toString());
            }
        }

        try {
            Class<?>[] types = hasLocale ? Arrays.copyOfRange(ptypes, 1, nParam) : ptypes;

            CallSite callsite = StringConcatFactory.makeConcatWithConstants(LOOKUP,
                    "format", MethodType.methodType(String.class, types),
                    recipe.toString(), constants.toArray());
            MethodHandle concat = callsite.getTarget();

            if (hasLocale) {
                concat = MethodHandles.dropArguments(concat, 0, Locale.class);
            }

            return concat;
        } catch(StringConcatException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private MethodHandle buildFormat(boolean hasLocale, List<FormatString> fsa) {
        MethodHandle mhComp;
        MethodHandle mh;
        int nParam = ptypes.length;
        int iParam = 0;
        mhComp = findStaticMethod(BUILDER, "create", BUILDER, Locale.class, int.class);

        if (hasLocale) {
            iParam++;
            mhComp = MethodHandles.insertArguments(mhComp, 1, resultSize());
        } else {
            Locale locale = Locale.getDefault(Locale.Category.FORMAT);
            mhComp = MethodHandles.insertArguments(mhComp, 0, locale, resultSize());
        }

        for (FormatString fs : fsa) {
            int index = fs.index();

            switch (index) {
                case -2:  // fixed string, "%n", or "%%"
                    mh = formatString(fs);
                    break;
                case 0:  // ordinary index
                    if (iParam < nParam) {
                        Class<?> ptype = ptypes[iParam++];
                        mh = formatSpecifier((FormatSpecifier)fs, ptype);
                    } else {
                        throw new MissingFormatArgumentException(fs.toString());
                    }
                    break;
                case -1:  // relative index
                default:  // explicit index
                    throw new MissingFormatArgumentException(fs.toString());
            }

            mhComp = MethodHandles.collectArguments(mh, 0, mhComp);
        }

        mh = findMethod(Object.class, "toString", String.class);
        mh = mh.asType(MethodType.methodType(String.class, BUILDER));
        mhComp = MethodHandles.collectArguments(mh, 0, mhComp);

        return mhComp;
    }

    MethodHandle build() {
        boolean hasLocale = 0 < ptypes.length && ptypes[0] == Locale.class;
        List<FormatString> fsa = Formatter.parse(format);
        boolean isSimple = isSimple(fsa);

        if (isSimple) {
            return  buildConcat(hasLocale, fsa);
        } else {
            return buildFormat(hasLocale, fsa);
        }
    }
}
