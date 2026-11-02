package com.lewis.rpg.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Everything about the game right now: the map, the player, enemies, traps, darts, arrows
 * and the last message. This class knows nothing about JavaFX - the view just reads from it
 * and draws. Keeping it that way means you can unit test all the rules without a window.
 *
 * Changes happen in two ways:
 *  - key presses call movePlayer(...), swing() and shootArrow()
 *  - update(seconds) is called every frame so traps, darts, arrows, enemies and
 *    sliding movement all run on their own clocks (real-time)
 */
public class GameState {
    private static final int CHEST_GOLD = 10;
    public static final int DART_DAMAGE = 30;

    // ---- player combat settings ----
    public static final double PLAYER_SLIDE_SPEED = 5.0;     // squares per second
    public static final double INVINCIBLE_SECONDS = 0.8;     // after being hit, can't be hit again
    public static final double SWING_COOLDOWN = 0.4;
    public static final double ARROW_COOLDOWN = 0.8;
    public static final int SWING_MIN_DAMAGE = 3;
    public static final int SWING_MAX_DAMAGE = 5;
    public static final int ARROW_MIN_DAMAGE = 2;
    public static final int ARROW_MAX_DAMAGE = 3;
    /** Arrows check for hits every quarter square so they can't skip through a wall or a rat. */
    private static final double ARROW_STEP = 0.25;

    private final GameMap map;
    private final Player player;
    private final List<DartTrap> traps;
    private final List<Enemy> enemies;
    private final List<Dart> darts = new ArrayList<>();
    private final List<Arrow> arrows = new ArrayList<>();
    private final List<DamagePopup> popups = new ArrayList<>();
    private MeleeSwing swing = null;   // null when not swinging
    private String message = "Arrows/WASD to move, Space to swing, F to shoot at the cursor.";

    // where the player comes back after collapsing - starts as the level's start square
    private int checkpointRow;
    private int checkpointCol;
    private double invincibleTime = 0;
    private double swingCooldown = 0;
    private double arrowCooldown = 0;

    // where the mouse is pointing, in squares (set by App when the mouse moves)
    private boolean hasAim = false;
    private double aimX;
    private double aimY;

    private Random random = new Random();

    public GameState(GameMap map, Player player) {
        this(map, player, new ArrayList<>(), new ArrayList<>());
    }

    public GameState(GameMap map, Player player, List<DartTrap> traps) {
        this(map, player, traps, new ArrayList<>());
    }

    public GameState(GameMap map, Player player, List<DartTrap> traps, List<Enemy> enemies) {
        this.map = map;
        this.player = player;
        this.traps = traps;
        this.enemies = enemies;
        this.checkpointRow = player.getRow();
        this.checkpointCol = player.getCol();
    }

    // ------------------------------------------------------------------ movement

    /**
     * Try to move the player one cell.
     * Walls and enemies block movement, locked doors open if you have a key,
     * chests give gold, keys are picked up and checkpoints are remembered.
     * @return true if the player actually moved
     */
    public boolean movePlayer(Direction direction) {
        player.setFacing(direction);   // turn even if something blocks the way
        int newRow = player.getRow() + direction.getRowChange();
        int newCol = player.getCol() + direction.getColChange();

        if (map.isInBounds(newRow, newCol) && map.getTile(newRow, newCol) == Tile.DOOR) {
            tryUnlockDoor(newRow, newCol);
            return false;   // unlocking takes your turn; walk through on the next one
        }

        Enemy blocker = enemyAt(newRow, newCol);
        if (blocker != null) {
            message = "A " + blocker.getName() + " is in the way. Space to swing!";
            return false;
        }

        if (!map.isWalkable(newRow, newCol)) {
            message = "A wall blocks your way.";
            return false;
        }

        player.moveTo(newRow, newCol);
        int tile = map.getTile(newRow, newCol);

        if (tile == Tile.CHEST) {
            player.addGold(CHEST_GOLD);
            map.setTile(newRow, newCol, Tile.FLOOR);
            message = "You open a chest and find " + CHEST_GOLD + " gold!";
        } else if (tile == Tile.KEY) {
            player.addKey();
            map.setTile(newRow, newCol, Tile.FLOOR);
            message = "You pick up a Rusty Key.";
        } else if (tile == Tile.CHECKPOINT) {
            checkpointRow = newRow;
            checkpointCol = newCol;
            message = "";
        } else {
            message = "";
        }
        return true;
    }

    private void tryUnlockDoor(int row, int col) {
        if (player.useKey()) {
            map.setTile(row, col, Tile.OPEN_DOOR);
            message = "You unlock the door with the Rusty Key.";
        } else {
            message = "The door is locked. Maybe there's a key nearby.";
        }
    }

    // ------------------------------------------------------------------ attacks

    /** Where the mouse is pointing, in squares. */
    public void setAim(double x, double y) {
        aimX = x;
        aimY = y;
        hasAim = true;
    }

