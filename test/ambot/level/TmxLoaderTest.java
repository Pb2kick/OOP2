package ambot.level;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

class TmxLoaderTest {
    static final String PATH = "/levels/test/t.tmx";
    static final String TILESET = "<tileset firstgid=\"1\" name=\"placeholder\" tilewidth=\"16\" tileheight=\"16\""
            + " tilecount=\"6\" columns=\"6\"><image source=\"../../tiles/placeholder.png\" width=\"96\" height=\"16\"/>"
            + "</tileset>";
    static final String SOLID_CSV = "0,0,0,0,\n0,0,0,0,\n1,1,1,1";

    static String map(String inner) {
        return map("tilewidth=\"16\" tileheight=\"16\"", inner);
    }

    static String map(String tileAttrs, String inner) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<map version=\"1.10\" orientation=\"orthogonal\""
                + " renderorder=\"right-down\" width=\"4\" height=\"3\" " + tileAttrs + " infinite=\"0\">\n"
                + inner + "\n</map>\n";
    }

    static String layer(String name, String csv) {
        return layer(name, "encoding=\"csv\"", csv);
    }

    static String layer(String name, String encoding, String csv) {
        return "<layer id=\"1\" name=\"" + name + "\" width=\"4\" height=\"3\"><data " + encoding + ">\n" + csv
                + "\n</data></layer>\n";
    }

    static String entities(String objects) {
        return "<objectgroup id=\"9\" name=\"entities\">" + objects + "</objectgroup>";
    }

    static Level parse(String xml) {
        return TmxLoader.parse(new ByteArrayInputStream(xml.getBytes(UTF_8)), PATH);
    }

    static void assertFails(String xml, String... mentions) {
        LevelFormatException e = assertThrows(LevelFormatException.class, () -> parse(xml));
        assertTrue(e.getMessage().startsWith(PATH), "message should start with the file: " + e.getMessage());
        for (String m : mentions) {
            assertTrue(e.getMessage().contains(m), "message should mention '" + m + "': " + e.getMessage());
        }
    }

    @Test
    void parsesSizesLayersAndProperties() {
        Level lvl = parse(map("<properties><property name=\"title\" value=\"Hello\"/>"
                + "<property name=\"world\" value=\"w1\"/><property name=\"boss\" type=\"bool\" value=\"true\"/>"
                + "</properties>" + TILESET + layer("solid", SOLID_CSV)
                + layer("oneway", "0,0,0,0,\n0,3,0,0,\n0,0,0,0")));
        assertEquals(64, lvl.widthPx());
        assertEquals(48, lvl.heightPx());
        assertEquals("Hello", lvl.title());
        assertEquals("w1", lvl.world());
        assertTrue(lvl.isBossLevel());
        assertTrue(lvl.collision().isOneWay(1, 1));
        assertTrue(lvl.collision().isSolid(0, 2));
    }

    @Test
    void solidWinsOverOneWayAndHazard() {
        Level lvl = parse(map(TILESET + layer("solid", "0,0,0,0,\n0,0,1,0,\n1,1,1,1")
                + layer("oneway", "0,0,0,0,\n0,3,3,0,\n0,0,0,0")
                + layer("hazard", "4,0,0,0,\n0,4,4,0,\n0,0,0,0")));
        TileMap c = lvl.collision();
        assertEquals(TileMap.SOLID, c.tileAt(2, 1));
        assertEquals(TileMap.ONEWAY, c.tileAt(1, 1), "one-way wins over hazard");
        assertEquals(TileMap.HAZARD, c.tileAt(0, 0));
    }

    @Test
    void missingOptionalLayersAreEmpty() {
        Level lvl = parse(map(TILESET + layer("solid", SOLID_CSV)));
        assertTrue(lvl.layer("foreground").isEmpty());
        assertTrue(lvl.layer("solid").isPresent());
        assertEquals("t", lvl.title(), "title falls back to the file name");
        assertEquals("placeholder", lvl.world());
        assertFalse(lvl.isBossLevel());
    }

    @Test
    void resolvesTilesetImageRelativeToFile() {
        Level lvl = parse(map(TILESET + layer("solid", SOLID_CSV)));
        assertEquals(16, lvl.tileset().tile(2).getWidth());
        assertEquals(16, lvl.tileset().tile(2).getHeight());
        assertEquals(1, lvl.tileset().firstGid());
        assertEquals(6, lvl.tileset().tileCount());
    }

    @Test
    void readsObjects() {
        Level lvl = parse(map(TILESET + layer("solid", SOLID_CSV) + entities(
                "<object id=\"1\" type=\"player_spawn\" x=\"40\" y=\"48\"><point/></object>"
                        + "<object id=\"2\" type=\"exit\" x=\"32\" y=\"0\" width=\"16\" height=\"32\"/>"
                        + "<object id=\"3\" type=\"enemy\" x=\"8\" y=\"8\"><properties>"
                        + "<property name=\"type\" value=\"walker\"/></properties><point/></object>")));
        assertEquals(new Spot(40, 48), lvl.spawn());
        assertEquals(new Rect(32, 0, 16, 32), lvl.exit());
        assertEquals("walker", lvl.objects(LevelObject.ENEMY).get(0).property("type"));
        assertEquals(3, lvl.objects().size());
        assertEquals(List.of(), lvl.checkpoints());
        assertTrue(lvl.bossArena().isEmpty());
    }

    @Test
    void csvWithCrLfAndSpacesParses() {
        Level lvl = parse(map(TILESET + layer("solid", "0, 0, 0, 0,\r\n0, 0, 0, 0,\r\n1, 1, 1, 1\r\n")));
        assertTrue(lvl.collision().isSolid(3, 2));
        assertFalse(lvl.collision().isSolid(3, 1));
    }

    @Test
    void flipFlagsAreStripped() {
        Level lvl = parse(map(TILESET + layer("solid", "0,0,0,0,\n0,0,0,0,\n2147483650,1,1,1")));
        assertEquals(2, lvl.layer("solid").orElseThrow().gid(0, 2));
        assertTrue(lvl.collision().isSolid(0, 2));
    }

    @Test
    void classAttributeActsAsType() {
        Level lvl = parse(map(TILESET + layer("solid", SOLID_CSV)
                + entities("<object id=\"1\" class=\"checkpoint\" x=\"16\" y=\"0\" width=\"16\" height=\"32\"/>")));
        assertEquals(List.of(new Rect(16, 0, 16, 32)), lvl.checkpoints());
    }

    @Test
    void base64EncodingIsRejectedWithHint() {
        assertFails(map(TILESET + layer("solid", "encoding=\"base64\"", "AAAA")), "CSV");
    }

    @Test
    void externalTilesetIsRejectedWithHint() {
        assertFails(map("<tileset firstgid=\"1\" source=\"tiles.tsx\"/>" + layer("solid", SOLID_CSV)), "embed");
    }

    @Test
    void wrongTileSizeIsRejected() {
        assertFails(map("tilewidth=\"32\" tileheight=\"32\"", TILESET + layer("solid", SOLID_CSV)), "16");
    }

    @Test
    void csvCountMismatchIsRejected() {
        assertFails(map(TILESET + layer("solid", "0,0,0,0,\n0,0,0,0,\n1,1,1")), "solid");
    }

    @Test
    void missingSolidLayerIsRejected() {
        assertFails(map(TILESET + layer("oneway", SOLID_CSV)), "solid");
    }

    @Test
    void unknownLayerNameIsRejected() {
        assertFails(map(TILESET + layer("solid", SOLID_CSV) + layer("decor", SOLID_CSV)), "decor");
    }

    @Test
    void malformedXmlIsRejected() {
        assertFails("<map width=\"4\"");
    }

    @Test
    void missingImageIsRejected() {
        assertFails(map(TILESET.replace("placeholder.png", "nope.png") + layer("solid", SOLID_CSV)), "nope.png");
    }
}
