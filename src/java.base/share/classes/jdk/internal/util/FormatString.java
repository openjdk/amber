package jdk.internal.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.DuplicateFormatFlagsException;
import java.util.FormatFlagsConversionMismatchException;
import java.util.Formatter;
import java.util.IllegalFormatConversionException;
import java.util.IllegalFormatFlagsException;
import java.util.IllegalFormatPrecisionException;
import java.util.IllegalFormatWidthException;
import java.util.List;
import java.util.MissingFormatWidthException;
import java.util.UnknownFormatConversionException;
import java.util.UnknownFormatFlagsException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides functionality for parsing and verifying format strings used by
 * {@link java.util.Formatter}. The code was extracted from {@code Formatter} and moved
 * to this class to be accessible from {@link java.lang.invoke.FormatterBootstraps}.
 */
public class FormatString {

    // %[argument_index$][flags][width][.precision][t]conversion
    private static final String formatSpecifier
            = "%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])";

    private static Pattern fsPattern = Pattern.compile(formatSpecifier);

    /**
     * Creates a parsed format list.
     * @param format to parse
     */
    public static List<FormatToken> parse(String format) {
        ArrayList<FormatToken> al = new ArrayList<>();
        Matcher m = fsPattern.matcher(format);
        for (int i = 0, len = format.length(); i < len; ) {
            if (m.find(i)) {
                // Anything between the start of the string and the beginning
                // of the format specifier is either fixed text or contains
                // an invalid format string.
                if (m.start() != i) {
                    // Make sure we didn't miss any invalid format specifiers
                    checkText(format, i, m.start());
                    // Assume previous characters were fixed text
                    al.add(new FixedString(format, i, m.start()));
                }

                al.add(new FormatSpecifier(format, m));
                i = m.end();
            } else {
                // No more valid format specifiers.  Check for possible invalid
                // format specifiers.
                checkText(format, i, len);
                // The rest of the string is fixed text
                al.add(new FixedString(format, i, format.length()));
                break;
            }
        }
        return al;
    }

    private static void checkText(String s, int start, int end) {
        for (int i = start; i < end; i++) {
            // Any '%' found in the region starts an invalid format specifier.
            if (s.charAt(i) == '%') {
                char c = (i == end - 1) ? '%' : s.charAt(i + 1);
                throw new UnknownFormatConversionException(String.valueOf(c));
            }
        }
    }

    /**
     * Interface for Formatter specifiers.
     */
    public interface FormatToken {

        /**
         * Return the specifier index.
         * @return the index
         */
        int index();

    }

    public static class FixedString implements FormatToken {
        private String s;
        private int start;
        private int end;

        FixedString(String s, int start, int end) {
            this.s = s;
            this.start = start;
            this.end = end;
        }

        public int index() {
            return -2;
        }

        public Formatter print(Formatter formatter) throws IOException {
            formatter.out().append(s, start, end);
            return formatter;
        }

        public String toString() {
            return s.substring(start, end);
        }
    }

    public static class FormatSpecifier implements FormatToken {

        private int index = -1;
        private int f = Flags.NONE;
        private int width;
        private int precision;
        private Conversion conversion;
        private DateTime dateTime;

        FormatSpecifier(String s, Matcher m) {
            index(s, m.start(1), m.end(1));
            flags(s, m.start(2), m.end(2));
            width(s, m.start(3), m.end(3));
            precision(s, m.start(4), m.end(4));

            int tTStart = m.start(5);
            if (tTStart >= 0) {
                conversion(s.charAt(tTStart));
                dateTime = DateTime.lookup(s.charAt(m.start(6)));
            } else {
                conversion(s.charAt(m.start(6)));
            }
            checkConversion();
        }

        private void index(String s, int start, int end) {
            if (start >= 0) {
                try {
                    // skip the trailing '$'
                    index = Integer.parseInt(s, start, end - 1, 10);
                } catch (NumberFormatException x) {
                    assert (false);
                }
            } else {
                index = 0;
            }
        }

