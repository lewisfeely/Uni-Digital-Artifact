package com.lewis.rpg.model;

/**
 * Anything that lives on the map and has health: the player, enemies, bosses.
 * Player and (later) Enemy both extend this class - this is your inheritance practice.
 */
public abstract class Entity {
    private final String name;
    private int hp;
    private final int maxHp;
    private int row;
    private int col;

    protected Entity(String name, int maxHp, int row, int col) {
        this.name = name;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.row = row;
        this.col = col;
    }

    public void moveTo(int newRow, int newCol) {
        this.row = newRow;
        this.col = newCol;
    }

    public void takeDamage(int amount) {
        hp = Math.max(0, hp - amount);
    }

    public void heal(int amount) {
        hp = Math.min(maxHp, hp + amount);
    }

    public boolean isAlive() {
        return hp > 0;
    }

    public String getName() {
        return name;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }
}
