package com.lewis.rpg.model;

/**
 * An enemy that hunts the player. Its behaviour is a small state machine:
 *
 *   IDLE     -> waits until the player comes within sight
 *   CHASE    -> steps one square towards the player every STEP_INTERVAL seconds
 *   WINDUP   -> next to the player: stops and flashes (the player's warning)
 *   BITE     -> a bite hitbox appears in the square it faces, for BITE_TIME seconds
 *   COOLDOWN -> rests, then goes back to CHASE
 */
public class Enemy extends Entity {

    public enum State { IDLE, CHASE, WINDUP, BITE, COOLDOWN }

    public static final double STEP_INTERVAL = 0.35;
    public static final double WINDUP_TIME = 0.4;
    public static final double BITE_TIME = 0.15;
    public static final double COOLDOWN_TIME = 0.6;
    /** How fast it glides between squares, in squares per second. */
    public static final double SLIDE_SPEED = 4.0;
    /** How long it blinks after being hit. */
    public static final double HURT_FLASH_TIME = 0.3;

    private final int minDamage;
    private final int maxDamage;
    private final int sightRange;
    private final int goldDrop;

    private State state = State.IDLE;
    private double stateTimer = 0;
    private Direction facing = Direction.LEFT;
    private boolean facingLeft = true;
    private boolean biteLanded = false;
    private double hurtFlashTime = 0;

    public Enemy(String name, int maxHp, int row, int col,
                 int minDamage, int maxDamage, int sightRange, int goldDrop) {
        super(name, maxHp, row, col);
        this.minDamage = minDamage;
        this.maxDamage = maxDamage;
        this.sightRange = sightRange;
        this.goldDrop = goldDrop;
    }

    /** The rat: weak, quick to anger, worth a little gold. */
    public static Enemy rat(int row, int col) {
        return new Enemy("Rat", 6, row, col, 1, 3, 5, 5);
    }

    /** Runs one frame of the enemy's behaviour. */
    void update(double deltaSeconds, GameState game) {
        updateSlide(deltaSeconds, SLIDE_SPEED);
        if (hurtFlashTime > 0) {
            hurtFlashTime = Math.max(0, hurtFlashTime - deltaSeconds);
        }

        Player player = game.getPlayer();
        int distance = Math.abs(getRow() - player.getRow()) + Math.abs(getCol() - player.getCol());

        switch (state) {
            case IDLE -> {
                if (distance <= sightRange) {
                    state = State.CHASE;
                    stateTimer = 0;
                }
            }
            case CHASE -> {
                if (distance > sightRange + 2) {
                    state = State.IDLE;                 // lost interest
                } else if (distance == 1 && !isMoving()) {
                    setFacing(directionTowards(player));
                    state = State.WINDUP;
                    stateTimer = WINDUP_TIME;
                } else {
                    stateTimer -= deltaSeconds;
                    if (stateTimer <= 0 && !isMoving()) {
                        stepTowards(player, game);
                        stateTimer = STEP_INTERVAL;
                    }
                }
            }
            case WINDUP -> {
                stateTimer -= deltaSeconds;
                if (stateTimer <= 0) {
                    state = State.BITE;
                    stateTimer = BITE_TIME;
                    biteLanded = false;
                }
            }
            case BITE -> {
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
                stateTimer -= deltaSeconds;
                if (stateTimer <= 0) {
                    state = State.CHASE;
                    stateTimer = 0;
                }
            }
        }
    }

    /** Moves one square towards the player, trying the longer gap first. Waits if both ways are blocked. */
    private void stepTowards(Player player, GameState game) {
        int rowGap = player.getRow() - getRow();
        int colGap = player.getCol() - getCol();
        Direction across = colGap > 0 ? Direction.RIGHT : Direction.LEFT;
        Direction upDown = rowGap > 0 ? Direction.DOWN : Direction.UP;

        Direction first = Math.abs(colGap) >= Math.abs(rowGap) ? across : upDown;
        Direction second = first == across ? upDown : across;

        for (Direction dir : new Direction[] {first, second}) {
            boolean worthTrying = (dir == across && colGap != 0) || (dir == upDown && rowGap != 0);
            int newRow = getRow() + dir.getRowChange();
            int newCol = getCol() + dir.getColChange();
            if (worthTrying && game.isFreeForEnemy(newRow, newCol)) {
                moveTo(newRow, newCol);
                setFacing(dir);
                return;
            }
        }
    }

    private Direction directionTowards(Player player) {
        int rowGap = player.getRow() - getRow();
        int colGap = player.getCol() - getCol();
        if (Math.abs(colGap) >= Math.abs(rowGap)) {
            return colGap > 0 ? Direction.RIGHT : Direction.LEFT;
        }
        return rowGap > 0 ? Direction.DOWN : Direction.UP;
    }

    private void setFacing(Direction dir) {
        facing = dir;
        if (dir == Direction.LEFT) {
            facingLeft = true;
        } else if (dir == Direction.RIGHT) {
            facingLeft = false;
        }
    }

    /** The square in front of the enemy, shrunk slightly, where its bite lands. */
    public Hitbox getBiteHitbox() {
        return new Hitbox(getCol() + facing.getColChange() + 0.1,
                getRow() + facing.getRowChange() + 0.1, 0.8, 0.8);
    }

    /** Called by GameState when this enemy takes a hit. */
    void flashHurt() {
        hurtFlashTime = HURT_FLASH_TIME;
    }

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
