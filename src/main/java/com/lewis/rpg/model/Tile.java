package com.lewis.rpg.model;

/**
 * The numbers stored in the map's 2D array, and what each one means.
 *
 * GameMap keeps the whole level as int[][] tiles. Rather than writing bare numbers like
 * "if (tile == 1)" all over the code, every number gets a name here, so the code reads
 * "if (tile == Tile.WALL)". Add new tile types here (stairs, pressure plates...) as the
 * game grows, and give each one a symbol in LevelLoader.
 */
public final class Tile {
    // ---- tile types (the value stored in each cell of the map array) ----
    public static final int FLOOR = 0;      // empty floor - can be walked on
    public static final int WALL = 1;       // solid wall - blocks movement, darts and arrows
    public static final int CHEST = 2;      // gives gold when walked onto, then becomes FLOOR
    public static final int DOOR = 3;       // locked - needs a key
    public static final int OPEN_DOOR = 4;  // unlocked - walkable
    public static final int KEY = 5;        // a key lying on the floor
    public static final int DART_TRAP = 6;  // shooter set into a wall - blocks like a wall
    public static final int CHECKPOINT = 7; // looks like floor; you respawn here after collapsing
    public static final int WALL_TORCH = 8; // a wall with a torch on it - behaves exactly like a wall
    public static final int STAIRS_DOWN = 9;
    public static final int STAIRS_UP = 10;

    /** Private constructor: this class only holds constants, so nobody should create one. */
    private Tile() {
        // constants only - never created
    }

    /**
     * True for every kind of plain wall (with or without a torch).
     * Used so the rest of the code doesn't need to remember that a torch wall is still a wall.
     */
    public static boolean isWall(int tile) {
        return tile == WALL || tile == WALL_TORCH;
    }
}
