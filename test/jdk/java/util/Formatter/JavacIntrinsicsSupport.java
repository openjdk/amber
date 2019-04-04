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

import java.io.PrintStream;
import java.lang.invoke.CallSite;
import java.lang.invoke.IntrinsicFactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

public class JavacIntrinsicsSupport extends Basic {


    static void formatAndCheck(String fs, String exp, Locale l, Object... args) {
        // Invoke via formatterFormatBootstrap intrinsic
        Formatter f = new Formatter(new StringBuilder(), l);
        formatterFormat(f, fs, args);
        ck(fs, exp, f.toString());
        // Invoke via formatterLocaleFormatBootstrap intrinsic
        f = new Formatter(new StringBuilder());
        formatterLocaleFormat(f, l, fs, args);
        ck(fs, exp, f.toString());
        // Invoke again via stringLocaleFormatBootstrap instrinsic
        ck(fs, exp, stringLocaleFormat(fs, l, args));
    }

    static void formatterFormat(Formatter f, String fs, Object... args) {
        CallSite cs;
        List<Object> invokeArgs;
        try {
            cs = IntrinsicFactory.formatterFormatBootstrap(MethodHandles.lookup(), "format",
                    getFormatterFormatMethodType(false, args), fs);

            invokeArgs = new ArrayList<>();
            invokeArgs.add(f);
            if (args != null) {
                Collections.addAll(invokeArgs, args);
            } else {
                invokeArgs.add(null);
            }
        } catch (Throwable t) {
            throw new BootstrapMethodError(t);
        }
        try {
            cs.dynamicInvoker().invokeWithArguments(invokeArgs);
        } catch (RuntimeException r) {
            throw r;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    static void formatterLocaleFormat(Formatter f, Locale l, String fs, Object... args) {
        CallSite cs;
        List<Object> invokeArgs;
        try {
            cs = IntrinsicFactory.formatterLocaleFormatBootstrap(MethodHandles.lookup(), "format",
                    getFormatterFormatMethodType(true, args), fs);
            invokeArgs = new ArrayList<>();
            invokeArgs.add(f);
            invokeArgs.add(l);
            if (args != null) {
                Collections.addAll(invokeArgs, args);
            } else {
                invokeArgs.add(null);
            }
        } catch (Throwable t) {
            throw new BootstrapMethodError(t);
        }
        try {
            cs.dynamicInvoker().invokeWithArguments(invokeArgs);
        } catch (RuntimeException r) {
            throw r;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    static String stringFormat(String fs, Object... args) {
        CallSite cs;
        List<Object> invokeArgs;
        try {
            cs = IntrinsicFactory.staticStringFormatBootstrap(MethodHandles.lookup(), "format",
                    getStringFormatMethodType(false, args), fs);
            invokeArgs = new ArrayList<>();
            if (args != null) {
                Collections.addAll(invokeArgs, args);
            } else {
                invokeArgs.add(null);
            }
        } catch (Throwable t) {
            throw new BootstrapMethodError(t);
        }
        try {
            return (String) cs.dynamicInvoker().invokeWithArguments(invokeArgs);
        } catch (RuntimeException r) {
            throw r;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    static String stringLocaleFormat(String fs, Locale l, Object... args) {
        CallSite cs;
        List<Object> invokeArgs;
        try {
            cs = IntrinsicFactory.staticStringLocaleFormatBootstrap(MethodHandles.lookup(), "format",
                    getStringFormatMethodType(true, args), fs);
            invokeArgs = new ArrayList<>();
            invokeArgs.add(l);
            if (args != null) {
                Collections.addAll(invokeArgs, args);
            } else {
                invokeArgs.add(null);
            }
        } catch (Throwable t) {
            throw new BootstrapMethodError(t);
        }
        try {
            return (String) cs.dynamicInvoker().invokeWithArguments(invokeArgs);
        } catch (RuntimeException r) {
            throw r;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static MethodType getFormatterFormatMethodType(boolean hasLocale, Object... args) {
        MethodType mt = MethodType.methodType(Formatter.class, Formatter.class);
        if (hasLocale) {
            mt = mt.appendParameterTypes(Locale.class);
        }
        if (args != null) {
            for (Object arg : args) {
                mt = mt.appendParameterTypes(arg == null ? Object.class : arg.getClass());
            }
        } else {
            mt = mt.appendParameterTypes(Object[].class);
        }
        return mt;
    }

    private static MethodType getStringFormatMethodType(boolean hasLocale, Object... args) {
        MethodType mt = MethodType.methodType(String.class);
        if (hasLocale) {
            mt = mt.appendParameterTypes(Locale.class);
        }
        if (args != null) {
            for (Object arg : args) {
                mt = mt.appendParameterTypes(arg == null ? Object.class : arg.getClass());
            }
        } else {
            mt = mt.appendParameterTypes(Object[].class);
        }
        return mt;
    }

    private static MethodType getPrintStreamFormatMethodType(Object... args) {
        MethodType mt = MethodType.methodType(PrintStream.class, PrintStream.class, Locale.class);
        if (args != null) {
            for (Object arg : args) {
                mt = mt.appendParameterTypes(arg == null ? Object.class : arg.getClass());
            }
        } else {
            mt = mt.appendParameterTypes(Object[].class);
        }
        return mt;
    }
}
