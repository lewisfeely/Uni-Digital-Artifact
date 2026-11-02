package com.lewis.rpg.model;

/**
 * The dungeon level, stored as a 2D array of tile numbers (see {@link Tile}).
 * tiles[row][col] - row is the vertical position, col the horizontal one.
 */
public class GameMap {
    private final int[][] tiles;

    public GameMap(int[][] tiles) {
        if (tiles == null || tiles.length == 0) {
            throw new IllegalArgumentException("A map needs at least one row");
        }
        this.tiles = tiles;
    }

    public int getRows() {
        return tiles.length;
    }

    public int getCols() {
        return tiles[0].length;
    }

    public int getTile(int row, int col) {
        return tiles[row][col];
    }

    public void setTile(int row, int col, int tile) {
        tiles[row][col] = tile;
    }

    public boolean isInBounds(int row, int col) {
        return row >= 0 && row < getRows() && col >= 0 && col < getCols();
    }

    /** True if an entity is allowed to stand on this cell. Walls and locked doors block. */
    public boolean isWalkable(int row, int col) {
        if (!isInBounds(row, col)) {
            return false;
        }
        int tile = tiles[row][col];
        return !Tile.isWall(tile) && tile != Tile.DOOR && tile != Tile.DART_TRAP;
    }

    /** True if a dart flying into this cell stops here. Darts fly over chests, keys and open doors. */
    public boolean blocksDarts(int row, int col) {
        if (!isInBounds(row, col)) {
            return true;
        }
        int tile = tiles[row][col];
        return Tile.isWall(tile) || tile == Tile.DOOR || tile == Tile.DART_TRAP;
    }

    /** Counts how many cells contain the given tile type - a handy loop exercise. */
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
}
