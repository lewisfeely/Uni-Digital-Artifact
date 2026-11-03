package com.lewis.rpg.model;

/**
 * An enemy that hunts the player. Its behaviour is a small state machine:
 * at any moment it is in exactly one State, and each frame the switch in update()
 * runs the code for that state, which may move it to another state.
 *
 *   IDLE     -> waits until the player comes within sight
 *   CHASE    -> steps one square towards the player every STEP_INTERVAL seconds
 *   WINDUP   -> next to the player: stops and flashes (the player's warning)
 *   BITE     -> a bite hitbox appears in the square it faces, for BITE_TIME seconds
 *   COOLDOWN -> rests, then goes back to CHASE
 *
 * Extends Entity, so it already has health, position, sliding and a hurtbox.
 */
public class Enemy extends Entity {

    /** The states the enemy can be in. */
    public enum State { IDLE, CHASE, WINDUP, BITE, COOLDOWN }

    // ---- timings, in seconds (shared by every enemy) ----
    public static final double STEP_INTERVAL = 0.35;   // time between steps while chasing
    public static final double WINDUP_TIME = 0.4;      // warning before a bite
    public static final double BITE_TIME = 0.15;       // how long the bite hitbox exists
    public static final double COOLDOWN_TIME = 0.6;    // rest after a bite
    /** How fast it glides between squares, in squares per second. */
    public static final double SLIDE_SPEED = 4.0;
    /** How long it blinks after being hit. */
    public static final double HURT_FLASH_TIME = 0.3;

    // ---- this enemy's stats (set once, in the constructor) ----
    private final int minDamage;
    private final int maxDamage;
    private final int sightRange;   // in squares, counted up/down + left/right
    private final int goldDrop;     // given to the player when it dies

    // ---- what it is doing right now ----
    private State state = State.IDLE;
    private double stateTimer = 0;               // counts down inside timed states
    private Direction facing = Direction.LEFT;   // which square its bite lands in
    private boolean facingLeft = true;           // which way to draw it (art only has left/right)
    private boolean biteLanded = false;          // so one bite can only hurt once
    private double hurtFlashTime = 0;            // blink after being hit

    public Enemy(String name, int maxHp, int row, int col,
                 int minDamage, int maxDamage, int sightRange, int goldDrop) {
        super(name, maxHp, row, col);   // Entity handles name, health and position
        this.minDamage = minDamage;
        this.maxDamage = maxDamage;
        this.sightRange = sightRange;
        this.goldDrop = goldDrop;
    }

    /**
     * The rat: weak, quick to anger, worth a little gold.
     * A "factory method" - a named shortcut so LevelLoader can write Enemy.rat(row, col)
     * instead of remembering the order of eight numbers.
     */
    public static Enemy rat(int row, int col) {
        return new Enemy("Rat", 6, row, col, 1, 3, 7, 5);
    }

    /** Runs one frame of the enemy's behaviour. Called by GameState.update() for every enemy. */
    void update(double deltaSeconds, GameState game) {
        // ---- every frame, whatever the state: glide towards its square, count down the blink ----
        updateSlide(deltaSeconds, SLIDE_SPEED);
        if (hurtFlashTime > 0) {
            hurtFlashTime = Math.max(0, hurtFlashTime - deltaSeconds);
        }

        // ---- how far away is the player? (squares up/down + squares left/right) ----
        Player player = game.getPlayer();
        int distance = Math.abs(getRow() - player.getRow()) + Math.abs(getCol() - player.getCol());

        // ---- the state machine ----
        switch (state) {
            case IDLE -> {
                // wake up once the player is close enough
                if (distance <= sightRange) {
                    state = State.CHASE;
                    stateTimer = 0;
                }
            }
            case CHASE -> {
                if (distance > sightRange + 2) {
                    state = State.IDLE;                 // lost interest
                } else if (distance == 1 && !isMoving()) {
                    // right next to the player: turn to face them and start the warning
                    setFacing(directionTowards(player));
                    state = State.WINDUP;
                    stateTimer = WINDUP_TIME;
                } else {
                    // otherwise take a step towards the player every STEP_INTERVAL seconds
                    stateTimer -= deltaSeconds;
                    if (stateTimer <= 0 && !isMoving()) {
                        stepTowards(player, game);
                        stateTimer = STEP_INTERVAL;
                    }
                }
            }
            case WINDUP -> {
                // stand still and flash until the timer runs out, then bite
                stateTimer -= deltaSeconds;
                if (stateTimer <= 0) {
                    state = State.BITE;
                    stateTimer = BITE_TIME;
                    biteLanded = false;
                }
            }
            case BITE -> {
                // while the bite lasts, hurt the player once if their hurtbox overlaps the bite
                if (!biteLanded && getBiteHitbox().intersects(player.getHurtbox())) {
                    biteLanded = true;
                    int damage = Combat.rollDamage(minDamage, maxDamage, game.getRandom());
                    game.damagePlayer(damage, "The " + getName() + " bites you");
                }
                stateTimer -= deltaSeconds;
                if (stateTimer <= 0) {
                    state = State.COOLDOWN;
                    stateTimer = COOLDOWN_TIME;
                }
            }
            case COOLDOWN -> {
                // rest, then start chasing again
                stateTimer -= deltaSeconds;
                if (stateTimer <= 0) {
                    state = State.CHASE;
                    stateTimer = 0;
                }
            }
        }
    }

