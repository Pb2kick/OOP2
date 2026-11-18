package ambot.tools;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ambot.level.Level;
import ambot.level.LevelObject;
import ambot.level.LevelValidator;
import ambot.level.Rect;
import ambot.level.Spot;
import ambot.level.TestLevels;
import ambot.level.TileLayer;
import ambot.level.TileMap;
import ambot.level.TmxLoader;

class AsciiTmxTest {
    static final Map<String, String> PROPS = Map.of("title", "T", "world", "placeholder");

    static Level load(String tmx) {
        return TmxLoader.parse(new ByteArrayInputStream(tmx.getBytes(UTF_8)), "/levels/test/a.tmx");
    }

    static TileLayer layer(Level l, String name) {
        return l.layer(name).orElseThrow();
    }

    @Test
    void hashIsSolidWithGrassOnTop() {
        Level l = load(AsciiTmx.toTmx(PROPS, "....", ".##.", "####"));
        assertEquals(PlaceholderTiles.GROUND_TOP, layer(l, "solid").gid(1, 1));
        assertEquals(PlaceholderTiles.GROUND, layer(l, "solid").gid(1, 2));
        assertEquals(PlaceholderTiles.GROUND_TOP, layer(l, "solid").gid(0, 2));
        assertTrue(l.collision().isSolid(1, 1));
        assertTrue(l.collision().isSolid(1, 2));
    }

    @Test
    void dashIsOneWayCaretIsHazard() {
        Level l = load(AsciiTmx.toTmx(PROPS, "-^..", "####"));
        assertEquals(PlaceholderTiles.ONEWAY, layer(l, "oneway").gid(0, 0));
        assertEquals(PlaceholderTiles.SPIKES, layer(l, "hazard").gid(1, 0));
        assertTrue(l.collision().isOneWay(0, 0));
        assertTrue(l.collision().isHazard(1, 0));
    }

    @Test
    void bAndFGoToDecorationLayers() {
        Level l = load(AsciiTmx.toTmx(PROPS, "bf..", "####"));
        assertEquals(PlaceholderTiles.BACK_WALL, layer(l, "background").gid(0, 0));
        assertEquals(PlaceholderTiles.GRASS, layer(l, "foreground").gid(1, 0));
        assertEquals(TileMap.EMPTY, l.collision().tileAt(0, 0));
        assertEquals(TileMap.EMPTY, l.collision().tileAt(1, 0));
    }

    @Test
    void pIsSpawnStandingOnCellBottom() {
        Level l = load(AsciiTmx.toTmx(PROPS, "......", "......", "......", "..P...", "######"));
        assertEquals(new Spot(40, 64), l.spawn());
    }

    @Test
    void cAndEAreTwoTileTallRects() {
        Level l = load(AsciiTmx.toTmx(PROPS, "........", "........", "........", ".....CE.", "########"));
        assertEquals(List.of(new Rect(80, 32, 16, 32)), l.checkpoints());
        assertEquals(new Rect(96, 32, 16, 32), l.exit());
    }

    @Test
    void enemyAndPickupChars() {
        Level l = load(AsciiTmx.toTmx(PROPS, ".wWsh..", "#######"));
        List<LevelObject> enemies = l.objects(LevelObject.ENEMY);
        assertEquals(3, enemies.size());
        assertEquals(new Rect(24, 16, 0, 0), enemies.get(0).bounds());
        assertEquals("walker", enemies.get(0).property("type"));
        assertEquals(null, enemies.get(0).property("drop"));
        assertEquals("walker", enemies.get(1).property("type"));
        assertEquals("heart", enemies.get(1).property("drop"));
        assertEquals("shooter", enemies.get(2).property("type"));
        assertEquals(null, enemies.get(2).property("drop"));
        List<LevelObject> pickups = l.objects(LevelObject.PICKUP);
        assertEquals(1, pickups.size());
        assertEquals("heart", pickups.get(0).property("type"));
        assertEquals(new Rect(72, 16, 0, 0), pickups.get(0).bounds());
    }

    @Test
    void headerBecomesMapProperties() {
        Level l = load(AsciiTmx.convert("title: Hello World\r\nworld: w1\r\n---\r\n....\r\n####\r\n"));
        assertEquals("Hello World", l.title());
        assertEquals("w1", l.world());
        assertEquals(4, l.widthTiles());
        assertEquals(2, l.heightTiles());
    }

    @Test
    void unknownCharFailsWithPosition() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> AsciiTmx.toTmx(PROPS, "....", "...x"));
        assertTrue(e.getMessage().contains("3,1"), e.getMessage());
    }

    @Test
    void raggedRowsFail() {
        assertThrows(IllegalArgumentException.class, () -> AsciiTmx.toTmx(PROPS, "....", "..."));
    }

    @Test
    void outputIsStableAcrossRuns() {
        Map<String, String> a = new HashMap<>();
        a.put("world", "placeholder");
        a.put("title", "T");
        a.put("boss", "false");
        String first = AsciiTmx.toTmx(a, ".P.E.", "#####");
        assertEquals(first, AsciiTmx.toTmx(new HashMap<>(a), ".P.E.", "#####"));
        assertTrue(first.indexOf("\"boss\"") < first.indexOf("\"title\""), "properties sorted by key");
        assertTrue(!first.contains("\r"), "LF line endings only");
    }

    @Test
    void testLevelsHelperRoundTrips() {
        Level l = TestLevels.ascii(
                "......",
                ".P..E.",
                "######");
        assertEquals(List.of(), LevelValidator.check(l));
        assertEquals("Test", l.title());
    }
}
