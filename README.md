# Dungeon RPG

A turn-based, tile-based dungeon crawler built in Java with JavaFX, as a 3-month
project for practising core programming skills: arrays, loops, methods, classes,
inheritance, interfaces, file handling and a SQLite database for save games.

## Getting started (Mac)

1. **Install IntelliJ IDEA Community Edition** (free): https://www.jetbrains.com/idea/download
2. **Open this folder** in IntelliJ (File → Open → `Uni-Digital-Artifact`).
   It detects `pom.xml` as a Maven project and downloads JavaFX, SQLite and JUnit.
   If it asks, click *Load Maven Project* / *Trust Project*.
3. **Set the JDK to Java 21**: File → Project Structure → Project → SDK.
   If you don't have one, choose *Add SDK → Download JDK* → version 21, vendor
   *Eclipse Temurin*.
4. **Run the game**: open `src/main/java/com/lewis/rpg/Launcher.java` and click the
   green ▶ next to `main`. Alternatively, in the Maven panel run
   *Plugins → javafx → javafx:run*.
5. **Run the tests**: right-click `src/test/java` → *Run 'All Tests'*.

Move with the arrow keys or WASD. Walk onto chests to collect gold.

## Project structure

```
src/main/java/com/lewis/rpg/
├── App.java            JavaFX window, key handling
├── Launcher.java       Run this from IntelliJ
├── model/              Game rules - no JavaFX code here
│   ├── Tile.java         Tile numbers stored in the map array
│   ├── Direction.java    UP / DOWN / LEFT / RIGHT
│   ├── GameMap.java      The 2D int array + helper methods
│   ├── Entity.java       Base class: name, hp, position
│   ├── Player.java       Extends Entity
│   └── GameState.java    Map + player + movement/chest rules
├── view/
│   └── GameRenderer.java Draws the map, player and HUD on a Canvas
└── util/
    └── LevelLoader.java  Reads levels/*.txt into a 2D array

src/main/resources/levels/level1.txt   # = wall  . = floor  C = chest  P = start
src/test/java/...                      JUnit tests for the game rules
```

The key design rule: **`model` never imports JavaFX.** The view reads the game state
and draws it. That keeps the rules testable and makes it easy to change how things look.

## Roadmap

**Month 1 – Core gameplay**
- [x] Map as a 2D array, loaded from a text file
- [x] Player movement and wall collision
- [x] Chests and gold
- [ ] `Enemy` class extending `Entity`, placed on the map (`E` in level files)
- [ ] Turn-based combat (attack, defence, random damage with `Random`)
- [ ] Inventory as an `ArrayList<Item>`; `Weapon` and `Potion` extend `Item`

**Month 2 – Visuals and content**
- [ ] Replace coloured squares with a tileset (kenney.nl, itch.io "16x16 dungeon")
- [ ] Inventory and combat screens, title menu
- [ ] Multiple levels with stairs between them
- [ ] Levelling up and XP

**Month 3 – Saving, AI and polish**
- [ ] SQLite save slots (`persistence/SaveManager.java`, `PreparedStatement` only)
- [ ] Enemy AI: chase the player, then BFS pathfinding
- [ ] A boss fight
- [ ] Sound effects, more tests
