package com.lewis.rpg.model;

/**
 * A shooter set into a wall that fires a dart in one direction at a steady rhythm.
 * Time is measured in seconds, so it runs at the same speed on any computer.
 */
public class DartTrap {
    /** How long before each shot the trap shows its warning glow. */
    public static final double WARNING_TIME = 0.4;

    private final int row;
    private final int col;
    private final Direction direction;
    private final double fireInterval;
    private double timeUntilFire;

    /**
     * @param fireInterval seconds between shots
     * @param firstShotDelay seconds before the very first shot - use different
     *                       values to make several traps fire in a pattern
     */
    public DartTrap(int row, int col, Direction direction, double fireInterval, double firstShotDelay) {
        this.row = row;
        this.col = col;
        this.direction = direction;
        this.fireInterval = fireInterval;
        this.timeUntilFire = firstShotDelay;
    }

    /**
     * Moves the trap's clock forward.
     * @param deltaSeconds time since the last update
     * @return true if the trap fires during this update
     */
    public boolean update(double deltaSeconds) {
        timeUntilFire -= deltaSeconds;
        if (timeUntilFire <= 0) {
            timeUntilFire += fireInterval;
            return true;
        }
        return false;
    }

    /** True just before a shot, so the player can see it coming. */
    public boolean isWarning() {
        return timeUntilFire <= WARNING_TIME;
    }

    /** Creates a dart in the square directly in front of the shooter. */
    public Dart fire() {
        return new Dart(row + direction.getRowChange(), col + direction.getColChange(), direction);
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public Direction getDirection() {
        return direction;
    }
}
