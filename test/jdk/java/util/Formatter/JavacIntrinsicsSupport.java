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
import java.lang.invoke.FormatterBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

public class JavacIntrinsicsSupport extends Basic {


    static void formatAndCheck(String fs, String exp, Locale l, Object... args) {
        try {

            CallSite cs = FormatterBootstraps.formatterBootstrap(MethodHandles.lookup(), "format",
                    getFormatterFormatMethodType(args), fs, false, false);
            Formatter f = new Formatter(new StringBuilder(), l);
            List<Object> invokeArgs = new ArrayList<>();
            invokeArgs.add(f);
            if (args != null) {
                Collections.addAll(invokeArgs, args);
            } else {
                invokeArgs.add(null);
            }
            cs.dynamicInvoker().invokeWithArguments(invokeArgs);
            ck(fs, exp, f.toString());

        } catch (RuntimeException rt) {
            throw rt;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    static void format(Formatter f, String fs, Object... args) {
        try {

            CallSite cs = FormatterBootstraps.formatterBootstrap(MethodHandles.lookup(), "format",
                    getFormatterFormatMethodType(args), fs, false, false);
            List<Object> invokeArgs = new ArrayList<>();
            invokeArgs.add(f);
            if (args != null) {
                Collections.addAll(invokeArgs, args);
            } else {
                invokeArgs.add(null);
            }
            cs.dynamicInvoker().invokeWithArguments(invokeArgs);

        } catch (RuntimeException rt) {
            throw rt;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    static String stringFormat(String fs, Object... args) {
        try {

            CallSite cs = FormatterBootstraps.formatterBootstrap(MethodHandles.lookup(), "format",
                    getStringFormatMethodType(args), fs, true, false);
            List<Object> invokeArgs = new ArrayList<>();
            if (args != null) {
                Collections.addAll(invokeArgs, args);
            } else {
                invokeArgs.add(null);
            }
            return (String) cs.dynamicInvoker().invokeWithArguments(invokeArgs);

        } catch (RuntimeException rt) {
            throw rt;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    static PrintStream printStreamFormat(PrintStream ps, Locale l, String fs, Object... args) {
        try {

            CallSite cs = FormatterBootstraps.formatterBootstrap(MethodHandles.lookup(), "format",
                    getPrintStreamFormatMethodType(args), fs, false, true);
            List<Object> invokeArgs = new ArrayList<>();
            invokeArgs.add(ps);
            invokeArgs.add(l);
            if (args != null) {
                Collections.addAll(invokeArgs, args);
            } else {
                invokeArgs.add(null);
            }
            return (PrintStream) cs.dynamicInvoker().invokeWithArguments(invokeArgs);

        } catch (RuntimeException rt) {
            throw rt;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static MethodType getFormatterFormatMethodType(Object... args) {
        MethodType mt = MethodType.methodType(Formatter.class, Formatter.class);
        if (args != null) {
            for (Object arg : args) {
                mt = mt.appendParameterTypes(arg == null ? Object.class : arg.getClass());
            }
        } else {
            mt = mt.appendParameterTypes(Object[].class);
        }
        return mt;
    }

    private static MethodType getStringFormatMethodType(Object... args) {
        MethodType mt = MethodType.methodType(String.class);
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
