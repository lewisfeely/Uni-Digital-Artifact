package com.lewis.rpg.model;

import java.util.Random;

/**
 * Shared combat rules, kept in one place so the sword, the bow and the rat's bite
 * all work out damage the same way.
 */
public final class Combat {

    /** Private constructor: this class only holds static helper methods. */
    private Combat() {
    }

    /**
     * A random whole number of damage between min and max, inclusive.
     *
     * random.nextInt(n) gives 0 to n-1, so nextInt(max - min + 1) gives 0 to (max - min),
     * and adding min shifts that to min..max. For 3..5: nextInt(3) is 0, 1 or 2 -> 3, 4 or 5.
     *
     * The Random is passed in rather than created here, so tests can pass one with a fixed
     * seed and get the same "random" numbers on every run.
     */
    public static int rollDamage(int min, int max, Random random) {
        return min + random.nextInt(max - min + 1);
    }
}
