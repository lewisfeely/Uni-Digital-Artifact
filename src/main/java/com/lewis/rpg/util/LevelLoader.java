package com.lewis.rpg.util;

import com.lewis.rpg.model.DartTrap;
import com.lewis.rpg.model.Direction;
import com.lewis.rpg.model.Enemy;
import com.lewis.rpg.model.GameMap;
import com.lewis.rpg.model.GameState;
import com.lewis.rpg.model.Level;
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
 *   D  locked door (needs a key)
 *   K  key
 *   T  wall with a torch (only shows on walls that have floor directly below)
 *   r  rat (lowercase, so it can't be confused with the checkpoint R)
 *   R  checkpoint (looks like floor; you respawn here after collapsing)
 *   > < ^ v  dart shooter in a wall, pointing the way it fires
 *   P  player start (becomes floor)
 *
 * Every line must be the same length.
 */
public final class LevelLoader {
    /** Seconds between dart shots. */
    private static final double DEFAULT_FIRE_INTERVAL = 2.0;

    /** Private constructor: this class only has static methods, so it is never created. */
    private LevelLoader() {
    }

    /**
     * Loads every level file into a Level[] array and starts the player on level 1.
     * @param resourcePaths the level files in order, e.g. {"/levels/level1.txt", "/levels/level2.txt"}
     */
    public static GameState loadGame(String[] resourcePaths) {
        if (resourcePaths.length == 0) {
            throw new IllegalArgumentException("No level files given");
        }
        // make an array with one space per file, then fill it in a loop
        Level[] levels = new Level[resourcePaths.length];
        for (int i = 0; i < resourcePaths.length; i++) {
            levels[i] = parseLevel(readLines(resourcePaths[i]));
        }
        // the player starts on level 1's start square (its first checkpoint)
        Level first = levels[0];
        Player player = new Player("Hero", first.getCheckpointRow(), first.getCheckpointCol());
        return new GameState(levels, player);
    }

    /**
     * Reads one level file and builds a one-level game from it (handy for testing a single level).
     * Resources are packaged with the game, so this works the same in IntelliJ and in a .jar.
     */
    public static GameState loadFromResource(String resourcePath) {
        return parse(readLines(resourcePath));
    }

    /** Reads a level file from src/main/resources into a list of lines (blank lines skipped). */
    private static List<String> readLines(String resourcePath) {
        // open the file; getResourceAsStream gives null if it doesn't exist
        InputStream in = LevelLoader.class.getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IllegalArgumentException("Level not found: " + resourcePath);
        }
        // try-with-resources: the reader is closed automatically, even if something goes wrong
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            // read line by line until readLine() returns null (the end of the file),
            // skipping blank lines such as a trailing empty line
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    lines.add(line);
                }
            }
            return lines;
        } catch (IOException e) {
            throw new IllegalStateException("Could not read level " + resourcePath, e);
        }
    }

    /**
     * Builds a one-level game from some lines. Public so tests can build tiny levels
     * from a few strings without needing a file. The level must have a 'P'.
     */
    public static GameState parse(List<String> lines) {
        Level level = parseLevel(lines);
        if (!level.hasPlayerStart()) {
            throw new IllegalArgumentException("Level has no player start 'P'");
        }
        return new GameState(new Level[] {level},
                new Player("Hero", level.getCheckpointRow(), level.getCheckpointCol()));
    }

    /**
     * Turns the lines of a level into a Level. A level without a 'P' is allowed if it has stairs:
     * its start square (first checkpoint) is then the stairs you arrive on.
     */
    public static Level parseLevel(List<String> lines) {
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Level file is empty");
        }

        // ---- set up: the array's size comes from the file (rows = lines, columns = line length) ----
        int rows = lines.size();
        int cols = lines.get(0).length();
        int[][] tiles = new int[rows][cols];
        int startRow = -1;   // -1 means "no P found yet"
        int startCol = -1;
        int stairsRow = -1;  // -1 means "no stairs found yet"
        int stairsCol = -1;
        List<DartTrap> traps = new ArrayList<>();
        List<Enemy> enemies = new ArrayList<>();

        // ---- nested loop: go down the lines, then across each character of a line ----
        for (int row = 0; row < rows; row++) {
            String text = lines.get(row);
            if (text.length() != cols) {
                // every line must be the same length, or the array would have gaps
                throw new IllegalArgumentException("Line " + (row + 1) + " has length "
                        + text.length() + " but expected " + cols);
            }
            for (int col = 0; col < cols; col++) {
                char symbol = text.charAt(col);
                // turn the symbol into a tile number (and create traps, enemies and the start point)
                switch (symbol) {
                    case '#' -> tiles[row][col] = Tile.WALL;
                    case '.' -> tiles[row][col] = Tile.FLOOR;
                    case 'C' -> tiles[row][col] = Tile.CHEST;
                    case 'D' -> tiles[row][col] = Tile.DOOR;
                    case 'K' -> tiles[row][col] = Tile.KEY;
                    case 'R' -> tiles[row][col] = Tile.CHECKPOINT;
                    case 'T' -> tiles[row][col] = Tile.WALL_TORCH;
                    case '-' -> {
                        // stairs back to the previous level - the spot you arrive on, so it's the
                        // preferred start square for a level with no P
                        tiles[row][col] = Tile.STAIRS_DOWN;
                        stairsRow = row;
                        stairsCol = col;
                    }
                    case '+' -> {
                        // stairs on to the next level - only used as the start if there's no '-'
                        tiles[row][col] = Tile.STAIRS_UP;
                        if (stairsRow == -1) {
                            stairsRow = row;
                            stairsCol = col;
                        }
                    }
                    case 'r' -> {
                        tiles[row][col] = Tile.FLOOR;      // enemies move, so they live in a list, not the array
                        enemies.add(Enemy.rat(row, col));
                    }
                    case '>', '<', '^', 'v' -> {
                        // the shooter is part of the wall, and also gets a DartTrap object for its timer
                        tiles[row][col] = Tile.DART_TRAP;
                        traps.add(new DartTrap(row, col, trapDirection(symbol),
                                DEFAULT_FIRE_INTERVAL, DEFAULT_FIRE_INTERVAL));
                    }
                    case 'P' -> {
                        // the player stands on floor; remember where they start
                        tiles[row][col] = Tile.FLOOR;
                        startRow = row;
                        startCol = col;
                    }
                    // anything else is a typo in the level file: stop with a message saying exactly where
                    default -> throw new IllegalArgumentException(
                            "Unknown symbol '" + symbol + "' at line " + (row + 1) + ", column " + (col + 1));
                }
            }
        }

        // ---- every level needs a start point: the P, or failing that the stairs ----
        boolean hasPlayerStart = startRow != -1;
        if (!hasPlayerStart) {
            if (stairsRow == -1) {
                throw new IllegalArgumentException("Level has no player start 'P' and no stairs");
            }
            startRow = stairsRow;
            startCol = stairsCol;
        }
        // ---- wrap the floor's own things up into a Level ----
        return new Level(new GameMap(tiles), traps, enemies, startRow, startCol, hasPlayerStart);
    }

    /** The arrow symbol points the way the trap fires. */
    private static Direction trapDirection(char symbol) {
        return switch (symbol) {
            case '>' -> Direction.RIGHT;
            case '<' -> Direction.LEFT;
            case '^' -> Direction.UP;
            default -> Direction.DOWN;   // 'v'
        };
    }
}
