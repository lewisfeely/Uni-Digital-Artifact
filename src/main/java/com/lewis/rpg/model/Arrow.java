package com.lewis.rpg.model;

/**
 * An arrow fired by the player. Unlike a dart it can fly at any angle, so instead of a
 * Direction it stores a speed across (vx) and a speed down (vy), both in squares per second.
 *
 * Example: aiming exactly right gives direction (1, 0), so vx = 10 and vy = 0.
 * Aiming diagonally down-right gives about (0.71, 0.71), so vx = vy = 7.1 -
 * still 10 squares a second in total, because the direction has length 1.
 */
public class Arrow {
    /** Squares per second. */
    public static final double SPEED = 10.0;

    // ---- state ----
    private double x;          // tip position across, in squares
    private double y;          // tip position down, in squares
    private final double vx;   // speed across (negative = moving left)
    private final double vy;   // speed down (negative = moving up)

    /**
     * @param x       starting centre, in squares
     * @param y       starting centre, in squares
     * @param dirX    direction across (the direction should have length 1)
     * @param dirY    direction down
     */
    public Arrow(double x, double y, double dirX, double dirY) {
        this.x = x;
        this.y = y;
        this.vx = dirX * SPEED;   // scale the length-1 direction up to the real speed
        this.vy = dirY * SPEED;
    }

    /** Moves the arrow: distance = speed x time, on each axis separately. */
    public void update(double deltaSeconds) {
        x += vx * deltaSeconds;
        y += vy * deltaSeconds;
    }

    // ---- which square the tip is in (used to check for walls) ----

    public int getRow() {
        return (int) Math.floor(y);
    }

    public int getCol() {
        return (int) Math.floor(x);
    }

    // ---- getters used for hit checks and drawing ----

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    /**
     * Angle of flight in degrees, 0 = right, 90 = down (screen y points down).
     * atan2(vy, vx) turns the two speeds back into an angle, so the arrow picture
     * can be rotated to point the way it is flying.
     */
    public double getAngleDegrees() {
        return Math.toDegrees(Math.atan2(vy, vx));
    }
}
