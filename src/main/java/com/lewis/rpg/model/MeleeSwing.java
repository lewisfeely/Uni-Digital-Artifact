package com.lewis.rpg.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A sword swing: a square hitbox one tile in front of the player that exists
 * for a fraction of a second. It remembers who it has already hit so that
 * one swing can only damage each enemy once.
 */
public class MeleeSwing {
    /** How long the hitbox stays active, in seconds. */
    public static final double DURATION = 0.15;

    private final double centreX;
    private final double centreY;
    private final double angleDegrees;
    private double timeLeft = DURATION;
    private final List<Enemy> alreadyHit = new ArrayList<>();

    public MeleeSwing(double centreX, double centreY, double angleDegrees) {
        this.centreX = centreX;
        this.centreY = centreY;
        this.angleDegrees = angleDegrees;
    }

    public Hitbox getHitbox() {
        return new Hitbox(centreX - 0.5, centreY - 0.5, 1.0, 1.0);
    }

    public boolean hasHit(Enemy enemy) {
        return alreadyHit.contains(enemy);
    }

    public void markHit(Enemy enemy) {
        alreadyHit.add(enemy);
    }

    public void update(double deltaSeconds) {
        timeLeft -= deltaSeconds;
    }

    public boolean isFinished() {
        return timeLeft <= 0;
    }

    /** 1.0 when the swing starts, falling to 0.0 when it ends - used to fade the drawing. */
    public double getProgressLeft() {
        return Math.max(0, timeLeft / DURATION);
    }

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
