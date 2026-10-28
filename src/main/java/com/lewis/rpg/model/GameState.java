package com.lewis.rpg.model;

/**
 * Everything about the game right now: the map, the player, and the last message.
 * This class knows nothing about JavaFX - the view just reads from it and draws.
 * Keeping it that way means you can unit test all the rules without a window.
 */
public class GameState {
    private static final int CHEST_GOLD = 10;

    private final GameMap map;
    private final Player player;
    private String message = "Use the arrow keys or WASD to explore.";

    public GameState(GameMap map, Player player) {
        this.map = map;
        this.player = player;
    }

    /**
     * Try to move the player one cell. Walls block movement; chests give gold.
     * @return true if the player actually moved
     */
    public boolean movePlayer(Direction direction) {
        int newRow = player.getRow() + direction.getRowChange();
        int newCol = player.getCol() + direction.getColChange();

        if (!map.isWalkable(newRow, newCol)) {
            message = "A wall blocks your way.";
            return false;
        }

        player.moveTo(newRow, newCol);

        if (map.getTile(newRow, newCol) == Tile.CHEST) {
            player.addGold(CHEST_GOLD);
            map.setTile(newRow, newCol, Tile.FLOOR);
            message = "You open a chest and find " + CHEST_GOLD + " gold!";
        } else {
            message = "";
        }
        return true;
    }

    public GameMap getMap() {
        return map;
    }

    public Player getPlayer() {
        return player;
    }

    public String getMessage() {
        return message;
    }
}