    /**
     * Swings the sword towards the cursor, snapped to the nearest of 8 directions.
     * @return true if a swing started (false while the sword is cooling down)
     */
    public boolean swing() {
        if (swingCooldown > 0) {
            return false;
        }
        double[] dir = aimDirection();
        // snap the angle to the nearest 45 degrees
        double angle = Math.atan2(dir[1], dir[0]);
        double snapped = Math.round(angle / (Math.PI / 4)) * (Math.PI / 4);
        double dx = Math.cos(snapped);
        double dy = Math.sin(snapped);

        swing = new MeleeSwing(player.getCentreX() + dx * 0.9, player.getCentreY() + dy * 0.9,
                Math.toDegrees(snapped));
        swingCooldown = SWING_COOLDOWN;
        faceTowards(dx, dy);
        return true;
    }

    /**
     * Fires an arrow at the exact angle of the cursor.
     * @return true if an arrow was fired (false while the bow is cooling down)
     */
    public boolean shootArrow() {
        if (arrowCooldown > 0) {
            return false;
        }
        double[] dir = aimDirection();
        arrows.add(new Arrow(player.getCentreX() + dir[0] * 0.4, player.getCentreY() + dir[1] * 0.4,
                dir[0], dir[1]));
        arrowCooldown = ARROW_COOLDOWN;
        faceTowards(dir[0], dir[1]);
        return true;
    }

    /**
     * A direction of length 1 from the player towards the cursor.
     * If the mouse hasn't been used (or is right on the player), use the way the player faces.
     */
    private double[] aimDirection() {
        double dx = 0;
        double dy = 0;
        if (hasAim) {
            dx = aimX - player.getCentreX();
            dy = aimY - player.getCentreY();
        }
        if (dx * dx + dy * dy < 0.01) {
            dx = player.getFacing().getColChange();
            dy = player.getFacing().getRowChange();
        }
        double length = Math.sqrt(dx * dx + dy * dy);
        return new double[] {dx / length, dy / length};
    }

    private void faceTowards(double dx, double dy) {
        if (Math.abs(dx) >= Math.abs(dy)) {
            player.setFacing(dx > 0 ? Direction.RIGHT : Direction.LEFT);
        } else {
            player.setFacing(dy > 0 ? Direction.DOWN : Direction.UP);
        }
    }

    // ------------------------------------------------------------------ the clock

    /**
     * Moves everything that runs on its own clock.
     * @param deltaSeconds time since the last update (about 0.016 at 60 frames a second)
     */
    public void update(double deltaSeconds) {
        player.updateSlide(deltaSeconds, PLAYER_SLIDE_SPEED);
        invincibleTime = Math.max(0, invincibleTime - deltaSeconds);
        swingCooldown = Math.max(0, swingCooldown - deltaSeconds);
        arrowCooldown = Math.max(0, arrowCooldown - deltaSeconds);

        updateTrapsAndDarts(deltaSeconds);
        updateSwing(deltaSeconds);
        updateArrows(deltaSeconds);

        for (Enemy enemy : enemies) {
            enemy.update(deltaSeconds, this);
        }
        enemies.removeIf(enemy -> !enemy.isAlive());

        for (DamagePopup popup : popups) {
            popup.update(deltaSeconds);
        }
        popups.removeIf(DamagePopup::isFinished);
    }

    private void updateTrapsAndDarts(double deltaSeconds) {
        for (DartTrap trap : traps) {
            if (trap.update(deltaSeconds)) {
                Dart dart = trap.fire();
                if (!map.blocksDarts(dart.getRow(), dart.getCol())) {
                    darts.add(dart);
                }
            }
        }

        // Use an Iterator so we can safely remove darts while looping over the list
        Iterator<Dart> it = darts.iterator();
        while (it.hasNext()) {
            Dart dart = it.next();
            if (moveDart(dart, deltaSeconds)) {
                it.remove();
            }
        }
    }

    /**
     * Moves one dart and checks every square it passed through this frame,
     * so a fast dart can't skip over the player or through a wall.
     * @return true if the dart is finished (hit something) and should be removed
     */
    private boolean moveDart(Dart dart, double deltaSeconds) {
        int fromRow = dart.getRow();
        int fromCol = dart.getCol();
        dart.update(deltaSeconds);
        int squaresTravelled = Math.abs(dart.getRow() - fromRow) + Math.abs(dart.getCol() - fromCol);

        Direction dir = dart.getDirection();
        for (int i = 0; i <= squaresTravelled; i++) {
            int row = fromRow + dir.getRowChange() * i;
            int col = fromCol + dir.getColChange() * i;
            if (map.blocksDarts(row, col)) {
                return true;
            }
            if (row == player.getRow() && col == player.getCol()) {
                damagePlayer(DART_DAMAGE, "A dart hits you");
                return true;
            }
        }
        return false;
    }

