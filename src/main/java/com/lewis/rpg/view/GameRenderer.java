package com.lewis.rpg.view;

import com.lewis.rpg.model.Arrow;
import com.lewis.rpg.model.DamagePopup;
import com.lewis.rpg.model.Dart;
import com.lewis.rpg.model.DartTrap;
import com.lewis.rpg.model.Direction;
import com.lewis.rpg.model.Enemy;
import com.lewis.rpg.model.GameMap;
import com.lewis.rpg.model.GameState;
import com.lewis.rpg.model.Hitbox;
import com.lewis.rpg.model.MeleeSwing;
import com.lewis.rpg.model.Player;
import com.lewis.rpg.model.Tile;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws the game state onto a JavaFX canvas.
 * Positions come from the model: entities store where they really are (x/y in squares),
 * so this class just multiplies by TILE_SIZE and draws.
 */
public class GameRenderer {
    public static final int TILE_SIZE = 32;
    public static final int HUD_HEIGHT = 60;

    // ---- player animation settings ----
    /** Frames in each row of player.png (frames are square, laid out left to right). */
    private static final int WALK_FRAMES = 4;
    /** Same idea as the FPS setting in Piskel's preview. */
    private static final double ANIMATION_FPS = 8;
    private static final long FRAME_TIME_NANOS = (long) (1_000_000_000 / ANIMATION_FPS);

    // ---- combat art ----
    // rat.png frames (drawn facing right): 0-1 scurry, 2 winding up, 3 bite
    private final Image ratSheet = loadImage("rat.png");
    private final Image arrowImage = loadImage("arrow.png");   // drawn pointing right
    private final Image swingImage = loadImage("swing.png");   // arc drawn bulging to the right
    private static final long RAT_STEP_NANOS = 120_000_000L;
    /** F3 toggles drawing every hitbox as an outline - handy for tuning combat. */
    private boolean showHitboxes = false;

    private final Image floorImage = loadImage("floor.png");
    private final Image doorImage = loadImage("door.png");
    private final Image doorOpenImage = loadImage("door_open.png");
    private final Image keyImage = loadImage("key.png");
    private final Image playerSheet = loadImage("player.png");
    private final Image dartTrapImage = loadImage("dart_trap.png");          // drawn firing right
    private final Image dartTrapWarningImage = loadImage("dart_trap_warning.png");
    private final Image dartImage = loadImage("dart.png");                  // drawn pointing right

    // ---- dungeon walls ----
    private final Image wallFaceImage = loadImage("wall_face.png");
    private final Image wallFaceCrackedImage = loadImage("wall_face_cracked.png");
    private final Image wallFaceMossyImage = loadImage("wall_face_mossy.png");
    private final Image wallRockImage = loadImage("wall_rock.png");
    private final Image torchSheet = loadImage("torch.png");               // 3 flame frames side by side
    private static final int TORCH_FRAMES = 3;
    private static final long TORCH_FRAME_NANOS = 130_000_000L;
    private static final Color WALL_RIM = Color.rgb(70, 64, 80);
    private static final Color WALL_SHADOW = Color.rgb(0, 0, 0, 0.27);

    // ---- chest animation ----
    // ChestAnimation.png frames: 0 closed, 1 lid lifting, 2 open, 3 open with sparkles
    private final Image chestSheet = loadImage("ChestAnimation.png");
    private static final int CHEST_FRAMES = 4;
    private static final int CHEST_OPEN_FRAME = 2;
    /** How fast the lid opens and closes, in frames per second. */
    private static final double CHEST_FPS = 10;
    /** Once open, swap between frames 2 and 3 this often so the gold twinkles. */
    private static final long CHEST_SPARKLE_NANOS = 250_000_000L;
    /** Chests open when the player is this many squares away or closer (diagonals count). */
    private static final int CHEST_OPEN_DISTANCE = 1;
    private final int chestFrameSize = (int) chestSheet.getWidth() / CHEST_FRAMES;
    /**
     * How open each chest is, from 0 (closed) to CHEST_OPEN_FRAME (fully open).
     * Keyed by row * columns + col so each square gets its own number.
     * This only affects how chests look, so it lives here in the view, not in GameState.
     */
    private final Map<Integer, Double> chestOpenness = new HashMap<>();
    private long lastRenderTime = -1;

