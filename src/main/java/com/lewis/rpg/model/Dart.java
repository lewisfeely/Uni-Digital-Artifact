package com.lewis.rpg.model;

/**
 * A dart fired by a trap. Unlike the player, a dart moves smoothly between squares,
 * so its position is stored as doubles measured in squares:
 * x = 3.5 means the middle of column 3. It only ever flies in one of the 4 directions.
 */
public class Dart {
    /** Squares per second - faster than walking pace, so you can't outrun it. */
    public static final double SPEED = 8.0;

    // ---- state ----
    private double x;                    // centre of the dart, across, in squares
    private double y;                    // centre of the dart, down, in squares
    private final Direction direction;   // never changes once fired

    /** Starts the dart in the middle of the given square (+ 0.5 = centre of the square). */
    public Dart(int row, int col, Direction direction) {
        this.x = col + 0.5;
        this.y = row + 0.5;
        this.direction = direction;
    }

    /**
     * Moves the dart forward. distance = speed x time, so a dart covers the same ground
     * per second whether the game runs at 30 or 120 frames a second.
     */
    public void update(double deltaSeconds) {
        x += direction.getColChange() * SPEED * deltaSeconds;
        y += direction.getRowChange() * SPEED * deltaSeconds;
    }

    // ---- which square the dart is in (rounding down: 3.7 -> square 3) ----

    public int getRow() {
        return (int) Math.floor(y);
    }

    public int getCol() {
        return (int) Math.floor(x);
    }

    // ---- getters used for drawing ----

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Direction getDirection() {
        return direction;
    }
}
