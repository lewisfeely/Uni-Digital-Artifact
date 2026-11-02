package com.lewis.rpg.model;

import java.util.Random;

/** Shared combat rules. */
public final class Combat {

    private Combat() {
    }

    /**
     * A random whole number of damage between min and max, inclusive.
     * The Random is passed in so tests can use a fixed seed.
     */
    public static int rollDamage(int min, int max, Random random) {
        return min + random.nextInt(max - min + 1);
    }
}
