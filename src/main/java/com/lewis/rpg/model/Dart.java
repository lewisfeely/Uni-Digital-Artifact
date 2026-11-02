package com.lewis.rpg.model;

/**
 * A dart in flight. Unlike the player, a dart moves smoothly between squares,
 * so its position is stored as doubles measured in squares:
 * x = 3.5 means the middle of column 3.
 */
public class Dart {
    /** Squares per second - four times walking pace, so you can't outrun it. */
    public static final double SPEED = 8.0;

    private double x;
    private double y;
    private final Direction direction;

    /** Starts the dart in the middle of the given square. */
    public Dart(int row, int col, Direction direction) {
        this.x = col + 0.5;
        this.y = row + 0.5;
        this.direction = direction;
    }

    public void update(double deltaSeconds) {
        x += direction.getColChange() * SPEED * deltaSeconds;
        y += direction.getRowChange() * SPEED * deltaSeconds;
    }

    /** The square the dart is currently in. */
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

    public Direction getDirection() {
        return direction;
    }
}
