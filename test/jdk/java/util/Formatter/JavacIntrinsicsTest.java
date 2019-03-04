/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.FormatFlagsConversionMismatchException;
import java.util.Formattable;
import java.util.Formatter;
import java.util.IllegalFormatCodePointException;
import java.util.IllegalFormatConversionException;
import java.util.IllegalFormatPrecisionException;
import java.util.Locale;
import java.util.UnknownFormatConversionException;

/**
 * @test
 * @bug 8211062
 * @summary Various additional tests for javac formatter intrinsics
 * @run testng JavacIntrinsicsTest
 */
public class JavacIntrinsicsTest {

    @DataProvider(name = "locales")
    public Object[][] getLocales() {
        return new Object[][]{
                { Locale.US, "123" },
                { new Locale("ar","SA"), "١٢٣"},
                { Locale.forLanguageTag("th-TH-u-nu-thai"), "๑๒๓" }
        };
    }

    private static final Locale[] locales = { Locale.US, new Locale("ar","SA"), Locale.forLanguageTag("th-TH-u-nu-thai") };

    String format(Formatter formatter, String fmt, Object... args) {
        formatter.format(fmt, args);
        return formatter.toString();
    }


    String formatIntrinsic(Formatter formatter, String fmt, Object... args) {
        JavacIntrinsicsSupport.formatterFormat(formatter, fmt, args);
        return formatter.toString();
    }

    String stringFormat(String fmt, Object... args) {
        return String.format(fmt, args);
    }


    String stringFormatIntrinsic(String fmt, Object... args) {
        return JavacIntrinsicsSupport.stringFormat(fmt, args);
    }

    @Test
    public void illegalFormatStringTest() {
        try {
            formatIntrinsic(new Formatter(), "%c", Integer.MAX_VALUE);
            Assert.fail("expected IllegalFormatCodePointException");
        } catch (Exception x) {
            Assert.assertEquals(x.getClass(), IllegalFormatCodePointException.class);
        }
        try {
            formatIntrinsic(new Formatter(), "%c", Boolean.TRUE);
            Assert.fail("expected IllegalFormatConversionException");
        } catch (Exception x) {
            Assert.assertEquals(x.getClass(), IllegalFormatConversionException.class);
        }
        try {
            formatIntrinsic(new Formatter(), "%#c");
            Assert.fail("expected FormatFlagsConversionMismatchException");
        } catch (Exception x) {
            Assert.assertEquals(x.getClass(), FormatFlagsConversionMismatchException.class);
        }
        try {
            formatIntrinsic(new Formatter(), "%,c");
            Assert.fail("expected FormatFlagsConversionMismatchException");
        } catch (Exception x) {
            Assert.assertEquals(x.getClass(), FormatFlagsConversionMismatchException.class);
        }
        try {
            formatIntrinsic(new Formatter(), "%(c");
            Assert.fail("expected FormatFlagsConversionMismatchException");
        } catch (Exception x) {
            Assert.assertEquals(x.getClass(), FormatFlagsConversionMismatchException.class);
        }
        try {
            formatIntrinsic(new Formatter(), "%$c");
            Assert.fail("expected UnknownFormatConversionException");
        } catch (Exception x) {
            Assert.assertEquals(x.getClass(), UnknownFormatConversionException.class);
        }
        try {
            formatIntrinsic(new Formatter(), "%.2c");
            Assert.fail("expected IllegalFormatPrecisionException");
        } catch (Exception x) {
            Assert.assertEquals(x.getClass(), IllegalFormatPrecisionException.class);
        }
        try {
            formatIntrinsic(new Formatter(), "%#s", -3);
            Assert.fail("expected FormatFlagsConversionMismatchException");
        } catch (Exception x) {
            Assert.assertEquals(x.getClass(), FormatFlagsConversionMismatchException.class);
        }
        try {
            formatIntrinsic(new Formatter(), "%#d", -3);
            Assert.fail("expected FormatFlagsConversionMismatchException");
        } catch (Exception x) {
            Assert.assertEquals(x.getClass(), FormatFlagsConversionMismatchException.class);
        }
        try {
            formatIntrinsic(new Formatter(), "%#s", (byte) -3);
            Assert.fail("expected FormatFlagsConversionMismatchException");
        } catch (Exception x) {
            Assert.assertEquals(x.getClass(), FormatFlagsConversionMismatchException.class);
        }
        try {
            formatIntrinsic(new Formatter(), "%#d", (byte) -3);
            Assert.fail("expected FormatFlagsConversionMismatchException");
        } catch (Exception x) {
            Assert.assertEquals(x.getClass(), FormatFlagsConversionMismatchException.class);
        }
    }

