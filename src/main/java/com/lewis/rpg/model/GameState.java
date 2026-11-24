package com.lewis.rpg.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Everything about the game right now: every level, which level you're on, the player,
 * arrows, the sword swing and the last message.
 *
 * The levels are kept in an array (Level[]) and currentLevel is the index of the one you're
 * on. getMap(), getEnemies(), getTraps() and getDarts() always answer for the current level,
 * so the rest of the code (and GameRenderer) doesn't need to know there is more than one.
 *
 * This class knows nothing about JavaFX - the view just reads from it
 * and draws. Keeping it that way means you can unit test all the rules without a window.
 *
 * Changes happen in two ways:
 *  - key presses call movePlayer(...), swing() and shootArrow()
 *  - update(seconds) is called every frame so traps, darts, arrows, enemies and
 *    sliding movement all run on their own clocks (real-time)
 */
public class GameState {
    // ---- rewards and trap damage ----
    private static final int CHEST_GOLD = 10;
    public static final int DART_DAMAGE = 12;   // 30 = a dart knocks the player out in one hit

    // ---- player combat settings ----
    public static final double PLAYER_SLIDE_SPEED = 5.0;     // squares per second
    public static final double INVINCIBLE_SECONDS = 0.8;     // after being hit, can't be hit again
    public static final double SWING_COOLDOWN = 0.4;
    public static final double ARROW_COOLDOWN = 0.8;
    public static final int SWING_MIN_DAMAGE = 3;
    public static final int SWING_MAX_DAMAGE = 5;
    public static final int ARROW_MIN_DAMAGE = 2;
    public static final int ARROW_MAX_DAMAGE = 3;
    /** How long the player's bow animation plays after a shot, in seconds. */
    public static final double BOW_ANIMATION_TIME = 0.35;
    /** How far the player's torch lights up around them at the start, in squares. */
    public static final double START_LIGHT_RADIUS = 7.0;
    /** Arrows check for hits every quarter square so they can't skip through a wall or a rat. */
    private static final double ARROW_STEP = 0.25;

    // ---- the world: every level, and which one the player is on ----
    private final Level[] levels;     // levels[0] is level 1, levels[1] is level 2...
    private int currentLevel = 0;     // index into levels: the floor the player is on now
    private final Player player;      // the player travels between levels, so lives here

    // ---- things that come and go while playing (ArrayLists grow and shrink as needed) ----
    private final List<Arrow> arrows = new ArrayList<>();
    private final List<DamagePopup> popups = new ArrayList<>();
    private MeleeSwing swing = null;   // null when not swinging
    private String message = "Arrows/W/D to move, J to swing, K to shoot at the cursor.";   // shown in the bottom bar
    private boolean shopOpen = false;  // true while the merchant's shop is on screen (the game pauses)
    private double lightRadius = START_LIGHT_RADIUS;   // how far the player can see, in squares

    // ---- timers, in seconds, counted down in update() ----
    private double invincibleTime = 0;   // > 0 means the player can't be hurt
    private double swingCooldown = 0;    // > 0 means the sword isn't ready yet
    private double arrowCooldown = 0;    // > 0 means the bow isn't ready yet
    private double bowAnimationTime = 0; // > 0 while the bow-shooting animation plays
    private boolean bowAimLeft = false;  // which way the last arrow went, so the bow art can face it

    // where the mouse is pointing, in squares (set by App when the mouse moves)
    private boolean hasAim = false;
    private double aimX;
    private double aimY;

    /** Used for every damage roll. Tests swap it for one with a fixed seed. */
    private Random random = new Random();

    // ---- constructors: the shorter ones build a single Level and call the main one with this(...) ----

    /** A level with no traps or enemies (used by simple tests). */
    public GameState(GameMap map, Player player) {
        this(map, player, new ArrayList<>(), new ArrayList<>());
    }

    /** A level with traps but no enemies. */
    public GameState(GameMap map, Player player, List<DartTrap> traps) {
        this(map, player, traps, new ArrayList<>());
    }

    /** A single level built from its parts (wraps them in a Level, then uses the main constructor). */
    public GameState(GameMap map, Player player, List<DartTrap> traps, List<Enemy> enemies) {
        this(new Level[] {new Level(map, traps, enemies, player.getRow(), player.getCol())}, player);
    }

    /**
     * The main constructor: LevelLoader uses this one.
     * @param levels every level in the game, in order (must have at least one)
     * @param player the player, who starts on levels[0]
     */
    public GameState(Level[] levels, Player player) {
        if (levels.length == 0) {
            throw new IllegalArgumentException("The game needs at least one level");
        }
        this.levels = levels;
        this.player = player;
    }

