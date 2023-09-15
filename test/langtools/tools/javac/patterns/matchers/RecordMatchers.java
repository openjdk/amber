/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @enablePreview
 * @compile RecordMatchers.java
 * @run main RecordMatchers
 */
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class RecordMatchers {
    public static void main(String... args) {
        assertEquals(1, testPointX(new Point(1, 2)));
        assertEquals(2, testPointY(new Point(1, 2)));
        assertEquals(2, testMultipleCasesPoint(new Point(42, 4)));
        assertEquals(3, testMultipleCasesPoint(new Point(4, 42)));
        assertEquals(List.of(1), testPoints(new Points(List.of(1), List.of(2))));
    }

    public static Integer testPointX(Object o) {
        if (o instanceof Point(Integer x, Integer y)) {
            return x;
        }
        return -1;
    }

    public static Integer testPointY(Object o) {
        if (o instanceof Point(Integer x, Integer y)) {
            return y;
        }
        return -1;
    }

    public static int testMultipleCasesPoint(Point o) {
        return switch (o) {
            case Point(Integer x, Integer y) when x == 42 -> 2;
            case Point(Integer x, Integer y) when y == 42 -> 3;
            case Point mm -> -1;
        };
    }

    public static List<Integer> testPoints(Object o) {
        if (o instanceof Points(List<Integer> xs, List<Integer> ys)) {
            return xs;
        }
        return List.of(-1);
    }

    public record Points(Collection<Integer> xs, Collection<Integer> ys) {
        @MatcherAnnotation
        public __matcher Points(@BindingAnnotation Collection<Integer> xs, @BindingAnnotation Collection<Integer> ys) {
            xs = this.xs;
            ys = this.ys;
        }
    }

    public record Point(Integer x, Integer y) {
        @MatcherAnnotation(annotField = 42)
        public __matcher Point(Integer x, Integer y) {
            x = this.x;
            y = this.y;
        }
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface MatcherAnnotation{
        int annotField();
    }

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface BindingAnnotation { }

    private static <T> void assertEquals(T expected, T actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected: " + expected + ", but got: " + actual);
        }
    }
}