    @Test
    public void paramTypesTest() {

        Assert.assertEquals(format(new Formatter(), "%s", -3), "-3");
        Assert.assertEquals(formatIntrinsic(new Formatter(), "%s", -3), "-3");
        Assert.assertEquals(stringFormat("%s", -3), "-3");
        Assert.assertEquals(stringFormatIntrinsic("%s", -3), "-3");

        Assert.assertEquals(format(new Formatter(), "%s", (byte) -3), "-3");
        Assert.assertEquals(formatIntrinsic(new Formatter(), "%s", (byte) -3), "-3");
        Assert.assertEquals(stringFormat("%s", (byte) -3), "-3");
        Assert.assertEquals(stringFormatIntrinsic("%s", (byte) -3), "-3");

        Assert.assertEquals(format(new Formatter(), "%s", (short) -3), "-3");
        Assert.assertEquals(formatIntrinsic(new Formatter(), "%s", (short) -3), "-3");
        Assert.assertEquals(stringFormat("%s", (short) -3), "-3");
        Assert.assertEquals(stringFormatIntrinsic("%s", (short) -3), "-3");

        Assert.assertEquals(format(new Formatter(), "%S", -3), "-3");
        Assert.assertEquals(formatIntrinsic(new Formatter(), "%S", -3), "-3");
        Assert.assertEquals(stringFormat("%S", -3), "-3");
        Assert.assertEquals(stringFormatIntrinsic("%S", -3), "-3");

        Assert.assertEquals(format(new Formatter(), "%S", (byte) -3), "-3");
        Assert.assertEquals(formatIntrinsic(new Formatter(), "%S", (byte) -3), "-3");
        Assert.assertEquals(stringFormat("%S", (byte) -3), "-3");
        Assert.assertEquals(stringFormatIntrinsic("%S", (byte) -3), "-3");

        Assert.assertEquals(format(new Formatter(), "%S", (short) -3), "-3");
        Assert.assertEquals(formatIntrinsic(new Formatter(), "%S", (short) -3), "-3");
        Assert.assertEquals(stringFormat("%S", (short) -3), "-3");
        Assert.assertEquals(stringFormatIntrinsic("%S", (short) -3), "-3");

        Assert.assertEquals(format(new Formatter(), "%d", -3), "-3");
        Assert.assertEquals(formatIntrinsic(new Formatter(), "%d", -3), "-3");
        Assert.assertEquals(stringFormat("%d", -3), "-3");
        Assert.assertEquals(stringFormatIntrinsic("%d", -3), "-3");

        Assert.assertEquals(format(new Formatter(), "%d", (byte) -3), "-3");
        Assert.assertEquals(formatIntrinsic(new Formatter(), "%d", (byte) -3), "-3");
        Assert.assertEquals(stringFormat("%d", (byte) -3), "-3");
        Assert.assertEquals(stringFormatIntrinsic("%d", (byte) -3), "-3");

        Assert.assertEquals(format(new Formatter(), "%d", (short) -3), "-3");
        Assert.assertEquals(formatIntrinsic(new Formatter(), "%d", (short) -3), "-3");
        Assert.assertEquals(stringFormat("%d", (short) -3), "-3");
        Assert.assertEquals(stringFormatIntrinsic("%d", (short) -3), "-3");

        Assert.assertEquals(format(new Formatter(), "%x", -3), "fffffffd");
        Assert.assertEquals(formatIntrinsic(new Formatter(), "%x", -3), "fffffffd");
        Assert.assertEquals(stringFormat("%x", -3), "fffffffd");
        Assert.assertEquals(stringFormatIntrinsic("%x", -3), "fffffffd");

        Assert.assertEquals(format(new Formatter(), "%x", (byte) -3), "fd");
        Assert.assertEquals(formatIntrinsic(new Formatter(), "%x", (byte) -3), "fd");
        Assert.assertEquals(stringFormat("%x", (byte) -3), "fd");
        Assert.assertEquals(stringFormatIntrinsic("%x", (byte) -3), "fd");

        Assert.assertEquals(format(new Formatter(), "%x", (short) -3), "fffd");
        Assert.assertEquals(formatIntrinsic(new Formatter(), "%x", (short) -3), "fffd");
        Assert.assertEquals(stringFormat("%x", (short) -3), "fffd");
        Assert.assertEquals(stringFormatIntrinsic("%x", (short) -3), "fffd");

        Assert.assertEquals(format(new Formatter(), "%X", -3), "FFFFFFFD");
        Assert.assertEquals(formatIntrinsic(new Formatter(), "%X", -3), "FFFFFFFD");
        Assert.assertEquals(stringFormat("%X", -3), "FFFFFFFD");
        Assert.assertEquals(stringFormatIntrinsic("%X", -3), "FFFFFFFD");

        Assert.assertEquals(format(new Formatter(), "%X", (byte) -3), "FD");
        Assert.assertEquals(formatIntrinsic(new Formatter(), "%X", (byte) -3), "FD");
        Assert.assertEquals(stringFormat("%X", (byte) -3), "FD");
        Assert.assertEquals(stringFormatIntrinsic("%X", (byte) -3), "FD");

        Assert.assertEquals(format(new Formatter(), "%X", (short) -3), "FFFD");
        Assert.assertEquals(formatIntrinsic(new Formatter(), "%X", (short) -3), "FFFD");
        Assert.assertEquals(stringFormat("%X", (short) -3), "FFFD");
        Assert.assertEquals(stringFormatIntrinsic("%X", (short) -3), "FFFD");

        Assert.assertEquals(format(new Formatter(), "%o", -3), "37777777775");
        Assert.assertEquals(formatIntrinsic(new Formatter(), "%o", -3), "37777777775");
        Assert.assertEquals(stringFormat("%o", -3), "37777777775");
        Assert.assertEquals(stringFormatIntrinsic("%o", -3), "37777777775");

        Assert.assertEquals(format(new Formatter(), "%o", (byte) -3), "375");
        Assert.assertEquals(formatIntrinsic(new Formatter(), "%o", (byte) -3), "375");
        Assert.assertEquals(stringFormat("%o", (byte) -3), "375");
        Assert.assertEquals(stringFormatIntrinsic("%o", (byte) -3), "375");

        Assert.assertEquals(format(new Formatter(), "%o", (short) -3), "177775");
        Assert.assertEquals(formatIntrinsic(new Formatter(), "%o", (short) -3), "177775");
        Assert.assertEquals(stringFormat("%o", (short) -3), "177775");
        Assert.assertEquals(stringFormatIntrinsic("%o", (short) -3), "177775");
    }