    /**
     * Moves one square towards the player, trying the longer gap first. Waits if both ways are blocked.
     * Example: player 4 squares right and 1 square down -> try RIGHT first, then DOWN.
     */
    private void stepTowards(Player player, GameState game) {
        // how far away the player is on each axis (positive = right / down)
        int rowGap = player.getRow() - getRow();
        int colGap = player.getCol() - getCol();
        Direction across = colGap > 0 ? Direction.RIGHT : Direction.LEFT;
        Direction upDown = rowGap > 0 ? Direction.DOWN : Direction.UP;

        // try the axis with the bigger gap first
        Direction first = Math.abs(colGap) >= Math.abs(rowGap) ? across : upDown;
        Direction second = first == across ? upDown : across;

        for (Direction dir : new Direction[] {first, second}) {
            // skip an axis where the gap is already 0 (moving that way would go further away)
            boolean worthTrying = (dir == across && colGap != 0) || (dir == upDown && rowGap != 0);
            int newRow = getRow() + dir.getRowChange();
            int newCol = getCol() + dir.getColChange();
            if (worthTrying && game.isFreeForEnemy(newRow, newCol)) {
                moveTo(newRow, newCol);
                setFacing(dir);
                return;
            }
        }
        // both ways blocked: stay put and try again next step
    }

    /** The one of the 4 directions that points most directly at the player. */
    private Direction directionTowards(Player player) {
        int rowGap = player.getRow() - getRow();
        int colGap = player.getCol() - getCol();
        if (Math.abs(colGap) >= Math.abs(rowGap)) {
            return colGap > 0 ? Direction.RIGHT : Direction.LEFT;
        }
        return rowGap > 0 ? Direction.DOWN : Direction.UP;
    }

    /**
     * Turns the enemy. Up and down don't change facingLeft, so the sprite
     * keeps whichever left/right way it was last drawn.
     */
    private void setFacing(Direction dir) {
        facing = dir;
        if (dir == Direction.LEFT) {
            facingLeft = true;
        } else if (dir == Direction.RIGHT) {
            facingLeft = false;
        }
    }

    /** The square in front of the enemy, shrunk slightly (0.1 in from each side), where its bite lands. */
    public Hitbox getBiteHitbox() {
        return new Hitbox(getCol() + facing.getColChange() + 0.1,
                getRow() + facing.getRowChange() + 0.1, 0.8, 0.8);
    }

    /** Called by GameState when this enemy takes a hit, to start the blink. */
    void flashHurt() {
        hurtFlashTime = HURT_FLASH_TIME;
    }

    // ---- getters used by GameState and the renderer ----

    public State getState() {
        return state;
    }

    public boolean isBiting() {
        return state == State.BITE;
    }

    /** Which way to draw the sprite: rats only have left/right art, so up/down keep the last one. */
    public boolean isFacingLeft() {
        return facingLeft;
    }

    public double getHurtFlashTime() {
        return hurtFlashTime;
    }

    public int getGoldDrop() {
        return goldDrop;
    }
}