        private void flags(String s, int start, int end) {
            f = Flags.parse(s, start, end);
            if (Flags.contains(f, Flags.PREVIOUS))
                index = -1;
        }

        private void width(String s, int start, int end) {
            width = -1;
            if (start >= 0) {
                try {
                    width = Integer.parseInt(s, start, end, 10);
                    if (width < 0)
                        throw new IllegalFormatWidthException(width);
                } catch (NumberFormatException x) {
                    assert (false);
                }
            }
        }

        private void precision(String s, int start, int end) {
            precision = -1;
            if (start >= 0) {
                try {
                    // skip the leading '.'
                    precision = Integer.parseInt(s, start + 1, end, 10);
                    if (precision < 0)
                        throw new IllegalFormatPrecisionException(precision);
                } catch (NumberFormatException x) {
                    assert (false);
                }
            }
        }

        private void conversion(char conv) {
            conversion = Conversion.lookup(conv);
            if (Character.isUpperCase(conv)) {
                f = Flags.add(f, Flags.UPPERCASE);
                conversion = Conversion.lookup(Character.toLowerCase(conv));
            }
            if (Conversion.isText(conversion)) {
                index = -2;
            }
        }

        public int index() {
            return index;
        }

        public String value() {
            return toString();
        }

        public Conversion conversion() {
            return conversion;
        }

        public DateTime dateTime() {
            return dateTime;
        }

        public int flags() {
            return f;
        }

        public int width() {
            return width;
        }

        public int precision() {
            return precision;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("%");
            // Flags.UPPERCASE is set internally for legal conversions.
            sb.append(Flags.toString(Flags.remove(f, Flags.UPPERCASE)));
            if (index > 0)
                sb.append(index).append('$');
            if (width != -1)
                sb.append(width);
            if (precision != -1)
                sb.append('.').append(precision);
            sb.append(Flags.contains(f, Flags.UPPERCASE)
                    ? Character.toUpperCase(conversion.c) : conversion.c);
            if (dateTime != null)
                sb.append(dateTime.c);
            return sb.toString();
        }

        private void checkConversion() {
            switch (conversion) {

                // Conversions applicable to all objects.
                case BOOLEAN:
                case BOOLEAN_UPPER:
                case STRING:
                case STRING_UPPER:
                case HASHCODE:
                case HASHCODE_UPPER:
                    checkGeneral();
                    break;

                // Conversions applicable to date objects.
                case DATE_TIME:
                case DATE_TIME_UPPER:
                    checkDateTime();
                    break;

                // Conversions applicable to character.
                case CHARACTER:
                case CHARACTER_UPPER:
                    checkCharacter();
                    break;

                // Conversions applicable to integer types.
                case DECIMAL_INTEGER:
                case OCTAL_INTEGER:
                case HEXADECIMAL_INTEGER:
                case HEXADECIMAL_INTEGER_UPPER:
                    checkInteger();
                    break;

                // Conversions applicable to floating-point types.
                case SCIENTIFIC:
                case SCIENTIFIC_UPPER:
                case GENERAL:
                case GENERAL_UPPER:
                case DECIMAL_FLOAT:
                case HEXADECIMAL_FLOAT:
                case HEXADECIMAL_FLOAT_UPPER:
                    checkFloat();
                    break;

                // Conversions that do not require an argument
                case LINE_SEPARATOR:
                case PERCENT_SIGN:
                    checkText();
                    break;

            }
        }

        private void checkGeneral() {
            if ((conversion == Conversion.BOOLEAN || conversion == Conversion.HASHCODE)
                    && Flags.contains(f, Flags.ALTERNATE))
                failMismatch(Flags.ALTERNATE, conversion);
            // '-' requires a width
            if (width == -1 && Flags.contains(f, Flags.LEFT_JUSTIFY))
                throw new MissingFormatWidthException(toString());
            checkBadFlags(Flags.PLUS, Flags.LEADING_SPACE, Flags.ZERO_PAD,
                    Flags.GROUP, Flags.PARENTHESES);
        }

