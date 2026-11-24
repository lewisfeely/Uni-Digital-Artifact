package com.lewis.rpg.model;

import com.lewis.rpg.util.LevelLoader;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** The merchant: opening the shop, buying upgrades, prices, and upgraded damage. */
class ShopTest {

    /** A player standing next to the merchant ($), with a rat further along the corridor. */
    private GameState shopLevel() {
        GameState state = LevelLoader.parse(List.of(
                "#######",
                "#$P...#",
                "#######"));
        state.setRandom(new Random(42));
        return state;
    }

    @Test
    void walkingIntoTheMerchantOpensTheShop() {
        GameState state = shopLevel();
        assertFalse(state.movePlayer(Direction.LEFT));   // he's solid
        assertTrue(state.isShopOpen());
        assertEquals(2, state.getPlayer().getCol());
    }

    @Test
    void buyingWithEnoughGoldUpgradesAndCharges() {
        Player player = new Player("Hero", 1, 1);
        player.addGold(20);
        Shop.buySword(player);
        assertEquals(1, player.getSwordLevel());
        assertEquals(5, player.getGold());               // 20 - 15
    }

    @Test
    void buyingWithoutEnoughGoldChangesNothing() {
        Player player = new Player("Hero", 1, 1);
        player.addGold(10);
        String message = Shop.buyBow(player);            // costs 20
        assertEquals(0, player.getBowLevel());
        assertEquals(10, player.getGold());
        assertTrue(message.startsWith("You need 20 gold"), message);
    }

    @Test
    void pricesGoUpEachLevelAndStopAtMax() {
        Player player = new Player("Hero", 1, 1);
        player.addGold(1000);
        for (int i = 0; i < 5; i++) {                     // try to buy more levels than exist
            Shop.buySword(player);
        }
        assertEquals(Shop.maxSwordLevel(), player.getSwordLevel());
        assertEquals(1000 - 15 - 30 - 60, player.getGold());
        assertEquals(-1, Shop.nextCost(Shop.SWORD_COSTS, player.getSwordLevel()));
    }

    @Test
    void shopKeysOnlyWorkWhileTheShopIsOpen() {
        GameState state = shopLevel();
        state.getPlayer().addGold(100);
        state.buySwordUpgrade();                         // shop closed: ignored
        assertEquals(0, state.getPlayer().getSwordLevel());
        state.movePlayer(Direction.LEFT);                // open it
        state.buySwordUpgrade();
        assertEquals(1, state.getPlayer().getSwordLevel());
        state.closeShop();
        assertFalse(state.isShopOpen());
    }

    @Test
    void theGameIsPausedWhileShopping() {
        GameState state = LevelLoader.parse(List.of(
                "#########",
                "#$P....r#",
                "#########"));
        state.movePlayer(Direction.LEFT);                // open the shop
        Enemy rat = state.getEnemies().get(0);
        int startCol = rat.getCol();
        for (int i = 0; i < 100; i++) {
            state.update(0.02);                          // 2 seconds pass...
        }
        assertEquals(startCol, rat.getCol());            // ...but the rat hasn't moved
    }

    @Test
    void upgradedSwordHitsHarder() {
        GameState state = LevelLoader.parse(List.of("#####", "#Pr.#", "#####"));
        state.setRandom(new Random(1));
        Player player = state.getPlayer();
        player.addGold(1000);
        for (int i = 0; i < 3; i++) {
            Shop.buySword(player);                       // max level: +6 damage
        }
        Enemy rat = state.getEnemies().get(0);
        state.setAim(2.5, 1.5);                          // aim at the rat
        state.swing();
        state.update(0.02);
        // a rat has 6 HP and a max-level sword does at least 3 + 6 = 9, so one hit kills it
        assertFalse(rat.isAlive());
    }

    @Test
    void shootingStartsTheBowAnimationFacingTheShot() {
        GameState state = shopLevel();
        state.setAim(0.5, 1.5);                          // cursor to the left of the player
        state.shootArrow();
        assertEquals(GameState.BOW_ANIMATION_TIME, state.getBowAnimationTime(), 1e-9);
        assertTrue(state.isBowAimLeft());
        for (int i = 0; i < 30; i++) {
            state.update(0.02);
        }
        assertEquals(0, state.getBowAnimationTime(), 1e-9);   // finished after 0.35 s
    }
}