    /** The level the player is on right now. Every "map", "enemies" etc. in the rules comes from here. */
    private Level level() {
        return levels[currentLevel];
    }

    // ------------------------------------------------------------------ movement

    /**
     * Try to move the player one cell.
     * Walls and enemies block movement, locked doors open if you have a key,
     * chests give gold, keys are picked up and checkpoints are remembered.
     * @return true if the player actually moved
     */
    public boolean movePlayer(Direction direction) {
        // 1. work out the square the player is trying to step onto
        player.setFacing(direction);   // turn even if something blocks the way
        int newRow = player.getRow() + direction.getRowChange();
        int newCol = player.getCol() + direction.getColChange();

        // 2. a locked door: try to unlock it instead of moving
        if (getMap().isInBounds(newRow, newCol) && getMap().getTile(newRow, newCol) == Tile.DOOR) {
            tryUnlockDoor(newRow, newCol);
            return false;   // unlocking takes your turn; walk through on the next one
        }

        // 3. an enemy is standing there: blocked
        Enemy blocker = enemyAt(newRow, newCol);
        if (blocker != null) {
            message = "A " + blocker.getName() + " is in the way. J to swing!";
            return false;
        }

        // 4. the merchant: blocked, and walking into him opens his shop
        if (getMap().isInBounds(newRow, newCol) && getMap().getTile(newRow, newCol) == Tile.MERCHANT) {
            openShop();
            return false;
        }

        // 5. a wall (or the edge of the map): blocked
        if (!getMap().isWalkable(newRow, newCol)) {
            message = "A wall blocks your way.";
            return false;
        }

        // 6. the way is clear: move, then react to whatever is on the new square
        player.moveTo(newRow, newCol);
        int tile = getMap().getTile(newRow, newCol);

        if (tile == Tile.CHEST) {
            player.addGold(CHEST_GOLD);
            getMap().setTile(newRow, newCol, Tile.FLOOR);
            message = "You open a chest and find " + CHEST_GOLD + " gold!";
        } else if (tile == Tile.KEY) {
            player.addKey();
            getMap().setTile(newRow, newCol, Tile.FLOOR);
            message = "You pick up a Rusty Key.";
        } else if (tile == Tile.STAIRS_UP) {
            // '+' stairs: on to the next level
            if (changeLevel(currentLevel + 1)) {
                message = "You move up to level " + (currentLevel + 1) + ".";
            } else {
                message = "These stairs don't lead anywhere yet.";
            }
        } else if (tile == Tile.STAIRS_DOWN) {
            // '-' stairs: back to the previous level
            if (changeLevel(currentLevel - 1)) {
                message = "You move back to level " + (currentLevel + 1) + ".";
            } else {
                message = "These stairs don't lead anywhere yet.";
            }
        } else if (tile == Tile.CHECKPOINT) {
            level().setCheckpoint(newRow, newCol);   // remembered by this level only
            message = "";
        } else {
            message = "";
        }
        return true;
    }

    /**
     * Moves the player to another level and puts them on that level's matching stairs.
     * Package-private (no "private") so tests in the same package can call it.
     * @param levelTo which level to go to (0 = level 1, 1 = level 2...)
     * @return true if the level changed, false if there is no level with that number
     */
    boolean changeLevel(int levelTo) {
        // ---- stop if there is no such level (e.g. the stairs on the last level) ----
        if (levelTo < 0 || levelTo >= levels.length) {
            return false;
        }
        boolean goingForward = levelTo > currentLevel;

        // ---- tidy up the level we're leaving (before switching, so getDarts() is the old level's) ----
        getDarts().clear();   // darts still flying on the old level
        arrows.clear();       // arrows in the air
        popups.clear();       // damage numbers drifting up
        swing = null;         // cancel any sword swing in progress

        // ---- switch level: from here on, getMap() and level() mean the new level ----
        currentLevel = levelTo;

        // ---- arrive on the stairs that lead back the way you came ----
        // going forward you arrive on the new level's '-' (the way back);
        // going back you arrive on the old level's '+' (the way you left it)
        int arriveOn = goingForward ? Tile.STAIRS_DOWN : Tile.STAIRS_UP;
        int[] stairs = getMap().findTile(arriveOn);
        if (stairs != null) {
            player.placeAt(stairs[0], stairs[1]);   // placeAt jumps there with no slide
        } else {
            // a level with no matching stairs: use its checkpoint instead
            player.placeAt(level().getCheckpointRow(), level().getCheckpointCol());
        }
        return true;
    }

