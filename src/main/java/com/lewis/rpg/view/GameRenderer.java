package com.lewis.rpg.view;

import com.lewis.rpg.model.GameMap;
import com.lewis.rpg.model.GameState;
import com.lewis.rpg.model.Player;
import com.lewis.rpg.model.Tile;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * Draws the game state onto a JavaFX canvas.
 * For now tiles are coloured squares; later swap fillRect for gc.drawImage(...)
 * with a tileset from kenney.nl or itch.io.
 */
public class GameRenderer {
    public static final int TILE_SIZE = 48;
    public static final int HUD_HEIGHT = 60;

    public void render(GraphicsContext gc, GameState state) {
        GameMap map = state.getMap();
        double width = gc.getCanvas().getWidth();
        double height = gc.getCanvas().getHeight();

        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, width, height);

        drawMap(gc, map);
        drawPlayer(gc, state.getPlayer());
        drawHud(gc, state, map.getRows() * TILE_SIZE, width);
    }

    private void drawMap(GraphicsContext gc, GameMap map) {
        for (int row = 0; row < map.getRows(); row++) {
            for (int col = 0; col < map.getCols(); col++) {
                double x = col * TILE_SIZE;
                double y = row * TILE_SIZE;
                int tile = map.getTile(row, col);

                if (tile == Tile.WALL) {
                    gc.setFill(Color.rgb(70, 64, 80));
                    gc.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                    gc.setStroke(Color.rgb(45, 40, 52));
                    gc.strokeRect(x + 0.5, y + 0.5, TILE_SIZE - 1, TILE_SIZE - 1);
                } else {
                    gc.setFill(Color.rgb(150, 132, 108));
                    gc.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                    if (tile == Tile.CHEST) {
                        drawChest(gc, x, y);
                    }
                }
            }
        }
    }

    private void drawChest(GraphicsContext gc, double x, double y) {
        double pad = TILE_SIZE * 0.2;
        gc.setFill(Color.rgb(140, 85, 30));
        gc.fillRect(x + pad, y + pad * 1.5, TILE_SIZE - pad * 2, TILE_SIZE - pad * 2.5);
        gc.setFill(Color.GOLD);
        gc.fillRect(x + TILE_SIZE / 2.0 - 3, y + TILE_SIZE / 2.0 - 2, 6, 8);
    }

    private void drawPlayer(GraphicsContext gc, Player player) {
        double pad = TILE_SIZE * 0.15;
        double x = player.getCol() * TILE_SIZE + pad;
        double y = player.getRow() * TILE_SIZE + pad;
        gc.setFill(Color.rgb(60, 140, 230));
        gc.fillOval(x, y, TILE_SIZE - pad * 2, TILE_SIZE - pad * 2);
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
        gc.setFill(Color.LIGHTGRAY);
        gc.fillText(state.getMessage(), 12, top + 48);
    }
}