        private void checkDateTime() {
            if (precision != -1)
                throw new IllegalFormatPrecisionException(precision);
            if (dateTime == null)
                throw new UnknownFormatConversionException(String.valueOf(conversion.c));
            checkBadFlags(Flags.ALTERNATE, Flags.PLUS, Flags.LEADING_SPACE,
                    Flags.ZERO_PAD, Flags.GROUP, Flags.PARENTHESES);
            // '-' requires a width
            if (width == -1 && Flags.contains(f, Flags.LEFT_JUSTIFY))
                throw new MissingFormatWidthException(toString());
        }

        private void checkCharacter() {
            if (precision != -1)
                throw new IllegalFormatPrecisionException(precision);
            checkBadFlags(Flags.ALTERNATE, Flags.PLUS, Flags.LEADING_SPACE,
                    Flags.ZERO_PAD, Flags.GROUP, Flags.PARENTHESES);
            // '-' requires a width
            if (width == -1 && Flags.contains(f, Flags.LEFT_JUSTIFY))
                throw new MissingFormatWidthException(toString());
        }

        private void checkInteger() {
            checkNumeric();
            if (precision != -1)
                throw new IllegalFormatPrecisionException(precision);

            if (conversion == Conversion.DECIMAL_INTEGER)
                checkBadFlags(Flags.ALTERNATE);
            else if (conversion == Conversion.OCTAL_INTEGER)
                checkBadFlags(Flags.GROUP);
            else
                checkBadFlags(Flags.GROUP);
        }

        public void checkBadFlags(int... badFlags) {
            for (int badFlag : badFlags)
                if (Flags.contains(f, badFlag))
                    failMismatch(badFlag, conversion);
        }

        private void checkFloat() {
            checkNumeric();
            if (conversion == Conversion.DECIMAL_FLOAT) {
                // no check
            } else if (conversion == Conversion.HEXADECIMAL_FLOAT) {
                checkBadFlags(Flags.PARENTHESES, Flags.GROUP);
            } else if (conversion == Conversion.SCIENTIFIC) {
                checkBadFlags(Flags.GROUP);
            } else if (conversion == Conversion.GENERAL) {
                checkBadFlags(Flags.ALTERNATE);
            }
        }

        private void checkNumeric() {
            if (width != -1 && width < 0)
                throw new IllegalFormatWidthException(width);

            if (precision != -1 && precision < 0)
                throw new IllegalFormatPrecisionException(precision);

            // '-' and '0' require a width
            if (width == -1
                    && (Flags.contains(f, Flags.LEFT_JUSTIFY) || Flags.contains(f, Flags.ZERO_PAD)))
                throw new MissingFormatWidthException(toString());

            // bad combination
            if ((Flags.contains(f, Flags.PLUS) && Flags.contains(f, Flags.LEADING_SPACE))
                    || (Flags.contains(f, Flags.LEFT_JUSTIFY) && Flags.contains(f, Flags.ZERO_PAD)))
                throw new IllegalFormatFlagsException(Flags.toString(f));
        }

        private void checkText() {
            if (precision != -1)
                throw new IllegalFormatPrecisionException(precision);
            switch (conversion) {
                case PERCENT_SIGN:
                    if (f != Flags.LEFT_JUSTIFY
                            && f != Flags.NONE)
                        throw new IllegalFormatFlagsException(Flags.toString(f));
                    // '-' requires a width
                    if (width == -1 && Flags.contains(f, Flags.LEFT_JUSTIFY))
                        throw new MissingFormatWidthException(toString());
                    break;
                case LINE_SEPARATOR:
                    if (width != -1)
                        throw new IllegalFormatWidthException(width);
                    if (f != Flags.NONE)
                        throw new IllegalFormatFlagsException(Flags.toString(f));
                    break;
                default:
                    assert false;
            }
        }

        // -- Methods to support throwing exceptions --

        public static void failMismatch(int flags, Conversion conersion) {
            String fs = Flags.toString(flags);
            throw new FormatFlagsConversionMismatchException(fs, conersion.c);
        }

    }


