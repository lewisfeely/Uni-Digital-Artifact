package com.lewis.rpg.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A sword swing: a square hitbox one tile in front of the player that exists
 * for a fraction of a second (DURATION).
 *
 * Because the hitbox lasts several frames, it could touch the same rat on every one of
 * them. To stop one swing doing damage 8 times, it remembers who it has already hit
 * (alreadyHit) and GameState skips those enemies.
 */
public class MeleeSwing {
    /** How long the hitbox stays active, in seconds. */
    public static final double DURATION = 0.15;

    // ---- where the swing is ----
    private final double centreX;        // centre of the hitbox, in squares
    private final double centreY;
    private final double angleDegrees;   // which way it was swung, for drawing the arc

    // ---- changes while the swing is active ----
    private double timeLeft = DURATION;
    private final List<Enemy> alreadyHit = new ArrayList<>();

    public MeleeSwing(double centreX, double centreY, double angleDegrees) {
        this.centreX = centreX;
        this.centreY = centreY;
        this.angleDegrees = angleDegrees;
    }

    /** A 1 x 1 square centred on the swing point (centre - 0.5 = top-left corner). */
    public Hitbox getHitbox() {
        return new Hitbox(centreX - 0.5, centreY - 0.5, 1.0, 1.0);
    }

    // ---- the "hit each enemy only once" list ----

    public boolean hasHit(Enemy enemy) {
        return alreadyHit.contains(enemy);
    }

    public void markHit(Enemy enemy) {
        alreadyHit.add(enemy);
    }

    // ---- lifetime ----

    /** Counts the swing's remaining time down. */
    public void update(double deltaSeconds) {
        timeLeft -= deltaSeconds;
    }

    /** True once the swing has ended - GameState then sets its swing back to null. */
    public boolean isFinished() {
        return timeLeft <= 0;
    }

    /** 1.0 when the swing starts, falling to 0.0 when it ends - used to fade the drawing. */
    public double getProgressLeft() {
        return Math.max(0, timeLeft / DURATION);
    }

    // ---- getters used for drawing ----

    public double getCentreX() {
        return centreX;
    }

    public double getCentreY() {
        return centreY;
    }

    public double getAngleDegrees() {
        return angleDegrees;
    }
}