    /** Opens the door if the player has a key (using it up), otherwise explains why it won't open. */
    private void tryUnlockDoor(int row, int col) {
        if (player.useKey()) {
            getMap().setTile(row, col, Tile.OPEN_DOOR);
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
            return false;   // still cooling down from the last swing
        }
        double[] dir = aimDirection();
        // snap the angle to the nearest 45 degrees:
        // atan2 turns the direction into an angle, dividing by 45 degrees (PI / 4) and rounding
        // picks the nearest of the 8 directions, multiplying back gives that exact angle
        double angle = Math.atan2(dir[1], dir[0]);
        double snapped = Math.round(angle / (Math.PI / 4)) * (Math.PI / 4);
        // cos and sin turn the snapped angle back into a direction of length 1
        double dx = Math.cos(snapped);
        double dy = Math.sin(snapped);

        // the hitbox sits 0.9 squares out from the player's centre, in the swing direction
        swing = new MeleeSwing(player.getCentreX() + dx * 0.9, player.getCentreY() + dy * 0.9,
                Math.toDegrees(snapped));
        swingCooldown = SWING_COOLDOWN;   // start the cooldown
        faceTowards(dx, dy);              // turn the sprite to face the swing
        return true;
    }

    /**
     * Fires an arrow at the exact angle of the cursor.
     * @return true if an arrow was fired (false while the bow is cooling down)
     */
    public boolean shootArrow() {
        if (arrowCooldown > 0) {
            return false;   // the bow isn't ready yet
        }
        double[] dir = aimDirection();
        // start the arrow 0.4 squares out from the player's centre so it doesn't begin inside them
        arrows.add(new Arrow(player.getCentreX() + dir[0] * 0.4, player.getCentreY() + dir[1] * 0.4,
                dir[0], dir[1]));
        arrowCooldown = ARROW_COOLDOWN;
        faceTowards(dir[0], dir[1]);
        bowAnimationTime = BOW_ANIMATION_TIME;   // start the bow animation
        bowAimLeft = dir[0] < 0;                 // the art faces right, so flip it for shots to the left
        return true;
    }

    /**
     * A direction of length 1 from the player towards the cursor.
     * If the mouse hasn't been used (or is right on the player), use the way the player faces.
     */
    private double[] aimDirection() {
        // the gap from the player to the cursor, on each axis
        double dx = 0;
        double dy = 0;
        if (hasAim) {
            dx = aimX - player.getCentreX();
            dy = aimY - player.getCentreY();
        }
        // no usable aim (cursor never moved, or right on top of the player): use the facing direction
        if (dx * dx + dy * dy < 0.01) {
            dx = player.getFacing().getColChange();
            dy = player.getFacing().getRowChange();
        }
        // divide by the length (Pythagoras) so the direction is exactly 1 long,
        // whether the cursor is 2 squares away or 20
        double length = Math.sqrt(dx * dx + dy * dy);
        return new double[] {dx / length, dy / length};
    }

    /** Turns the player to whichever of the 4 directions is closest to (dx, dy). */
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
        // 0. while the shop is open the whole game is paused: nothing moves, no timers count down
        if (shopOpen) {
            return;
        }

        // 1. slide the player and count down every timer (Math.max stops them going below 0)
        player.updateSlide(deltaSeconds, PLAYER_SLIDE_SPEED);
        invincibleTime = Math.max(0, invincibleTime - deltaSeconds);
        swingCooldown = Math.max(0, swingCooldown - deltaSeconds);
        arrowCooldown = Math.max(0, arrowCooldown - deltaSeconds);
        bowAnimationTime = Math.max(0, bowAnimationTime - deltaSeconds);

        // 2. traps fire and darts fly, then the player's attacks are checked against enemies
        updateTrapsAndDarts(deltaSeconds);
        updateNests(deltaSeconds);
        updateSwing(deltaSeconds);
        updateArrows(deltaSeconds);

        // 3. every enemy runs its state machine, then the dead ones are taken out of the list
        //    (removeIf deletes every item the test is true for - safer than removing inside the loop)
        for (Enemy enemy : getEnemies()) {
            enemy.update(deltaSeconds, this);
        }
        getEnemies().removeIf(enemy -> !enemy.isAlive());

