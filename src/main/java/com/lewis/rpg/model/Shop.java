package com.lewis.rpg.model;

/**
 * The merchant's prices and the rules for buying weapon upgrades.
 *
 * Each weapon has an array of prices: COSTS[level] is what the NEXT level costs.
 * A weapon at level 0 pays COSTS[0], at level 1 pays COSTS[1]... and once its level
 * equals COSTS.length there is nothing left to buy.
 *
 * Every upgrade level adds a fixed bonus to both the lowest and highest damage roll.
 * No JavaFX here, so the shop can be tested without opening a window.
 */
public final class Shop {
    // ---- prices for each upgrade level, in gold ----
    public static final int[] SWORD_COSTS = {15, 30, 60};
    public static final int[] BOW_COSTS = {20, 40, 80};

    // ---- extra damage per upgrade level ----
    public static final int SWORD_BONUS_PER_LEVEL = 5;   // sword 3-5 -> 5-7 -> 7-9 -> 9-11
    public static final int BOW_BONUS_PER_LEVEL = 3;     // bow 2-3 -> 3-4 -> 4-5 -> 5-6

    /** Private constructor: this class only has static methods, so it is never created. */
    private Shop() {
    }

    /** Tries to buy the next sword level. Returns the message to show the player. */
    public static String buySword(Player player) {
        int price = nextCost(SWORD_COSTS, player.getSwordLevel());
        if (price == -1) {
            return "Your sword can't be made any sharper.";
        }
        if (!player.spendGold(price)) {
            return "You need " + price + " gold for that (you have " + player.getGold() + ").";
        }
        player.upgradeSword();
        return "Sword upgraded to level " + player.getSwordLevel() + "!";
    }

    /** Tries to buy the next bow level. Returns the message to show the player. */
    public static String buyBow(Player player) {
        int price = nextCost(BOW_COSTS, player.getBowLevel());
        if (price == -1) {
            return "Your bow can't be made any stronger.";
        }
        if (!player.spendGold(price)) {
            return "You need " + price + " gold for that (you have " + player.getGold() + ").";
        }
        player.upgradeBow();
        return "Bow upgraded to level " + player.getBowLevel() + "!";
    }

    /**
     * The price of the next level, or -1 if the weapon is already at its highest level.
     * @param costs the weapon's price list
     * @param level the weapon's current level (0 = never upgraded)
     */
    public static int nextCost(int[] costs, int level) {
        if (level >= costs.length) {
            return -1;   // past the end of the array: fully upgraded
        }
        return costs[level];
    }

    /** The sword's highest level (one level per price in the array). */
    public static int maxSwordLevel() {
        return SWORD_COSTS.length;
    }

    /** The bow's highest level. */
    public static int maxBowLevel() {
        return BOW_COSTS.length;
    }
}
