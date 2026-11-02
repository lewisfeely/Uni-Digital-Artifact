package com.lewis.rpg.model;

import com.lewis.rpg.util.LevelLoader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Darts and dart traps - all tested by calling update(seconds), no window needed. */
class DartTrapTest {

    /** Runs the game clock forward in small steps, like the game loop does. */
    private void runFor(GameState state, double seconds) {
        double step = 0.05;
        for (double t = 0; t < seconds - 1e-9; t += step) {
            state.update(step);
        }
    }

    @Test
    void trapFiresOnlyAfterItsInterval() {
        DartTrap trap = new DartTrap(0, 0, Direction.RIGHT, 2.0, 2.0);
        assertFalse(trap.update(1.9));
        assertTrue(trap.update(0.2));
        assertFalse(trap.update(0.1));   // timer restarted
    }

    @Test
    void trapWarnsJustBeforeFiring() {
        DartTrap trap = new DartTrap(0, 0, Direction.RIGHT, 2.0, 2.0);
        trap.update(1.0);
        assertFalse(trap.isWarning());
        trap.update(0.7);
        assertTrue(trap.isWarning());
    }

    @Test
    void dartMovesEightSquaresPerSecond() {
        Dart dart = new Dart(0, 2, Direction.RIGHT);   // starts at x = 2.5
        dart.update(0.5);
        assertEquals(6.5, dart.getX(), 1e-9);
        assertEquals(6, dart.getCol());
    }

    @Test
    void dartHitsPlayerInItsPath() {
        GameState state = LevelLoader.parse(List.of(
                "#########",
                "#>.....P#",
                "#########"));
        runFor(state, 3.0);   // first shot at 2.0s, reaches the player well before the next one at 4.0s
        int maxHp = state.getPlayer().getMaxHp();
        // a dart that does full damage knocks you out and you respawn with full HP
        int expectedHp = GameState.DART_DAMAGE >= maxHp ? maxHp : maxHp - GameState.DART_DAMAGE;
        assertEquals(expectedHp, state.getPlayer().getHp());
        assertTrue(state.getMessage().contains("dart") || state.getMessage().contains("collapse"));
        assertTrue(state.getDarts().isEmpty());
    }

    @Test
    void standingInAnAlcoveIsSafe() {
        GameState state = LevelLoader.parse(List.of(
                "#########",
                "#>.....P#",
                "#######.#"));
        state.movePlayer(Direction.DOWN);   // step into the alcove
        runFor(state, 3.0);
        assertEquals(30, state.getPlayer().getHp());
        assertTrue(state.getDarts().isEmpty());   // dart hit the far wall and was removed
    }

    @Test
    void lockedDoorStopsDarts() {
        GameState state = LevelLoader.parse(List.of(
                "#########",
                "#>..D..P#",
                "#########"));
        runFor(state, 3.0);
        assertEquals(30, state.getPlayer().getHp());
    }

    @Test
    void collapsingSendsPlayerBackToCheckpointWithFullHealth() {
        GameState state = LevelLoader.parse(List.of(
                "#########",
                "#>....RP#",
                "#########"));
        state.movePlayer(Direction.LEFT);    // reach the checkpoint at column 6
        state.movePlayer(Direction.RIGHT);   // step back to column 7
        state.getPlayer().takeDamage(28);    // only 2 HP left
        runFor(state, 3.0);
        assertEquals(30, state.getPlayer().getHp());
        assertEquals(6, state.getPlayer().getCol());
    }
}