    public static class Flags {

        public static final int NONE          = 0;         // ''

        // duplicate declarations from Formattable.java
        public static final int LEFT_JUSTIFY  = 1 << 0;   // '-'
        public static final int UPPERCASE     = 1 << 1;   // '^'
        public static final int ALTERNATE     = 1 << 2;   // '#'

        // numerics
        public static final int PLUS          = 1 << 3;   // '+'
        public static final int LEADING_SPACE = 1 << 4;   // ' '
        public static final int ZERO_PAD      = 1 << 5;   // '0'
        public static final int GROUP         = 1 << 6;   // ','
        public static final int PARENTHESES   = 1 << 7;   // '('

        // indexing
        public static final int PREVIOUS      = 1 << 8;   // '<'

        private Flags() {}

        public static boolean contains(int flags, int f) {
            return (flags & f) == f;
        }

        public static int remove(int flags, int f) {
            return flags & ~f;
        }

        private static int add(int flags, int f) {
            return flags | f;
        }

        private static int parse(String s, int start, int end) {
            int f = NONE;
            for (int i = start; i < end; i++) {
                char c = s.charAt(i);
                int v = parse(c);
                if (contains(f, v))
                    throw new DuplicateFormatFlagsException(toString(v));
                f = add(f, v);
            }
            return f;
        }

        // parse those flags which may be provided by users
        private static int parse(char c) {
            switch (c) {
                case '-': return LEFT_JUSTIFY;
                case '#': return ALTERNATE;
                case '+': return PLUS;
                case ' ': return LEADING_SPACE;
                case '0': return ZERO_PAD;
                case ',': return GROUP;
                case '(': return PARENTHESES;
                case '<': return PREVIOUS;
                default:
                    throw new UnknownFormatFlagsException(String.valueOf(c));
            }
        }

        // Returns a string representation of the current {@code flags}.
        public static String toString(int f) {
            StringBuilder sb = new StringBuilder();
            if (contains(f, LEFT_JUSTIFY))  sb.append('-');
            if (contains(f, UPPERCASE))     sb.append('^');
            if (contains(f, ALTERNATE))     sb.append('#');
            if (contains(f, PLUS))          sb.append('+');
            if (contains(f, LEADING_SPACE)) sb.append(' ');
            if (contains(f, ZERO_PAD))      sb.append('0');
            if (contains(f, GROUP))         sb.append(',');
            if (contains(f, PARENTHESES))   sb.append('(');
            if (contains(f, PREVIOUS))      sb.append('<');
            return sb.toString();
        }
    }

    public enum Conversion {
        NONE('\0'),
        // Byte, Short, Integer, Long, BigInteger
        // (and associated primitives due to autoboxing)
        DECIMAL_INTEGER('d'),            // 'd'
        OCTAL_INTEGER('o'),              // 'o'
        HEXADECIMAL_INTEGER('x'),        // 'x'
        HEXADECIMAL_INTEGER_UPPER('X'),  // 'X'

        // Float, Double, BigDecimal
        // (and associated primitives due to autoboxing)
        SCIENTIFIC('e'),                 // 'e';
        SCIENTIFIC_UPPER('E'),           // 'E';
        GENERAL('g'),                    // 'g';
        GENERAL_UPPER('G'),              // 'G';
        DECIMAL_FLOAT('f'),              // 'f';
        HEXADECIMAL_FLOAT('a'),          // 'a';
        HEXADECIMAL_FLOAT_UPPER('A'),   // 'A';

        // Character, Byte, Short, Integer
        // (and associated primitives due to autoboxing)
        CHARACTER('c'),                 // 'c';
        CHARACTER_UPPER('C'),           // 'C';

        // java.util.Date, java.util.Calendar, long
        DATE_TIME('t'),                 // 't';
        DATE_TIME_UPPER('T'),           // 'T';

