package com.lewis.rpg.model;

/**
 * The dungeon level, stored as a 2D array of tile numbers (see {@link Tile}).
 *
 * tiles[row][col] - row is the vertical position (0 = top), col the horizontal one (0 = left).
 * The array is built by LevelLoader from a text file, and can be changed while the game
 * runs (opening a chest turns it into FLOOR, unlocking a door turns it into OPEN_DOOR).
 *
 * Enemies, darts and arrows are NOT stored in this array because they move;
 * they live in lists in GameState instead.
 */
public class GameMap {
    /** The level: one int per square, each value one of the Tile constants. */
    private final int[][] tiles;

    /** Wraps an already-built array. Refuses an empty map, which would break getCols(). */
    public GameMap(int[][] tiles) {
        if (tiles == null || tiles.length == 0) {
            throw new IllegalArgumentException("A map needs at least one row");
        }
        this.tiles = tiles;
    }

    // ---- size of the map ----

    /** Number of rows = how many inner arrays the outer array holds. */
    public int getRows() {
        return tiles.length;
    }

    /** Number of columns = the length of one row (every row is the same length). */
    public int getCols() {
        return tiles[0].length;
    }

    // ---- reading and changing single squares ----

    /** The tile number at one square. */
    public int getTile(int row, int col) {
        return tiles[row][col];
    }

    /** Changes one square, e.g. a chest becoming floor once opened. */
    public void setTile(int row, int col, int tile) {
        tiles[row][col] = tile;
    }

    /**
     * True if (row, col) is inside the array. Checking this first stops the game reading
     * outside the array, which would crash with an ArrayIndexOutOfBoundsException.
     */
    public boolean isInBounds(int row, int col) {
        return row >= 0 && row < getRows() && col >= 0 && col < getCols();
    }

    // ---- rules about what can pass through a square ----

    /** True if an entity is allowed to stand on this cell. Walls, locked doors and dart shooters block. */
    public boolean isWalkable(int row, int col) {
        if (!isInBounds(row, col)) {
            return false;   // the edge of the map counts as a wall
        }
        int tile = tiles[row][col];
        return !Tile.isWall(tile) && tile != Tile.DOOR && tile != Tile.DART_TRAP;
    }

    /** True if a dart or arrow flying into this cell stops here. They fly over chests, keys and open doors. */
    public boolean blocksDarts(int row, int col) {
        if (!isInBounds(row, col)) {
            return true;    // anything leaving the map is stopped
        }
        int tile = tiles[row][col];
        return Tile.isWall(tile) || tile == Tile.DOOR || tile == Tile.DART_TRAP;
    }

    // ---- searching the whole map ----

    /**
     * Counts how many cells contain the given tile type.
     * A nested loop visits every square: the outer loop goes down the rows,
     * the inner loop goes across each row.
     */
    public int countTiles(int tileType) {
        int count = 0;
        for (int row = 0; row < getRows(); row++) {
            for (int col = 0; col < getCols(); col++) {
                if (tiles[row][col] == tileType) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Finds the first square holding this tile type, searching row by row from the top left.
     * Used to find the stairs the player arrives on when changing level.
     * @return {row, col} of the first match, or null if the map has none
     */
    public int[] findTile(int tileType) {
        for (int row = 0; row < getRows(); row++) {
            for (int col = 0; col < getCols(); col++) {
                if (tiles[row][col] == tileType) {
                    return new int[] {row, col};   // found it: stop searching straight away
                }
            }
        }
        return null;
    }
}
