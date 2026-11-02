package com.lewis.rpg.model;

/**
 * The numbers stored in the map's 2D array, and what each one means.
 * Add new tile types here (stairs, pressure plates, water...) as the game grows.
 */
public final class Tile {
    public static final int FLOOR = 0;
    public static final int WALL = 1;
    public static final int CHEST = 2;
    public static final int DOOR = 3;       // locked - needs a key
    public static final int OPEN_DOOR = 4;  // unlocked - walkable
    public static final int KEY = 5;        // a key lying on the floor
    public static final int DART_TRAP = 6;  // shooter set into a wall - blocks like a wall
    public static final int CHECKPOINT = 7; // looks like floor; you respawn here after collapsing
    public static final int WALL_TORCH = 8; // a wall with a torch on it - behaves exactly like a wall

    private Tile() {
        // constants only - never created
    }

    /** True for every kind of plain wall (with or without a torch). */
    public static boolean isWall(int tile) {
        return tile == WALL || tile == WALL_TORCH;
    }
}
