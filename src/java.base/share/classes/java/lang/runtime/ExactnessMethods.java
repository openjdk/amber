/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
package java.lang.runtime;

/**
 * Exactness methods to test whether a conversion between types would be
 * exact when not enough static information is present. These methods may
 * be used, for example, by Java compiler implementations to implement checks
 * for instanceof and pattern matching runtime implementations.
 *
 * @since 21
 */
public class ExactnessMethods {

    private ExactnessMethods() { }
    
    /** Exactness method from byte to char
     *
     * @param n value
     * @return  true if the passed value can be converted exactly to the target type
     *
     * */
    public static boolean byte_char(byte n)    {return n == (char) n;}

    /** Exactness method from short to byte
     *
     * @param n value
     * @return  true if the passed value can be converted exactly to the target type
     *
     * */
    public static boolean short_byte(short n)  {return n == (short)(byte)(n);}
    
    /** Exactness method from short to char
     *
     * @param n value
     * @return  true if the passed value can be converted exactly to the target type
     *
     * */
    public static boolean short_char(short n)  {return n == (char)(n);}
    
    /** Exactness method from char to byte
     *
     * @param n value
     * @return  true if the passed value can be converted exactly to the target type
     *
     * */
    public static boolean char_byte(char n)    {return n == (byte)(n);}
    
    /** Exactness method from char to short
     *
     * @param n value
     * @return  true if the passed value can be converted exactly to the target type
     *
     * */
    public static boolean char_short(char n)   {return n == (short)(n);}

     /** Exactness method from int to byte
     *
     * @param n value
     * @return  true if the passed value can be converted exactly to the target type
     *
     * */
    public static boolean int_byte(int n)      {return n == (int)(byte)(n);}
    
    /** Exactness method from int to short
     *
     * @param n value
     * @return  true if the passed value can be converted exactly to the target type
     *
     * */
    public static boolean int_short(int n)     {return n == (int)(short)(n);}
    
    /** Exactness method from int to char
     *
     * @param n value
     * @return  true if the passed value can be converted exactly to the target type
     *
     * */
    public static boolean int_char(int n)      {return n == (char)(n);}

    /** Exactness method from int to float
     *
     * @param n value
     * @return  true if the passed value can be converted exactly to the target type
     *
     * */
    public static boolean int_float(int n) {
        if (n == Integer.MIN_VALUE)
            return true;
        n = Math.abs(n);
        return Float.PRECISION >= // 24
                (32 - (Integer.numberOfLeadingZeros(n) +
                        Integer.numberOfTrailingZeros(n))) ;
    }

    /** Exactness method from long to byte
     *
     * @param n value
     * @return  true if the passed value can be converted exactly to the target type
     *
     * */
    public static boolean long_byte(long n)    {return n == (long)(byte)(n);}
    
    /** Exactness method from long to short
     *
     * @param n value
     * @return  true if the passed value can be converted exactly to the target type
     *
     * */
    public static boolean long_short(long n)   {return n == (long)(short)(n);}
    
    /** Exactness method from long to char
     *
     * @param n value
     * @return  true if the passed value can be converted exactly to the target type
     *
     * */
    public static boolean long_char(long n)    {return n == (char)(n);}
    
    /** Exactness method from long to int
     *
     * @param n value
     * @return  true if the passed value can be converted exactly to the target type
     *
     * */
    public static boolean long_int(long n)     {return n == (long)(int)(n);}

    /** Exactness method from long to float
     *
     * @param n value
     * @return  true if the passed value can be converted exactly to the target type
     *
     * */
    public static boolean long_float(long n) {
        if (n == Long.MIN_VALUE)
            return true;
        n = Math.abs(n);
        return Float.PRECISION >= // 24
                (64 - (Long.numberOfLeadingZeros(n) +
                        Long.numberOfTrailingZeros(n))) ;
    }
    
    /** Exactness method from long to double
     *
     * @param n value
     * @return  true if the passed value can be converted exactly to the target type
     *
     * */
    public static boolean long_double(long n) {
        if (n == Long.MIN_VALUE)
            return true;
        n = Math.abs(n);
        return Double.PRECISION >= // 53
                (64 - (Long.numberOfLeadingZeros(n) +
                        Long.numberOfTrailingZeros(n))) ;
    }

    /** Exactness method from float to byte
     *
     * @param n value
     * @return  true if the passed value can be converted exactly to the target type
     *
     * */
    public static boolean float_byte(float n)  {return Float.compare(n, (float)(byte)(n)) == 0;}
    
    /** Exactness method from float to short
     *
     * @param n value
     * @return  true if the passed value can be converted exactly to the target type
     *
     * */
    public static boolean float_short(float n) {return Float.compare(n, (float)(short)(n)) == 0;}
    
    /** Exactness method from float to char
     *
     * @param n value
     * @return  true if the passed value can be converted exactly to the target type
     *
     * */
    public static boolean float_char(float n)  {return Float.compare(n, (float)(char)(n)) == 0;}
    
    /** Exactness method from float to int
     *
     * @param n value
     * @return  true if the passed value can be converted exactly to the target type
     *
     * */
    public static boolean float_int(float n) {
        return Double.compare((double)n, (double)((int)n)) == 0;
    }
    
    /** Exactness method from float to long
     *
     * @param n value
     * @return  true if the passed value can be converted exactly to the target type
     *
     * */
    public static boolean float_long(float n) {
        if (Float.compare(n, -0.0f) == 0 ||
                Float.compare(n, Float.NaN) == 0 ||
                Float.compare(n, Float.NEGATIVE_INFINITY) == 0 ||
                Float.compare(n, Float.POSITIVE_INFINITY) == 0) return false;
        return n == (long)n && n != (float)Long.MAX_VALUE + 1;
    }

    /** Exactness method from double to byte
     *
     * @param n value
     * @return  true if the passed value can be converted exactly to the target type
     *
     * */
    public static boolean double_byte(double n) {return Double.compare(n, (double)(byte)(n)) == 0;}
    
    /** Exactness method from double to short
     *
     * @param n value
     * @return  true if the passed value can be converted exactly to the target type
     *
     * */
    public static boolean double_short(double n){return Double.compare(n, (double)(short)(n)) == 0;}
    
    /** Exactness method from double to char
     *
     * @param n value
     * @return  true if the passed value can be converted exactly to the target type
     *
     * */
    public static boolean double_char(double n) {return Double.compare(n, (double)(char)(n)) == 0;}
    
    /** Exactness method from double to int
     *
     * @param n value
     * @return  true if the passed value can be converted exactly to the target type
     *
     * */
    public static boolean double_int(double n)  {return Double.compare(n, (double)(int)(n)) == 0;}
    
    /** Exactness method from double to long
     *
     * @param n value
     * @return  true if the passed value can be converted exactly to the target type
     *
     * */
    public static boolean double_long(double n) {
        if (Double.compare(n, -0.0f) == 0 ||
                Double.compare(n, Double.NaN) == 0 ||
                Double.compare(n, Double.NEGATIVE_INFINITY) == 0 ||
                Double.compare(n, Double.POSITIVE_INFINITY) == 0) return false;
        return n == (long)n && n != (double)Long.MAX_VALUE + 1;
    }
    
    /** Exactness method from double to float
     *
     * @param n value
     * @return  true if the passed value can be converted exactly to the target type
     *
     * */
    public static boolean double_float(double n) {return Double.compare(n, (double)(float)(n)) == 0;}
}