    // Empty specs test
    @Test
    public void emptySpecsTest() {
        Assert.assertEquals(format(new Formatter(), ""), "");
        Assert.assertEquals(formatIntrinsic(new Formatter(), ""), "");
        Assert.assertEquals(stringFormat(""), "");
        Assert.assertEquals(stringFormatIntrinsic(""), "");
    }

    // Locale test
    @Test(dataProvider = "locales")
    public void localeTest(Locale locale, String expected) {
        Locale defLocale = Locale.getDefault();
        try {
            Locale.setDefault(locale);
            Assert.assertEquals(format(new Formatter(), "%d", 123), expected);
            Assert.assertEquals(formatIntrinsic(new Formatter(), "%d", 123), expected);
            Assert.assertEquals(stringFormat("%d", 123), expected);
            Assert.assertEquals(stringFormatIntrinsic("%d", 123), expected);
        } finally {
            Locale.setDefault(defLocale);
        }
    }

    // Formattable tests
    @Test
    public void formattableTest() {
        Formatter fmt1 = new Formatter();
        try {
            format(fmt1, "%s%-#10.5S", "ok", new BrokenFormattable());
        } catch (Exception e) {

        }
        Formatter fmt2 = new Formatter();
        try {
            formatIntrinsic(fmt2, "%s%-#10.5S", "ok", new BrokenFormattable());
        } catch (Exception e) {

        }
        Assert.assertEquals(fmt1.toString(), "ok flags: 7, width: 10, precision: 5");
        Assert.assertEquals(fmt2.toString(), "ok flags: 7, width: 10, precision: 5");
    }

    // Exception tests
    @Test
    public void exceptionTest() {
        Formatter fmt1 = new Formatter();
        try {
            format(fmt1, "%s%s", "ok", new BrokenToString());
        } catch (Exception e) {

        }
        Formatter fmt2 = new Formatter();
        try {
            formatIntrinsic(fmt2, "%s%s", "ok", new BrokenToString());
        } catch (Exception e) {

        }
        Assert.assertEquals(fmt1.toString(), "ok");
        Assert.assertEquals(fmt2.toString(), "ok");
    }

    static class BrokenToString {
        @Override
        public String toString() {
            throw new RuntimeException();
        }
    }

    static class BrokenFormattable implements Formattable {
        @Override
        public void formatTo(Formatter formatter, int flags, int width, int precision) {
            try {
                formatter.out().append(" flags: " + flags).append(", width: " + width).append(", precision: " + precision);
            } catch (IOException x) {
                throw new RuntimeException(x);
            }
            throw new RuntimeException();
        }
    }
}
