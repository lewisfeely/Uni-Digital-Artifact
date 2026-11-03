package com.lewis.rpg;

import com.lewis.rpg.model.Direction;
import com.lewis.rpg.model.GameState;
import com.lewis.rpg.util.LevelLoader;
import com.lewis.rpg.view.GameRenderer;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * JavaFX entry point: creates the window, loads the first level, handles keys and the mouse,
 * and runs the game loop (about 60 frames a second) that updates and draws the game.
 *
 * App is the "glue" between the player and the game: it turns key presses and mouse movement
 * into calls on GameState (the rules), and asks GameRenderer (the drawing) to draw each frame.
 */
public class App extends Application {

    /**
     * Every level in the game, in order: index 0 is level 1, index 1 is level 2...
     * Every level must be the same size, because the window is sized from level 1.
     * Swap test_small.txt for level1.txt once level1.txt has its '+' stairs.
     */
    private static final String[] LEVEL_FILES = {
            "/levels/level1.txt",
            "/levels/level2.txt"
    };

    // ---- the two halves of the game: the rules and the drawing ----
    private GameState state;
    private final GameRenderer renderer = new GameRenderer();

    /** Direction keys currently held down, most recently pressed last. */
    private final List<Direction> heldDirections = new ArrayList<>();

    /** JavaFX calls this once when the program starts, passing in the window (the Stage). */
    @Override
    public void start(Stage stage) {
        // ---- load every level up front, so each one remembers its state when you leave it ----
        state = LevelLoader.loadGame(LEVEL_FILES);

        // ---- size the window to fit the map exactly, plus the bar at the bottom ----
        int width = state.getMap().getCols() * GameRenderer.TILE_SIZE;
        int height = state.getMap().getRows() * GameRenderer.TILE_SIZE + GameRenderer.HUD_HEIGHT;

        // ---- build the window's contents: a Canvas we can draw on, inside a Scene ----
        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();   // the "pen" used for all drawing
        Scene scene = new Scene(new StackPane(canvas));

        // ---- keyboard: a key goes down ----
        scene.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            // attack and debug keys are checked first; "return" stops them also being treated as movement
            if (code == KeyCode.J) {
                state.swing();              // sword towards the cursor (ignored while cooling down)
                return;
            }
            if (code == KeyCode.K) {
                state.shootArrow();         // arrow at the cursor (ignored while cooling down)
                return;
            }
            if (code == KeyCode.H) {
                renderer.toggleHitboxes();  // debug view
                return;
            }
            // movement keys: remember the key is held, and take a step straight away
            Direction direction = toDirection(code);
            if (direction != null && !heldDirections.contains(direction)) {
                heldDirections.add(direction);
                // a quick tap still moves one square, even if released before the next frame
                if (!state.getPlayer().isMoving()) {
                    state.movePlayer(direction);
                }
            }
        });
        // ---- mouse: aiming ----
        // the mouse aims: convert pixels to squares and tell the game where the cursor is
        scene.setOnMouseMoved(event -> state.setAim(
                event.getX() / GameRenderer.TILE_SIZE, event.getY() / GameRenderer.TILE_SIZE));
        scene.setOnMouseDragged(event -> state.setAim(
                event.getX() / GameRenderer.TILE_SIZE, event.getY() / GameRenderer.TILE_SIZE));
        // ---- keyboard: a key comes back up - stop walking that way ----
        scene.setOnKeyReleased(event -> {
            Direction direction = toDirection(event.getCode());
            if (direction != null) {
                heldDirections.remove(direction);
            }
        });
        // if the window loses focus we never see the key-up, so forget held keys
        stage.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                heldDirections.clear();
            }
        });

        // ---- the game loop: JavaFX calls handle() before every screen refresh (about 60 times a second) ----
        new AnimationTimer() {
            private long lastNow = -1;   // time of the previous frame, -1 = no frame yet

            @Override
            public void handle(long now) {
                // seconds since the last frame; capped so a pause (e.g. dragging the window)
                // doesn't make traps and darts jump forward
                double deltaSeconds = lastNow < 0 ? 0 : Math.min((now - lastNow) / 1_000_000_000.0, 0.1);
                lastNow = now;
                // 1. move everything that runs on its own clock (traps, darts, arrows, rats, sliding)
                state.update(deltaSeconds);

                // 2. holding a key keeps walking: take the next step once the slide has finished
                //    (the most recently pressed key wins, so pressing a new key changes direction)
                if (!heldDirections.isEmpty() && !state.getPlayer().isMoving()) {
                    state.movePlayer(heldDirections.get(heldDirections.size() - 1));
                }
                // 3. draw the frame
                renderer.render(gc, state, now);
            }
        }.start();

        // ---- show the window ----
        stage.setTitle("Dungeon RPG");
        stage.setScene(scene);
        stage.setResizable(false);   // the map is a fixed size, so the window is too
        stage.show();
    }

    /**
     * Turns a key into a direction: arrow keys or WASD. Any other key gives null.
     * Note: S and A are checked as attack keys first in setOnKeyPressed, so they
     * no longer reach this method when pressed - only the arrow keys move down and left.
     */
    private Direction toDirection(KeyCode code) {
        return switch (code) {
            case UP, W -> Direction.UP;
            case DOWN, S -> Direction.DOWN;
            case LEFT, A -> Direction.LEFT;
            case RIGHT, D -> Direction.RIGHT;
            default -> null;
        };
    }

    /** Starts JavaFX, which then calls start(...) above. Launcher calls this. */
    public static void main(String[] args) {
        launch(args);
    }
}