        // if (arg.TYPE != boolean) return boolean
        // if (arg != null) return true; else return false;
        BOOLEAN('b'),                   // 'b';
        BOOLEAN_UPPER('B'),             // 'B';
        // if (arg instanceof Formattable) arg.formatTo()
        // else arg.toString();
        STRING('s'),                    // 's';
        STRING_UPPER('S'),              // 'S';
        // arg.hashCode()
        HASHCODE('h'),                  // 'h';
        HASHCODE_UPPER('H'),            // 'H';

        LINE_SEPARATOR('n'),            // 'n';
        PERCENT_SIGN('%');              // '%';

        private final char c;

        Conversion (char c) {
            this.c = c;
        }

        static Conversion lookup(char c) {
            switch (c) {
            case 'd': return DECIMAL_INTEGER;
            case 'o': return OCTAL_INTEGER;
            case 'x': return HEXADECIMAL_INTEGER;
            case 'X': return HEXADECIMAL_INTEGER_UPPER;
            case 'e': return SCIENTIFIC;
            case 'E': return SCIENTIFIC_UPPER;
            case 'g': return GENERAL;
            case 'G': return GENERAL_UPPER;
            case 'f': return DECIMAL_FLOAT;
            case 'a': return HEXADECIMAL_FLOAT;
            case 'A': return HEXADECIMAL_FLOAT_UPPER;
            case 'c': return CHARACTER;
            case 'C': return CHARACTER_UPPER;
            case 't': return DATE_TIME;
            case 'T': return DATE_TIME_UPPER;
            case 'b': return BOOLEAN;
            case 'B': return BOOLEAN_UPPER;
            case 's': return STRING;
            case 'S': return STRING_UPPER;
            case 'h': return HASHCODE;
            case 'H': return HASHCODE_UPPER;
            case 'n': return LINE_SEPARATOR;
            case '%': return PERCENT_SIGN;
            default:
                throw new UnknownFormatConversionException(String.valueOf(c));
            }
        }

        public void fail(Object arg) {
            throw new IllegalFormatConversionException(c, arg.getClass());
        }

        static boolean isText(Conversion conv) {
            switch (conv) {
            case LINE_SEPARATOR:
            case PERCENT_SIGN:
                return true;
            default:
                return false;
            }
        }
    }

    public enum DateTime {
        HOUR_OF_DAY_0('H'),            // 'H' (00 - 23)
        HOUR_0('I'),                   // 'I' (01 - 12)
        HOUR_OF_DAY('k'),              // 'k'  (0 - 23) -- like H
        HOUR('l'),                     // 'l'  (1 - 12) -- like I
        MINUTE('M'),                   // 'M'  (00 - 59)
        NANOSECOND('N'),               // 'N'  (000000000 - 999999999)
        MILLISECOND('L'),              // 'L'  jdk, not in gnu (000 - 999)
        MILLISECOND_SINCE_EPOCH('Q'), // 'Q'  (0 - 99...?)
        AM_PM('p'),                    // 'p'  (am or pm)
        SECONDS_SINCE_EPOCH('s'),     // 's'  (0 - 99...?)
        SECOND('S'),                   // 'S'  (00 - 60 - leap second)
        TIME('T'),                     // 'T'  (24 hour hh:mm:ss)
        ZONE_NUMERIC('z'),             // 'z'  (-1200 - +1200) - ls minus?
        ZONE('Z'),                     // 'Z'  (symbol)

