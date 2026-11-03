package com.lewis.rpg.model;

/**
 * Anything that lives on the map and has health: the player and enemies.
 * Player and Enemy both extend this class, so they share everything below
 * without repeating it (inheritance). It is abstract because there is no such
 * thing as a plain "Entity" in the game - only players and enemies.
 *
 * An entity has two positions:
 *  - row/col: the square it is on (used for walls, doors, chests and who stands where)
 *  - x/y: where it actually is right now, in squares; it glides from square to square
 *    so movement looks smooth, and hitboxes use it so hits match what you see.
 */
public abstract class Entity {
    // ---- identity and health ----
    private final String name;
    private int hp;
    private final int maxHp;

    // ---- grid position: which square it is on ----
    private int row;
    private int col;

    // ---- exact position: where it is drawn, gliding towards (col, row) ----
    private double x;
    private double y;

    /** Starts at full health, standing exactly on its square. Protected = only subclasses call it. */
    protected Entity(String name, int maxHp, int row, int col) {
        this.name = name;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.row = row;
        this.col = col;
        this.x = col;
        this.y = row;
    }

    // ---- movement ----

    /** Changes the square straight away; x/y then glide there in updateSlide. */
    public void moveTo(int newRow, int newCol) {
        this.row = newRow;
        this.col = newCol;
    }

    /** Jumps straight to a square with no glide (used when respawning). */
    public void placeAt(int newRow, int newCol) {
        moveTo(newRow, newCol);
        this.x = newCol;
        this.y = newRow;
    }

    /**
     * Glides x/y towards the current square. Called every frame.
     * maxStep is how far it may move this frame: speed (squares per second) x time.
     */
    public void updateSlide(double deltaSeconds, double squaresPerSecond) {
        double maxStep = squaresPerSecond * deltaSeconds;
        x = moveTowards(x, col, maxStep);
        y = moveTowards(y, row, maxStep);
    }

    /**
     * Moves current towards target by at most maxStep, without overshooting.
     * If the target is closer than one step, it lands exactly on it.
     * Math.signum gives +1 or -1, the direction to move in.
     */
    private static double moveTowards(double current, double target, double maxStep) {
        if (Math.abs(target - current) <= maxStep) {
            return target;
        }
        return current + Math.signum(target - current) * maxStep;
    }

    /** True while still gliding between squares (the exact position hasn't reached the square yet). */
    public boolean isMoving() {
        return x != col || y != row;
    }

    // ---- combat ----

    /**
     * The area that can be hit: a bit smaller than a whole square
     * (about 20 x 24 pixels of the 32 x 32 tile), so near misses feel fair.
     * Built from x/y, so it follows the entity while it slides.
     */
    public Hitbox getHurtbox() {
        return new Hitbox(x + 0.2, y + 0.15, 0.6, 0.75);
    }

    /** Loses HP, never going below 0. */
    public void takeDamage(int amount) {
        hp = Math.max(0, hp - amount);
    }

    /** Gains HP, never going above maxHp. */
    public void heal(int amount) {
        hp = Math.min(maxHp, hp + amount);
    }

    public boolean isAlive() {
        return hp > 0;
    }

    // ---- getters ----

    public String getName() {
        return name;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    /** Left edge of the entity right now, in squares. */
    public double getX() {
        return x;
    }

    /** Top edge of the entity right now, in squares. */
    public double getY() {
        return y;
    }

    /** Middle of the entity across (left edge + half a square). */
    public double getCentreX() {
        return x + 0.5;
    }

    /** Middle of the entity down (top edge + half a square). */
    public double getCentreY() {
        return y + 0.5;
    }
}
