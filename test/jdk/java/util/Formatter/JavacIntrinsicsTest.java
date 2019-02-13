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
import java.util.Formattable;
import java.util.Formatter;
import java.util.Locale;

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

    String format(Formatter formatter, String fmt, Object... args) throws IOException {
        formatter.format(fmt, args);
        return formatter.toString();
    }


    String formatIntrinsic(Formatter formatter, String fmt, Object... args) throws IOException {
        JavacIntrinsicsSupport.formatterFormat(formatter, fmt, args);
        return formatter.toString();
    }

    String stringFormat(String fmt, Object... args) throws IOException {
        return String.format(fmt, args);
    }


    String stringFormatIntrinsic(String fmt, Object... args) throws IOException {
        return JavacIntrinsicsSupport.stringFormat(fmt, args);
    }

    // Empty specs test
    @Test
    public void emptySpecsTest() throws IOException {
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
        } catch (IOException x) {
            throw new RuntimeException(x);
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
