package com.lewis.rpg.model;

import java.util.ArrayList;
import java.util.List;

/**
 * One floor of the dungeon: everything that belongs to that floor and stays there when you leave.
 *
 * GameState keeps every level in an array (Level[]) and points at the one you're on. Because
 * each Level holds its own map, traps, enemies and darts, leaving a level and coming back later
 * finds it exactly as you left it: opened chests stay open, dead rats stay dead.
 *
 * Things that travel WITH the player (HP, gold, keys, arrows in the air, the sword swing,
 * damage numbers) are not here - they stay in GameState.
 */
public class Level {
    // ---- the floor itself, built once by LevelLoader ----
    private final GameMap map;
    private final List<DartTrap> traps;
    private final List<Enemy> enemies;              // dead enemies are removed from this list
    private final List<Dart> darts = new ArrayList<>();   // darts currently flying on this floor

    // ---- where the player comes back after collapsing on this floor ----
    private int checkpointRow;
    private int checkpointCol;

    // ---- true if the level file had a 'P' (false = the start square is the stairs) ----
    private final boolean hasPlayerStart;

    /**
     * @param map     the tiles of this floor
     * @param traps   the dart traps on this floor
     * @param enemies the enemies on this floor
     * @param startRow where the checkpoint starts (the player's start square, or later the stairs)
     * @param startCol see startRow
     */
    public Level(GameMap map, List<DartTrap> traps, List<Enemy> enemies, int startRow, int startCol) {
        this(map, traps, enemies, startRow, startCol, true);
    }

    /**
     * As above, also saying whether the start square came from a 'P' in the level file.
     * @param hasPlayerStart true if the file had a 'P', false if the start is the stairs
     */
    public Level(GameMap map, List<DartTrap> traps, List<Enemy> enemies, int startRow, int startCol,
                 boolean hasPlayerStart) {
        this.hasPlayerStart = hasPlayerStart;
        this.map = map;
        this.traps = traps;
        this.enemies = enemies;
        this.checkpointRow = startRow;
        this.checkpointCol = startCol;
    }

    /** Remembers a new respawn square for this floor (called when the player steps on a checkpoint). */
    public void setCheckpoint(int row, int col) {
        checkpointRow = row;
        checkpointCol = col;
    }

    // ------------------------------------------------------------------ getters

    public GameMap getMap() {
        return map;
    }

    public List<DartTrap> getTraps() {
        return traps;
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public List<Dart> getDarts() {
        return darts;
    }

    public int getCheckpointRow() {
        return checkpointRow;
    }

    public int getCheckpointCol() {
        return checkpointCol;
    }

    /** True if the level file had a 'P' start square. */
    public boolean hasPlayerStart() {
        return hasPlayerStart;
    }
}
