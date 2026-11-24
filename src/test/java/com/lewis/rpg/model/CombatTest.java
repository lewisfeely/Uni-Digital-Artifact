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
        // further away than the rat's sight range (15 squares), so it never notices you
        GameState state = level("######################", "#P..................r#", "######################");
        Enemy rat = onlyEnemy(state);
        runFor(state, 1.0);
        assertEquals(20, rat.getCol());
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

    // ---------------------------------------------------------------- the orc

    @Test
    void capitalOInALevelMakesAnOrc() {
        GameState state = level("#####", "#P.O#", "#####");
        Enemy orc = onlyEnemy(state);
        assertEquals(Enemy.Type.ORC, orc.getType());
        assertEquals(50, orc.getMaxHp());
    }

    @Test
    void ratsAreStillRats() {
        GameState state = level("#####", "#P.r#", "#####");
        assertEquals(Enemy.Type.RAT, onlyEnemy(state).getType());
    }

    @Test
    void orcWarnsForLongerThanARat() {
        GameState state = level("#####", "#PO.#", "#####");
        Player player = state.getPlayer();
        runFor(state, 0.5);                        // a rat would have bitten by now (0.4 s windup)
        assertEquals(player.getMaxHp(), player.getHp());
        assertEquals(Enemy.State.WINDUP, onlyEnemy(state).getState());
    }

    @Test
    void orcSmashesHardAndSaysSo() {
        GameState state = level("#####", "#PO.#", "#####");
        Player player = state.getPlayer();
        runFor(state, 0.8);                        // 0.7 s windup, then the smash
        int damage = player.getMaxHp() - player.getHp();
        assertTrue(damage >= 10 && damage <= 15, "damage was " + damage);
        assertTrue(state.getMessage().startsWith("The Orc smashes you"), state.getMessage());
    }

    // ---------------------------------------------------------------- the knight boss

    @Test
    void capitalBInALevelMakesTheKnight() {
        GameState state = level("######", "#P..B#", "######");
        Enemy knight = onlyEnemy(state);
        assertEquals(Enemy.Type.KNIGHT, knight.getType());
        assertEquals(120, knight.getMaxHp());
    }

    @Test
    void knightsHurtboxCoversHisWholeBody() {
        Enemy knight = Enemy.knight(5, 5);
        Hitbox body = knight.getHurtbox();
        // a point at head height, most of a square above his feet, still counts as a hit
        assertTrue(body.intersects(new Hitbox(5.5, 4.5, 0.05, 0.05)));
        // a rat's hurtbox at the same spot doesn't reach that high
        assertFalse(Enemy.rat(5, 5).getHurtbox().intersects(new Hitbox(5.5, 4.5, 0.05, 0.05)));
    }

    @Test
    void knightSlashesHardAndSaysSo() {
        GameState state = level("#####", "#PB.#", "#####");
        Player player = state.getPlayer();
        runFor(state, 0.5);                        // still raising his sword (0.6 s windup)
        assertEquals(player.getMaxHp(), player.getHp());
        runFor(state, 0.3);                        // the slash lands
        int damage = player.getMaxHp() - player.getHp();
        assertTrue(damage >= 12 && damage <= 18, "damage was " + damage);
        assertTrue(state.getMessage().startsWith("The Knight slashes you"), state.getMessage());
    }

    @Test
    void knightBlocksHitsUntilHeAttacks() {
        // the knight is 3 squares away, so he marches (CHASE) before he can attack
        GameState state = level("#######", "#P..B.#", "#######");
        Enemy knight = onlyEnemy(state);
        runFor(state, 0.1);
        assertEquals(Enemy.State.CHASE, knight.getState());
        assertTrue(knight.isGuarding());
        state.setAim(4.5, 1.5);
        state.shootArrow();
        runFor(state, 0.3);                          // the arrow reaches him...
        assertEquals(knight.getMaxHp(), knight.getHp());   // ...and is blocked
        assertTrue(state.getMessage().contains("blocks"), state.getMessage());
    }

    @Test
    void knightCanBeHitWhileAttacking() {
        GameState state = level("#####", "#PB.#", "#####");
        Enemy knight = onlyEnemy(state);
        runFor(state, 0.1);                          // next to you: he raises his sword
        assertEquals(Enemy.State.WINDUP, knight.getState());
        assertFalse(knight.isGuarding());
        state.setAim(2.5, 1.5);
        state.swing();
        runFor(state, 0.05);
        assertTrue(knight.getHp() < knight.getMaxHp());   // guard open: the hit lands
    }

    @Test
    void otherEnemiesNeverGuard() {
        assertFalse(Enemy.rat(1, 1).isGuarding());
        assertFalse(Enemy.orc(1, 1).isGuarding());
    }

    @Test
    void knightMarchesWithoutStopping() {
        // a long corridor, so he has several squares to walk before reaching you
        GameState state = level("#############", "#P.........B#", "#############");
        Enemy knight = onlyEnemy(state);
        runFor(state, 0.6);                          // let him wake up and start walking
        int framesStill = 0;
        for (int i = 0; i < 90; i++) {               // 1.5 seconds of marching, 60 frames a second
            state.update(1.0 / 60);
            if (knight.getState() == Enemy.State.CHASE && !knight.isMoving()) {
                framesStill++;
            }
        }
        // at most a single frame's pause per step (about 3 steps in 1.5 s) - before, he stood still for ~12
        assertTrue(framesStill <= 3, "stood still for " + framesStill + " frames");
    }

    @Test
    void knightKeepsHisOverallSpeed() {
        GameState state = level("#############", "#P.........B#", "#############");
        Enemy knight = onlyEnemy(state);
        int startCol = knight.getCol();
        runFor(state, 0.45 * 4 + 0.05);              // four step intervals
        assertEquals(4, startCol - knight.getCol(), 1);   // about 4 squares, the same pace as before
    }

    @Test
    void ratsAndOrcsAlsoMoveWithoutStopping() {
        for (String enemy : new String[] {"r", "O"}) {
            GameState state = level("#############", "#P........." + enemy + "#", "#############");
            Enemy e = onlyEnemy(state);
            runFor(state, 0.5);                      // let it wake up and start moving
            int framesStill = 0;
            for (int i = 0; i < 60; i++) {           // 1 second at 60 frames a second
                state.update(1.0 / 60);
                if (e.getState() == Enemy.State.CHASE && !e.isMoving()) {
                    framesStill++;
                }
            }
            assertTrue(framesStill <= 3, enemy + " stood still for " + framesStill + " frames");
        }
    }
}
