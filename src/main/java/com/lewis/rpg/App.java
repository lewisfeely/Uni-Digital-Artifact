package com.lewis.rpg;

import com.lewis.rpg.model.Direction;
import com.lewis.rpg.model.GameState;
import com.lewis.rpg.util.LevelLoader;
import com.lewis.rpg.view.GameRenderer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * JavaFX entry point: creates the window, loads the first level and handles key presses.
 * The game is turn-based, so we only redraw after the player does something.
 */
public class App extends Application {

    private GameState state;
    private final GameRenderer renderer = new GameRenderer();

    @Override
    public void start(Stage stage) {
        state = LevelLoader.loadFromResource("/levels/level1.txt");

        int width = state.getMap().getCols() * GameRenderer.TILE_SIZE;
        int height = state.getMap().getRows() * GameRenderer.TILE_SIZE + GameRenderer.HUD_HEIGHT;

        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        Scene scene = new Scene(new StackPane(canvas));

        scene.setOnKeyPressed(event -> {
            Direction direction = toDirection(event.getCode());
            if (direction != null) {
                state.movePlayer(direction);
                renderer.render(gc, state);
            }
        });

        renderer.render(gc, state);
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