    /** Size of one frame in player.png, worked out from the image (16 for a 64x16 strip). */
    private final int playerFrameSize = (int) playerSheet.getWidth() / WALK_FRAMES;
    /** 1 row = one animation for every direction; 4 rows = down, left, right, up. */
    private final int playerSheetRows = (int) playerSheet.getHeight() / playerFrameSize;

    /** Loads an image from src/main/resources/images, with a clear error if it's missing. */
    private Image loadImage(String fileName) {
        var stream = getClass().getResourceAsStream("/images/" + fileName);
        if (stream == null) {
            throw new IllegalStateException("Missing image: src/main/resources/images/" + fileName);
        }
        return new Image(stream);
    }

    public void toggleHitboxes() {
        showHitboxes = !showHitboxes;
    }

    /**
     * Draws one frame of the game.
     * @param now the current time in nanoseconds, as given by AnimationTimer
     */
    public void render(GraphicsContext gc, GameState state, long now) {
        GameMap map = state.getMap();
        double width = gc.getCanvas().getWidth();
        double height = gc.getCanvas().getHeight();

        gc.setImageSmoothing(false);   // keep pixel art crisp
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, width, height);

        double deltaSeconds = lastRenderTime < 0 ? 0 : (now - lastRenderTime) / 1_000_000_000.0;
        lastRenderTime = now;

