package com.lewis.rpg.model;

/**
 * A rectangle used to check whether two things touch.
 * Measured in squares (1.0 = one tile), with (x, y) as the top-left corner.
 */
public class Hitbox {
    private final double x;
    private final double y;
    private final double width;
    private final double height;

    public Hitbox(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /** Two rectangles overlap when they overlap on BOTH the x axis and the y axis. */
    public boolean intersects(Hitbox other) {
        return x < other.x + other.width && x + width > other.x
                && y < other.y + other.height && y + height > other.y;
    }

    /** True if the point (px, py) is inside this rectangle. */
    public boolean contains(double px, double py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }

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
