package com.lewis.rpg.model;

/**
 * A damage number ("-3") that floats up from whoever was hit and fades out.
 * GameState creates one on every hit and removes it once its time runs out;
 * the renderer only reads it.
 */
public class DamagePopup {
    /** Seconds the number stays on screen. */
    public static final double LIFETIME = 0.7;

    // ---- state ----
    private final double x;              // centre, in squares
    private double y;                    // moves up over time
    private final String text;           // e.g. "-3"
    private final boolean playerWasHit;  // true = draw in red, false = draw in yellow
    private double timeLeft = LIFETIME;

    public DamagePopup(double x, double y, String text, boolean playerWasHit) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.playerWasHit = playerWasHit;
    }

    /** Counts down the lifetime and drifts the number upwards (y gets smaller going up). */
    public void update(double deltaSeconds) {
        timeLeft -= deltaSeconds;
        y -= 0.8 * deltaSeconds;   // 0.8 squares per second upwards
    }

    /** True once the number has faded out - GameState then removes it from its list. */
    public boolean isFinished() {
        return timeLeft <= 0;
    }

    /** 1.0 when it appears, falling to 0.0 when it disappears - used as the drawing's transparency. */
    public double getOpacity() {
        return Math.max(0, timeLeft / LIFETIME);
    }

    // ---- getters used for drawing ----

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public String getText() {
        return text;
    }

    public boolean isPlayerWasHit() {
        return playerWasHit;
    }
}
