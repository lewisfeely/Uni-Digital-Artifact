package com.lewis.rpg.util;

import com.lewis.rpg.model.GameMap;
import com.lewis.rpg.model.GameState;
import com.lewis.rpg.model.Player;
import com.lewis.rpg.model.Tile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds a level from a text file in src/main/resources/levels.
 *
 *   #  wall
 *   .  floor
 *   C  chest
 *   P  player start (becomes floor)
 *
 * Every line must be the same length.
 */
public final class LevelLoader {

    private LevelLoader() {
    }

    public static GameState loadFromResource(String resourcePath) {
        InputStream in = LevelLoader.class.getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IllegalArgumentException("Level not found: " + resourcePath);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    lines.add(line);
                }
            }
            return parse(lines);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read level " + resourcePath, e);
        }
    }

    public static GameState parse(List<String> lines) {
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Level file is empty");
        }
        int rows = lines.size();
        int cols = lines.get(0).length();
        int[][] tiles = new int[rows][cols];
        int startRow = -1;
        int startCol = -1;

        for (int row = 0; row < rows; row++) {
            String text = lines.get(row);
            if (text.length() != cols) {
                throw new IllegalArgumentException("Line " + (row + 1) + " has length "
                        + text.length() + " but expected " + cols);
            }
            for (int col = 0; col < cols; col++) {
                char symbol = text.charAt(col);
                switch (symbol) {
                    case '#' -> tiles[row][col] = Tile.WALL;
                    case '.' -> tiles[row][col] = Tile.FLOOR;
                    case 'C' -> tiles[row][col] = Tile.CHEST;
                    case 'P' -> {
                        tiles[row][col] = Tile.FLOOR;
                        startRow = row;
                        startCol = col;
                    }
                    default -> throw new IllegalArgumentException(
                            "Unknown symbol '" + symbol + "' at line " + (row + 1) + ", column " + (col + 1));
                }
            }
        }

        if (startRow == -1) {
            throw new IllegalArgumentException("Level has no player start 'P'");
        }
        return new GameState(new GameMap(tiles), new Player("Hero", startRow, startCol));
    }
}
