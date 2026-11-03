package com.lewis.rpg;

/**
 * The program's real starting point - run THIS class from IntelliJ (green arrow next to main).
 *
 * Why it exists: JavaFX checks at start-up whether the class holding main() extends
 * Application. If it does and the project isn't set up as a Java module, the program stops
 * with "JavaFX runtime components are missing". Starting from this plain class, which then
 * calls App.main(), avoids that check.
 */
public class Launcher {

    /** Hands straight over to App, which opens the game window. */
    public static void main(String[] args) {
        App.main(args);
    }
}
