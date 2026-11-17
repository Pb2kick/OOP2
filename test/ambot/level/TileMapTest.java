package ambot.level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TileMapTest {
    static final String[] ROOM = {
            "........",
            "........",
            "...##...",
            "........",
            "########" };

    @Test
    void outOfBoundsRules() {
        TileMap m = TileMap.fromAscii(ROOM);
        assertTrue(m.isSolid(-1, 0), "left of the map is a wall");
        assertTrue(m.isSolid(8, 0), "right of the map is a wall");
        assertFalse(m.isSolid(0, -1), "above the map is open");
        assertFalse(m.isSolid(0, 5), "below the map is open (pits)");
        assertTrue(m.isSolid(3, 2));
        assertEquals(0, m.tileAt(-1, 0));
    }

    @Test
    void sizesInTilesAndPixels() {
        TileMap m = TileMap.fromAscii(ROOM);
        assertEquals(8, m.widthTiles());
        assertEquals(5, m.heightTiles());
        assertEquals(128, m.widthPx());
        assertEquals(80, m.heightPx());
    }

    @Test
    void rejectsRaggedRowsAndUnknownChars() {
        assertThrows(IllegalArgumentException.class, () -> TileMap.fromAscii("...", ".."));
        assertThrows(IllegalArgumentException.class, () -> TileMap.fromAscii("..x"));
    }

    @Test
    void asciiReadsOneWayAndHazard() {
        TileMap m = TileMap.fromAscii("-^#", "...");
        assertTrue(m.isOneWay(0, 0));
        assertTrue(m.isHazard(1, 0));
        assertFalse(m.isSolid(0, 0));
        assertFalse(m.isSolid(1, 0));
        assertFalse(m.isOneWay(2, 0));
        assertFalse(m.isOneWay(-1, 0), "off the map is never one-way");
        assertFalse(m.isHazard(0, 5), "below the map is never a hazard");
    }

    @Test
    void overlapsChecksKind() {
        TileMap m = TileMap.fromAscii("#-.", "...");
        Rect box = new Rect(10, 4, 12, 8); // over tiles (0,0) and (1,0)
        assertTrue(m.overlaps(box, TileMap.SOLID));
        assertTrue(m.overlaps(box, TileMap.ONEWAY));
        assertFalse(m.overlaps(box, TileMap.HAZARD));
    }

    @Test
    void hazardInsetForgivesGrazing() {
        TileMap m = TileMap.fromAscii(".^.", "..."); // hazard at x 16..32
        assertFalse(m.touchesHazard(new Rect(3, 2, 14, 12)), "right edge at 17 only grazes");
        assertTrue(m.touchesHazard(new Rect(5, 2, 14, 12)), "right edge at 19 touches");
    }

    @Test
    void asciiLevelFindsSingleSpawn() {
        AsciiLevel lvl = AsciiLevel.parse(
                "........",
                "........",
                "........",
                "..P.....",
                "########");
        assertEquals(32, lvl.spawnX());
        assertEquals(48, lvl.spawnY());
        assertFalse(lvl.map().isSolid(2, 3), "the spawn tile is empty");
        assertThrows(IllegalArgumentException.class, () -> AsciiLevel.parse(ROOM));
        assertThrows(IllegalArgumentException.class, () -> AsciiLevel.parse("P..P", "####"));
    }
}