        // 4. damage numbers drift and fade, and finished ones are removed
        for (DamagePopup popup : popups) {
            popup.update(deltaSeconds);
        }
        popups.removeIf(DamagePopup::isFinished);
    }

    /**
     * Runs every rat nest on this level. When one says it's time, a new rat is placed on a free
     * square next to it. (This loops over the nests, not the enemies, so adding to the enemy list
     * here is safe - changing a list while looping over that same list would crash.)
     */
    private void updateNests(double deltaSeconds) {
        for (RatNest nest : getNests()) {
            if (nest.update(deltaSeconds, player.getRow(), player.getCol())) {
                int[] spot = freeSquareNextTo(nest.getRow(), nest.getCol());
                if (spot != null) {   // boxed in? no rat this time; it tries again next interval
                    Enemy rat = Enemy.rat(spot[0], spot[1]);
                    getEnemies().add(rat);
                    nest.addSpawned(rat);
                    message = "A rat scurries out of the nest!";
                }
            }
        }
    }

    /**
     * The first free square next to (row, col), checking below first (the way the nest's
     * mouth faces), then left, right and above.
     * @return {row, col} of a free square, or null if all four are blocked
     */
    private int[] freeSquareNextTo(int row, int col) {
        Direction[] order = {Direction.DOWN, Direction.LEFT, Direction.RIGHT, Direction.UP};
        for (Direction dir : order) {
            int r = row + dir.getRowChange();
            int c = col + dir.getColChange();
            if (isFreeForEnemy(r, c)) {
                return new int[] {r, c};
            }
        }
        return null;
    }

    /** Counts every trap down, adds a dart for each one that fires, then moves all the darts. */
    private void updateTrapsAndDarts(double deltaSeconds) {
        for (DartTrap trap : getTraps()) {
            if (trap.update(deltaSeconds)) {
                Dart dart = trap.fire();
                if (!getMap().blocksDarts(dart.getRow(), dart.getCol())) {
                    getDarts().add(dart);
                }
            }
        }

        // Use an Iterator so we can safely remove darts while looping over the list
        Iterator<Dart> it = getDarts().iterator();
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
        // remember where the dart started this frame, move it, then see how many squares it crossed
        int fromRow = dart.getRow();
        int fromCol = dart.getCol();
        dart.update(deltaSeconds);
        int squaresTravelled = Math.abs(dart.getRow() - fromRow) + Math.abs(dart.getCol() - fromCol);

        // check every square along the way, starting with the one it began in (i = 0)
        Direction dir = dart.getDirection();
        for (int i = 0; i <= squaresTravelled; i++) {
            int row = fromRow + dir.getRowChange() * i;
            int col = fromCol + dir.getColChange() * i;
            if (getMap().blocksDarts(row, col)) {
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
            return;   // not swinging right now
        }
        // damage every living enemy the hitbox touches that this swing hasn't already hit
        for (Enemy enemy : getEnemies()) {
            if (enemy.isAlive() && !swing.hasHit(enemy) && swing.getHitbox().intersects(enemy.getHurtbox())) {
                swing.markHit(enemy);
                // every sword upgrade adds the same bonus to the lowest and highest roll
                int bonus = player.getSwordLevel() * Shop.SWORD_BONUS_PER_LEVEL;
                damageEnemy(enemy, Combat.rollDamage(SWING_MIN_DAMAGE + bonus, SWING_MAX_DAMAGE + bonus, random));
            }
        }
        // count the swing down; once it ends there is no swing (null) until the next key press
        swing.update(deltaSeconds);
        if (swing.isFinished()) {
            swing = null;
        }
    }

    /** Moves each arrow in small steps; it stops at walls and damages the first enemy it touches. */
    private void updateArrows(double deltaSeconds) {
        // an Iterator lets us remove finished arrows safely while looping over the list
        Iterator<Arrow> it = arrows.iterator();
        while (it.hasNext()) {
            Arrow arrow = it.next();
            // split this frame's movement into small steps (at most ARROW_STEP squares each),
            // e.g. 0.1 s at 10 squares/s = 1 square = 4 steps of a quarter square
            int steps = Math.max(1, (int) Math.ceil(Arrow.SPEED * deltaSeconds / ARROW_STEP));
            boolean finished = false;
            for (int i = 0; i < steps && !finished; i++) {
                arrow.update(deltaSeconds / steps);
                if (getMap().blocksDarts(arrow.getRow(), arrow.getCol())) {
                    finished = true;   // hit a wall or locked door
                } else {
                    // did the tip land inside any enemy's hurtbox? Only the first one is hit.
                    for (Enemy enemy : getEnemies()) {
                        if (enemy.isAlive() && enemy.getHurtbox().contains(arrow.getX(), arrow.getY())) {
                            int bonus = player.getBowLevel() * Shop.BOW_BONUS_PER_LEVEL;   // bow upgrades
                            damageEnemy(enemy, Combat.rollDamage(ARROW_MIN_DAMAGE + bonus, ARROW_MAX_DAMAGE + bonus, random));
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

    /**
     * Hurts an enemy: lowers its HP, makes it blink, shows the damage number and updates the message.
     * A dead enemy pays out its gold here; it is removed from the list at the end of update().
     */
    private void damageEnemy(Enemy enemy, int damage) {
        // a guarding enemy (the knight with his shield up) takes no damage at all
        if (enemy.isGuarding()) {
            popups.add(new DamagePopup(enemy.getCentreX(), enemy.getY(), "Blocked!", false));
            message = "The " + enemy.getName() + " blocks with his shield! Strike while he attacks.";
            return;   // stop here: no damage, no blink
        }
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
            return;   // hit very recently: ignore this one
        }
        player.takeDamage(amount);
        invincibleTime = INVINCIBLE_SECONDS;   // start the invincibility (the renderer blinks the player)
        popups.add(new DamagePopup(player.getCentreX(), player.getY(), "-" + amount, true));
        if (player.isAlive()) {
            message = whatHappened + "! (-" + amount + " HP)";
        } else {
            // out of HP: jump back to the last checkpoint with full health
            player.placeAt(level().getCheckpointRow(), level().getCheckpointCol());
            player.heal(player.getMaxHp());
            message = "You collapse... and wake up back at the checkpoint.";
        }
    }

    // ------------------------------------------------------------------ helpers for enemies

    /** The living enemy standing on this square, or null. A linear search through the enemy list. */
    public Enemy enemyAt(int row, int col) {
        for (Enemy enemy : getEnemies()) {
            if (enemy.isAlive() && enemy.getRow() == row && enemy.getCol() == col) {
                return enemy;
            }
        }
        return null;
    }

    /** An enemy may step here if it's walkable and nobody (player or enemy) is on it. */
    boolean isFreeForEnemy(int row, int col) {
        boolean playerThere = player.getRow() == row && player.getCol() == col;
        return getMap().isWalkable(row, col) && !playerThere && enemyAt(row, col) == null;
    }

    // ------------------------------------------------------------------ getters

    /** The map of the level the player is on. */
    public GameMap getMap() {
        return level().getMap();
    }

    public Player getPlayer() {
        return player;
    }

    // ------------------------------------------------------------------ the merchant's shop

    /** Opens the shop and pauses the game. Called when the player walks into the merchant. */
    void openShop() {
        shopOpen = true;
        message = "\"Welcome, traveller! Care to sharpen your weapons?\"";
    }

    /** Buys the next sword upgrade if the player can afford it. Does nothing if the shop is closed. */
    public void buySwordUpgrade() {
        if (shopOpen) {
            message = Shop.buySword(player);
        }
    }

    /** Buys the next bow upgrade if the player can afford it. Does nothing if the shop is closed. */
    public void buyBowUpgrade() {
        if (shopOpen) {
            message = Shop.buyBow(player);
        }
    }

    /** Leaves the shop; the game carries on from where it paused. */
    public void closeShop() {
        shopOpen = false;
        message = "\"Safe travels!\"";
    }

    public boolean isShopOpen() {
        return shopOpen;
    }

    /**
     * How far the player can see, in squares. Kept here in the rules (not in the renderer) so that
     * gameplay can change it later - torch fuel running low, picking up a torch, a lantern upgrade.
     */
    public double getLightRadius() {
        return lightRadius;
    }

    /** Changes how far the player can see (never below 1 square, so they can always see themselves). */
    public void setLightRadius(double squares) {
        lightRadius = Math.max(1, squares);
    }

    /** Seconds left of the bow animation (0 = not shooting). The renderer picks the frame from this. */
    public double getBowAnimationTime() {
        return bowAnimationTime;
    }

    /** True if the last arrow was shot to the left, so the bow art should be mirrored. */
    public boolean isBowAimLeft() {
        return bowAimLeft;
    }

    public String getMessage() {
        return message;
    }

    /** The traps on the current level. */
    public List<DartTrap> getTraps() {
        return level().getTraps();
    }

    /** The rat nests on the current level. */
    public List<RatNest> getNests() {
        return level().getNests();
    }

    /** The darts flying on the current level. */
    public List<Dart> getDarts() {
        return level().getDarts();
    }

    /** The enemies on the current level. */
    public List<Enemy> getEnemies() {
        return level().getEnemies();
    }

    /** Which level the player is on: 0 for level 1, 1 for level 2... */
    public int getCurrentLevelIndex() {
        return currentLevel;
    }

    /** One level from the array, by index (0 = level 1). */
    public Level getLevel(int index) {
        return levels[index];
    }

    /** How many levels the game has (the length of the levels array). */
    public int getLevelCount() {
        return levels.length;
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
