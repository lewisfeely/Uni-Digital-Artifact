package com.lewis.rpg.model;

import com.lewis.rpg.util.LevelLoader;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Hitboxes, the sword, the bow and the rat - all tested by calling update(seconds), no window needed. */
class CombatTest {

    private void runFor(GameState state, double seconds) {
        double step = 0.02;
        for (double t = 0; t < seconds - 1e-9; t += step) {
            state.update(step);
        }
    }

    private GameState level(String... lines) {
        GameState state = LevelLoader.parse(List.of(lines));
        state.setRandom(new Random(42));
        return state;
    }

    private Enemy onlyEnemy(GameState state) {
        assertEquals(1, state.getEnemies().size());
        return state.getEnemies().get(0);
    }

    // ---------------------------------------------------------------- hitboxes

    @Test
    void overlappingHitboxesIntersect() {
        Hitbox a = new Hitbox(0, 0, 1, 1);
        assertTrue(a.intersects(new Hitbox(0.5, 0.5, 1, 1)));
        assertFalse(a.intersects(new Hitbox(1.5, 0, 1, 1)));    // gap on the x axis
        assertFalse(a.intersects(new Hitbox(0, 1.5, 1, 1)));    // gap on the y axis
    }

    @Test
    void damageRollStaysInRange() {
        Random random = new Random(1);
        for (int i = 0; i < 100; i++) {
            int damage = Combat.rollDamage(3, 5, random);
            assertTrue(damage >= 3 && damage <= 5);
        }
    }

    // ---------------------------------------------------------------- sword

    @Test
    void swingHitsRatInFront() {
        GameState state = level("######", "#Pr..#", "######");
        Enemy rat = onlyEnemy(state);
        state.setAim(4.5, 1.5);            // cursor to the right
        assertTrue(state.swing());
        state.update(0.02);
        int damage = rat.getMaxHp() - rat.getHp();
        assertTrue(damage >= 3 && damage <= 5, "damage was " + damage);
    }

    @Test
    void swingBehindMisses() {
        GameState state = level("######", "#.Pr.#", "######");
        Enemy rat = onlyEnemy(state);
        state.setAim(0.5, 1.5);            // cursor to the left, rat is on the right
        state.swing();
        runFor(state, 0.2);
        assertEquals(rat.getMaxHp(), rat.getHp());
    }

    @Test
    void oneSwingOnlyHitsOnce() {
        GameState state = level("######", "#Pr..#", "######");
        Enemy rat = onlyEnemy(state);
        state.setAim(4.5, 1.5);
        state.swing();
        runFor(state, MeleeSwing.DURATION + 0.05);   // the hitbox overlaps the rat on several frames
        int damage = rat.getMaxHp() - rat.getHp();
        assertTrue(damage >= 3 && damage <= 5, "damage was " + damage);
    }

    @Test
    void swingHasCooldown() {
        GameState state = level("#####", "#P..#", "#####");
        assertTrue(state.swing());
        assertFalse(state.swing());
        runFor(state, GameState.SWING_COOLDOWN + 0.02);
        assertTrue(state.swing());
    }

    @Test
    void killingRatRemovesItAndGivesGold() {
        GameState state = level("######", "#Pr..#", "######");
        Enemy rat = onlyEnemy(state);
        rat.takeDamage(rat.getMaxHp() - 1);   // 1 HP left
        state.setAim(4.5, 1.5);
        state.swing();
        state.update(0.02);
        assertTrue(state.getEnemies().isEmpty());
        assertEquals(rat.getGoldDrop(), state.getPlayer().getGold());
    }

    // ---------------------------------------------------------------- bow

    @Test
    void arrowHitsRatAtRange() {
        GameState state = level("##########", "#P......r#", "##########");
        Enemy rat = onlyEnemy(state);
        state.setAim(8.5, 1.5);
        assertTrue(state.shootArrow());
        runFor(state, 0.8);                // about 7 squares at 10 squares a second
        int damage = rat.getMaxHp() - rat.getHp();
        assertTrue(damage >= 2 && damage <= 3, "damage was " + damage);
        assertTrue(state.getArrows().isEmpty());
    }

    @Test
    void arrowStopsAtWall() {
        GameState state = level("######", "#P...#", "######");
        state.setAim(4.5, 1.5);
        state.shootArrow();
        runFor(state, 1.0);
        assertTrue(state.getArrows().isEmpty());
    }

    @Test
    void bowHasCooldown() {
        GameState state = level("######", "#P...#", "######");
        assertTrue(state.shootArrow());
        assertFalse(state.shootArrow());
        assertEquals(1, state.getArrows().size());
    }

    // ---------------------------------------------------------------- the rat

    @Test
    void playerCannotWalkIntoRat() {
        GameState state = level("#####", "#Pr.#", "#####");
        assertFalse(state.movePlayer(Direction.RIGHT));
        assertEquals(1, state.getPlayer().getCol());
    }

    @Test
    void ratChasesPlayerInSight() {
        GameState state = level("##########", "#P....r..#", "##########");   // 5 squares away: in sight
        Enemy rat = onlyEnemy(state);
        runFor(state, 1.0);
        assertTrue(rat.getCol() < 6, "rat stayed at column " + rat.getCol());
    }

    @Test
    void ratStaysIdleWhenFarAway() {
        GameState state = level("############", "#P........r#", "############");
        Enemy rat = onlyEnemy(state);
        runFor(state, 1.0);
        assertEquals(10, rat.getCol());
        assertEquals(Enemy.State.IDLE, rat.getState());
    }

    @Test
    void ratWindsUpThenBites() {
        GameState state = level("#####", "#Pr.#", "#####");
        Player player = state.getPlayer();
        runFor(state, 0.2);                       // still winding up
        assertEquals(player.getMaxHp(), player.getHp());
        runFor(state, 0.5);                       // the bite has landed
        int damage = player.getMaxHp() - player.getHp();
        assertTrue(damage >= 1 && damage <= 3, "damage was " + damage);
    }

    @Test
    void steppingAwayDuringWindupDodgesTheBite() {
        GameState state = level("#######", "#.Pr..#", "#######");
        Player player = state.getPlayer();
        runFor(state, 0.1);                        // rat starts winding up
        state.movePlayer(Direction.LEFT);
        runFor(state, 0.4);                        // bite happens where the player was
        assertEquals(player.getMaxHp(), player.getHp());
    }

    @Test
    void invincibilityStopsTwoBitesAtOnce() {
        GameState state = level("#####", "#rPr#", "#####");   // a rat on each side bites at the same moment
        Player player = state.getPlayer();
        runFor(state, 0.7);
        int damage = player.getMaxHp() - player.getHp();
        assertTrue(damage >= 1 && damage <= 3, "damage was " + damage);
    }
}
