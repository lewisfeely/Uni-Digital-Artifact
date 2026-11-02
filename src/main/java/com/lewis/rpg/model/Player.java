package com.lewis.rpg.model;

/** The hero. Add attack, defence, XP, level and an inventory here as you go. */
public class Player extends Entity {
    private int gold;
    private int keys;
    private Direction facing = Direction.DOWN;

    public Player(String name, int row, int col) {
        super(name, 30, row, col);
        this.gold = 0;
        this.keys = 0;
    }

    public void addGold(int amount) {
        gold += amount;
    }

    public int getGold() {
        return gold;
    }

    public void addKey() {
        keys++;
    }

    /**
     * Uses up one key if the player has any.
     * @return true if a key was used
     */
    public boolean useKey() {
        if (keys > 0) {
            keys--;
            return true;
        }
        return false;
    }

    public int getKeys() {
        return keys;
    }

    /** The way the player is looking - used to pick the right row of the sprite sheet. */
    public Direction getFacing() {
        return facing;
    }

    public void setFacing(Direction facing) {
        this.facing = facing;
    }
}
