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
 * JavaFX entry point: creates the window, loads the first level, handles keys
 * and runs the game loop (about 60 frames a second) that draws the animation.
 */
public class App extends Application {

    private GameState state;
    private final GameRenderer renderer = new GameRenderer();

    /** Direction keys currently held down, most recently pressed last. */
    private final List<Direction> heldDirections = new ArrayList<>();

    @Override
    public void start(Stage stage) {
        state = LevelLoader.loadFromResource("/levels/level1.txt");

        int width = state.getMap().getCols() * GameRenderer.TILE_SIZE;
        int height = state.getMap().getRows() * GameRenderer.TILE_SIZE + GameRenderer.HUD_HEIGHT;

        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        Scene scene = new Scene(new StackPane(canvas));

        scene.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            if (code == KeyCode.SPACE) {
                state.swing();              // sword towards the cursor (ignored while cooling down)
                return;
            }
            if (code == KeyCode.F) {
                state.shootArrow();         // arrow at the cursor (ignored while cooling down)
                return;
            }
            if (code == KeyCode.F3) {
                renderer.toggleHitboxes();  // debug view
                return;
            }
            Direction direction = toDirection(code);
            if (direction != null && !heldDirections.contains(direction)) {
                heldDirections.add(direction);
                // a quick tap still moves one square, even if released before the next frame
                if (!state.getPlayer().isMoving()) {
                    state.movePlayer(direction);
                }
            }
        });
        // the mouse aims: convert pixels to squares and tell the game where the cursor is
        scene.setOnMouseMoved(event -> state.setAim(
                event.getX() / GameRenderer.TILE_SIZE, event.getY() / GameRenderer.TILE_SIZE));
        scene.setOnMouseDragged(event -> state.setAim(
                event.getX() / GameRenderer.TILE_SIZE, event.getY() / GameRenderer.TILE_SIZE));
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

        new AnimationTimer() {
            private long lastNow = -1;

            @Override
            public void handle(long now) {
                // seconds since the last frame; capped so a pause (e.g. dragging the window)
                // doesn't make traps and darts jump forward
                double deltaSeconds = lastNow < 0 ? 0 : Math.min((now - lastNow) / 1_000_000_000.0, 0.1);
                lastNow = now;
                state.update(deltaSeconds);

                // holding a key keeps walking: take the next step once the slide has finished
                if (!heldDirections.isEmpty() && !state.getPlayer().isMoving()) {
                    state.movePlayer(heldDirections.get(heldDirections.size() - 1));
                }
                renderer.render(gc, state, now);
            }
        }.start();

        stage.setTitle("Dungeon RPG");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    private Direction toDirection(KeyCode code) {
        return switch (code) {
            case UP, W -> Direction.UP;
            case DOWN, S -> Direction.DOWN;
            case LEFT, A -> Direction.LEFT;
            case RIGHT, D -> Direction.RIGHT;
            default -> null;
        };
    }

    public static void main(String[] args) {
        launch(args);
    }
}
