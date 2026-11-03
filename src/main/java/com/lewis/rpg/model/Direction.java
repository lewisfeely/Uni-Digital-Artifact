package com.lewis.rpg.model;

/**
 * The four ways something can move or face on the grid.
 *
 * An enum is a fixed list of named values. Each value here also stores how it changes
 * a position, so code can move in any direction without if statements:
 *
 *     newRow = row + direction.getRowChange();
 *     newCol = col + direction.getColChange();
 *
 * Rows count downwards from the top of the map, so UP takes 1 off the row.
 */
public enum Direction {
    // ---- the four values: (change to row, change to column) ----
    UP(-1, 0),      // one row higher on screen
    DOWN(1, 0),     // one row lower on screen
    LEFT(0, -1),    // one column to the left
    RIGHT(0, 1);    // one column to the right

    // ---- the data each value carries ----
    private final int rowChange;
    private final int colChange;

    /** Runs once for each value above, storing its two numbers. */
    Direction(int rowChange, int colChange) {
        this.rowChange = rowChange;
        this.colChange = colChange;
    }

    // ---- getters ----

    /** How many rows this direction moves: -1, 0 or 1. */
    public int getRowChange() {
        return rowChange;
    }

    /** How many columns this direction moves: -1, 0 or 1. */
    public int getColChange() {
        return colChange;
    }
}
