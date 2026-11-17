package ambot.level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class LevelValidatorTest {
    static final Tileset TILES = new Tileset(1, 6, 6, new BufferedImage(96, 16, BufferedImage.TYPE_INT_ARGB));
    /** 10x6 tiles, floor top at y=80, map 160x96 px. */
    static final String[] ROOM = {
            "..........",
            "..........",
            "..........",
            "..........",
            "..........",
            "##########" };

    static LevelObject spawn(double x, double y) {
        return new LevelObject(LevelObject.PLAYER_SPAWN, new Rect(x, y, 0, 0), Map.of());
    }

    static LevelObject rect(String type, double x, double y) {
        return new LevelObject(type, new Rect(x, y, 16, 32), Map.of());
    }

    static LevelObject point(String type, double x, double y, Map<String, String> props) {
        return new LevelObject(type, new Rect(x, y, 0, 0), props);
    }

    static List<LevelObject> validObjects() {
        return new ArrayList<>(List.of(spawn(24, 80), rect(LevelObject.EXIT, 128, 48),
                rect(LevelObject.CHECKPOINT, 64, 48)));
    }

    /** '#' solid (gid 1), '-' one-way (gid 3), '^' hazard (gid 4), '9' a solid tile with unknown gid 9. */
    static Level level(Map<String, String> props, List<LevelObject> objects, String... rows) {
        int w = rows[0].length(), h = rows.length;
        int[] solid = new int[w * h], oneway = new int[w * h], hazard = new int[w * h];
        for (int ty = 0; ty < h; ty++) {
            for (int tx = 0; tx < w; tx++) {
                int i = ty * w + tx;
                switch (rows[ty].charAt(tx)) {
                    case '#' -> solid[i] = 1;
                    case '9' -> solid[i] = 9;
                    case '-' -> oneway[i] = 3;
                    case '^' -> hazard[i] = 4;
                    default -> { }
                }
            }
        }
        return new Level("v", props, TILES, w, h, List.of(new TileLayer("solid", w, h, solid),
                new TileLayer("oneway", w, h, oneway), new TileLayer("hazard", w, h, hazard)), objects);
    }

    static List<String> check(List<LevelObject> objects, String... rows) {
        return LevelValidator.check(level(Map.of(), objects, rows));
    }

    static void assertProblem(List<String> problems, String... phrases) {
        boolean found = problems.stream().anyMatch(p -> {
            for (String ph : phrases) if (!p.contains(ph)) return false;
            return true;
        });
        assertTrue(found, "expected a problem mentioning " + List.of(phrases) + ", got " + problems);
    }

    @Test
    void validLevelHasNoProblems() {
        assertEquals(List.of(), check(validObjects(), ROOM));
    }

    @Test
    void heartPickupIsKnown() {
        List<LevelObject> objs = validObjects();
        objs.add(point(LevelObject.PICKUP, 100, 70, Map.of("type", "heart")));
        assertEquals(List.of(), check(objs, ROOM));
    }

    @Test
    void flagsMissingSpawn() {
        List<LevelObject> objs = validObjects();
        objs.remove(0);
        assertProblem(check(objs, ROOM), "player_spawn", "found 0");
    }

    @Test
    void flagsTwoExits() {
        List<LevelObject> objs = validObjects();
        objs.add(rect(LevelObject.EXIT, 96, 48));
        assertProblem(check(objs, ROOM), "exit", "found 2");
    }

    @Test
    void flagsUnknownObjectType() {
        List<LevelObject> objs = validObjects();
        objs.add(point("chest", 40, 70, Map.of()));
        assertProblem(check(objs, ROOM), "unknown object type 'chest'");
    }

    @Test
    void flagsUnknownEnemyType() {
        List<LevelObject> objs = validObjects();
        objs.add(point(LevelObject.ENEMY, 40, 70, Map.of("type", "dragon")));
        assertProblem(check(objs, ROOM), "unknown enemy type 'dragon'");
    }

    @Test
    void flagsEnemyWithoutType() {
        List<LevelObject> objs = validObjects();
        objs.add(point(LevelObject.ENEMY, 40, 70, Map.of()));
        assertProblem(check(objs, ROOM), "enemy", "'type'");
    }

    @Test
    void flagsUnknownPickupType() {
        List<LevelObject> objs = validObjects();
        objs.add(point(LevelObject.PICKUP, 40, 70, Map.of("type", "coin")));
        assertProblem(check(objs, ROOM), "unknown pickup type 'coin'");
    }

    @Test
    void flagsUnknownTileId() {
        assertProblem(check(validObjects(),
                "..........",
                "..........",
                "..........",
                "..........",
                "..........",
                "###9######"), "tile id 9", "solid", "3,5");
    }

    @Test
    void flagsCheckpointInsideSolid() {
        List<LevelObject> objs = validObjects();
        objs.set(2, rect(LevelObject.CHECKPOINT, 64, 64)); // reaches 16 px into the floor
        assertProblem(check(objs, ROOM), "checkpoint", "solid");
    }

    @Test
    void flagsSpawnInsideSolid() {
        List<LevelObject> objs = validObjects();
        objs.set(0, spawn(24, 90)); // feet 10 px below the floor top
        assertProblem(check(objs, ROOM), "player_spawn", "solid");
    }

    @Test
    void flagsCheckpointOverHazard() {
        assertProblem(check(validObjects(),
                "..........",
                "..........",
                "..........",
                "..........",
                "....^.....",
                "##########"), "checkpoint", "hazard");
    }

    @Test
    void flagsSpawnOverHazard() {
        assertProblem(check(validObjects(),
                "..........",
                "..........",
                "..........",
                "..........",
                ".^........",
                "##########"), "player_spawn", "hazard");
    }

    @Test
    void flagsObjectOutsideMap() {
        List<LevelObject> objs = validObjects();
        objs.set(1, rect(LevelObject.EXIT, 200, 48));
        assertProblem(check(objs, ROOM), "exit", "outside");
    }

    @Test
    void flagsBossLevelWithoutArena() {
        assertProblem(LevelValidator.check(level(Map.of("boss", "true"), validObjects(), ROOM)), "boss_arena");
    }

    @Test
    void flagsArenaInNonBossLevel() {
        List<LevelObject> objs = validObjects();
        objs.add(new LevelObject(LevelObject.BOSS_ARENA, new Rect(0, 0, 160, 80), Map.of()));
        assertProblem(check(objs, ROOM), "boss_arena", "not a boss level");
    }

    @Test
    void bossLevelWithArenaIsValid() {
        List<LevelObject> objs = validObjects();
        objs.add(new LevelObject(LevelObject.BOSS_ARENA, new Rect(0, 0, 160, 80), Map.of()));
        assertEquals(List.of(), LevelValidator.check(level(Map.of("boss", "true"), objs, ROOM)));
    }

    @Test
    void reportsEveryProblem() {
        List<LevelObject> objs = validObjects();
        objs.remove(0);
        objs.add(point("chest", 40, 70, Map.of()));
        List<String> problems = check(objs, ROOM);
        assertProblem(problems, "player_spawn");
        assertProblem(problems, "chest");
    }
}
