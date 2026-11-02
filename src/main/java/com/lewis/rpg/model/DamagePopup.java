package com.lewis.rpg.model;

/** A damage number that floats up from whoever was hit and fades out. */
public class DamagePopup {
    public static final double LIFETIME = 0.7;

    private final double x;
    private double y;
    private final String text;
    private final boolean playerWasHit;
    private double timeLeft = LIFETIME;

    public DamagePopup(double x, double y, String text, boolean playerWasHit) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.playerWasHit = playerWasHit;
    }

    public void update(double deltaSeconds) {
        timeLeft -= deltaSeconds;
        y -= 0.8 * deltaSeconds;   // drift upwards
    }

    public boolean isFinished() {
        return timeLeft <= 0;
    }

    /** 1.0 when it appears, falling to 0.0 when it disappears. */
    public double getOpacity() {
        return Math.max(0, timeLeft / LIFETIME);
    }

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