        // Date
        NAME_OF_DAY_ABBREV('a'),       // 'a' 'a'
        NAME_OF_DAY('A'),              // 'A' 'A'
        NAME_OF_MONTH_ABBREV('b'),    // 'b' 'b'
        NAME_OF_MONTH('B'),            // 'B'  'B'
        CENTURY('C'),                  // 'C' (00 - 99)
        DAY_OF_MONTH_0('d'),           // 'd' (01 - 31)
        DAY_OF_MONTH('e'),             // 'e' (1 - 31) -- like d
// *    ISO_WEEK_OF_YEAR_2('g'),       // 'g'  cross %y %V
// *    ISO_WEEK_OF_YEAR_4('G'),       // 'G'  cross %Y %V
        NAME_OF_MONTH_ABBREV_X('h'),  // 'h'  -- same b
        DAY_OF_YEAR('j'),              // 'j'  (001 - 366)
        MONTH('m'),                    // 'm'  (01 - 12)
// *    DAY_OF_WEEK_1('u'),             // 'u'  (1 - 7) Monday
// *    WEEK_OF_YEAR_SUNDAY('U'),       // 'U'  (0 - 53) Sunday+
// *    WEEK_OF_YEAR_MONDAY_01('V'),    // 'V'  (01 - 53) Monday+
// *    DAY_OF_WEEK_0('w'),             // 'w'  (0 - 6) Sunday
// *    WEEK_OF_YEAR_MONDAY('W'),       // 'W'  (00 - 53) Monday
        YEAR_2('y'),                    // 'y'  (00 - 99)
        YEAR_4('Y'),                    // 'Y'  (0000 - 9999)

        // Composites
        TIME_12_HOUR('r'),             // 'r'  (hh:mm:ss [AP]M)
        TIME_24_HOUR('R'),             // 'R'  (hh:mm same as %H:%M)
// *    LOCALE_TIME('X'),               // 'X'  (%H:%M:%S) - parse format?
        DATE_TIME('c'),                // 'c'  (Sat Nov 04 12:02:33 EST 1999)
        DATE('D'),                     // 'D'  (mm/dd/yy)
        ISO_STANDARD_DATE('F');       // 'F'  (%Y-%m-%d)
// *    LOCALE_DATE('x')                // 'x'  (mm/dd/yy)

        static DateTime lookup(char c) {
            switch (c) {
            case 'H': return HOUR_OF_DAY_0;
            case 'I': return HOUR_0;
            case 'k': return HOUR_OF_DAY;
            case 'l': return HOUR;
            case 'M': return MINUTE;
            case 'N': return NANOSECOND;
            case 'L': return MILLISECOND;
            case 'Q': return MILLISECOND_SINCE_EPOCH;
            case 'p': return AM_PM;
            case 's': return SECONDS_SINCE_EPOCH;
            case 'S': return SECOND;
            case 'T': return TIME;
            case 'z': return ZONE_NUMERIC;
            case 'Z': return ZONE;

            // Date
            case 'a': return NAME_OF_DAY_ABBREV;
            case 'A': return NAME_OF_DAY;
            case 'b': return NAME_OF_MONTH_ABBREV;
            case 'B': return NAME_OF_MONTH;
            case 'C': return CENTURY;
            case 'd': return DAY_OF_MONTH_0;
            case 'e': return DAY_OF_MONTH;
// *        case 'g': return ISO_WEEK_OF_YEAR_2;
// *        case 'G': return ISO_WEEK_OF_YEAR_4;
            case 'h': return NAME_OF_MONTH_ABBREV_X;
            case 'j': return DAY_OF_YEAR;
            case 'm': return MONTH;
// *        case 'u': return DAY_OF_WEEK_1;
// *        case 'U': return WEEK_OF_YEAR_SUNDAY;
// *        case 'V': return WEEK_OF_YEAR_MONDAY_01;
// *        case 'w': return DAY_OF_WEEK_0;
// *        case 'W': return WEEK_OF_YEAR_MONDAY;
            case 'y': return YEAR_2;
            case 'Y': return YEAR_4;

            // Composites
            case 'r': return TIME_12_HOUR;
            case 'R': return TIME_24_HOUR;
// *        case 'X': return LOCALE_TIME;
            case 'c': return DATE_TIME;
            case 'D': return DATE;
            case 'F': return ISO_STANDARD_DATE;
// *        case 'x': return LOCALE_DATE;
            default:
                throw new UnknownFormatConversionException("t" + c);
            }
        }

        private final char c;

        DateTime(char c) {
            this.c = c;
        }

        public void fail(Object arg) {
            throw new IllegalFormatConversionException(c, arg.getClass());
        }
    }

}
