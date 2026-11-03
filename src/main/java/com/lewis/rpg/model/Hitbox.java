package com.lewis.rpg.model;

/**
 * A rectangle used to check whether two things touch.
 *
 * Measured in squares (1.0 = one tile), with (x, y) as the top-left corner.
 * Every character has a "hurtbox" (the area that can be hit), and every attack creates
 * a hitbox for as long as it is active. A hit happens when the two rectangles overlap.
 */
public class Hitbox {
    // ---- position and size, all in squares ----
    private final double x;        // left edge
    private final double y;        // top edge
    private final double width;
    private final double height;

    public Hitbox(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Two rectangles overlap when they overlap on BOTH the x axis and the y axis.
     *
     * On one axis, two ranges overlap if each one starts before the other one ends:
     *   this starts before other ends  ->  x < other.x + other.width
     *   other starts before this ends  ->  x + width > other.x
     * The same test is then repeated for y. If any of the four is false, there is a gap.
     */
    public boolean intersects(Hitbox other) {
        return x < other.x + other.width && x + width > other.x
                && y < other.y + other.height && y + height > other.y;
    }

    /** True if the point (px, py) is inside this rectangle - used for arrow tips. */
    public boolean contains(double px, double py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }

    // ---- getters (used by the F3 debug view to draw outlines) ----

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }
}
