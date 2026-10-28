package com.lewis.rpg.model;

/** The hero. Add attack, defence, XP, level and an inventory here as you go. */
public class Player extends Entity {
    private int gold;

    public Player(String name, int row, int col) {
        super(name, 30, row, col);
        this.gold = 0;
    }

    public void addGold(int amount) {
        gold += amount;
    }

    public int getGold() {
        return gold;
    }
}
