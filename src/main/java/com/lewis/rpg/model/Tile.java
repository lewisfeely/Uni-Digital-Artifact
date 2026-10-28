package com.lewis.rpg.model;

/**
 * The numbers stored in the map's 2D array, and what each one means.
 * Add new tile types here (doors, stairs, water...) as the game grows.
 */
public final class Tile {
    public static final int FLOOR = 0;
    public static final int WALL = 1;
    public static final int CHEST = 2;

    private Tile() {
        // constants only - never created
    }
}
