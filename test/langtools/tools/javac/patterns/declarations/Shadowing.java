/**
 * @test
 * @enablePreview
 * @compile Shadowing.java
 */
public class Shadowing {
    public class Point {
        final int x;
        final int y;

        protected Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        protected pattern Point(int x, int y) {
            match Point (this.x, this.y);
        }
    }

    public class GreatPoint extends Point {
        final int magnitude;

        public GreatPoint(int x, int y, int magnitude) {
            super(x, y);
            if (magnitude < 0) throw new IllegalArgumentException();
            this.magnitude = magnitude;
        }

        public pattern GreatPoint(int x, int y, int magnitude) {
            switch (this) {
                case Point(var x, var y):
                    match GreatPoint (x, y, this.magnitude);
            }
        }
    }
}