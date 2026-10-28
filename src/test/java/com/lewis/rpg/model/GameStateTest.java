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

    @Test
    void unevenLinesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> LevelLoader.parse(List.of("###", "#P", "###")));
    }
}
