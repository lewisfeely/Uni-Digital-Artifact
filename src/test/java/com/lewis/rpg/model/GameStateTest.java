package com.lewis.rpg.model;

import com.lewis.rpg.util.LevelLoader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
}
