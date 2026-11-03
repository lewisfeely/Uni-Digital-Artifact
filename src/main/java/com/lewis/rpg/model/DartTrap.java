package com.lewis.rpg.model;

/**
 * A shooter set into a wall that fires a dart in one direction at a steady rhythm.
 *
 * It works like a countdown timer: each frame takes the time that has passed off
 * timeUntilFire, and when it reaches 0 the trap fires and the timer starts again.
 * Time is measured in seconds, so it runs at the same speed on any computer.
 */
public class DartTrap {
    /** How long before each shot the trap shows its warning glow. */
    public static final double WARNING_TIME = 0.4;

    // ---- fixed settings ----
    private final int row;               // the wall square the shooter sits in
    private final int col;
    private final Direction direction;   // the way it fires
    private final double fireInterval;   // seconds between shots

    // ---- changes every frame ----
    private double timeUntilFire;        // counts down to the next shot

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
     * Adding fireInterval back (rather than setting it to fireInterval) keeps any leftover
     * fraction of a second, so the rhythm stays exact over a long time.
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

    /** True just before a shot, so the renderer can draw the glowing warning image. */
    public boolean isWarning() {
        return timeUntilFire <= WARNING_TIME;
    }

    /** Creates a dart in the square directly in front of the shooter. */
    public Dart fire() {
        return new Dart(row + direction.getRowChange(), col + direction.getColChange(), direction);
    }

    // ---- getters used for drawing ----

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