        drawMap(gc, map, now);
        drawChests(gc, state, deltaSeconds, now);
        drawTorchGlow(gc, map, now);
        drawTraps(gc, state);
        drawEnemies(gc, state, now);
        drawPlayer(gc, state, now);
        drawSwing(gc, state);
        drawDarts(gc, state);
        drawArrows(gc, state);
        drawAim(gc, state);
        drawPopups(gc, state);
        if (showHitboxes) {
            drawHitboxes(gc, state);
        }
        drawHud(gc, state, map.getRows() * TILE_SIZE, width);
    }

    // ------------------------------------------------------------------ combat drawing

    private void drawEnemies(GraphicsContext gc, GameState state, long now) {
        int frameSize = (int) ratSheet.getHeight();
        for (Enemy enemy : state.getEnemies()) {
            double hurt = enemy.getHurtFlashTime();
            if (hurt > 0 && (int) (hurt * 20) % 2 == 0) {
                continue;   // blink after being hit
            }
            int frame = switch (enemy.getState()) {
                case WINDUP -> 2;
                case BITE -> 3;
                default -> enemy.isMoving() ? (int) ((now / RAT_STEP_NANOS) % 2) : 0;
            };
            double px = enemy.getX() * TILE_SIZE;
            double py = enemy.getY() * TILE_SIZE;

            gc.save();
            if (enemy.isFacingLeft()) {
                // mirror the image: move to the right edge, then flip the x axis
                gc.translate(px + TILE_SIZE, py);
                gc.scale(-1, 1);
            } else {
                gc.translate(px, py);
            }
            gc.drawImage(ratSheet, frame * frameSize, 0, frameSize, frameSize, 0, 0, TILE_SIZE, TILE_SIZE);
            gc.restore();

            if (enemy.getState() == Enemy.State.WINDUP) {
                gc.setFill(Color.rgb(255, 80, 60));
                gc.setFont(Font.font(16));
                gc.setTextAlign(TextAlignment.CENTER);
                gc.fillText("!", px + TILE_SIZE / 2.0, py + 8);
                gc.setTextAlign(TextAlignment.LEFT);
            }
            if (enemy.getHp() < enemy.getMaxHp()) {
                double fraction = (double) enemy.getHp() / enemy.getMaxHp();
                gc.setFill(Color.rgb(40, 10, 10));
                gc.fillRect(px + 6, py + 6, TILE_SIZE - 12, 3);
                gc.setFill(Color.rgb(220, 60, 50));
                gc.fillRect(px + 6, py + 6, (TILE_SIZE - 12) * fraction, 3);
            }
        }
    }

    private void drawSwing(GraphicsContext gc, GameState state) {
        MeleeSwing swing = state.getSwing();
        if (swing == null) {
            return;
        }
        gc.setGlobalAlpha(swing.getProgressLeft());
        drawRotatedAt(gc, swingImage, swing.getCentreX() * TILE_SIZE, swing.getCentreY() * TILE_SIZE,
                swing.getAngleDegrees());
        gc.setGlobalAlpha(1.0);
    }

    private void drawArrows(GraphicsContext gc, GameState state) {
        for (Arrow arrow : state.getArrows()) {
            // the arrow flies at its exact angle, but the picture snaps to 16 angles so the pixels stay crisp
            double angle = Math.round(arrow.getAngleDegrees() / 22.5) * 22.5;
            drawRotatedAt(gc, arrowImage, arrow.getX() * TILE_SIZE, arrow.getY() * TILE_SIZE, angle);
        }
    }

    /** A faint dotted line from the player to the cursor; brighter when the bow is ready. */
    private void drawAim(GraphicsContext gc, GameState state) {
        if (!state.hasAim()) {
            return;
        }
        Player player = state.getPlayer();
        double fromX = player.getCentreX() * TILE_SIZE;
        double fromY = player.getCentreY() * TILE_SIZE;
        double toX = state.getAimX() * TILE_SIZE;
        double toY = state.getAimY() * TILE_SIZE;
        boolean bowReady = state.getArrowCooldown() <= 0;

        gc.setStroke(Color.rgb(255, 255, 255, bowReady ? 0.45 : 0.15));
        gc.setLineWidth(1.5);
        gc.setLineDashes(4, 6);
        gc.strokeLine(fromX, fromY, toX, toY);
        gc.setLineDashes();
        // small crosshair at the cursor
        gc.strokeLine(toX - 5, toY, toX + 5, toY);
        gc.strokeLine(toX, toY - 5, toX, toY + 5);
        gc.setLineWidth(1.0);
    }

    private void drawPopups(GraphicsContext gc, GameState state) {
        gc.setFont(Font.font(14));
        gc.setTextAlign(TextAlignment.CENTER);
        for (DamagePopup popup : state.getPopups()) {
            gc.setGlobalAlpha(popup.getOpacity());
            gc.setFill(popup.isPlayerWasHit() ? Color.rgb(255, 90, 80) : Color.rgb(255, 235, 140));
            gc.fillText(popup.getText(), popup.getX() * TILE_SIZE, popup.getY() * TILE_SIZE);
        }
        gc.setGlobalAlpha(1.0);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    /** Debug view (F3): hurtboxes in green, attack hitboxes in red. */
    private void drawHitboxes(GraphicsContext gc, GameState state) {
        gc.setLineWidth(1.0);
        gc.setStroke(Color.rgb(80, 255, 120));
        outline(gc, state.getPlayer().getHurtbox());
        for (Enemy enemy : state.getEnemies()) {
            outline(gc, enemy.getHurtbox());
        }
        gc.setStroke(Color.rgb(255, 60, 60));
        if (state.getSwing() != null) {
            outline(gc, state.getSwing().getHitbox());
        }
        for (Enemy enemy : state.getEnemies()) {
            if (enemy.isBiting()) {
                outline(gc, enemy.getBiteHitbox());
            }
        }
        for (Arrow arrow : state.getArrows()) {
            gc.strokeRect(arrow.getX() * TILE_SIZE - 2, arrow.getY() * TILE_SIZE - 2, 4, 4);
        }
    }

    private void outline(GraphicsContext gc, Hitbox box) {
        gc.strokeRect(box.getX() * TILE_SIZE, box.getY() * TILE_SIZE,
                box.getWidth() * TILE_SIZE, box.getHeight() * TILE_SIZE);
    }

    /** Draws a square image that faces right, centred on (centreX, centreY) and turned by angle degrees. */
    private void drawRotatedAt(GraphicsContext gc, Image image, double centreX, double centreY, double angle) {
        gc.save();
        gc.translate(centreX, centreY);
        gc.rotate(angle);
        gc.drawImage(image, -TILE_SIZE / 2.0, -TILE_SIZE / 2.0);
        gc.restore();
    }

    private void drawMap(GraphicsContext gc, GameMap map, long now) {
        for (int row = 0; row < map.getRows(); row++) {
            for (int col = 0; col < map.getCols(); col++) {
                double x = col * TILE_SIZE;
                double y = row * TILE_SIZE;
                int tile = map.getTile(row, col);

                if (Tile.isWall(tile)) {
                    drawWall(gc, map, row, col, x, y, now);
                } else if (tile == Tile.DOOR) {
                    gc.drawImage(doorImage, x, y);
                } else if (tile == Tile.OPEN_DOOR) {
                    gc.drawImage(doorOpenImage, x, y);
                } else if (tile == Tile.DART_TRAP) {
                    // drawn by drawTraps, which knows which way it faces and whether it's about to fire
                } else {
                    gc.drawImage(floorImage, x, y);   // chests are drawn on top of this by drawChests
                    if (row > 0 && Tile.isWall(map.getTile(row - 1, col))) {
                        // shadow at the foot of the wall above
                        gc.setFill(WALL_SHADOW);
                        gc.fillRect(x, y, TILE_SIZE, 6);
                    }
                    if (tile == Tile.KEY) {
                        gc.drawImage(keyImage, x, y);   // transparent background, so the floor shows through
                    }
                }
            }
        }
    }

    /**
     * A wall with open floor directly below it shows its brick front (the "face").
     * Every other wall is the dark rock on top, with a lit rim on any edge next to open floor.
     */
    private void drawWall(GraphicsContext gc, GameMap map, int row, int col, double x, double y, long now) {
        if (isOpen(map, row + 1, col)) {
            gc.drawImage(wallFaceFor(row, col), x, y);
            if (map.getTile(row, col) == Tile.WALL_TORCH) {
                // each torch starts its flicker at a different point so they don't move in step
                int frame = (int) ((now / TORCH_FRAME_NANOS + row + col) % TORCH_FRAMES);
                gc.drawImage(torchSheet, frame * TILE_SIZE, 0, TILE_SIZE, TILE_SIZE, x, y, TILE_SIZE, TILE_SIZE);
            }
            return;
        }

        gc.drawImage(wallRockImage, x, y);
        gc.setFill(WALL_RIM);
        int rim = 3;
        if (isOpen(map, row - 1, col)) {
            gc.fillRect(x, y, TILE_SIZE, rim);
        }
        if (isOpen(map, row, col - 1)) {
            gc.fillRect(x, y, rim, TILE_SIZE);
        }
        if (isOpen(map, row, col + 1)) {
            gc.fillRect(x + TILE_SIZE - rim, y, rim, TILE_SIZE);
        }
        // small corner pieces where the rock meets a room diagonally
        if (isOpen(map, row - 1, col - 1)) {
            gc.fillRect(x, y, rim, rim);
        }
        if (isOpen(map, row - 1, col + 1)) {
            gc.fillRect(x + TILE_SIZE - rim, y, rim, rim);
        }
    }

    /** Picks plain, cracked or mossy bricks. The same square always gets the same choice. */
    private Image wallFaceFor(int row, int col) {
        int hash = Math.floorMod((row * 73856093) ^ (col * 19349663), 10);
        if (hash < 6) {
            return wallFaceImage;
        }
        return hash < 8 ? wallFaceCrackedImage : wallFaceMossyImage;
    }

    /** True if this square is open space (floor, chest, key...) rather than part of a wall. */
    private boolean isOpen(GameMap map, int row, int col) {
        if (!map.isInBounds(row, col)) {
            return false;
        }
        int tile = map.getTile(row, col);
        return !Tile.isWall(tile) && tile != Tile.DART_TRAP && tile != Tile.DOOR && tile != Tile.OPEN_DOOR;
    }

    /** A warm, flickering pool of light on the floor below each torch. */
    private void drawTorchGlow(GraphicsContext gc, GameMap map, long now) {
        double radius = TILE_SIZE * 3.5;
        for (int row = 0; row < map.getRows(); row++) {
            for (int col = 0; col < map.getCols(); col++) {
                if (map.getTile(row, col) != Tile.WALL_TORCH || !isOpen(map, row + 1, col)) {
                    continue;
                }
                double flicker = 0.20 + 0.03 * Math.sin(now / 90_000_000.0 + col * 1.7);
                double cx = col * TILE_SIZE + TILE_SIZE / 2.0;
                double cy = (row + 1) * TILE_SIZE + 8;
                gc.setFill(new RadialGradient(0, 0, cx, cy, radius, false, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.rgb(255, 160, 70, flicker)),
                        new Stop(1, Color.rgb(255, 160, 70, 0))));
                gc.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
            }
        }
    }

    /**
     * Draws every chest. Each chest opens (frames 0 -> 1 -> 2) when the player is next to it,
     * twinkles (2 <-> 3) while they stay close, and closes again when they walk away.
     */
    private void drawChests(GraphicsContext gc, GameState state, double deltaSeconds, long now) {
        GameMap map = state.getMap();
        Player player = state.getPlayer();
        double frameChange = CHEST_FPS * deltaSeconds;

        for (int row = 0; row < map.getRows(); row++) {
            for (int col = 0; col < map.getCols(); col++) {
                if (map.getTile(row, col) != Tile.CHEST) {
                    continue;
                }
                int key = row * map.getCols() + col;
                boolean playerNear = Math.abs(row - player.getRow()) <= CHEST_OPEN_DISTANCE
                        && Math.abs(col - player.getCol()) <= CHEST_OPEN_DISTANCE;

                double openness = chestOpenness.getOrDefault(key, 0.0);
                if (playerNear) {
                    openness = Math.min(CHEST_OPEN_FRAME, openness + frameChange);
                } else {
                    openness = Math.max(0, openness - frameChange);
                }
                chestOpenness.put(key, openness);

                int frame;
                if (openness >= CHEST_OPEN_FRAME) {
                    frame = (now / CHEST_SPARKLE_NANOS) % 2 == 0 ? 2 : 3;   // fully open: twinkle
                } else {
                    frame = (int) openness;                                  // opening or closing
                }

                gc.drawImage(chestSheet,
                        frame * chestFrameSize, 0, chestFrameSize, chestFrameSize,
                        col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }
    }

    private void drawTraps(GraphicsContext gc, GameState state) {
        for (DartTrap trap : state.getTraps()) {
            Image image = trap.isWarning() ? dartTrapWarningImage : dartTrapImage;
            drawRotated(gc, image, trap.getCol() * TILE_SIZE, trap.getRow() * TILE_SIZE, trap.getDirection());
        }
    }

    private void drawDarts(GraphicsContext gc, GameState state) {
        for (Dart dart : state.getDarts()) {
            // dart x/y are measured in squares from its centre; convert to the top-left pixel of a square-sized image
            double x = dart.getX() * TILE_SIZE - TILE_SIZE / 2.0;
            double y = dart.getY() * TILE_SIZE - TILE_SIZE / 2.0;
            drawRotated(gc, dartImage, x, y, dart.getDirection());
        }
    }

    /** Draws a square image that faces right, turned to face the given direction. */
    private void drawRotated(GraphicsContext gc, Image image, double x, double y, Direction facing) {
        double angle = switch (facing) {
            case RIGHT -> 0;
            case DOWN -> 90;     // positive angles turn clockwise on screen
            case LEFT -> 180;
            case UP -> 270;
        };
        gc.save();
        gc.translate(x + TILE_SIZE / 2.0, y + TILE_SIZE / 2.0);   // rotate around the image's centre
        gc.rotate(angle);
        gc.drawImage(image, -TILE_SIZE / 2.0, -TILE_SIZE / 2.0);
        gc.restore();
    }

    private void drawPlayer(GraphicsContext gc, GameState state, long now) {
        Player player = state.getPlayer();
        double drawX = player.getX() * TILE_SIZE;   // the model glides x/y between squares
        double drawY = player.getY() * TILE_SIZE;

        // walking: cycle through the frames over time; standing still: frame 0
        int frame = player.isMoving() ? (int) ((now / FRAME_TIME_NANOS) % WALK_FRAMES) : 0;
        int row = playerSheetRows >= 4 ? rowFor(player.getFacing()) : 0;

        // just been hit: blink by skipping every other tenth of a second
        double hurt = state.getHurtFlashTime();
        if (hurt > 0 && (int) (hurt * 10) % 2 == 0) {
            return;
        }

        // cut one frame out of the sheet (source x, y, w, h) and draw it scaled to a square
        gc.drawImage(playerSheet,
                frame * playerFrameSize, row * playerFrameSize, playerFrameSize, playerFrameSize,
                drawX, drawY, TILE_SIZE, TILE_SIZE);
    }

    /** Sprite sheet row for each direction: down, left, right, up (the Piskel export order). */
    private int rowFor(Direction facing) {
        return switch (facing) {
            case DOWN -> 0;
            case LEFT -> 1;
            case RIGHT -> 2;
            case UP -> 3;
        };
    }

    private void drawHud(GraphicsContext gc, GameState state, double top, double width) {
        Player player = state.getPlayer();

        gc.setFill(Color.rgb(25, 25, 30));
        gc.fillRect(0, top, width, HUD_HEIGHT);

        // health bar
        double barWidth = 160;
        double hpFraction = (double) player.getHp() / player.getMaxHp();
        gc.setFill(Color.rgb(80, 20, 20));
        gc.fillRect(12, top + 12, barWidth, 14);
        gc.setFill(Color.rgb(200, 50, 50));
        gc.fillRect(12, top + 12, barWidth * hpFraction, 14);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font(14));
        gc.fillText("HP " + player.getHp() + "/" + player.getMaxHp(), 180, top + 24);
        gc.setFill(Color.GOLD);
        gc.fillText("Gold: " + player.getGold(), 280, top + 24);
        gc.setFill(Color.rgb(208, 140, 80));
        gc.fillText("Keys: " + player.getKeys(), 370, top + 24);

        // weapons: bright when ready, grey with a countdown while cooling down
        drawWeaponStatus(gc, "[Space] Sword", state.getSwingCooldown(), 460, top + 24);
        drawWeaponStatus(gc, "[F] Bow", state.getArrowCooldown(), 600, top + 24);
        gc.setFill(Color.rgb(120, 120, 130));
        gc.fillText("F3: hitboxes", width - 100, top + 24);
        gc.setFill(Color.LIGHTGRAY);
        gc.fillText(state.getMessage(), 12, top + 48);
    }

    private void drawWeaponStatus(GraphicsContext gc, String label, double cooldown, double x, double y) {
        if (cooldown <= 0) {
            gc.setFill(Color.rgb(140, 220, 255));
            gc.fillText(label, x, y);
        } else {
            gc.setFill(Color.rgb(110, 110, 120));
            gc.fillText(label + String.format(" %.1fs", cooldown), x, y);
        }
    }
}