    /** The sword's hitbox damages every enemy it touches - each one only once per swing. */
    private void updateSwing(double deltaSeconds) {
        if (swing == null) {
            return;
        }
        for (Enemy enemy : enemies) {
            if (enemy.isAlive() && !swing.hasHit(enemy) && swing.getHitbox().intersects(enemy.getHurtbox())) {
                swing.markHit(enemy);
                damageEnemy(enemy, Combat.rollDamage(SWING_MIN_DAMAGE, SWING_MAX_DAMAGE, random));
            }
        }
        swing.update(deltaSeconds);
        if (swing.isFinished()) {
            swing = null;
        }
    }

    /** Moves each arrow in small steps; it stops at walls and damages the first enemy it touches. */
    private void updateArrows(double deltaSeconds) {
        Iterator<Arrow> it = arrows.iterator();
        while (it.hasNext()) {
            Arrow arrow = it.next();
            int steps = Math.max(1, (int) Math.ceil(Arrow.SPEED * deltaSeconds / ARROW_STEP));
            boolean finished = false;
            for (int i = 0; i < steps && !finished; i++) {
                arrow.update(deltaSeconds / steps);
                if (map.blocksDarts(arrow.getRow(), arrow.getCol())) {
                    finished = true;
                } else {
                    for (Enemy enemy : enemies) {
                        if (enemy.isAlive() && enemy.getHurtbox().contains(arrow.getX(), arrow.getY())) {
                            damageEnemy(enemy, Combat.rollDamage(ARROW_MIN_DAMAGE, ARROW_MAX_DAMAGE, random));
                            finished = true;
                            break;
                        }
                    }
                }
            }
            if (finished) {
                it.remove();
            }
        }
    }

    private void damageEnemy(Enemy enemy, int damage) {
        enemy.takeDamage(damage);
        enemy.flashHurt();
        popups.add(new DamagePopup(enemy.getCentreX(), enemy.getY(), "-" + damage, false));
        if (enemy.isAlive()) {
            message = "You hit the " + enemy.getName() + " for " + damage + ".";
        } else {
            player.addGold(enemy.getGoldDrop());
            message = "You defeated the " + enemy.getName() + "! +" + enemy.getGoldDrop() + " gold";
        }
    }

    /**
     * The one place the player takes damage from anything (darts, bites...).
     * After a hit the player can't be hurt again for INVINCIBLE_SECONDS.
     */
    void damagePlayer(int amount, String whatHappened) {
        if (invincibleTime > 0) {
            return;
        }
        player.takeDamage(amount);
        invincibleTime = INVINCIBLE_SECONDS;
        popups.add(new DamagePopup(player.getCentreX(), player.getY(), "-" + amount, true));
        if (player.isAlive()) {
            message = whatHappened + "! (-" + amount + " HP)";
        } else {
            player.placeAt(checkpointRow, checkpointCol);
            player.heal(player.getMaxHp());
            message = "You collapse... and wake up back at the checkpoint.";
        }
    }

    // ------------------------------------------------------------------ helpers for enemies

    /** The living enemy standing on this square, or null. */
    public Enemy enemyAt(int row, int col) {
        for (Enemy enemy : enemies) {
            if (enemy.isAlive() && enemy.getRow() == row && enemy.getCol() == col) {
                return enemy;
            }
        }
        return null;
    }

    /** An enemy may step here if it's walkable and nobody (player or enemy) is on it. */
    boolean isFreeForEnemy(int row, int col) {
        boolean playerThere = player.getRow() == row && player.getCol() == col;
        return map.isWalkable(row, col) && !playerThere && enemyAt(row, col) == null;
    }

    // ------------------------------------------------------------------ getters

    public GameMap getMap() {
        return map;
    }

    public Player getPlayer() {
        return player;
    }

    public String getMessage() {
        return message;
    }

    public List<DartTrap> getTraps() {
        return traps;
    }

    public List<Dart> getDarts() {
        return darts;
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public List<Arrow> getArrows() {
        return arrows;
    }

    public List<DamagePopup> getPopups() {
        return popups;
    }

    /** The sword swing happening right now, or null. */
    public MeleeSwing getSwing() {
        return swing;
    }

    public boolean hasAim() {
        return hasAim;
    }

    public double getAimX() {
        return aimX;
    }

    public double getAimY() {
        return aimY;
    }

    public double getSwingCooldown() {
        return swingCooldown;
    }

    public double getArrowCooldown() {
        return arrowCooldown;
    }

    /** Seconds of invincibility left after a hit - the renderer blinks the player while this is above 0. */
    public double getHurtFlashTime() {
        return invincibleTime;
    }

    Random getRandom() {
        return random;
    }

    /** Lets tests use a fixed seed so "random" damage is the same every run. */
    public void setRandom(Random random) {
        this.random = random;
    }
}
