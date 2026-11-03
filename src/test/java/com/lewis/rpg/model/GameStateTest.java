package com.lewis.rpg.model;

import com.lewis.rpg.util.LevelLoader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the movement rules: walls, chests, keys, doors, facing and level loading.
 *
 * Each test builds a tiny level from a few lines of text with LevelLoader.parse,
 * does something (moves the player), then uses assertions to check the result.
 * No window is opened, because the rules in GameState never use JavaFX.
 */
class GameStateTest {

    private GameState smallLevel() {
        return LevelLoader.parse(List.of(
                "#####",
                "#P.C#",
                "#####"
        ));
    }

    @Test
    void playerStartsOnP() {
        GameState state = smallLevel();
        assertEquals(1, state.getPlayer().getRow());
        assertEquals(1, state.getPlayer().getCol());
    }

    @Test
    void wallsBlockMovement() {
        GameState state = smallLevel();
        assertFalse(state.movePlayer(Direction.UP));
        assertEquals(1, state.getPlayer().getRow());
    }

    @Test
    void walkingOntoChestGivesGoldAndRemovesChest() {
        GameState state = smallLevel();
        state.movePlayer(Direction.RIGHT);
        state.movePlayer(Direction.RIGHT);
        assertEquals(10, state.getPlayer().getGold());
        assertEquals(0, state.getMap().countTiles(Tile.CHEST));
    }

    private GameState doorLevel() {
        return LevelLoader.parse(List.of(
                "######",
                "#KP.D.",
                "######"
        ));
    }

    @Test
    void lockedDoorBlocksWithoutKey() {
        GameState state = doorLevel();
        state.movePlayer(Direction.RIGHT);           // now next to the door
        assertFalse(state.movePlayer(Direction.RIGHT));
        assertEquals(Tile.DOOR, state.getMap().getTile(1, 4));
        assertEquals(3, state.getPlayer().getCol());
    }

    @Test
    void keyUnlocksDoorAndIsUsedUp() {
        GameState state = doorLevel();
        state.movePlayer(Direction.LEFT);            // pick up the key
        assertEquals(1, state.getPlayer().getKeys());
        state.movePlayer(Direction.RIGHT);
        state.movePlayer(Direction.RIGHT);
        state.movePlayer(Direction.RIGHT);           // unlock
        assertEquals(Tile.OPEN_DOOR, state.getMap().getTile(1, 4));
        assertEquals(0, state.getPlayer().getKeys());
        assertTrue(state.movePlayer(Direction.RIGHT)); // walk through
        assertEquals(4, state.getPlayer().getCol());
    }

    @Test
    void playerTurnsToFaceWallEvenWhenBlocked() {
        GameState state = smallLevel();
        state.movePlayer(Direction.UP);
        assertEquals(Direction.UP, state.getPlayer().getFacing());
    }

    @Test
    void torchWallsBlockLikeNormalWalls() {
        GameState state = LevelLoader.parse(List.of(
                "#T#",
                "#P#",
                "###"));
        assertFalse(state.movePlayer(Direction.UP));
        assertTrue(state.getMap().blocksDarts(0, 1));
    }

    @Test
    void unevenLinesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> LevelLoader.parse(List.of("###", "#P", "###")));
    }

    // ------------------------------------------------------------------ levels (step 3)

    /** Builds a GameState with two tiny levels in its array, the player starting on level 1. */
    private GameState twoLevels() {
        Level first = LevelLoader.parse(List.of(
                "#####",
                "#P.C#",
                "#####")).getLevel(0);
        Level second = LevelLoader.parse(List.of(
                "#####",
                "#P..#",
                "#####")).getLevel(0);
        return new GameState(new Level[] {first, second}, new Player("Hero", 1, 1));
    }

    @Test
    void gameStartsOnTheFirstLevel() {
        GameState state = twoLevels();
        assertEquals(0, state.getCurrentLevelIndex());
        assertEquals(2, state.getLevelCount());
        assertEquals(1, state.getMap().countTiles(Tile.CHEST));   // level 1's map, which has the chest
    }

    @Test
    void loadedLevelIsWrappedInAOneLevelArray() {
        GameState state = smallLevel();
        assertEquals(1, state.getLevelCount());
        assertSame(state.getLevel(0).getMap(), state.getMap());
    }

    @Test
    void emptyLevelArrayIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new GameState(new Level[0], new Player("Hero", 1, 1)));
    }

    // ------------------------------------------------------------------ travelling (steps 4 and 5)

    /** Level 1: start, chest, then '+' stairs. Level 2: '-' stairs back, then floor. */
    private GameState stairsGame() {
        Level first = LevelLoader.parseLevel(List.of(
                "######",
                "#PC.+#",
                "######"));
        Level second = LevelLoader.parseLevel(List.of(
                "######",
                "#-...#",
                "######"));
        return new GameState(new Level[] {first, second},
                new Player("Hero", first.getCheckpointRow(), first.getCheckpointCol()));
    }

    /** Walks right n times. */
    private void walkRight(GameState state, int n) {
        for (int i = 0; i < n; i++) {
            state.movePlayer(Direction.RIGHT);
        }
    }

    @Test
    void levelWithoutPStartsOnItsStairs() {
        Level level = LevelLoader.parseLevel(List.of(
                "#####",
                "#-..#",
                "#####"));
        assertFalse(level.hasPlayerStart());
        assertEquals(1, level.getCheckpointRow());
        assertEquals(1, level.getCheckpointCol());
    }

    @Test
    void parseStillNeedsAP() {
        assertThrows(IllegalArgumentException.class,
                () -> LevelLoader.parse(List.of("#####", "#-..#", "#####")));
    }

    @Test
    void stairsUpTakeYouToTheNextLevelOnItsStairs() {
        GameState state = stairsGame();
        walkRight(state, 3);                                   // chest, floor, stairs
        assertEquals(1, state.getCurrentLevelIndex());
        assertEquals(1, state.getPlayer().getRow());
        assertEquals(1, state.getPlayer().getCol());           // standing on level 2's '-'
        assertEquals("You move up to level 2.", state.getMessage());
    }

    @Test
    void goingBackKeepsTheFirstLevelAsYouLeftIt() {
        GameState state = stairsGame();
        walkRight(state, 3);                                   // opens the chest, then up to level 2
        state.movePlayer(Direction.RIGHT);                     // step off the stairs...
        state.movePlayer(Direction.LEFT);                      // ...and back onto them
        assertEquals(0, state.getCurrentLevelIndex());
        assertEquals(4, state.getPlayer().getCol());           // back on level 1's '+'
        assertEquals(0, state.getMap().countTiles(Tile.CHEST)); // the chest is still opened
        assertEquals(10, state.getPlayer().getGold());          // and the gold came with you
    }

    @Test
    void changingLevelClearsArrowsAndSwing() {
        GameState state = stairsGame();
        state.shootArrow();
        state.swing();
        assertTrue(state.changeLevel(1));
        assertTrue(state.getArrows().isEmpty());
        assertNull(state.getSwing());
    }

    @Test
    void stairsOnTheLastLevelGoNowhere() {
        GameState state = stairsGame();
        assertFalse(state.changeLevel(2));                     // there is no third level
        assertFalse(state.changeLevel(-1));
        assertEquals(0, state.getCurrentLevelIndex());
    }

    @Test
    void realLevelFilesLoadTogether() {
        GameState state = LevelLoader.loadGame(new String[] {"/levels/test_small.txt", "/levels/level2.txt"});
        assertEquals(2, state.getLevelCount());
        assertEquals(0, state.getCurrentLevelIndex());
    }
}
