package com.lewis.rpg.model;

/**
 * The hero. Inherits health, position, sliding movement and its hurtbox from Entity,
 * and adds the things only the player has: gold, keys and which way they are facing.
 * Add attack, defence, XP, level and an inventory here as you go.
 */
public class Player extends Entity {
    // ---- player-only state ----
    private int gold;
    private int keys;
    private Direction facing = Direction.DOWN;   // starts facing the camera

    /** Creates the hero with 30 HP at the given square. super(...) runs Entity's constructor. */
    public Player(String name, int row, int col) {
        super(name, 30, row, col);
        this.gold = 0;
        this.keys = 0;
    }

    // ---- gold ----

    /** Adds gold from chests and defeated enemies. */
    public void addGold(int amount) {
        gold += amount;
    }

    public int getGold() {
        return gold;
    }

    // ---- keys ----

    /** Picks up one key. */
    public void addKey() {
        keys++;
    }

    /**
     * Uses up one key if the player has any.
     * @return true if a key was used (the door can open), false if the player had none
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

    // ---- facing direction ----

    /** The way the player is looking - used to pick the right row of the sprite sheet. */
    public Direction getFacing() {
        return facing;
    }

    /** Called when the player moves, bumps into something or attacks. */
    public void setFacing(Direction facing) {
        this.facing = facing;
    }
}
