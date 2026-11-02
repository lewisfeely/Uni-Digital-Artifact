package com.lewis.rpg.model;

/**
 * An arrow fired by the player. It can fly at any angle, so it stores
 * a speed across (vx) and a speed down (vy), both in squares per second.
 */
public class Arrow {
    /** Squares per second. */
    public static final double SPEED = 10.0;

    private double x;
    private double y;
    private final double vx;
    private final double vy;

    /**
     * @param x       starting centre, in squares
     * @param y       starting centre, in squares
     * @param dirX    direction across (the direction should have length 1)
     * @param dirY    direction down
     */
    public Arrow(double x, double y, double dirX, double dirY) {
        this.x = x;
        this.y = y;
        this.vx = dirX * SPEED;
        this.vy = dirY * SPEED;
    }

    public void update(double deltaSeconds) {
        x += vx * deltaSeconds;
        y += vy * deltaSeconds;
    }

    /** The square the arrow's tip is in. */
    public int getRow() {
        return (int) Math.floor(y);
    }

    public int getCol() {
        return (int) Math.floor(x);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    /** Angle of flight in degrees, 0 = right, 90 = down (screen y points down). */
    public double getAngleDegrees() {
        return Math.toDegrees(Math.atan2(vy, vx));
    }
}
